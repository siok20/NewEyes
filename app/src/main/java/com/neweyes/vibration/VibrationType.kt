package com.neweyes.vibration

enum class VibrationType {
    CONTINUA, INTERMITENTE, PULSANTE;

    companion object {
        fun fromString(value: String): VibrationType {
            return when (value.lowercase()) {
                "continua"     -> CONTINUA
                "intermitente" -> INTERMITENTE
                "pulsante"     -> PULSANTE
                else           -> CONTINUA // Default
            }
        }
        fun fromConfig(): VibrationType = fromString(AppConfig.getVibrationType())
    }
}