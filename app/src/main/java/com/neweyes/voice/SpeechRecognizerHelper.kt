package com.neweyes.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import java.util.Locale

class SpeechRecognizerHelper(
    private val context: Context,
    private val listener: Listener
) {
    interface Listener {
        fun onPartialResult(text: String)
        fun onFinalResult(text: String)
        fun onError(errorMsg: String)
    }

    private val recognizerIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
        putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
    }

    private var speechRecognizer: SpeechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)

    init {
        speechRecognizer.setRecognitionListener(object : RecognitionListener {
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}

            override fun onError(error: Int) {
                listener.onError("Error: $error")
            }

            override fun onResults(results: Bundle?) {
                val final = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()
                final?.let { listener.onFinalResult(it) }
            }

            override fun onPartialResults(partialResults: Bundle?) {
                val partial = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()
                partial?.let { listener.onPartialResult(it) }
            }
        })
    }

    fun startListening() {
        speechRecognizer.startListening(recognizerIntent)
    }

    fun stopListening() {
        speechRecognizer.stopListening()
    }

    fun destroy() {
        speechRecognizer.destroy()
    }
}
