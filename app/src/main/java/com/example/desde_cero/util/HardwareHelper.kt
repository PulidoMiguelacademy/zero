package com.example.desde_cero.util

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

object HardwareHelper {

    /**
     * Emite un patrón de vibración táctil confirmando que la compra de divisas fue exitosa.
     * Patrón de doble pulso de éxito háptico (Tick-Tick vigoroso).
     */
    fun triggerSuccessVibration(context: Context) {
        try {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }

            vibrator?.let { v ->
                if (v.hasVibrator()) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        // Patrón: esperar 0ms, vibrar 120ms, pausa 80ms, vibrar 220ms
                        val timings = longArrayOf(0, 120, 80, 220)
                        val amplitudes = intArrayOf(0, 200, 0, 255)
                        val effect = VibrationEffect.createWaveform(timings, amplitudes, -1)
                        v.vibrate(effect)
                    } else {
                        @Suppress("DEPRECATION")
                        v.vibrate(300)
                    }
                }
            }
        } catch (_: Exception) {
            // Manejo silencioso en caso de dispositivos sin motor de vibración (ej. emulador sin haptics)
        }
    }

    /**
     * Emite una vibración corta de alerta/advertencia.
     */
    fun triggerAlertVibration(context: Context) {
        try {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }

            vibrator?.let { v ->
                if (v.hasVibrator()) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        val effect = VibrationEffect.createOneShot(150, VibrationEffect.DEFAULT_AMPLITUDE)
                        v.vibrate(effect)
                    } else {
                        @Suppress("DEPRECATION")
                        v.vibrate(150)
                    }
                }
            }
        } catch (_: Exception) {}
    }
}
