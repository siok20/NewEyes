package com.neweyes

import android.os.Bundle
import android.util.Log
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.neweyes.databinding.ActivitySettingsBinding

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private var TAG = "Settings"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
        setupListeners()
    }

    private fun setupUI() {
        Log.d(TAG, "cargarPreferencias - Inicio")

        // Idioma
        val idiomaGuardado = AppConfig.defaultLanguage
        Log.d(TAG, "Idioma guardado: $idiomaGuardado")
        val listaIdiomas = listOf("Español", "Inglés")
        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            listaIdiomas
        )

        binding.spinnerIdioma.adapter = adapter
        val indexIdioma = listaIdiomas.indexOf(idiomaGuardado).coerceAtLeast(0)
        binding.spinnerIdioma.setSelection(indexIdioma)

        // Intensidad de vibración
        val intensidadGuardada = AppConfig.vibrationIntensity
        Log.d(TAG, "Intensidad guardada: $intensidadGuardada")
        when (intensidadGuardada) {
            "Media" -> binding.rgIntensidad.check(binding.rbMedia.id)
            "Alta"  -> binding.rgIntensidad.check(binding.rbAlta.id)
            else    -> binding.rgIntensidad.check(binding.rbSuave.id)
        }

        // Tipo de vibración
        val tipoGuardado = AppConfig.vibrationType
        Log.d(TAG, "Tipo vibración guardado: $tipoGuardado")
        when (tipoGuardado) {
            "Intermitente" -> binding.rgTipo.check(binding.rbIntermitente.id)
            else           -> binding.rgTipo.check(binding.rbContinua.id)
        }

        // Alto contraste
        val contrasteGuardado = AppConfig.isContrastModeEnabled
        Log.d(TAG, "Contraste guardado: $contrasteGuardado")
        binding.switchContraste.isChecked = contrasteGuardado
        // Aquí podrías aplicar inmediatamente tu tema de alto contraste si lo necesitas

        // Velocidad de voz
        val velocidadGuardada = AppConfig.voiceSpeed
        Log.d(TAG, "Velocidad TTS guardada: $velocidadGuardada")
        when (velocidadGuardada) {
            "Lenta"  -> binding.rgVelocidad.check(binding.rbLenta.id)
            "Rápida" -> binding.rgVelocidad.check(binding.rbRapida.id)
            else     -> binding.rgVelocidad.check(binding.rbNormal.id)
        }

        // Tema: modo oscuro
        val modoOscuro = AppConfig.isDarkModeEnabled
        Log.d(TAG, "Modo oscuro guardado: $modoOscuro")
        binding.switchTheme.isChecked = modoOscuro
        if (modoOscuro) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }
    }

    private fun setupListeners() {
        // Idioma Spinner
        binding.spinnerIdioma.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?, view: android.view.View?, position: Int, id: Long
            ) {
                // TODO: manejar selección de idioma
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                // TODO: manejar nada seleccionado si es necesario
            }
        }

        // Intensidad de vibración RadioGroup
        binding.rgIntensidad.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                binding.rbSuave.id -> {
                    // TODO: vibración suave seleccionada
                }
                binding.rbMedia.id -> {
                    // TODO: vibración media seleccionada
                }
                binding.rbAlta.id -> {
                    // TODO: vibración alta seleccionada
                }
            }
        }

        // Tipo de vibración RadioGroup
        binding.rgTipo.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                binding.rbContinua.id -> {
                    // TODO: vibración continua seleccionada
                }
                binding.rbIntermitente.id -> {
                    // TODO: vibración intermitente seleccionada
                }
                binding.rbPulsante.id -> {
                    // TODO: vibración pulsante seleccionada
                }
            }
        }

        // Switch alto contraste
        binding.switchContraste.setOnCheckedChangeListener { _, isChecked ->
            // TODO: manejar activación/desactivación alto contraste
        }

        // Velocidad de voz RadioGroup
        binding.rgVelocidad.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                binding.rbLenta.id -> {
                    // TODO: velocidad lenta seleccionada
                }
                binding.rbNormal.id -> {
                    // TODO: velocidad normal seleccionada
                }
                binding.rbRapida.id -> {
                    // TODO: velocidad rápida seleccionada
                }
            }
        }

        // Botón probar vibración
        binding.btnTestVibration.setOnClickListener {
            // TODO: implementar prueba de vibración
        }

        // Switch modo oscuro
        binding.switchTheme.setOnCheckedChangeListener { _, isChecked ->
            // TODO: activar/desactivar modo oscuro
        }

        // Botón restaurar configuración
        binding.btnResetDefaults.setOnClickListener {
            // TODO: restaurar configuraciones predeterminadas
        }

        // Botón guardar y volver
        binding.btnGuardar.setOnClickListener {
            // TODO: guardar configuraciones y cerrar Activity
        }
    }
}
