package com.example.desde_cero.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.random.Random

data class MarketTick(
    val timestamp: Long = System.currentTimeMillis(),
    val isRunning: Boolean = false,
    val lastVariationPercent: Double = 0.0,
    val statusMessage: String = "Mercado de divisas en espera"
)

class CurrencyMarketService : Service() {

    private val job = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.Default + job)
    private val binder = LocalBinder()

    inner class LocalBinder : Binder() {
        fun getService(): CurrencyMarketService = this@CurrencyMarketService
    }

    companion object {
        const val ACTION_START_FEED = "com.example.desde_cero.ACTION_START_FEED"
        const val ACTION_STOP_FEED = "com.example.desde_cero.ACTION_STOP_FEED"
        private const val CHANNEL_ID = "currency_market_channel"
        private const val NOTIFICATION_ID = 2002

        private val _marketState = MutableStateFlow(MarketTick())
        val marketState: StateFlow<MarketTick> = _marketState.asStateFlow()

        fun startService(context: Context) {
            val intent = Intent(context, CurrencyMarketService::class.java).apply {
                action = ACTION_START_FEED
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopService(context: Context) {
            val intent = Intent(context, CurrencyMarketService::class.java).apply {
                action = ACTION_STOP_FEED
            }
            context.stopService(intent)
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_FEED -> {
                startForeground(NOTIFICATION_ID, buildNotification("Monitoreando tasas de divisas en tiempo real..."))
                startMarketMonitoring()
            }
            ACTION_STOP_FEED -> {
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    private fun startMarketMonitoring() {
        serviceScope.launch {
            _marketState.value = MarketTick(isRunning = true, statusMessage = "Servicio activo monitoreando cotizaciones")
            while (true) {
                delay(8000) // Ticks cada 8 segundos
                val delta = Random.nextDouble(-1.5, 1.8)
                val roundedDelta = Math.round(delta * 100.0) / 100.0
                _marketState.value = MarketTick(
                    timestamp = System.currentTimeMillis(),
                    isRunning = true,
                    lastVariationPercent = roundedDelta,
                    statusMessage = "Última variación de mercado: ${if (roundedDelta >= 0) "+" else ""}$roundedDelta%"
                )
                updateNotification("Servicio Activo • Variación mercado: $roundedDelta%")
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Monitor de Mercado de Divisas",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Notificaciones del servicio de tasas de divisas en segundo plano"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(text: String): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Mercado de Divisas Activo")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setOngoing(true)
            .build()
    }

    private fun updateNotification(text: String) {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        manager?.notify(NOTIFICATION_ID, buildNotification(text))
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        _marketState.value = MarketTick(isRunning = false, statusMessage = "Servicio detenido")
    }
}
