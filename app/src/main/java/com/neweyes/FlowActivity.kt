package com.neweyes

import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.neweyes.databinding.ActivityFlowBinding
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.util.*

class FlowActivity : AppCompatActivity() {

    private lateinit var binding: ActivityFlowBinding
    private lateinit var speechRecognizer: SpeechRecognizer
    private lateinit var recognizerIntent: Intent
    private lateinit var tts: TextToSpeech

    private val voiceflowApiKey = "VF.DM.683f6891f1e7c8c6b6bc55c4.bopBPRHK4k4uQTAT"
    private val voiceflowProjectId = "683f2e2732eeaac8ead5ebde"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFlowBinding.inflate(layoutInflater)
        setContentView(binding.root)

        tts = TextToSpeech(this) {
            if (it == TextToSpeech.SUCCESS) {
                tts.language = Locale("es", "ES")
            }
        }

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
        recognizerIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
        }

        speechRecognizer.setRecognitionListener(object : RecognitionListener {
            override fun onResults(results: Bundle?) {
                val text = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.get(0) ?: ""
                Log.d("VoiceflowDebug", "Texto reconocido: $text")
                enviarAVoiceflow(text)
            }

            override fun onError(error: Int) {
                hablar("Lo siento, no entendí eso.")
                Toast.makeText(this@FlowActivity, "Error de reconocimiento ($error)", Toast.LENGTH_SHORT).show()
            }

            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })

        binding.btnHablar.setOnClickListener {
            Toast.makeText(this, "Escuchando...", Toast.LENGTH_SHORT).show()
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

        val requestBody = json.toString().toRequestBody("application/json".toMediaType())
        val request = Request.Builder()
            .url("https://general-runtime.voiceflow.com/state/$voiceflowProjectId/user/1234/interact")
            .addHeader("Authorization", voiceflowApiKey)
            .post(requestBody)
            .build()

        OkHttpClient().newCall(request).enqueue(object : Callback {
            override fun onResponse(call: Call, response: Response) {
                val data = response.body?.string()
                Log.d("VoiceflowDebug", "Respuesta completa: $data")

                try {
                    val jsonResponse = JSONObject(data ?: "")
                    if (jsonResponse.has("outputs")) {
                        val outputs = jsonResponse.getJSONArray("outputs")
                        val messages = StringBuilder()

                        for (i in 0 until outputs.length()) {
                            val block = outputs.getJSONObject(i)
                            if (block.getString("type") == "text") {
                                val msg = block.getJSONObject("payload").getString("message")
                                messages.append(msg).append(" ")
                            }
                        }

                        val finalMessage = messages.toString().trim()
                        runOnUiThread {
                            if (finalMessage.isNotEmpty()) {
                                hablar(finalMessage)
                            } else {
                                hablar("No recibí una respuesta válida del asistente.")
                            }
                        }
                    } else {
                        runOnUiThread {
                            hablar("No se recibió salida del asistente.")
                        }
                    }
                } catch (e: Exception) {
                    Log.e("VoiceflowDebug", "Error al procesar JSON", e)
                    runOnUiThread {
                        hablar("Hubo un error al interpretar la respuesta.")
                        Toast.makeText(this@FlowActivity, "Error: respuesta no válida", Toast.LENGTH_SHORT).show()
                    }
                }
            }

            override fun onFailure(call: Call, e: IOException) {
                Log.e("VoiceflowDebug", "Error de red", e)
                runOnUiThread {
                    hablar("No se pudo conectar con el asistente.")
                    Toast.makeText(this@FlowActivity, "Fallo de conexión", Toast.LENGTH_SHORT).show()
                }
            }
        })
    }

    private fun hablar(mensaje: String) {
        tts.speak(mensaje, TextToSpeech.QUEUE_FLUSH, null, "TTS_ID")
        Toast.makeText(this, mensaje, Toast.LENGTH_LONG).show()
    }

    override fun onDestroy() {
        super.onDestroy()
        speechRecognizer.destroy()
        tts.shutdown()
    }
}