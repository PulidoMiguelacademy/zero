package com.example.desde_cero.model

data class Currency(
    val code: String,        // e.g. "USD", "EUR", "GBP", "BRL"
    val name: String,        // e.g. "Dólar Estadounidense"
    val symbol: String,      // e.g. "$", "€", "£", "R$"
    val flagEmoji: String,   // e.g. "🇺🇸", "🇪🇺", "🇬🇧", "🇧🇷"
    val baseRate: Double     // Tasa en moneda base (COP)
)
