package com.example.desde_cero.model

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID
import kotlin.math.abs

class TransactionRepository(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("currency_tx_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_TRANSACTIONS = "transactions_history"
        private const val KEY_WALLETS = "user_wallets"
    }

    // ==========================================
    // GESTIÓN DE BILLETERAS PERSISTENTES
    // ==========================================

    fun getWallet(userId: String): Wallet {
        val walletsMap = loadAllWallets()
        return walletsMap[userId] ?: Wallet(userId = userId).also {
            saveWallet(it)
        }
    }

    fun saveWallet(wallet: Wallet) {
        val current = loadAllWallets().toMutableMap()
        current[wallet.userId] = wallet
        saveAllWallets(current)
    }

    private fun loadAllWallets(): Map<String, Wallet> {
        val jsonStr = prefs.getString(KEY_WALLETS, null) ?: return emptyMap()
        val result = mutableMapOf<String, Wallet>()
        try {
            val jsonObject = JSONObject(jsonStr)
            val keys = jsonObject.keys()
            while (keys.hasNext()) {
                val userId = keys.next()
                val walletObj = jsonObject.getJSONObject(userId)
                val baseBalance = walletObj.optDouble("baseBalance", 15000000.0)
                val balancesObj = walletObj.optJSONObject("balances")
                val balances = mutableMapOf<String, Double>()
                if (balancesObj != null) {
                    val bKeys = balancesObj.keys()
                    while (bKeys.hasNext()) {
                        val cCode = bKeys.next()
                        balances[cCode] = balancesObj.getDouble(cCode)
                    }
                }
                result[userId] = Wallet(userId, baseBalance, balances)
            }
        } catch (_: Exception) {}
        return result
    }

    private fun saveAllWallets(wallets: Map<String, Wallet>) {
        val root = JSONObject()
        for ((uId, w) in wallets) {
            val wObj = JSONObject()
            wObj.put("baseBalance", w.baseBalance)
            val bObj = JSONObject()
            for ((c, amount) in w.balances) {
                bObj.put(c, amount)
            }
            wObj.put("balances", bObj)
            root.put(uId, wObj)
        }
        prefs.edit().putString(KEY_WALLETS, root.toString()).apply()
    }

    // ==========================================
    // GESTIÓN DE TRANSACCIONES Y AUDITORÍA
    // ==========================================

    fun getAllTransactions(): List<CurrencyTransaction> {
        val jsonStr = prefs.getString(KEY_TRANSACTIONS, null) ?: return emptyList()
        val list = mutableListOf<CurrencyTransaction>()
        try {
            val array = JSONArray(jsonStr)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val type = try { TransactionType.valueOf(obj.getString("type")) } catch (_: Exception) { TransactionType.COMPRA }
                val status = try { TransactionStatus.valueOf(obj.getString("status")) } catch (_: Exception) { TransactionStatus.PENDIENTE_AUDITORIA }
                list.add(
                    CurrencyTransaction(
                        id = obj.getString("id"),
                        userId = obj.getString("userId"),
                        userFullName = obj.getString("userFullName"),
                        type = type,
                        currencyCode = obj.getString("currencyCode"),
                        amountForeign = obj.getDouble("amountForeign"),
                        rateAtOrder = obj.getDouble("rateAtOrder"),
                        totalBaseAmount = obj.getDouble("totalBaseAmount"),
                        timestamp = obj.getLong("timestamp"),
                        rateAtTransition = obj.optDouble("rateAtTransition", obj.getDouble("rateAtOrder")),
                        deviationPercentage = obj.optDouble("deviationPercentage", 0.0),
                        status = status,
                        auditObservation = obj.optString("auditObservation", ""),
                        auditedBy = if (obj.isNull("auditedBy")) null else obj.optString("auditedBy"),
                        auditedAt = if (obj.isNull("auditedAt")) null else obj.optLong("auditedAt")
                    )
                )
            }
        } catch (_: Exception) {}
        return list.sortedByDescending { it.timestamp }
    }

    fun getTransactionsForUser(userId: String): List<CurrencyTransaction> {
        return getAllTransactions().filter { it.userId == userId }
    }

    fun getPendingTransactions(): List<CurrencyTransaction> {
        return getAllTransactions().filter { it.status == TransactionStatus.PENDIENTE_AUDITORIA }
    }

    fun createTransaction(
        userId: String,
        userFullName: String,
        type: TransactionType,
        currencyCode: String,
        amountForeign: Double,
        rateAtOrder: Double
    ): Result<CurrencyTransaction> {
        val wallet = getWallet(userId)
        val totalBase = amountForeign * rateAtOrder

        // Validaciones previas de fondos
        if (type == TransactionType.COMPRA) {
            if (wallet.baseBalance < totalBase) {
                return Result.failure(IllegalArgumentException("Saldo insuficiente en COP para comprar ${amountForeign} $currencyCode."))
            }
        } else {
            val foreignBalance = wallet.getBalance(currencyCode)
            if (foreignBalance < amountForeign) {
                return Result.failure(IllegalArgumentException("Saldo insuficiente en $currencyCode ($foreignBalance disponible) para vender."))
            }
        }

        val tx = CurrencyTransaction(
            id = "TX_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(4)}",
            userId = userId,
            userFullName = userFullName,
            type = type,
            currencyCode = currencyCode,
            amountForeign = amountForeign,
            rateAtOrder = rateAtOrder,
            totalBaseAmount = totalBase,
            timestamp = System.currentTimeMillis(),
            rateAtTransition = rateAtOrder,
            deviationPercentage = 0.0,
            status = TransactionStatus.PENDIENTE_AUDITORIA
        )

        val all = getAllTransactions().toMutableList()
        all.add(0, tx)
        saveTransactions(all)

        return Result.success(tx)
    }

    /**
     * El Administrador audita la transacción comprobando que no supere el 2.5% de desviación.
     * @param transactionId ID de la transacción
     * @param adminName Nombre del auditor
     * @param rateAtTransition Tasa de la divisa al momento exacto de la transición/auditoría
     * @param approve true para aprobar, false para rechazar
     * @param observation Nota u observación de auditoría
     */
    fun auditTransaction(
        transactionId: String,
        adminName: String,
        rateAtTransition: Double,
        approve: Boolean,
        observation: String
    ): Result<CurrencyTransaction> {
        val all = getAllTransactions().toMutableList()
        val index = all.indexOfFirst { it.id == transactionId }
        if (index == -1) {
            return Result.failure(IllegalArgumentException("Transacción no encontrada."))
        }

        val currentTx = all[index]

        // Cálculo de variación porcentual: |rateTransition - rateOrder| / rateOrder * 100
        val deviation = if (currentTx.rateAtOrder > 0) {
            abs(rateAtTransition - currentTx.rateAtOrder) / currentTx.rateAtOrder * 100.0
        } else 0.0
        val formattedDeviation = Math.round(deviation * 100.0) / 100.0

        val newStatus: TransactionStatus
        val finalObservation: String

        if (approve) {
            // Regla de auditoría: No debe superar 2.5% el valor de la divisa al momento de la transición
            if (formattedDeviation > 2.5) {
                return Result.failure(
                    IllegalArgumentException(
                        "REGLA DE AUDITORÍA: La variación (${formattedDeviation}%) supera el límite permitido de 2.5%. La transacción no puede ser aprobada como exitosa."
                    )
                )
            }
            newStatus = TransactionStatus.EXITOSA
            finalObservation = if (observation.isBlank()) "Aprobada por auditoría. Variación dentro del margen permitido (${formattedDeviation}% <= 2.5%)." else observation

            // Ejecutar movimiento real de saldo en la billetera del usuario
            applyWalletMovement(currentTx)
        } else {
            newStatus = TransactionStatus.RECHAZADA
            finalObservation = if (observation.isBlank()) "Rechazada en auditoría. Variación detectada: ${formattedDeviation}%." else observation
        }

        val auditedTx = currentTx.copy(
            rateAtTransition = rateAtTransition,
            deviationPercentage = formattedDeviation,
            status = newStatus,
            auditObservation = finalObservation,
            auditedBy = adminName,
            auditedAt = System.currentTimeMillis()
        )

        all[index] = auditedTx
        saveTransactions(all)

        return Result.success(auditedTx)
    }

    private fun applyWalletMovement(tx: CurrencyTransaction) {
        val wallet = getWallet(tx.userId)
        val currentForeignBalances = wallet.balances.toMutableMap()
        val currentForeign = currentForeignBalances[tx.currencyCode] ?: 0.0

        val updatedWallet = if (tx.type == TransactionType.COMPRA) {
            // Usuario compra divisa: paga COP, recibe divisa
            currentForeignBalances[tx.currencyCode] = currentForeign + tx.amountForeign
            wallet.copy(
                baseBalance = (wallet.baseBalance - tx.totalBaseAmount).coerceAtLeast(0.0),
                balances = currentForeignBalances
            )
        } else {
            // Usuario vende divisa: entrega divisa, recibe COP
            currentForeignBalances[tx.currencyCode] = (currentForeign - tx.amountForeign).coerceAtLeast(0.0)
            wallet.copy(
                baseBalance = wallet.baseBalance + tx.totalBaseAmount,
                balances = currentForeignBalances
            )
        }

        saveWallet(updatedWallet)
    }

    private fun saveTransactions(transactions: List<CurrencyTransaction>) {
        val array = JSONArray()
        for (tx in transactions) {
            val obj = JSONObject().apply {
                put("id", tx.id)
                put("userId", tx.userId)
                put("userFullName", tx.userFullName)
                put("type", tx.type.name)
                put("currencyCode", tx.currencyCode)
                put("amountForeign", tx.amountForeign)
                put("rateAtOrder", tx.rateAtOrder)
                put("totalBaseAmount", tx.totalBaseAmount)
                put("timestamp", tx.timestamp)
                put("rateAtTransition", tx.rateAtTransition)
                put("deviationPercentage", tx.deviationPercentage)
                put("status", tx.status.name)
                put("auditObservation", tx.auditObservation)
                if (tx.auditedBy != null) put("auditedBy", tx.auditedBy)
                if (tx.auditedAt != null) put("auditedAt", tx.auditedAt)
            }
            array.put(obj)
        }
        prefs.edit().putString(KEY_TRANSACTIONS, array.toString()).apply()
    }
}
