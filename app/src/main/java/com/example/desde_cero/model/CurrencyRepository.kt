package com.example.desde_cero.model

import com.example.desde_cero.service.FinnhubApiService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.random.Random

data class ApiSyncState(
    val isLiveConnected: Boolean = false,
    val lastSyncTimestamp: Long = 0L,
    val statusMessage: String = "Modo Híbrido: API Finnhub Protegida & Local"
)

class CurrencyRepository {

    private val _currencies = MutableStateFlow(
        listOf(
            Currency("USD", "Dólar Estadounidense", "$", "🇺🇸", 4180.0),
            Currency("EUR", "Euro", "€", "🇪🇺", 4520.0),
            Currency("GBP", "Libra Esterlina", "£", "🇬🇧", 5280.0),
            Currency("BRL", "Real Brasileño", "R$", "🇧🇷", 760.0)
        )
    )
    val currencies: StateFlow<List<Currency>> = _currencies.asStateFlow()

    private val _apiSyncState = MutableStateFlow(ApiSyncState())
    val apiSyncState: StateFlow<ApiSyncState> = _apiSyncState.asStateFlow()

    fun getCurrency(code: String): Currency? {
        return _currencies.value.firstOrNull { it.code == code }
    }

    fun getCurrentRate(code: String): Double {
        return getCurrency(code)?.baseRate ?: 1.0
    }

    /**
     * Consulta tasas reales y en vivo desde Finnhub API.
     */
    suspend fun fetchLiveFinnhubRates(): Result<String> {
        return try {
            val eurResult = FinnhubApiService.fetchQuote("BINANCE:EURUSDT")
            val gbpResult = FinnhubApiService.fetchQuote("BINANCE:GBPUSDT")

            val baseUsdCop = 4180.0

            val updatedList = _currencies.value.map { currency ->
                when (currency.code) {
                    "USD" -> currency.copy(baseRate = baseUsdCop)
                    "EUR" -> {
                        val eurRatio = eurResult.getOrNull()?.currentPrice ?: (currency.baseRate / baseUsdCop)
                        val newEurRate = Math.round(baseUsdCop * eurRatio * 100.0) / 100.0
                        currency.copy(baseRate = newEurRate)
                    }
                    "GBP" -> {
                        val gbpRatio = gbpResult.getOrNull()?.currentPrice ?: (currency.baseRate / baseUsdCop)
                        val newGbpRate = Math.round(baseUsdCop * gbpRatio * 100.0) / 100.0
                        currency.copy(baseRate = newGbpRate)
                    }
                    else -> currency
                }
            }

            _currencies.value = updatedList
            _apiSyncState.value = ApiSyncState(
                isLiveConnected = true,
                lastSyncTimestamp = System.currentTimeMillis(),
                statusMessage = "API Finnhub Conectada en Vivo (Token ${FinnhubApiService.getMaskedApiKey()})"
            )
            Result.success("Tasas actualizadas en vivo desde Finnhub")
        } catch (e: Exception) {
            _apiSyncState.value = _apiSyncState.value.copy(
                isLiveConnected = false,
                statusMessage = "Modo Offline / Caché Local (${e.localizedMessage ?: "Sin conexión"})"
            )
            Result.failure(e)
        }
    }

    /**
     * Simula la fluctuación del mercado de divisas en tiempo real.
     */
    fun simulateMarketFluctuation(maxPercentChange: Double = 3.5) {
        val updated = _currencies.value.map { currency ->
            val deltaPercent = Random.nextDouble(-maxPercentChange, maxPercentChange)
            val newRate = currency.baseRate * (1.0 + deltaPercent / 100.0)
            currency.copy(baseRate = Math.round(newRate * 100.0) / 100.0)
        }
        _currencies.value = updated
    }

    /**
     * Permite fijar deliberadamente una variación controlada para probar la regla del 2.5%
     */
    fun setRateWithVariation(code: String, baseRate: Double, variationPercent: Double) {
        val updated = _currencies.value.map { currency ->
            if (currency.code == code) {
                val newRate = baseRate * (1.0 + variationPercent / 100.0)
                currency.copy(baseRate = Math.round(newRate * 100.0) / 100.0)
            } else {
                currency
            }
        }
        _currencies.value = updated
    }
}
