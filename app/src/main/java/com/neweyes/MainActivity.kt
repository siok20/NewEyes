package com.neweyes

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.neweyes.databinding.ActivityMainBinding
import com.neweyes.databinding.ActivityVoiceBinding

class MainActivity : AppCompatActivity() {
    lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        //setContentView(R.layout.activity_main)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Correctamente vinculamos el botón por ID
        binding.btnVoiceNav.setOnClickListener {
            val intent = Intent(this, VoiceActivity::class.java)
            startActivity(intent)
        }

        binding.btnSettings.setOnClickListener {
            // Aquí podrías abrir una actividad de configuración, si existe
            // Por ahora solo mostramos un ejemplo con un toast
            // Toast.makeText(this, "Abrir configuración", Toast.LENGTH_SHORT).show()
        }

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }
}