package com.example.desde_cero.model

data class Wallet(
    val userId: String,
    val baseBalance: Double = 15000000.0, // Saldo en COP (15 millones iniciales para operar)
    val balances: Map<String, Double> = mapOf(
        "USD" to 500.0,
        "EUR" to 200.0,
        "GBP" to 100.0,
        "BRL" to 500.0
    )
) {
    fun getBalance(currencyCode: String): Double {
        return balances[currencyCode] ?: 0.0
    }
}
