// ChatActivity.kt
package com.neweyes

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.view.GravityCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.neweyes.chat.*
import com.neweyes.chat.history.*
import com.neweyes.data.AppDatabase
import com.neweyes.data.DatabaseModule
import com.neweyes.data.entity.ChatEntity
import com.neweyes.data.entity.MessageEntity
import com.neweyes.databinding.ActivityChatBinding
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.forEach
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Activity que muestra la interfaz de chat en tiempo real.
 * Utiliza ViewBinding para inflar views y manejar el RecyclerView.
 */
class ChatActivity : AppCompatActivity() {

    private lateinit var binding: ActivityChatBinding
    private lateinit var chatAdapter: ChatAdapter

    private lateinit var chatHistoryAdapter: ChatHistoryAdapter
    private val chatHistoryViewModel: ChatHistoryViewModel by viewModels {
        ChatViewModelFactory(ChatRepository(
            chatDao = DatabaseModule.provideDatabase(this).chatDao(),
            messageDao = DatabaseModule.provideDatabase(this).messageDao()
        ))
    }
    private var currentChatId: Long = -1L
    private var currentChatJob: Job? = null

    private lateinit var speechRecognizer: SpeechRecognizer
    private val REQUEST_RECORD_AUDIO_PERMISSION = 100

    private lateinit var cameraLauncher: ActivityResultLauncher<Uri>
    private var photoUri: Uri? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Inflar layout
        binding = ActivityChatBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecyclerView()
        setupSendMessage()
        setSupportActionBar(binding.chatToolbar)
        supportActionBar?.title = "NewEyes Chat"
        Log.d("ToolbarAction", "Toolbar asignado: ${supportActionBar != null}")

        chatHistoryAdapter = ChatHistoryAdapter { chatId ->
            // Cargar el chat seleccionado
            loadChatById(chatId)
            binding.drawerLayout.closeDrawers()
        }

        binding.recyclerViewChatHistory.layoutManager = LinearLayoutManager(this)
        binding.recyclerViewChatHistory.adapter = chatHistoryAdapter

        // Observar lista de chats
        lifecycleScope.launchWhenStarted {
            chatHistoryViewModel.chatSummaries.collect { summaries ->
                chatHistoryAdapter.submitList(summaries)
            }
        }


        cameraLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            Log.d("CameraDebug", "TakePicture result: $success")
            Log.d("CameraDebug", "photoUri: $photoUri")

            try {
                if (success && photoUri != null) {
                    Log.d("CameraDebug", "Uri válido, agregando mensaje")
                    val imageMessage = Message(imageUri = photoUri.toString(), isUser = true)
                    chatAdapter.addMessage(imageMessage)

                    val messageEntity = MessageEntity(
                        chatId = currentChatId,
                        timestamp = System.currentTimeMillis(),
                        text = null,
                        imageUri = imageMessage.imageUri,
                        isUser = true
                    )
                    chatHistoryViewModel.saveMessage(messageEntity)

                    binding.recyclerViewMessages.scrollToPosition(chatAdapter.itemCount - 1)
                } else {
                    Log.w("CameraDebug", "No se capturó la imagen o Uri nulo")
                    Toast.makeText(this, "No se capturó la imagen", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e("CameraDebug", "Error al procesar la imagen", e)
                Toast.makeText(this, "Error procesando la imagen", Toast.LENGTH_SHORT).show()
            }
        }


        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)

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

        binding.buttonCamera.setOnClickListener {
            val imageFile = createImageFile()
            val uri = try {
                FileProvider.getUriForFile(this, "${packageName}.provider", imageFile)
            } catch (e: Exception) {
                Log.e("ChatActivity", "Error al obtener Uri: ${e.message}")
                Toast.makeText(this, "Error al preparar la cámara", Toast.LENGTH_SHORT).show()
                null
            }

            uri?.let {
                photoUri = it
                cameraLauncher.launch(it)
            }
        }

        /*receiveMessageFromOther("¡Bienvenido a Neweyes!\n\n" +
                "Tu guía inteligente diseñada especialmente para ti.\n" +
                "Con Neweyes, podrás:\n\n" +
                "🔧 Configurar patrones de vibración personalizados para que tu celular te comunique lo que ves sin necesidad de mirar.\n" +
                "📷 Usar la cámara del dispositivo para detectar obstáculos y recibir alertas en tiempo real, ayudándote a navegar de forma segura.\n" +
                "🗣️ Recibir indicaciones por voz que te orientan mientras caminas, para que siempre sepas por dónde ir.\n\n" +
                "No necesitas ver la pantalla: Neweyes te habla, vibra y te cuida.\n" +
                "¡Comencemos a explorar el mundo con nuevos ojos! 👁️📲✨")
         */
        startNewChat()
    }

    private fun checkAudioPermission(): Boolean {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestAudioPermission() {
        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), REQUEST_RECORD_AUDIO_PERMISSION)
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

            val messageEntity = MessageEntity(
                chatId = currentChatId,
                timestamp = System.currentTimeMillis(),
                text = texto,
                imageUri = null,
                isUser = true
            )
            chatHistoryViewModel.saveMessage(messageEntity)

            // 2. Limpiar el campo de texto
            binding.editTextMessage.text?.clear()

            // 3. Hacer scroll al último mensaje
            binding.recyclerViewMessages.scrollToPosition(chatAdapter.itemCount - 1)

            val request = ChatRequest(
                model = "meta-llama/llama-4-scout-17b-16e-instruct",
                messages = listOf(ChatMessage(role = "user", content = texto))
            )

            Log.d("GROQ", "Empieza")

            groqApi.getChatCompletion(request).enqueue(object : Callback<ChatResponse> {

                override fun onResponse(call: Call<ChatResponse>, response: Response<ChatResponse>) {
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

                override fun onFailure(call: Call<ChatResponse>, t: Throwable) {
                    Log.d("GROQ","Fallo en la llamada: ${t.message}")
                }
            })
        }
    }

    /**
     * Métod de ejemplo para recibir un mensaje "de otro".
     * Úsalo cuando llegue la respuesta de tu API/WebSocket y quieras mostrarla.
     */
    fun receiveMessageFromOther(content: String) {
        val incoming = Message(text = content, isUser = false)
        chatAdapter.addMessage(incoming)

        val messageEntity = MessageEntity(
            chatId = currentChatId,
            timestamp = System.currentTimeMillis(),
            text = content,
            imageUri = null,
            isUser = false
        )
        chatHistoryViewModel.saveMessage(messageEntity)

        binding.recyclerViewMessages.scrollToPosition(chatAdapter.itemCount - 1)
    }

    private fun createImageFile(): File {
        val storageDir = cacheDir
        return File.createTempFile(
            "IMG_${System.currentTimeMillis()}",
            ".jpg",
            storageDir
        )
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.chat_toolbar_menu, menu)
        Log.d("ToolbarAction", "Menú inflado con ${menu?.size()} items")
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        Log.d("ToolbarAction", "Item seleccionado: ${item.itemId}")
        return when (item.itemId) {
            R.id.action_new_chat -> {
                Log.d("ToolbarAction", "Nuevo chat seleccionado")
                Toast.makeText(this, "Nuevo chat", Toast.LENGTH_SHORT).show()
                startNewChat()
                true
            }
            R.id.action_history -> {
                Log.d("ToolbarAction", "Historial seleccionado")
                Toast.makeText(this, "Historial", Toast.LENGTH_SHORT).show()
                binding.drawerLayout.openDrawer(GravityCompat.START)
                true
            }
            R.id.action_settings -> {
                Log.d("ToolbarAction", "Ajustes seleccionados")
                Toast.makeText(this, "Ajustes", Toast.LENGTH_SHORT).show()
                val intent = Intent(this, SettingsActivity::class.java)
                startActivity(intent)
                true
            }
            R.id.action_exit -> {
                Log.d("ToolbarAction", "Salir seleccionado")
                Toast.makeText(this, "Saliendo...", Toast.LENGTH_SHORT).show()
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun loadChatById(chatId: Long) {
        lifecycleScope.launch {
            DatabaseModule.provideDatabase(this@ChatActivity)
                .messageDao()
                .getMessagesForChat(chatId)
                .collect { messages ->
                    chatAdapter.clearMessages()
                    messages.forEach { entity ->
                        chatAdapter.addMessage(
                            Message(
                                text = entity.text,
                                imageUri = entity.imageUri,
                                isUser = entity.isUser
                            )
                        )
                    }
                }
        }
    }

    private fun startNewChat() {
        lifecycleScope.launch {
            val timestamp = System.currentTimeMillis()
            val newChat = ChatEntity(title = "Chat del ${getFormattedDate(timestamp)}", createdAt = timestamp)
            val newChatId = chatHistoryViewModel.createNewChat(newChat)
            currentChatId = newChatId
            loadChatById(newChatId)
            receiveMessageFromOther("¡Nuevo chat creado!\nPuedes empezar a hablar ahora.")
            Log.d("Mensaje enviado", "mensaje")
            binding.drawerLayout.closeDrawer(GravityCompat.START)
        }
    }

    private fun getFormattedDate(timestamp: Long): String {
        val format = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        return format.format(Date(timestamp))
    }
}