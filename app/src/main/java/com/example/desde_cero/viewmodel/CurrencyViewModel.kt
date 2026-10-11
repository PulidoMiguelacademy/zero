package com.example.desde_cero.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.desde_cero.model.Currency
import com.example.desde_cero.model.CurrencyRepository
import com.example.desde_cero.model.CurrencyTransaction
import com.example.desde_cero.model.TransactionRepository
import com.example.desde_cero.model.TransactionStatus
import com.example.desde_cero.model.TransactionType
import com.example.desde_cero.model.User
import com.example.desde_cero.model.Wallet
import com.example.desde_cero.service.CurrencyMarketService
import com.example.desde_cero.service.MarketTick
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.math.abs

data class OrderFormState(
    val selectedCurrencyCode: String = "USD",
    val type: TransactionType = TransactionType.COMPRA,
    val amountInput: String = "100",
    val errorMessage: String? = null,
    val successMessage: String? = null
)

class CurrencyViewModel(application: Application) : AndroidViewModel(application) {

    private val currencyRepository = CurrencyRepository()
    private val transactionRepository = TransactionRepository(application.applicationContext)

    // Mercado de divisas
    val currencies: StateFlow<List<Currency>> = currencyRepository.currencies

    // Servicio en segundo plano
    val marketServiceState: StateFlow<MarketTick> = CurrencyMarketService.marketState
        .stateIn(viewModelScope, SharingStarted.Eagerly, MarketTick())

    // Formulario de compra / venta del usuario
    private val _orderForm = MutableStateFlow(OrderFormState())
    val orderForm: StateFlow<OrderFormState> = _orderForm.asStateFlow()

    // Billetera del usuario activo
    private val _userWallet = MutableStateFlow<Wallet?>(null)
    val userWallet: StateFlow<Wallet?> = _userWallet.asStateFlow()

    // Transacciones del usuario
    private val _userTransactions = MutableStateFlow<List<CurrencyTransaction>>(emptyList())
    val userTransactions: StateFlow<List<CurrencyTransaction>> = _userTransactions.asStateFlow()

    // Todas las transacciones (para el Administrador)
    private val _allTransactions = MutableStateFlow<List<CurrencyTransaction>>(emptyList())
    val allTransactions: StateFlow<List<CurrencyTransaction>> = _allTransactions.asStateFlow()

    // Mensaje de estado de auditoría
    private val _auditMessage = MutableStateFlow<String?>(null)
    val auditMessage: StateFlow<String?> = _auditMessage.asStateFlow()

    // Estado de sincronización con la API protegida de Finnhub
    val apiSyncState = currencyRepository.apiSyncState

    val maskedApiKey: String
        get() = com.example.desde_cero.service.FinnhubApiService.getMaskedApiKey()

    init {
        // Iniciar servicio en segundo plano
        CurrencyMarketService.startService(application.applicationContext)
        refreshData()
        fetchLiveApiRates()
    }

    fun fetchLiveApiRates() {
        viewModelScope.launch {
            currencyRepository.fetchLiveFinnhubRates()
        }
    }

    fun loadUserData(user: User) {
        _userWallet.value = transactionRepository.getWallet(user.id)
        _userTransactions.value = transactionRepository.getTransactionsForUser(user.id)
        refreshData()
    }

    fun refreshData() {
        _allTransactions.value = transactionRepository.getAllTransactions()
        _userWallet.value?.let { currentWallet ->
            _userWallet.value = transactionRepository.getWallet(currentWallet.userId)
            _userTransactions.value = transactionRepository.getTransactionsForUser(currentWallet.userId)
        }
    }

    // ==========================================
    // OPERACIONES DEL USUARIO (COMPRA / VENTA)
    // ==========================================

    fun onCurrencySelected(code: String) {
        _orderForm.value = _orderForm.value.copy(selectedCurrencyCode = code, errorMessage = null, successMessage = null)
    }

    fun onTransactionTypeChange(type: TransactionType) {
        _orderForm.value = _orderForm.value.copy(type = type, errorMessage = null, successMessage = null)
    }

    fun onAmountInputChange(amount: String) {
        _orderForm.value = _orderForm.value.copy(amountInput = amount, errorMessage = null, successMessage = null)
    }

    fun submitOrder(currentUser: User): Boolean {
        val form = _orderForm.value
        val amount = form.amountInput.toDoubleOrNull()
        if (amount == null || amount <= 0) {
            _orderForm.value = form.copy(errorMessage = "Ingresa un monto válido mayor a 0.")
            return false
        }

        val rate = currencyRepository.getCurrentRate(form.selectedCurrencyCode)
        val result = transactionRepository.createTransaction(
            userId = currentUser.id,
            userFullName = currentUser.fullName,
            type = form.type,
            currencyCode = form.selectedCurrencyCode,
            amountForeign = amount,
            rateAtOrder = rate
        )

        return if (result.isSuccess) {
            _orderForm.value = form.copy(
                amountInput = "",
                errorMessage = null,
                successMessage = "¡Orden de ${form.type.name.lowercase()} creada! Enviada a auditoría del Administrador."
            )
            loadUserData(currentUser)
            true
        } else {
            _orderForm.value = form.copy(
                errorMessage = result.exceptionOrNull()?.message ?: "Error al procesar la orden."
            )
            false
        }
    }

    // ==========================================
    // OPERACIONES DEL ADMINISTRADOR (AUDITORÍA)
    // ==========================================

    /**
     * Calcula la variación porcentual entre la tasa al momento de la orden
     * y la tasa al momento de la transición (tasa actual de mercado).
     */
    fun calculateDeviation(tx: CurrencyTransaction, currentRate: Double): Double {
        if (tx.rateAtOrder <= 0) return 0.0
        val dev = abs(currentRate - tx.rateAtOrder) / tx.rateAtOrder * 100.0
        return Math.round(dev * 100.0) / 100.0
    }

    /**
     * El Administrador audita una transacción aplicando la regla del 2.5%
     */
    fun auditTransaction(
        tx: CurrencyTransaction,
        adminUser: User,
        approve: Boolean,
        simulatedRateAtTransition: Double? = null,
        observation: String = ""
    ): Boolean {
        val rateAtTransition = simulatedRateAtTransition ?: currencyRepository.getCurrentRate(tx.currencyCode)
        val result = transactionRepository.auditTransaction(
            transactionId = tx.id,
            adminName = adminUser.fullName,
            rateAtTransition = rateAtTransition,
            approve = approve,
            observation = observation
        )

        return if (result.isSuccess) {
            val audited = result.getOrNull()
            _auditMessage.value = if (approve) {
                "Transacción ${tx.id} aprobada con éxito. Variación: ${audited?.deviationPercentage}% (Dentro del límite <= 2.5%)."
            } else {
                "Transacción ${tx.id} rechazada correctamente."
            }
            refreshData()
            true
        } else {
            _auditMessage.value = result.exceptionOrNull()?.message ?: "Fallo al auditar la transacción."
            false
        }
    }

    fun clearAuditMessage() {
        _auditMessage.value = null
    }

    // ==========================================
    // HERRAMIENTAS DE PRUEBA DE VOLATILIDAD
    // ==========================================

    fun simulateMarketFluctuation(maxPercent: Double = 3.5) {
        currencyRepository.simulateMarketFluctuation(maxPercent)
    }

    fun applyControlledVariation(currencyCode: String, percentDelta: Double) {
        val current = currencyRepository.getCurrentRate(currencyCode)
        currencyRepository.setRateWithVariation(currencyCode, current, percentDelta)
    }
}
