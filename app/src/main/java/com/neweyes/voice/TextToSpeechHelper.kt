package com.neweyes.voice

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale

class TextToSpeechHelper(context: Context) {
    private val tts: TextToSpeech = TextToSpeech(context) {
        if (it == TextToSpeech.SUCCESS) {
            tts.language = Locale.getDefault()
        }
    }

    fun speak(text: String) {
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
    }

    fun shutdown() {
        tts.shutdown()
    }
}
