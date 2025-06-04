package com.neweyes

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.os.Vibrator
import android.speech.tts.TextToSpeech
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.neweyes.databinding.ActivitySettingsBinding
import com.neweyes.vibration.*
import java.util.*

class SettingsActivity : BaseActivity(), TextToSpeech.OnInitListener {

    private lateinit var binding: ActivitySettingsBinding
    private lateinit var prefs: SharedPreferences
    private lateinit var tts: TextToSpeech
    private lateinit var vibrator: Vibrator
    private lateinit var vibrationModel: VibrationViewModel

    private var primeraCarga = true

    private val TAG = "SettingsActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        val isDarkMode = PreferenceManager.isDarkMode(this)
        val isHighContrast = PreferenceManager.isHighContrast(this)

        // Configura el modo oscuro ANTES de setTheme()
        AppCompatDelegate.setDefaultNightMode(
            if (isDarkMode) AppCompatDelegate.MODE_NIGHT_YES
            else AppCompatDelegate.MODE_NIGHT_NO
        )

        // Aplica el tema correcto antes de inflar la vista
        if (isDarkMode && isHighContrast) {
            setTheme(R.style.AppTheme_Dark_HighContrast)
        } else if (isDarkMode) {
            setTheme(R.style.AppTheme_Dark)
        } else if (isHighContrast) {
            setTheme(R.style.AppTheme_Light_HighContrast)
        } else {
            setTheme(R.style.AppTheme_Light)
        }

        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate - Inicio")
        Toast.makeText(this, "SettingsActivity: onCreate", Toast.LENGTH_SHORT).show()

        // 0) Inflar con View Binding
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 1) Inicializar SharedPreferences
        prefs = getSharedPreferences("settings", Context.MODE_PRIVATE)
        Log.d(TAG, "SharedPreferences inicializadas")

        // 2) Inicializar TTS
        tts = TextToSpeech(this, this)
        Log.d(TAG, "TextToSpeech inicializado (pendiente onInit)")

        // 3) Inicializar Vibrator
        vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        Log.d(TAG, "Vibrator inicializado")

        // 4) Cargar valores previos en los controles
        cargarPreferencias()

        // 5) Asignar descripciones en audio para cada control
        asignarDescripcionesAudio()

        // 6) Listener unificado para botón “Probar vibración”
        val vibrationManager = VibrationManager(this)
        vibrationModel = VibrationViewModel(vibrationManager)

        binding.btnTestVibration.setOnClickListener {
            VibrationSettings.type = when (binding.rgTipo.checkedRadioButtonId) {
                binding.rbContinua.id -> VibrationType.CONTINUA
                binding.rbIntermitente.id -> VibrationType.INTERMITENTE
                binding.rbPulsante.id -> VibrationType.PULSANTE
                else -> VibrationType.CONTINUA
            }

            VibrationSettings.intensity = when (binding.rgIntensidad.checkedRadioButtonId) {
                binding.rbMedia.id -> VibrationIntensity.MEDIA
                binding.rbAlta.id -> VibrationIntensity.ALTA
                else -> VibrationIntensity.SUAVE
            }

            vibrationModel.testVibration()
        }



        // 7) Switch Modo Oscuro / Claro
        binding.switchTheme.setOnCheckedChangeListener(null)
        binding.switchTheme.isChecked = isDarkMode
        binding.switchTheme.setOnCheckedChangeListener { _, isChecked ->
            Log.d(TAG, "switchTheme cambiado: isChecked=$isChecked")
            Toast.makeText(this, "Modo oscuro: $isChecked", Toast.LENGTH_SHORT).show()

            if (!primeraCarga && isChecked != isDarkMode) {
                PreferenceManager.setDarkMode(this, isChecked)
                recreate()
            } else if (primeraCarga) {
                // Solo guardar al inicio para que se mantenga sin recrear
                PreferenceManager.setDarkMode(this, isChecked)
            }
        }


        //8)
        binding.switchContraste.setOnCheckedChangeListener(null)
        binding.switchContraste.isChecked = isHighContrast
        binding.switchContraste.setOnCheckedChangeListener { _, isChecked ->
            if (!primeraCarga && isChecked != isHighContrast) {
                PreferenceManager.setHighContrast(this, isChecked)
                recreate()
            } else if (primeraCarga) {
                PreferenceManager.setHighContrast(this, isChecked)
            }
        }

        primeraCarga = false



        // 9) Botón “Restaurar configuración”
        binding.btnResetDefaults.setOnClickListener {
            Log.d(TAG, "btnResetDefaults clickeado")
            Toast.makeText(this, "Restaurando valores por defecto", Toast.LENGTH_SHORT).show()
            prefs.edit().clear().apply()
            Log.d(TAG, "SharedPreferences limpiadas")
            cargarPreferencias()
        }

        // 10) Botón “Guardar y volver”
        binding.btnGuardar.setOnClickListener {
            Log.d(TAG, "btnGuardar clickeado")
            guardarPreferencias()
            Toast.makeText(this, "Guardando cambios y volviendo", Toast.LENGTH_SHORT).show()
            finish()
        }

        Log.d(TAG, "onCreate - Fin")
    }

    override fun onInit(status: Int) {
        Log.d(TAG, "onInit de TextToSpeech - status = $status")
        Toast.makeText(this, "TTS onInit status: $status", Toast.LENGTH_SHORT).show()
        if (status == TextToSpeech.SUCCESS) {
            // Establecer idioma TTS (por defecto: español de España)
            val resultado = tts.setLanguage(Locale("es", "ES"))
            Log.d(TAG, "TTS setLanguage resultado = $resultado")
            if (resultado == TextToSpeech.LANG_MISSING_DATA || resultado == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e(TAG, "Idioma TTS no soportado o faltan datos")
                Toast.makeText(this, "Idioma TTS no soportado", Toast.LENGTH_LONG).show()
            }
            // Ajustar tasa inicial según lo que haya cargado en las preferencias
            val tasaInicial = obtenerTasaHabla()
            tts.setSpeechRate(tasaInicial)
            Log.d(TAG, "TTS setSpeechRate = $tasaInicial")
        } else {
            Log.e(TAG, "Error al inicializar TTS (status != SUCCESS)")
            Toast.makeText(this, "Error al inicializar TTS", Toast.LENGTH_LONG).show()
        }
    }

    override fun onDestroy() {
        Log.d(TAG, "onDestroy - limpiando TTS y Vibrator")
        tts.stop()
        tts.shutdown()
        vibrator.cancel() // Detener cualquier vibración residual
        super.onDestroy()
    }

    override fun recreate() {
        overridePendingTransition(0, 0)
        super.recreate()
        overridePendingTransition(0, 0)
    }


    // -------------------------
    // Carga todas las preferencias y actualiza los controles
    // -------------------------
    private fun cargarPreferencias() {
        Log.d(TAG, "cargarPreferencias - Inicio")
        Toast.makeText(this, "cargarPreferencias", Toast.LENGTH_SHORT).show()

        // Idioma
        val idiomaGuardado = prefs.getString("idioma", "Español") ?: "Español"
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
        val intensidadGuardada = prefs.getString("intensidad", "Suave")
        Log.d(TAG, "Intensidad guardada: $intensidadGuardada")
        when (intensidadGuardada) {
            "Media" -> binding.rgIntensidad.check(binding.rbMedia.id)
            "Alta"  -> binding.rgIntensidad.check(binding.rbAlta.id)
            else    -> binding.rgIntensidad.check(binding.rbSuave.id)
        }

        // Tipo de vibración
        val tipoGuardado = prefs.getString("tipo", "Continua")
        Log.d(TAG, "Tipo vibración guardado: $tipoGuardado")
        when (tipoGuardado) {
            "Intermitente" -> binding.rgTipo.check(binding.rbIntermitente.id)
            else           -> binding.rgTipo.check(binding.rbContinua.id)
        }

        // Alto contraste
        val contrasteGuardado = prefs.getBoolean("contraste", false)
        Log.d(TAG, "Contraste guardado: $contrasteGuardado")
        binding.switchContraste.isChecked = contrasteGuardado
        // Aquí podrías aplicar inmediatamente tu tema de alto contraste si lo necesitas

        // Velocidad de voz
        val velocidadGuardada = prefs.getString("velocidad", "Normal")
        Log.d(TAG, "Velocidad TTS guardada: $velocidadGuardada")
        when (velocidadGuardada) {
            "Lenta"  -> binding.rgVelocidad.check(binding.rbLenta.id)
            "Rápida" -> binding.rgVelocidad.check(binding.rbRapida.id)
            else     -> binding.rgVelocidad.check(binding.rbNormal.id)
        }

        // Tema: modo oscuro
        val modoOscuro = prefs.getBoolean("modo_oscuro", false)
        Log.d(TAG, "Modo oscuro guardado: $modoOscuro")
        binding.switchTheme.isChecked = modoOscuro
        if (modoOscuro) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }

        Log.d(TAG, "cargarPreferencias - Fin")
    }

    // -------------------------
    // Guarda todas las preferencias según el estado actual de los controles
    // -------------------------
    private fun guardarPreferencias() {
        Log.d(TAG, "guardarPreferencias - Inicio")
        Toast.makeText(this, "guardando preferencias...", Toast.LENGTH_SHORT).show()

        val idiomaSeleccionado = binding.spinnerIdioma.selectedItem.toString()
        Log.d(TAG, "Idioma seleccionado para guardar: $idiomaSeleccionado")

        val intensidadSeleccionada = when (binding.rgIntensidad.checkedRadioButtonId) {
            binding.rbMedia.id -> "Media"
            binding.rbAlta.id  -> "Alta"
            else               -> "Suave"
        }
        Log.d(TAG, "Intensidad seleccionada para guardar: $intensidadSeleccionada")

        val tipoSeleccionado = if (binding.rgTipo.checkedRadioButtonId == binding.rbIntermitente.id) {
            "Intermitente"
        } else {
            "Continua"
        }
        Log.d(TAG, "Tipo de vibración seleccionado para guardar: $tipoSeleccionado")

        val contraste = binding.switchContraste.isChecked
        Log.d(TAG, "Contraste para guardar: $contraste")

        val velocidadSeleccionada = when (binding.rgVelocidad.checkedRadioButtonId) {
            binding.rbLenta.id  -> "Lenta"
            binding.rbRapida.id -> "Rápida"
            else                -> "Normal"
        }
        Log.d(TAG, "Velocidad TTS para guardar: $velocidadSeleccionada")

        val modoOscuro = binding.switchTheme.isChecked
        Log.d(TAG, "Modo oscuro para guardar: $modoOscuro")

        prefs.edit()
            .putString("idioma", idiomaSeleccionado)
            .putString("intensidad", intensidadSeleccionada)
            .putString("tipo", tipoSeleccionado)
            .putBoolean("contraste", contraste)
            .putString("velocidad", velocidadSeleccionada)
            .putBoolean("modo_oscuro", modoOscuro)
            .apply()
        Log.d(TAG, "guardarPreferencias - Fin")
    }

    // -------------------------
    // Devuelve la tasa de habla (TextToSpeech) según la opción seleccionada
    // -------------------------
    private fun obtenerTasaHabla(): Float {
        return when (binding.rgVelocidad.checkedRadioButtonId) {
            binding.rbLenta.id  -> 0.75f
            binding.rbRapida.id -> 1.25f
            else                -> 1.0f // Normal
        }
    }

    // -------------------------
    // Asigna listeners para que, al interactuar con cada control, se reproduzca su descripción en audio
    // -------------------------
    private fun asignarDescripcionesAudio() {
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
                speakOut("Idioma seleccionado: $idioma")
            }
        }

        // Etiqueta “Idioma”
        binding.tvIdiomaLabel.setOnClickListener {
            Log.d(TAG, "tvIdiomaLabel clickeado")
            speakOut("Selecciona el idioma de la aplicación")
        }

        // Etiqueta “Intensidad de vibración”
        binding.tvIntensidadLabel.setOnClickListener {
            Log.d(TAG, "tvIntensidadLabel clickeado")
            speakOut("Elige la intensidad de vibración: suave, media o alta")
        }
        binding.rbSuave.setOnClickListener {
            Log.d(TAG, "rbSuave clickeado")
            speakOut("Vibración suave")
        }
        binding.rbMedia.setOnClickListener {
            Log.d(TAG, "rbMedia clickeado")
            speakOut("Vibración media")
        }
        binding.rbAlta.setOnClickListener {
            Log.d(TAG, "rbAlta clickeado")
            speakOut("Vibración alta")
        }

        // Etiqueta “Tipo de vibración”
        binding.tvTipoLabel.setOnClickListener {
            Log.d(TAG, "tvTipoLabel clickeado")
            speakOut("Elige el tipo de vibración: continua o intermitente")
        }
        binding.rbContinua.setOnClickListener {
            Log.d(TAG, "rbContinua clickeado")
            speakOut("Vibración continua")
        }
        binding.rbIntermitente.setOnClickListener {
            Log.d(TAG, "rbIntermitente clickeado")
            speakOut("Vibración intermitente")
        }

        // Etiqueta “Alto contraste”
        binding.tvContrasteLabel.setOnClickListener {
            Log.d(TAG, "tvContrasteLabel clickeado")
            speakOut("Activa o desactiva el modo de alto contraste")
        }

        // Etiqueta “Velocidad de voz”
        binding.tvVelocidadLabel.setOnClickListener {
            Log.d(TAG, "tvVelocidadLabel clickeado")
            speakOut("Ajusta la velocidad de la voz: lenta, normal o rápida")
        }
        binding.rbLenta.setOnClickListener {
            Log.d(TAG, "rbLenta clickeado")
            tts.setSpeechRate(0.75f)
            speakOut("Voz lenta")
        }
        binding.rbNormal.setOnClickListener {
            Log.d(TAG, "rbNormal clickeado")
            tts.setSpeechRate(1.0f)
            speakOut("Voz normal")
        }
        binding.rbRapida.setOnClickListener {
            Log.d(TAG, "rbRapida clickeado")
            tts.setSpeechRate(1.25f)
            speakOut("Voz rápida")
        }

        // Etiqueta “Modo oscuro”
        binding.tvThemeLabel.setOnClickListener {
            Log.d(TAG, "tvThemeLabel clickeado")
            speakOut("Activa o desactiva el modo oscuro")
        }

        // Botón “Restaurar configuración”
        binding.btnResetDefaults.setOnClickListener {
            Log.d(TAG, "btnResetDefaults (en asignarDescripcionesAudio) clickeado")
            speakOut("Restaurando valores por defecto")
        }

        // Botón “Guardar y volver”
        binding.btnGuardar.setOnClickListener {
            Log.d(TAG, "btnGuardar (en asignarDescripcionesAudio) clickeado")
            speakOut("Guardando cambios y volviendo")
        }

        Log.d(TAG, "asignarDescripcionesAudio - Fin")
    }

    // -------------------------
    // Función auxiliar para hablar
    // -------------------------
    private fun speakOut(texto: String) {
        if (::tts.isInitialized) {
            val tasa = obtenerTasaHabla()
            tts.setSpeechRate(tasa)
            Log.d(TAG, "speakOut: \"$texto\" con tasa=$tasa")
            tts.speak(texto, TextToSpeech.QUEUE_FLUSH, null, UUID.randomUUID().toString())
        } else {
            Log.e(TAG, "speakOut: TTS no inicializado")
        }
    }
}
