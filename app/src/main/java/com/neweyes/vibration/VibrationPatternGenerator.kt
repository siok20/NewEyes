package com.neweyes.vibration

import android.os.VibrationEffect

object VibrationPatternGenerator {

    fun generate(
        type: VibrationType,
        intensity: VibrationIntensity,
        hasAmplitudeControl: Boolean
    ): VibrationEffect {
        val amplitude = if (hasAmplitudeControl) intensity.amplitude else VibrationEffect.DEFAULT_AMPLITUDE

        return when (type) {
            VibrationType.CONTINUA -> VibrationEffect.createOneShot(intensity.durationMs, amplitude)

            VibrationType.INTERMITENTE -> {
                val pattern = longArrayOf(0, intensity.durationMs, 100, intensity.durationMs)
                val amplitudes = intArrayOf(0, intensity.amplitude, 0, intensity.amplitude)
                VibrationEffect.createWaveform(pattern, if (hasAmplitudeControl) amplitudes else null, -1)
            }

            VibrationType.PULSANTE -> {
                val pattern = longArrayOf(0, 100, 100, 100, 100, 100)
                val amplitudes = intArrayOf(0, intensity.amplitude, 0, intensity.amplitude, 0, intensity.amplitude)
                VibrationEffect.createWaveform(pattern, if (hasAmplitudeControl) amplitudes else null, -1)
            }

            VibrationType.LATIDO -> {
                val pattern = longArrayOf(0, 80, 100, 80, 250)
                val amplitudes = intArrayOf(0, intensity.amplitude, 0, intensity.amplitude, 0)
                VibrationEffect.createWaveform(pattern, if (hasAmplitudeControl) amplitudes else null, -1)
            }

            VibrationType.ALARMA -> {
                val pattern = longArrayOf(0, 500, 200, 500, 200, 500)
                val amplitudes = intArrayOf(0, intensity.amplitude, 0, intensity.amplitude, 0, intensity.amplitude)
                VibrationEffect.createWaveform(pattern, if (hasAmplitudeControl) amplitudes else null, -1)
            }
        }
    }
}
