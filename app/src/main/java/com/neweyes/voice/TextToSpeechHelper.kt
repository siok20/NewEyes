package com.neweyes.voice

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale

class TextToSpeechHelper(
    context: Context,
)  {
    private var speed: VoiceSpeed = VoiceSpeed.fromString(AppConfig.voiceSpeed)
    private var language: VoiceLanguage = VoiceLanguage.fromString(AppConfig.defaultLanguage)

    private val tts: TextToSpeech = TextToSpeech(context) {
        if (it == TextToSpeech.SUCCESS) {
            tts.language = Locale.getDefault()
        }
    }

    private fun setupTTS() {
        tts.language = language.locale
        tts.setSpeechRate(speed.rate)
    }

    fun speak(text: String) {
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
    }

    fun setSpeed(newSpeed: VoiceSpeed) {
        speed = newSpeed
        tts.setSpeechRate(speed.rate)
    }

    fun setLanguage(newLanguage: VoiceLanguage) {
        language = newLanguage
        tts.language = language.locale
    }


    fun shutdown() {
        tts.shutdown()
    }
}
