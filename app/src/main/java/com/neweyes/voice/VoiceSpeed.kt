package com.neweyes.voice

import com.neweyes.config.AppConfig

enum class VoiceSpeed(val rate: Float) {
    SLOW(0.5f),
    NORMAL(1.0f),
    FAST(1.5f);

    companion object {
        fun fromString(value: String): VoiceSpeed = when (value.lowercase()) {
            "lenta", "slow" -> SLOW
            "rápida", "fast" -> FAST
            else -> NORMAL
        }

        fun fromConfig(): VoiceSpeed = fromString(AppConfig.getVoiceSpeed())
    }
}