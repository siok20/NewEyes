package com.neweyes

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.speech.tts.TextToSpeech
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.neweyes.databinding.ActivitySettingsBinding
import java.util.*


class SettingsActivity : AppCompatActivity(), TextToSpeech.OnInitListener {

    private lateinit var binding: ActivitySettingsBinding
    private lateinit var prefs: SharedPreferences
    private lateinit var tts: TextToSpeech
    private lateinit var vibrator: Vibrator

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Inflate con View Binding
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Inicializar SharedPreferences
        prefs = getSharedPreferences("settings", Context.MODE_PRIVATE)

        // Inicializar TTS
        tts = TextToSpeech(this, this)

        // Inicializar Vibrator
        vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator

        // 1) CARGAR VALORES PREVIOS
        cargarPreferencias()

        // 2) CONFIGURAR LISTENERS PARA DESCRIPCIONES EN AUDIO (cuando toquen cada label o control)
        asignarDescripcionesAudio()

        binding.btnTestVibration.setOnClickListener {
            // 1) Determinar amplitud según RadioGroup de intensidad
            //    (0–255 es el rango válido para amplitudes en API ≥ 26)
            val intensidadValor = when (binding.rgIntensidad.checkedRadioButtonId) {
                binding.rbMedia.id -> VibrationEffect.DEFAULT_AMPLITUDE / 2  // ~127
                binding.rbAlta.id  -> VibrationEffect.DEFAULT_AMPLITUDE      // 255
                else               -> VibrationEffect.DEFAULT_AMPLITUDE / 4  // ~63
            }.toInt()

            // 2) Verificar tipo de vibración (continua vs intermitente)
            val esContinua = (binding.rgTipo.checkedRadioButtonId == binding.rbContinua.id)

            // 3) Si la versión de Android es >= Oreo (API 26), uso VibrationEffect
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                if (esContinua) {
                    // VIBRACIÓN CONTINUA: un pulso de 300 ms
                    val effect = VibrationEffect.createOneShot(
                        300L,           // duración en ms
                        intensidadValor // amplitud (0–255)
                    )
                    vibrator.vibrate(effect)

                } else {
                    // VIBRACIÓN INTERMITENTE: ON 100 ms, OFF 100 ms, ON 100 ms
                    val pattern = longArrayOf(
                        0L,   // sin retardo inicial
                        100L, // ON 100 ms
                        100L, // OFF 100 ms
                        100L  // ON 100 ms
                    )
                    // amplitudes: el primer 0 es el retardo, luego intensidad/off/intensidad
                    val amplitudes = intArrayOf(
                        0,                // retardo: 0
                        intensidadValor,  // ON
                        0,                // OFF
                        intensidadValor   // ON
                    )
                    val effect = VibrationEffect.createWaveform(pattern, amplitudes, /*repeat*/ -1)
                    vibrator.vibrate(effect)
                }

            } else {
                // 4) Si es API < 26, uso la versión simple vibrator.vibrate(duración)
                if (esContinua) {
                    vibrator.vibrate(300L) // 300 ms de vibración “continua”
                } else {
                    // “Intermitente” ⇒ hago dos pulsos cortos manualmente
                    // (no puedo controlar amplitud, sólo duración y pausas)
                    vibrator.vibrate(longArrayOf(0L, 100L, 100L, 100L), /*repeat*/ -1)
                    // Si quisieras detenerlo manualmente tras un tiempo, podrías usar un Handler.
                    // Por simplicidad, aquí lo dejo corriendo indefinidamente,
                    // pero en API <26 no se controla amplitud y no podemos usar createWaveform con amplitud.
                    // Si prefieres apagarlo después de 300 ms en API <26, podrías programar:
                    // Handler(Looper.getMainLooper()).postDelayed({ vibrator.cancel() }, 300L)
                }
            }
        }



        // 4) SWITCH MODO OSCURO / CLARO
        binding.switchTheme.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            }
        }

        // 5) BOTÓN "Restaurar configuración"
        binding.btnResetDefaults.setOnClickListener {
            prefs.edit().clear().apply()
            cargarPreferencias() // recargar para volver a valores por defecto
        }

        // 6) BOTÓN "Guardar y volver"
        binding.btnGuardar.setOnClickListener {
            guardarPreferencias()
            finish() // cierra esta Activity y vuelve a la anterior
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            // Establecer idioma TTS (por defecto: español de España)
            tts.language = Locale("es", "ES")
            tts.setSpeechRate(obtenerTasaHabla())
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        tts.stop()
        tts.shutdown()
    }

    // -------------------------
    // Función para cargar todas las preferencias
    // -------------------------
    private fun cargarPreferencias() {
        // Idioma
        val idiomaGuardado = prefs.getString("idioma", "Español") ?: "Español"
        val listaIdiomas = listOf("Español", "Inglés")
        val adapter = android.widget.ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            listaIdiomas
        )
        binding.spinnerIdioma.adapter = adapter
        val indexIdioma = listaIdiomas.indexOf(idiomaGuardado).coerceAtLeast(0)
        binding.spinnerIdioma.setSelection(indexIdioma)

        // Intensidad de vibración
        when (prefs.getString("intensidad", "Suave")) {
            "Media" -> binding.rgIntensidad.check(binding.rbMedia.id)
            "Alta" -> binding.rgIntensidad.check(binding.rbAlta.id)
            else -> binding.rgIntensidad.check(binding.rbSuave.id)
        }

        // Tipo de vibración
        when (prefs.getString("tipo", "Continua")) {
            "Intermitente" -> binding.rgTipo.check(binding.rbIntermitente.id)
            else -> binding.rgTipo.check(binding.rbContinua.id)
        }

        // Alto contraste
        binding.switchContraste.isChecked = prefs.getBoolean("contraste", false)

        // Velocidad de voz
        when (prefs.getString("velocidad", "Normal")) {
            "Lenta" -> binding.rgVelocidad.check(binding.rbLenta.id)
            "Rápida" -> binding.rgVelocidad.check(binding.rbRapida.id)
            else -> binding.rgVelocidad.check(binding.rbNormal.id)
        }

        // Tema: modo oscuro
        val modoOscuro = prefs.getBoolean("modo_oscuro", false)
        binding.switchTheme.isChecked = modoOscuro
        if (modoOscuro) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }
    }

    // -------------------------
    // Función para guardar todas las preferencias
    // -------------------------
    private fun guardarPreferencias() {
        val idiomaSeleccionado = binding.spinnerIdioma.selectedItem.toString()
        val intensidadSeleccionada = when (binding.rgIntensidad.checkedRadioButtonId) {
            binding.rbMedia.id -> "Media"
            binding.rbAlta.id -> "Alta"
            else -> "Suave"
        }
        val tipoSeleccionado = if (binding.rgTipo.checkedRadioButtonId == binding.rbIntermitente.id) {
            "Intermitente"
        } else {
            "Continua"
        }
        val contraste = binding.switchContraste.isChecked

        val velocidadSeleccionada = when (binding.rgVelocidad.checkedRadioButtonId) {
            binding.rbLenta.id -> "Lenta"
            binding.rbRapida.id -> "Rápida"
            else -> "Normal"
        }

        val modoOscuro = binding.switchTheme.isChecked

        prefs.edit()
            .putString("idioma", idiomaSeleccionado)
            .putString("intensidad", intensidadSeleccionada)
            .putString("tipo", tipoSeleccionado)
            .putBoolean("contraste", contraste)
            .putString("velocidad", velocidadSeleccionada)
            .putBoolean("modo_oscuro", modoOscuro)
            .apply()
    }

    // -------------------------
    // Devuelve la tasa de habla (TextToSpeech) según la opción seleccionada
    // -------------------------
    private fun obtenerTasaHabla(): Float {
        return when (binding.rgVelocidad.checkedRadioButtonId) {
            binding.rbLenta.id -> 0.75f
            binding.rbRapida.id -> 1.25f
            else -> 1.0f // Normal
        }
    }

    // -------------------------
    // Asigna listeners para que, al tocar cada etiqueta o control, se reproduzca su descripción en audio
    // -------------------------
    private fun asignarDescripcionesAudio() {
        // Etiqueta “Idioma”
        binding.tvIdiomaLabel.setOnClickListener {
            speakOut("Selecciona el idioma de la aplicación")
        }
        // Spinner “Idioma”
        binding.spinnerIdioma.setOnTouchListener { _, _ ->
            speakOut("Lista de idiomas. Desliza y selecciona Español o Inglés")
            false
        }

        // Etiqueta “Intensidad de vibración”
        binding.tvIntensidadLabel.setOnClickListener {
            speakOut("Elige la intensidad de vibración: suave, media o alta")
        }
        // RadioButtons intensidad
        binding.rbSuave.setOnClickListener { speakOut("Vibración suave") }
        binding.rbMedia.setOnClickListener { speakOut("Vibración media") }
        binding.rbAlta.setOnClickListener { speakOut("Vibración alta") }

        // Etiqueta “Tipo de vibración”
        binding.tvTipoLabel.setOnClickListener {
            speakOut("Elige el tipo de vibración: continua o intermitente")
        }
        binding.rbContinua.setOnClickListener { speakOut("Vibración continua") }
        binding.rbIntermitente.setOnClickListener { speakOut("Vibración intermitente") }

        // Etiqueta “Alto contraste”
        binding.tvContrasteLabel.setOnClickListener {
            speakOut("Activa o desactiva el modo de alto contraste")
        }
        binding.switchContraste.setOnClickListener {
            val estado = if (binding.switchContraste.isChecked) "Activado" else "Desactivado"
            speakOut("Alto contraste $estado")
        }

        // Etiqueta “Velocidad de voz”
        binding.tvVelocidadLabel.setOnClickListener {
            speakOut("Ajusta la velocidad de la voz: lenta, normal o rápida")
        }
        binding.rbLenta.setOnClickListener { speakOut("Voz lenta") }
        binding.rbNormal.setOnClickListener { speakOut("Voz normal") }
        binding.rbRapida.setOnClickListener { speakOut("Voz rápida") }

        // Botón “Probar vibración”
        binding.btnTestVibration.setOnClickListener {
            speakOut("Probando vibración")
        }

        // Etiqueta “Modo oscuro”
        binding.tvThemeLabel.setOnClickListener {
            speakOut("Activa o desactiva el modo oscuro")
        }
        binding.switchTheme.setOnClickListener {
            val estado = if (binding.switchTheme.isChecked) "Modo oscuro activado" else "Modo oscuro desactivado"
            speakOut(estado)
        }

        // Botón “Restaurar configuración”
        binding.btnResetDefaults.setOnClickListener {
            speakOut("Restaurando valores por defecto")
        }

        // Botón “Guardar y volver”
        binding.btnGuardar.setOnClickListener {
            speakOut("Guardando cambios y volviendo")
        }
    }

    // -------------------------
    // Función auxiliar para hablar
    // -------------------------
    private fun speakOut(texto: String) {
        if (::tts.isInitialized) {
            tts.setSpeechRate(obtenerTasaHabla())
            tts.speak(texto, TextToSpeech.QUEUE_FLUSH, null, UUID.randomUUID().toString())
        }
    }
}
