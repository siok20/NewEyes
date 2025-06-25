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
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.neweyes.chat.*
import com.neweyes.databinding.ActivityMultiBinding
import com.neweyes.voice.TextToSpeechHelper
import kotlinx.coroutines.Job
import java.io.File
import io.socket.client.IO
import io.socket.client.Socket
import org.json.JSONObject

class MultiActivity : AppCompatActivity() {

    private val TAG = "TestChatActivity"

    private lateinit var binding: ActivityMultiBinding
    private lateinit var chatAdapter: ChatAdapter

    private var messageJob: Job? = null

    private lateinit var speechRecognizer: SpeechRecognizer
    private lateinit var ttsHelper : TextToSpeechHelper

    private val REQUEST_RECORD_AUDIO_PERMISSION = 100
    private val CAMERA_PERMISSION_REQUEST_CODE = 1003

    private lateinit var cameraLauncher: ActivityResultLauncher<Uri>
    private var photoUri: Uri? = null

    private var currentImageFile: File? = null
    private var haveImage: Boolean = false

    private lateinit var mSocket: Socket
    private var numberRoom: Int = 0
    private var userName: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate: ChatActivity iniciada")

        try {
            // Cambia esta IP y Puerto al de tu servidor
            Log.d(TAG, "Inicio conexion socket")
            mSocket = IO.socket("https://siok-support-groq-neweyes.hf.space")
            mSocket.connect()
            Log.d(TAG, "conectó socket")

            // Cuando te conectes
            mSocket.on(Socket.EVENT_CONNECT) {
                runOnUiThread {
                    Log.d("SocketIO", "✅ Conectado al servidor")
                }
            }

            // Cuando recibas un mensaje
            mSocket.on("new_message") { args ->
                val data = args[0] as JSONObject
                val user = data.getString("user")
                val message = data.getString("message")
                runOnUiThread {
                    // Aquí actualizas tu chat
                    if (user != userName){
                        receiveMessageFromOther(message.toString())
                    }
                    Log.d("SocketIO", "Mensaje de $user: $message")
                }
            }

        } catch (e: Exception) {
            e.printStackTrace()
            Log.d(TAG, "falló socket ${e.toString()}")
        }



        binding = ActivityMultiBinding.inflate(layoutInflater)
        setContentView(binding.root)

        userName = intent.getStringExtra("username").toString()
        numberRoom = intent.getIntExtra("room", 0)
        binding.textRoom.text = "$userName bienvenido al room $numberRoom"

        val roomData = JSONObject().apply {
            put("room", numberRoom)
        }
        mSocket.emit("join_room", roomData)

        setupRecyclerView()
        setupSendMessage()
        supportActionBar?.title = "NewEyes Chat"
        Log.d(TAG, "Toolbar asignado: ${supportActionBar != null}")

        ttsHelper = TextToSpeechHelper(this)

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
                Toast.makeText(this@MultiActivity, "Habla ahora...", Toast.LENGTH_SHORT).show()
                Log.d(TAG, "Reconocimiento iniciado")
            }

            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}

            override fun onError(error: Int) {
                Log.e(TAG, "Error en reconocimiento: $error")
                Toast.makeText(this@MultiActivity, "Error en reconocimiento: $error", Toast.LENGTH_SHORT).show()
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
            layoutManager = LinearLayoutManager(this@MultiActivity).apply {
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
            Log.d(TAG, "sendMessage: enviando mensaje")
            say(texto)
            val newMessage = Message(text = texto, isUser = true)
            chatAdapter.addMessage(newMessage)

            binding.editTextMessage.text?.clear()
            binding.recyclerViewMessages.scrollToPosition(chatAdapter.itemCount - 1)

            val user = userName

            val data = JSONObject().apply {
                put("user", user)
                put("message", texto)
                put("room", numberRoom)
            }
            mSocket.emit("send_message", data)

        } else {
            Log.d(TAG, "sendMessage: mensaje vacío, no se envía")
        }
    }


    fun receiveMessageFromOther(content: String) {
        Log.d(TAG, "receiveMessageFromOther: mensaje recibido -> $content")
        val incoming = Message(text = content, isUser = false)
        chatAdapter.addMessage(incoming)

        say(content)

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

    private fun say(message: String){
        ttsHelper.speak(text = message)
    }

    override fun onDestroy() {
        super.onDestroy()
        mSocket.disconnect()
        messageJob?.cancel()
    }
}