package com.neweyes.vibration

import androidx.lifecycle.ViewModel

class VibrationViewModel(private val vibrationManager: VibrationManager) : ViewModel() {

    fun testVibration() {
        val effect = VibrationPatternGenerator.generate(
            VibrationSettings.type,
            VibrationSettings.intensity,
            vibrationManager.hasAmplitudeControl()
        )
        vibrationManager.vibrate(effect)
    }
}