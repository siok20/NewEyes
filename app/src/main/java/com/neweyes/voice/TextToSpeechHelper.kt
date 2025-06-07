package com.neweyes.voice

import android.content.Context
import android.speech.tts.TextToSpeech

class TextToSpeechHelper(
    context: Context,
    speed: VoiceSpeed = VoiceSpeed.fromConfig(),
    language: VoiceLanguage = VoiceLanguage.fromConfig()
) {
    private var currentSpeed: VoiceSpeed = speed
    private var currentLanguage: VoiceLanguage = language

    private val tts: TextToSpeech = TextToSpeech(context) {
        if (it == TextToSpeech.SUCCESS) {
            setupTTS()
        }
    }

    private fun setupTTS() {
        tts.language = currentLanguage.locale
        tts.setSpeechRate(currentSpeed.rate)
    }

    fun speak(text: String) {
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
    }

    fun setSpeed(newSpeed: VoiceSpeed) {
        currentSpeed = newSpeed
        tts.setSpeechRate(currentSpeed.rate)
    }

    fun setLanguage(newLanguage: VoiceLanguage) {
        currentLanguage = newLanguage
        tts.language = currentLanguage.locale
    }

    fun shutdown() {
        tts.shutdown()
    }
}
