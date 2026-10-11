package com.example.desde_cero.service

import android.util.Base64
import com.example.desde_cero.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

data class FinnhubQuote(
    val currentPrice: Double,
    val change: Double,
    val percentChange: Double,
    val high: Double,
    val low: Double,
    val timestamp: Long
)

object FinnhubApiService {

    private const val BASE_URL = "https://finnhub.io/api/v1"

    // Ofuscación de respaldo seguro en caso de que BuildConfig no esté poblado
    private val OBFUSCATED_BACKUP = "ZGI1NGgwaHIwMXF2MW44c2tudTBkYjU0aDBocjAxcXYxbjhza251Zw=="

    /**
     * Retorna la clave de API de forma segura sin exponerla en texto plano en la UI.
     */
    private fun getSecureApiKey(): String {
        val configKey = BuildConfig.FINNHUB_API_KEY
        if (configKey.isNotBlank()) {
            return configKey
        }
        return try {
            String(Base64.decode(OBFUSCATED_BACKUP, Base64.DEFAULT), Charsets.UTF_8)
        } catch (_: Exception) {
            ""
        }
    }

    /**
     * Retorna la clave enmascarada para mostrar en la interfaz de forma segura (ej. "db54***knug").
     */
    fun getMaskedApiKey(): String {
        val key = getSecureApiKey()
        if (key.length <= 8) return "••••••••"
        return "${key.take(4)}••••••••${key.takeLast(4)}"
    }

    /**
     * Consulta cotización en tiempo real desde Finnhub.
     * Ejemplo de símbolos: "BINANCE:EURUSDT", "BINANCE:GBPUSDT", "BINANCE:BTCUSDT".
     */
    suspend fun fetchQuote(symbol: String): Result<FinnhubQuote> = withContext(Dispatchers.IO) {
        try {
            val apiKey = getSecureApiKey()
            val urlString = "$BASE_URL/quote?symbol=$symbol&token=$apiKey"
            val url = URL(urlString)
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 6000
                readTimeout = 6000
                setRequestProperty("Accept", "application/json")
            }

            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                val reader = BufferedReader(InputStreamReader(connection.inputStream))
                val response = reader.readText()
                reader.close()

                val json = JSONObject(response)
                val c = json.optDouble("c", 0.0)
                if (c > 0.0) {
                    val d = json.optDouble("d", 0.0)
                    val dp = json.optDouble("dp", 0.0)
                    val h = json.optDouble("h", 0.0)
                    val l = json.optDouble("l", 0.0)
                    val t = json.optLong("t", System.currentTimeMillis())
                    Result.success(FinnhubQuote(c, d, dp, h, l, t))
                } else {
                    Result.failure(Exception("Sin datos disponibles para $symbol"))
                }
            } else {
                Result.failure(Exception("Error de conexión: HTTP ${connection.responseCode}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
