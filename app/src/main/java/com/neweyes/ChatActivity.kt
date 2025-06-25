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
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.view.GravityCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.neweyes.chat.*
import com.neweyes.chat.gemini.ImageChatResponse
import com.neweyes.chat.gemini.geminiApi
import com.neweyes.chat.groq.ChatMessage
import com.neweyes.chat.groq.ChatRequest
import com.neweyes.chat.groq.ChatResponse
import com.neweyes.chat.groq.groqApi
import com.neweyes.chat.history.*
import com.neweyes.data.DatabaseModule
import com.neweyes.data.entity.ChatEntity
import com.neweyes.data.entity.MessageEntity
import com.neweyes.databinding.ActivityChatBinding
import com.neweyes.voice.TextToSpeechHelper
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ChatActivity : AppCompatActivity() {

    private val TAG = "TestChatActivity"

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
    private var messageJob: Job? = null

    private lateinit var speechRecognizer: SpeechRecognizer
    private lateinit var ttsHelper : TextToSpeechHelper

    private val REQUEST_RECORD_AUDIO_PERMISSION = 100
    private val CAMERA_PERMISSION_REQUEST_CODE = 1003

    private lateinit var cameraLauncher: ActivityResultLauncher<Uri>
    private var photoUri: Uri? = null

    private var currentImageFile: File? = null
    private var haveImage: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate: ChatActivity iniciada")

        binding = ActivityChatBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecyclerView()
        setupSendMessage()
        setSupportActionBar(binding.chatToolbar)
        supportActionBar?.title = "NewEyes Chat"
        Log.d(TAG, "Toolbar asignado: ${supportActionBar != null}")

        chatHistoryAdapter = ChatHistoryAdapter ({ chatId ->
            Log.d(TAG, "Chat seleccionado: $chatId")
            loadChatById(chatId)
            binding.drawerLayout.closeDrawers()
        }, onChatLongClick = { chatIdToDelete ->
            Log.d(TAG, "Chat long click: eliminar $chatIdToDelete")
            AlertDialog.Builder(this)
                .setTitle("Eliminar chat")
                .setMessage("¿Estás seguro de que quieres eliminar este chat?")
                .setPositiveButton("Eliminar") { _, _ ->
                    chatHistoryViewModel.deleteChat(chatIdToDelete)
                }
                .setNegativeButton("Cancelar", null)
                .show()
        })

        ttsHelper = TextToSpeechHelper(this)

        binding.recyclerViewChatHistory.layoutManager = LinearLayoutManager(this)
        binding.recyclerViewChatHistory.adapter = chatHistoryAdapter

        lifecycleScope.launchWhenStarted {
            chatHistoryViewModel.chatSummaries.collect { summaries ->
                chatHistoryAdapter.submitList(summaries)
            }
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.CAMERA),
                CAMERA_PERMISSION_REQUEST_CODE
            )
        }

        cameraLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            Log.d(TAG, "TakePicture result: $success")
            Log.d(TAG, "photoUri: $photoUri")

            try {
                if (success && photoUri != null) {
                    Log.d(TAG, "Uri válido, agregando mensaje")
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

                    say("Imagen subida correctamente")
                    haveImage = true
                    binding.buttonCamera.isEnabled = false
                    receiveMessageFromOther("Ahora descríbeme que quieres que haga com la imagen")
                } else {
                    Log.w(TAG, "No se capturó la imagen o Uri nulo")
                    Toast.makeText(this, "No se capturó la imagen", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error al procesar la imagen", e)
                Toast.makeText(this, "Error procesando la imagen", Toast.LENGTH_SHORT).show()
            }
        }

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)

        val listener = object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                Toast.makeText(this@ChatActivity, "Habla ahora...", Toast.LENGTH_SHORT).show()
                Log.d(TAG, "Reconocimiento iniciado")
            }

            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}

            override fun onError(error: Int) {
                Log.e(TAG, "Error en reconocimiento: $error")
                Toast.makeText(this@ChatActivity, "Error en reconocimiento: $error", Toast.LENGTH_SHORT).show()
            }

            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                matches?.firstOrNull()?.let { textoReconocido ->
                    Log.d(TAG, "Texto reconocido: $textoReconocido")
                    binding.editTextMessage.setText(textoReconocido)
                    sendMessage()
                }
            }

            override fun onPartialResults(partialResults: Bundle?) {
                val partial = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                partial?.firstOrNull()?.let {
                    Log.d(TAG, "Texto parcial: $it")
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
            Log.d(TAG, "Botón cámara presionado")
            val imageFile = createImageFile()
            currentImageFile = imageFile
            val uri = try {
                FileProvider.getUriForFile(this, "${packageName}.provider", imageFile)
            } catch (e: Exception) {
                Log.e(TAG, "Error al obtener Uri: ${e.message}")
                Toast.makeText(this, "Error al preparar la cámara", Toast.LENGTH_SHORT).show()
                null
            }
            uri?.let {
                photoUri = it
                cameraLauncher.launch(it)
            }
        }

        startNewChat()
    }

    private fun checkAudioPermission(): Boolean {
        val granted = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        Log.d(TAG, "checkAudioPermission: $granted")
        return granted
    }

    private fun requestAudioPermission() {
        Log.d(TAG, "requestAudioPermission: solicitando permiso de audio")
        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), REQUEST_RECORD_AUDIO_PERMISSION)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_RECORD_AUDIO_PERMISSION) {
            if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                Log.d(TAG, "Permiso de audio concedido")
                startListening()
            } else {
                Log.d(TAG, "Permiso de audio denegado")
                Toast.makeText(this, "Permiso de micrófono necesario", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun startListening() {
        Log.d(TAG, "startListening: iniciando reconocimiento de voz")
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "es-ES")
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        }
        speechRecognizer.startListening(intent)
    }

    private fun setupRecyclerView() {
        Log.d(TAG, "setupRecyclerView: inicializando RecyclerView")
        chatAdapter = ChatAdapter()
        binding.recyclerViewMessages.apply {
            layoutManager = LinearLayoutManager(this@ChatActivity).apply {
                stackFromEnd = true
            }
            adapter = chatAdapter
        }
    }

    private fun setupSendMessage() {
        Log.d(TAG, "setupSendMessage: configurando botón de envío y enter del teclado")
        binding.buttonSend.setOnClickListener {
            Log.d(TAG, "Botón de enviar presionado")
            sendMessage()
        }

        binding.editTextMessage.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                Log.d(TAG, "IME_ACTION_SEND detectado")
                sendMessage()
                true
            } else {
                false
            }
        }
    }

    private fun sendMessage() {
        val texto = binding.editTextMessage.text.toString().trim()
        if (texto.isNotEmpty()) {
            Log.d(TAG, "sendMessage: enviando mensaje -> $texto en el chat $currentChatId")
            say(texto)
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

            binding.editTextMessage.text?.clear()
            binding.recyclerViewMessages.scrollToPosition(chatAdapter.itemCount - 1)

            val request = ChatRequest(
                model = "meta-llama/llama-4-scout-17b-16e-instruct",
                messages = chatAdapter.getMessages()
            )

            Log.d(TAG, "Enviando solicitud a Groq")
            Log.d(TAG, "${haveImage} and ${currentImageFile}")
            if (haveImage && currentImageFile!=null){
                Log.d(TAG, "Proceso por gemini")
                subirImagen(currentImageFile!!, texto)
                binding.buttonCamera.isEnabled = true
                haveImage = false
            }
            else if (!haveImage) {
                Log.d(TAG, "Proceso por groq")
                groqApi.getChatCompletion(request).enqueue(object : Callback<ChatResponse> {

                    override fun onResponse(
                        call: Call<ChatResponse>,
                        response: Response<ChatResponse>
                    ) {
                        try {
                            Log.d(TAG, "Respuesta recibida de Groq")
                            if (response.isSuccessful) {
                                val chatResponse = response.body()
                                val respuesta =
                                    chatResponse?.choices?.get(0)?.message?.content.toString()
                                Log.d(TAG, "Respuesta del modelo: $respuesta")
                                receiveMessageFromOther(respuesta)
                            } else {
                                val errorMsg = response.errorBody()?.string()
                                Log.e(TAG, "Error en la respuesta: $errorMsg")
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Excepción en onResponse: ${e.message}", e)
                        }
                    }

                    override fun onFailure(call: Call<ChatResponse>, t: Throwable) {
                        try {
                            Log.e(TAG, "Fallo en la llamada a Groq: ${t.message}", t)
                        } catch (e: Exception) {
                            Log.e(TAG, "Excepción en onFailure: ${e.message}", e)
                        }
                    }

                })
            }
        } else {
            Log.d(TAG, "sendMessage: mensaje vacío, no se envía")
        }
    }
    private fun subirImagen(imageFile: File, prompt: String) {
        val requestFile = RequestBody.create("image/jpeg".toMediaTypeOrNull(), imageFile)
        val body = MultipartBody.Part.createFormData("file", imageFile.name, requestFile)

        val promptRequest = RequestBody.create("text/plain".toMediaTypeOrNull(), prompt)

        geminiApi.sendImageAndPrompt(body, promptRequest).enqueue(object : Callback<ImageChatResponse> {
            override fun onResponse(call: Call<ImageChatResponse>, response: Response<ImageChatResponse>) {
                if (response.isSuccessful) {
                    val result = response.body()

                    Log.d(TAG, "✅ Resultado: ${result?.response}")
                    receiveMessageFromOther(result?.response.toString())
                    Toast.makeText(this@ChatActivity, result?.response, Toast.LENGTH_LONG).show()
                } else {
                    Log.e(TAG, "❌ Error en la respuesta: ${response.errorBody()?.string()}")
                }
            }

            override fun onFailure(call: Call<ImageChatResponse>, t: Throwable) {
                Log.e(TAG, "❌ Error en la solicitud: ${t.message}")
            }
        })
    }


    fun receiveMessageFromOther(content: String) {
        Log.d(TAG, "receiveMessageFromOther: mensaje recibido -> $content")
        val incoming = Message(text = content, isUser = false)
        chatAdapter.addMessage(incoming)

        say(content)

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
        val file = File.createTempFile(
            "IMG_${System.currentTimeMillis()}",
            ".jpg",
            cacheDir
        )
        Log.d(TAG, "createImageFile: archivo creado -> ${file.absolutePath}")
        return file
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.chat_toolbar_menu, menu)
        Log.d(TAG, "onCreateOptionsMenu: menú inflado con ${menu?.size()} items")
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        Log.d(TAG, "onOptionsItemSelected: item -> ${item.itemId}")
        return when (item.itemId) {
            R.id.action_new_chat -> {
                Log.d(TAG, "Opción: nuevo chat")
                Toast.makeText(this, "Nuevo chat", Toast.LENGTH_SHORT).show()
                startNewChat()
                true
            }
            R.id.action_history -> {
                Log.d(TAG, "Opción: historial")
                Toast.makeText(this, "Historial", Toast.LENGTH_SHORT).show()
                binding.drawerLayout.openDrawer(GravityCompat.START)
                true
            }
            R.id.action_settings -> {
                Log.d(TAG, "Opción: ajustes")
                Toast.makeText(this, "Ajustes", Toast.LENGTH_SHORT).show()
                val intent = Intent(this, SettingsActivity::class.java)
                startActivity(intent)
                true
            }
            R.id.action_exit -> {
                Log.d(TAG, "Opción: salir")
                Toast.makeText(this, "Saliendo...", Toast.LENGTH_SHORT).show()
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun loadChatById(chatId: Long) {
        Log.d(TAG, "loadChatById: cargando chat con ID $chatId")

        messageJob?.cancel()

        messageJob = lifecycleScope.launch {
            DatabaseModule.provideDatabase(this@ChatActivity)
                .messageDao()
                .getMessagesForChat(chatId)
                .collect { messages ->
                    Log.d(TAG, "Mensajes cargados: ${messages.size}")
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
                    binding.recyclerViewMessages.scrollToPosition(chatAdapter.itemCount - 1)
                }
        }
    }

    private fun startNewChat() {
        Log.d(TAG, "startNewChat: creando nuevo chat")
        lifecycleScope.launch {
            val timestamp = System.currentTimeMillis()
            val newChat = ChatEntity(title = "Chat del ${getFormattedDate(timestamp)}", createdAt = timestamp)
            val newChatId = chatHistoryViewModel.createNewChat(newChat)
            currentChatId = newChatId
            loadChatById(newChatId)
            receiveMessageFromOther("¡Nuevo chat creado!\nPuedes empezar a hablar ahora.")
            Log.d(TAG, "Nuevo chat creado con ID $newChatId")
            binding.drawerLayout.closeDrawer(GravityCompat.START)
        }
    }

    private fun getFormattedDate(timestamp: Long): String {
        val format = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        return format.format(Date(timestamp))
    }

    private fun say(message: String){
        ttsHelper.speak(text = message)
    }

    override fun onDestroy() {
        super.onDestroy()
        messageJob?.cancel()
    }
}