package com.neweyes

import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import androidx.appcompat.app.AppCompatActivity
import com.neweyes.databinding.ActivityFlowBinding
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.util.Locale

class FlowActivity : AppCompatActivity() {

    private lateinit var binding: ActivityFlowBinding
    private lateinit var speechRecognizer: SpeechRecognizer
    private lateinit var recognizerIntent: Intent
    private lateinit var tts: TextToSpeech

    // 🔐 Reemplaza estos valores con los tuyos
    private val voiceflowApiKey = "VF.DM.683f6891f1e7c8c6b6bc55c4.bopBPRHK4k4uQTAT"
    private val voiceflowProjectId = "683f2e2732eeaac8ead5ebde"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Inflamos el binding
        binding = ActivityFlowBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Inicializar TextToSpeech en español
        tts = TextToSpeech(this) {
            if (it == TextToSpeech.SUCCESS) {
                tts.language = Locale("es", "ES")
            }
        }

        // Inicializar SpeechRecognizer
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
        recognizerIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
        }

        // Asignar RecognitionListener
        speechRecognizer.setRecognitionListener(object : RecognitionListener {
            override fun onResults(results: Bundle?) {
                val text = results
                    ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.get(0) ?: ""
                enviarAVoiceflow(text)
            }

            override fun onError(error: Int) {
                hablar("Lo siento, no entendí eso.")
            }

            // Métodos vacíos, sólo requeridos por la interfaz
            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })

        // Botón para iniciar el reconocimiento por voz
        binding.btnHablar.setOnClickListener {
            speechRecognizer.startListening(recognizerIntent)
        }
    }

    private fun enviarAVoiceflow(mensaje: String) {
        val json = JSONObject().apply {
            put("action", "text")
            put("message", mensaje)
            put("config", JSONObject().apply { put("tts", false) })
            put("state", JSONObject())
        }

        val requestBody = json.toString()
            .toRequestBody("application/json".toMediaType())

        val request = Request.Builder()
            .url("https://general-runtime.voiceflow.com/state/$voiceflowProjectId/user/1234/interact")
            .addHeader("Authorization", voiceflowApiKey)
            .post(requestBody)
            .build()

        OkHttpClient().newCall(request).enqueue(object : Callback {
            override fun onResponse(call: Call, response: Response) {
                val data = response.body?.string()
                if (data != null) {
                    // Extraer mensaje de la respuesta JSON
                    val mensajeRespuesta = JSONObject(data)
                        .getJSONArray("outputs")
                        .getJSONObject(0)
                        .getJSONObject("payload")
                        .getString("message")

                    runOnUiThread {
                        hablar(mensajeRespuesta)
                    }
                } else {
                    runOnUiThread {
                        hablar("Llegó una respuesta vacía del asistente.")
                    }
                }
            }

            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    hablar("Hubo un problema al conectar con el asistente.")
                }
            }
        })
    }

    private fun hablar(mensaje: String) {
        tts.speak(mensaje, TextToSpeech.QUEUE_FLUSH, null, "TTS_ID")
    }

    override fun onDestroy() {
        super.onDestroy()
        speechRecognizer.destroy()
        tts.shutdown()
    }
}
