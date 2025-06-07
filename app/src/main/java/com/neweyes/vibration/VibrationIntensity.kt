package com.neweyes.vibration

enum class VibrationIntensity {
    SUAVE, MEDIA, ALTA;

    companion object {
        fun fromString(value: String): VibrationIntensity {
            return when (value.lowercase()) {
                "suave" -> SUAVE
                "media" -> MEDIA
                "alta"  -> ALTA
                else    -> MEDIA // Default
            }
        }

        fun fromConfig(): VibrationIntensity = fromString(AppConfig.vibrationIntensity)
    }
}
