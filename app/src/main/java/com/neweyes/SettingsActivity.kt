package com.neweyes

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.neweyes.databinding.ActivitySettingsBinding
import com.neweyes.vibration.VibrationIntensity
import com.neweyes.vibration.VibrationManager
import com.neweyes.vibration.VibrationType
import com.neweyes.voice.TextToSpeechHelper
import com.neweyes.voice.VoiceSpeed

class SettingsActivity : AppCompatActivity() {

    private lateinit var ttsHelper : TextToSpeechHelper
    private lateinit var vibrateHelper : VibrationManager

    private lateinit var binding: ActivitySettingsBinding
    private var TAG = "SettingsActivityLOG"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ttsHelper = TextToSpeechHelper(this)
        vibrateHelper = VibrationManager(this)

        setupUI()
        setupListeners()
        setUpDescriptions()
    }

    private fun setupUI() {
        Log.d(TAG, "cargarPreferencias - Inicio")

        // Idioma
        val idiomaGuardado = AppConfig.getDefaultLanguage()

        Log.d(TAG, "Idioma guardado: $idiomaGuardado")

        val listaIdiomas = listOf("Español", "Inglés")
        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            listaIdiomas
        )

        binding.spinnerIdioma.adapter = adapter

        val idiomaMostrado = when (idiomaGuardado) {
            "es" -> "Español"
            "en" -> "Inglés"
            else -> "Español"
        }
        val indexIdioma = listaIdiomas.indexOf(idiomaMostrado).coerceAtLeast(0)
        binding.spinnerIdioma.setSelection(indexIdioma)

        // Intensidad de vibración
        val intensidadGuardada = AppConfig.getVibrationIntensity()
        Log.d(TAG, "Intensidad guardada: $intensidadGuardada")
        when (intensidadGuardada) {
            "media" -> binding.rgIntensidad.check(binding.rbMedia.id)
            "alta"  -> binding.rgIntensidad.check(binding.rbAlta.id)
            else    -> binding.rgIntensidad.check(binding.rbSuave.id)
        }

        // Tipo de vibración
        val tipoGuardado = AppConfig.getVibrationType()
        Log.d(TAG, "Tipo vibración guardado: $tipoGuardado")
        when (tipoGuardado) {
            "intermitente" -> binding.rgTipo.check(binding.rbIntermitente.id)
            "pulsante" -> binding.rgTipo.check(binding.rbPulsante.id)
            else           -> binding.rgTipo.check(binding.rbContinua.id)
        }

        // Alto contraste
        val contrasteGuardado = AppConfig.isContrastModeEnabled()
        Log.d(TAG, "Contraste guardado: $contrasteGuardado")
        binding.switchContraste.isChecked = contrasteGuardado
        // Aquí podrías aplicar inmediatamente tu tema de alto contraste si lo necesitas

        // Velocidad de voz
        val velocidadGuardada = AppConfig.getVoiceSpeed()
        Log.d(TAG, "Velocidad TTS guardada: $velocidadGuardada")
        when (velocidadGuardada) {
            "lenta"  -> binding.rgVelocidad.check(binding.rbLenta.id)
            "rapida" -> binding.rgVelocidad.check(binding.rbRapida.id)
            else     -> binding.rgVelocidad.check(binding.rbNormal.id)
        }

        // Tema: modo oscuro
        val modoOscuro = AppConfig.isDarkModeEnabled()
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
                    vibrateHelper.setIntensity(VibrationIntensity.SUAVE)
                }
                binding.rbMedia.id -> {
                    vibrateHelper.setIntensity(VibrationIntensity.MEDIA)
                }
                binding.rbAlta.id -> {
                    vibrateHelper.setIntensity(VibrationIntensity.ALTA)
                }
            }
        }

        // Tipo de vibración RadioGroup
        binding.rgTipo.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                binding.rbContinua.id -> {
                    vibrateHelper.setType(VibrationType.CONTINUA)
                }
                binding.rbIntermitente.id -> {
                    vibrateHelper.setType(VibrationType.INTERMITENTE)
                }
                binding.rbPulsante.id -> {
                    vibrateHelper.setType(VibrationType.PULSANTE)
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
                    ttsHelper.setSpeed(VoiceSpeed.SLOW)
                }
                binding.rbNormal.id -> {
                    ttsHelper.setSpeed(VoiceSpeed.NORMAL)
                }
                binding.rbRapida.id -> {
                    ttsHelper.setSpeed(VoiceSpeed.FAST)
                }
            }
        }

        // Botón probar vibración
        binding.btnTestVibration.setOnClickListener {
            vibrateHelper.vibrate()
        }

        // Switch modo oscuro
        binding.switchTheme.setOnCheckedChangeListener { _, isChecked ->
            // TODO: activar/desactivar modo oscuro
        }

        // Botón restaurar configuración
        binding.btnResetDefaults.setOnClickListener {
            AppConfig.resetToDefaults()
            ttsHelper = TextToSpeechHelper(this)
            vibrateHelper = VibrationManager(this)

            setupUI()

            ttsHelper.speak("Configuraciones restablecidas a los valores predeterminados")

            Log.d(TAG, "btnResetDefaults (en asignarDescripcionesAudio) clickeado")
        }

        // Botón guardar y volver
        binding.btnGuardar.setOnClickListener {
            try {
                // 1. Idioma
                val idiomaSeleccionado = binding.spinnerIdioma.selectedItem.toString()
                val codigoIdioma = when (idiomaSeleccionado.lowercase()) {
                    "español" -> "es"
                    "inglés" -> "en"
                    else -> "es"
                }
                AppConfig.setDefaultLanguage(codigoIdioma)

                // 2. Intensidad de vibración
                val intensidad = when (binding.rgIntensidad.checkedRadioButtonId) {
                    binding.rbMedia.id -> "media"
                    binding.rbAlta.id -> "alta"
                    else -> "suave"
                }
                AppConfig.setVibrationIntensity(intensidad)

                // 3. Tipo de vibración
                val tipo = when (binding.rgTipo.checkedRadioButtonId) {
                    binding.rbIntermitente.id -> "intermitente"
                    binding.rbPulsante.id -> "pulsante"
                    else -> "continua"
                }
                AppConfig.setVibrationType(tipo)

                // 4. Alto contraste
                AppConfig.setContrastMode(binding.switchContraste.isChecked)

                // 5. Velocidad de voz
                val velocidad = when (binding.rgVelocidad.checkedRadioButtonId) {
                    binding.rbLenta.id -> "lenta"
                    binding.rbRapida.id -> "rápida"
                    else -> "normal"
                }
                AppConfig.setVoiceSpeed(velocidad)

                // 6. Tema oscuro
                val modoOscuro = binding.switchTheme.isChecked
                AppConfig.setDarkMode(modoOscuro)
                AppCompatDelegate.setDefaultNightMode(
                    if (modoOscuro) AppCompatDelegate.MODE_NIGHT_YES
                    else AppCompatDelegate.MODE_NIGHT_NO
                )

                // 7. Guardar y confirmar
                AppConfig.save(this)

                ttsHelper.speak("Configuraciones guardadas")
                Log.d(TAG, "SAVE: \n$AppConfig")

                finish()

            } catch (e: IllegalArgumentException) {
                Toast.makeText(this, "Error al guardar configuración: ${e.message}", Toast.LENGTH_LONG).show()
                Log.e(TAG, "Error al guardar configuración", e)
                ttsHelper.speak("Ocurrió un error al guardar las configuraciones")
            }
        }
    }

    // -------------------------
    // Asigna listeners para que, al interactuar con cada control, se reproduzca su descripción en audio
    // -------------------------
    private fun setUpDescriptions() {
        Log.d(TAG, "asignarDescripcionesAudio - Inicio")

        // Spinner “Idioma”: cuando cambie la selección, describirlo
        binding.spinnerIdioma.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) {
                Log.d(TAG, "Spinner idioma: onNothingSelected")
            }
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                val idioma = parent?.getItemAtPosition(position).toString()
                Log.d(TAG, "Spinner idioma - onItemSelected: $idioma")
                ttsHelper.speak("Idioma seleccionado: $idioma")
            }
        }

        // Etiqueta “Idioma”
        binding.tvIdiomaLabel.setOnClickListener {
            Log.d(TAG, "tvIdiomaLabel clickeado")
            ttsHelper.speak("Selecciona el idioma de la aplicación")
        }

        // Etiqueta “Intensidad de vibración”
        binding.tvIntensidadLabel.setOnClickListener {
            Log.d(TAG, "tvIntensidadLabel clickeado")
            ttsHelper.speak("Elige la intensidad de vibración: suave, media o alta")
        }
        binding.rbSuave.setOnClickListener {
            Log.d(TAG, "rbSuave clickeado")
            ttsHelper.speak("Vibración suave")
        }
        binding.rbMedia.setOnClickListener {
            Log.d(TAG, "rbMedia clickeado")
            ttsHelper.speak("Vibración media")
        }
        binding.rbAlta.setOnClickListener {
            Log.d(TAG, "rbAlta clickeado")
            ttsHelper.speak("Vibración alta")
        }

        // Etiqueta “Tipo de vibración”
        binding.tvTipoLabel.setOnClickListener {
            Log.d(TAG, "tvTipoLabel clickeado")
            ttsHelper.speak("Elige el tipo de vibración: continua o intermitente")
        }
        binding.rbContinua.setOnClickListener {
            Log.d(TAG, "rbContinua clickeado")
            ttsHelper.speak("Vibración continua")
        }
        binding.rbIntermitente.setOnClickListener {
            Log.d(TAG, "rbIntermitente clickeado")
            ttsHelper.speak("Vibración intermitente")
        }

        // Etiqueta “Alto contraste”
        binding.tvContrasteLabel.setOnClickListener {
            Log.d(TAG, "tvContrasteLabel clickeado")
            ttsHelper.speak("Activa o desactiva el modo de alto contraste")
        }

        // Etiqueta “Velocidad de voz”
        binding.tvVelocidadLabel.setOnClickListener {
            Log.d(TAG, "tvVelocidadLabel clickeado")
            ttsHelper.speak("Ajusta la velocidad de la voz: lenta, normal o rápida")
        }
        binding.rbLenta.setOnClickListener {
            Log.d(TAG, "rbLenta clickeado")
            ttsHelper.setSpeed(VoiceSpeed.SLOW)
            ttsHelper.speak("Voz lenta")
        }
        binding.rbNormal.setOnClickListener {
            Log.d(TAG, "rbNormal clickeado")
            ttsHelper.setSpeed(VoiceSpeed.NORMAL)
            ttsHelper.speak("Voz normal")
        }
        binding.rbRapida.setOnClickListener {
            Log.d(TAG, "rbRapida clickeado")
            ttsHelper.setSpeed(VoiceSpeed.FAST)
            ttsHelper.speak("Voz rápida")
        }

        // Etiqueta “Modo oscuro”
        binding.tvThemeLabel.setOnClickListener {
            Log.d(TAG, "tvThemeLabel clickeado")
            ttsHelper.speak("Activa o desactiva el modo oscuro")
        }

        Log.d(TAG, "asignarDescripcionesAudio - Fin")
    }
}
