package com.neweyes

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.neweyes.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        //setContentView(R.layout.activity_main)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Correctamente vinculamos el botón por ID
        binding.btnVoiceNav.setOnClickListener {
            Log.d("ChatTest", "Botón presionado")
            try {
                val intent = Intent(this, VoiceActivity::class.java)
                Log.d("ChatTest", "Intent creado correctamente")
                startActivity(intent)
            } catch (e: Exception) {
                Log.e("ChatTest", "Error al abrir ChatActivity", e)
                Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
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


        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }
}