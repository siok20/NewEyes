package com.neweyes.vibration

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

class VibrationManager(private val context: Context) {

    private var intensity = VibrationIntensity.fromConfig()
    private var type = VibrationType.fromConfig()

    private val vibrator: Vibrator by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            manager.defaultVibrator
        } else {
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
    }

    fun vibrate() {
        val pattern = getPatternForTypeAndIntensity(type, intensity)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val effect = VibrationEffect.createWaveform(pattern, -1)
            vibrator.vibrate(effect)
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(pattern, -1)
        }
    }

    fun setIntensity(newIntensity: VibrationIntensity) {
        intensity = newIntensity
    }

    fun setType(newType: VibrationType) {
        type = newType
    }

    private fun getPatternForTypeAndIntensity(
        type: VibrationType,
        intensity: VibrationIntensity
    ): LongArray {
        val base = when (intensity) {
            VibrationIntensity.SUAVE -> 50L
            VibrationIntensity.MEDIA -> 150L
            VibrationIntensity.ALTA  -> 300L
        }

        return when (type) {
            VibrationType.CONTINUA -> longArrayOf(0, base)
            VibrationType.INTERMITENTE -> longArrayOf(0, base, 100, base, 100, base)
            VibrationType.PULSANTE -> longArrayOf(0, base, 50, base / 2, 50, base)
        }
    }
}
