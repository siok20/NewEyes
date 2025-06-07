package com.neweyes.voice

enum class VoiceSpeed(val rate: Float) {
    SLOW(0.25f),
    NORMAL(1.0f),
    FAST(1.75f);

    companion object {
        fun fromString(value: String): VoiceSpeed = when (value.lowercase()) {
            "lenta", "slow" -> SLOW
            "rapida", "fast" -> FAST
            else -> NORMAL
        }
    }
}