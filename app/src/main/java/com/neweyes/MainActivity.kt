package com.neweyes

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.neweyes.databinding.ActivityMainBinding

class MainActivity : BaseActivity() {
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        if (PreferenceManager.isHighContrast(this)) {
            setTheme(R.style.AppTheme_HighContrast)
        } else {
            setTheme(R.style.AppTheme)
        }

        val darkMode = PreferenceManager.isDarkMode(this)
        AppCompatDelegate.setDefaultNightMode(
            if (darkMode) AppCompatDelegate.MODE_NIGHT_YES
            else AppCompatDelegate.MODE_NIGHT_NO
        )

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
            //val intent = Intent(this, FlowActivity::class.java)
            val intent = Intent(this, SettingsActivity::class.java)
            startActivity(intent)
        }

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }
}