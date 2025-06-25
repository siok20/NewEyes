package com.neweyes

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.neweyes.camera.Posiciones
import com.neweyes.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {

        if (AppConfig.isDarkModeEnabled()) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        //setContentView(R.layout.activity_main)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        AppConfig.load(this)

        // Correctamente vinculamos el botón por ID
        binding.btnVoiceNav.setOnClickListener {
            val intent = Intent(this, CameraActivity::class.java)
            startActivity(intent)
        }

        binding.btnSettings.setOnClickListener {
            val intent = Intent(this, SettingsActivity::class.java)
            startActivity(intent)
        }

        binding.btnChatNav.setOnClickListener {
            Log.d("ChatTest", "Botón presionado")
            try {
                val intent = Intent(this, ChatActivity::class.java)
                Log.d("ChatTest", "Intent creado correctamente")
                startActivity(intent)
            } catch (e: Exception) {
                Log.e("ChatTest", "Error al abrir ChatActivity", e)
                Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }

        binding.btnChatPerson.setOnClickListener {
            Log.d("TestChatActivity", "Botón presionado")
            try {
                val intent = Intent(this, MultiActivity::class.java)
                Log.d("TestChatActivity", "Intent creado correctamente")

                val builder = AlertDialog.Builder(this).apply {
                    setTitle("Datos de acceso")

                    // Crear layout vertical para los dos EditText
                    val layout = LinearLayout(this@MainActivity).apply {
                        orientation = LinearLayout.VERTICAL
                        setPadding(50, 20, 50, 10)
                    }

                    // Input para el nombre de usuario
                    val usernameInput = EditText(this@MainActivity).apply {
                        hint = "Nombre de usuario"
                        inputType = android.text.InputType.TYPE_CLASS_TEXT
                    }

                    // Input para el número de room
                    val roomInput = EditText(this@MainActivity).apply {
                        hint = "Número de room (ejm.: 431)"
                        inputType = android.text.InputType.TYPE_CLASS_NUMBER
                    }

                    // Agregar los EditText al layout
                    layout.addView(usernameInput)
                    layout.addView(roomInput)
                    setView(layout)

                    // Botón positivo
                    setPositiveButton("Go") { dialog, _ ->
                        val username = usernameInput.text.toString().trim()
                        val roomText = roomInput.text.toString().trim()

                        if (username.isNotBlank() && roomText.isNotBlank()) {
                            try {
                                val roomNumber = roomText.toInt()
                                Log.d("TestChatActivity", "username: $username, room: $roomNumber")

                                intent.putExtra("username", username)
                                intent.putExtra("room", roomNumber)
                                startActivity(intent)
                                dialog.dismiss()
                            } catch (nfe: NumberFormatException) {
                                Toast.makeText(this@MainActivity, "Número de room no válido", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            Toast.makeText(this@MainActivity, "Completa ambos campos", Toast.LENGTH_SHORT).show()
                        }
                    }

                    // Botón negativo
                    setNegativeButton("Cancelar") { dialog, _ ->
                        dialog.cancel()
                    }
                }

                builder.show()
            } catch (e: Exception) {
                Log.e("TestChatActivity", "Error al abrir ChatActivity", e)
                Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }
}