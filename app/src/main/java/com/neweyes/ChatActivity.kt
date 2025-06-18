// ChatActivity.kt
package com.neweyes

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.neweyes.chat.*
import com.neweyes.databinding.ActivityChatBinding
import android.speech.tts.TextToSpeech
import java.util.Locale

/**
 * Activity que muestra la interfaz de chat en tiempo real.
 * Utiliza ViewBinding para inflar views y manejar el RecyclerView.
 */
class ChatActivity : AppCompatActivity() {

    private lateinit var binding: ActivityChatBinding
    private lateinit var chatAdapter: ChatAdapter

    private lateinit var tts: TextToSpeech

    private lateinit var speechRecognizer: SpeechRecognizer
    private val REQUEST_RECORD_AUDIO_PERMISSION = 100

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Inflar layout con ViewBinding
        binding = ActivityChatBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecyclerView()
        setupSendMessage()

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)

        tts = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts.language = Locale("es", "ES") // o Locale.US para inglés
            }
        }


        val listener = object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                Toast.makeText(this@ChatActivity, "Habla ahora...", Toast.LENGTH_SHORT).show()
            }

            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}

            override fun onError(error: Int) {
                Toast.makeText(this@ChatActivity, "Error en reconocimiento: $error", Toast.LENGTH_SHORT).show()
            }

            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                matches?.firstOrNull()?.let { textoReconocido ->
                    binding.editTextMessage.setText(textoReconocido)
                    // Envía el mensaje automáticamente cuando termine de reconocer
                    sendMessage()
                }
            }

            override fun onPartialResults(partialResults: Bundle?) {
                val partial = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                partial?.firstOrNull()?.let {
                    // Mostrar texto en vivo mientras hablas
                    binding.editTextMessage.setText(it)
                    binding.editTextMessage.setSelection(it.length)
                }
            }

            override fun onEvent(eventType: Int, params: Bundle?) {}
        }

        speechRecognizer.setRecognitionListener(listener)

        binding.buttonMic.setOnClickListener {
            if (checkAudioPermission()) {
                startListening()
            } else {
                requestAudioPermission()
            }
        }

        receiveMessageFromOther("¡Bienvenido a Neweyes!\n\n" +
                "Tu guía inteligente diseñada especialmente para ti.\n" +
                "Con Neweyes, podrás:\n\n" +
                "🔧 Configurar patrones de vibración personalizados para que tu celular te comunique lo que ves sin necesidad de mirar.\n" +
                "📷 Usar la cámara del dispositivo para detectar obstáculos y recibir alertas en tiempo real, ayudándote a navegar de forma segura.\n" +
                "🗣️ Recibir indicaciones por voz que te orientan mientras caminas, para que siempre sepas por dónde ir.\n\n" +
                "No necesitas ver la pantalla: Neweyes te habla, vibra y te cuida.\n" +
                "¡Comencemos a explorar el mundo con nuevos ojos! 👁️📲✨")

        tts.speak("¡Bienvenido a Neweyes!\n\n" +
                "Tu guía inteligente diseñada especialmente para ti.\n" +
                "Con Neweyes, podrás:\n\n" +
                "🔧 Configurar patrones de vibración personalizados para que tu celular te comunique lo que ves sin necesidad de mirar.\n" +
                "📷 Usar la cámara del dispositivo para detectar obstáculos y recibir alertas en tiempo real, ayudándote a navegar de forma segura.\n" +
                "🗣️ Recibir indicaciones por voz que te orientan mientras caminas, para que siempre sepas por dónde ir.\n\n" +
                "No necesitas ver la pantalla: Neweyes te habla, vibra y te cuida.\n" +
                "¡Comencemos a explorar el mundo con nuevos ojos! 👁️📲✨", TextToSpeech.QUEUE_FLUSH, null, null)
    }

    private fun checkAudioPermission(): Boolean {
        return ContextCompat.checkSelfPermission(this, android.Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestAudioPermission() {
        ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.RECORD_AUDIO), REQUEST_RECORD_AUDIO_PERMISSION)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_RECORD_AUDIO_PERMISSION) {
            if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                startListening()
            } else {
                Toast.makeText(this, "Permiso de micrófono necesario", Toast.LENGTH_SHORT).show()
            }
        }
    }


    private fun startListening() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "es-ES")  // Cambia por el idioma que quieras
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        }
        speechRecognizer.startListening(intent)
    }


    /**
     * Inicializa el RecyclerView y le asigna el adapter.
     */
    private fun setupRecyclerView() {
        chatAdapter = ChatAdapter()
        binding.recyclerViewMessages.apply {
            layoutManager = LinearLayoutManager(this@ChatActivity).apply {
                // Mostrar los mensajes desde el final cuando se agregue uno nuevo
                stackFromEnd = true
            }
            adapter = chatAdapter
        }
    }

    /**
     * Configura el envío de mensajes: tanto al pulsar el botón como al pulsar "Enter" en el teclado.
     */
    private fun setupSendMessage() {
        // Al pulsar el botón de envío
        binding.buttonSend.setOnClickListener {
            sendMessage()
        }

        // Si el usuario presiona "Enter" (actionSend) en el teclado virtual
        binding.editTextMessage.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                sendMessage()
                true
            } else {
                false
            }
        }
    }

    /**
     * Toma el texto del EditText, lo añade al chat como mensaje de usuario y limpia el campo.
     * (Aquí únicamente agrega a la lista; la lógica de API para enviar queda para más adelante).
     */
    private fun sendMessage() {
        val texto = binding.editTextMessage.text.toString().trim()
        if (texto.isNotEmpty()) {
            // 1. Agregar el mensaje del usuario
            val newMessage = Message(text = texto, isUser = true)
            chatAdapter.addMessage(newMessage)

            // 2. Limpiar el campo de texto
            binding.editTextMessage.text?.clear()

            // 3. Hacer scroll al último mensaje
            binding.recyclerViewMessages.scrollToPosition(chatAdapter.itemCount - 1)

            val request = ChatRequest(
                model = "meta-llama/llama-4-scout-17b-16e-instruct",
                messages = listOf(ChatMessage(role = "user", content = texto))
            )

            Log.d("GROQ", "Empieza")

            groqApi.getChatCompletion(request).enqueue(object : retrofit2.Callback<ChatResponse> {

                override fun onResponse(call: retrofit2.Call<ChatResponse>, response: retrofit2.Response<ChatResponse>) {
                    Log.d("GROQ", "Recibe")
                    if (response.isSuccessful) {
                        val chatResponse = response.body()
                        // Aquí accedes a la respuesta: chatResponse?.choices?.get(0)?.message?.content
                        Log.d("GROQ","Respuesta: ${chatResponse?.choices?.get(0)?.message?.content}")
                        receiveMessageFromOther(chatResponse?.choices?.get(0)?.message?.content.toString())
                    } else {
                        Log.d("GROQ","Error en la respuesta: ${response.errorBody()?.string()}")
                    }
                }

                override fun onFailure(call: retrofit2.Call<ChatResponse>, t: Throwable) {
                    Log.d("GROQ","Fallo en la llamada: ${t.message}")
                }
            })


            // 4. (Opcional) Aquí podrías invocar tu función para enviar el mensaje a la API / WebSocket.
            // sendToServer(texto)
        }
    }

    /**
     * Método de ejemplo para recibir un mensaje "de otro".
     * Úsalo cuando llegue la respuesta de tu API/WebSocket y quieras mostrarla.
     */
    fun receiveMessageFromOther(content: String) {
        val incoming = Message(text = content, isUser = false)
        chatAdapter.addMessage(incoming)
        binding.recyclerViewMessages.scrollToPosition(chatAdapter.itemCount - 1)

        // Hablar el mensaje
        tts.speak(content, TextToSpeech.QUEUE_FLUSH, null, null)
    }
}
