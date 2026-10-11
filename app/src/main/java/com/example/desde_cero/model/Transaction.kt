package com.example.desde_cero.model

enum class TransactionType {
    COMPRA,
    VENTA
}

enum class TransactionStatus {
    PENDIENTE_AUDITORIA,
    EXITOSA,
    RECHAZADA
}

data class CurrencyTransaction(
    val id: String,
    val userId: String,
    val userFullName: String,
    val type: TransactionType,
    val currencyCode: String,
    val amountForeign: Double,            // Monto en moneda extranjera (ej. 150 USD)
    val rateAtOrder: Double,              // Tasa acordada al momento de la solicitud
    val totalBaseAmount: Double,          // Total en moneda base (COP)
    val timestamp: Long = System.currentTimeMillis(),
    val rateAtTransition: Double = rateAtOrder, // Tasa registrada al momento de la transición/auditoría
    val deviationPercentage: Double = 0.0,       // Diferencia porcentual (|rateTransition - rateOrder| / rateOrder * 100)
    val status: TransactionStatus = TransactionStatus.PENDIENTE_AUDITORIA,
    val auditObservation: String = "",
    val auditedBy: String? = null,
    val auditedAt: Long? = null
) {
    // Regla de negocio: la variación no debe superar el 2.5%
    val exceedsToleranceLimit: Boolean
        get() = deviationPercentage > 2.5
}
