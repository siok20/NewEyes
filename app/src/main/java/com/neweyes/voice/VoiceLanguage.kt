package com.neweyes.voice

import java.util.Locale

enum class VoiceLanguage(val locale: Locale) {
    ES(Locale("es", "ES")),
    EN(Locale.ENGLISH);

    companion object {
        fun fromString(value: String): VoiceLanguage = when (value.lowercase()) {
            "en" -> EN
            "es" -> ES
            else -> ES
        }
    }
}
