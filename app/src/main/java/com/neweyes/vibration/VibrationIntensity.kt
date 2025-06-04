package com.neweyes.vibration

enum class VibrationIntensity(val amplitude: Int, val durationMs: Long) {
    SUAVE(50, 150),
    MEDIA(150, 300),
    ALTA(255, 500)
}