import android.content.Context

object AppConfig {
    private var darkMode = false
    private var contrastMode = false
    private var voiceSpeed = "normal"
    private var defaultLanguage = "es"
    private var vibrationIntensity = "media"
    private var vibrationType = "continua"

    private const val PREFS_NAME = "AppSettings"
    private const val KEY_DARK_MODE = "darkMode"
    private const val KEY_CONTRAST_MODE = "contrastMode"
    private const val KEY_LANGUAGE = "language"
    private const val KEY_VIBRATION_INTENSITY = "vibrationIntensity"
    private const val KEY_VIBRATION_TYPE = "vibrationType"
    private const val KEY_VOICE_SPEED = "voiceSpeed"

    // Load settings
    fun load(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        darkMode = prefs.getBoolean(KEY_DARK_MODE, false)
        contrastMode = prefs.getBoolean(KEY_CONTRAST_MODE, false)
        defaultLanguage = prefs.getString(KEY_LANGUAGE, "es") ?: "es"
        vibrationIntensity = prefs.getString(KEY_VIBRATION_INTENSITY, "media") ?: "media"
        vibrationType = prefs.getString(KEY_VIBRATION_TYPE, "continua") ?: "continua"
        voiceSpeed = prefs.getString(KEY_VOICE_SPEED, "normal") ?: "normal"
    }

    // Save settings
    fun save(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().apply {
            putBoolean(KEY_DARK_MODE, darkMode)
            putBoolean(KEY_CONTRAST_MODE, contrastMode)
            putString(KEY_LANGUAGE, defaultLanguage)
            putString(KEY_VIBRATION_INTENSITY, vibrationIntensity)
            putString(KEY_VIBRATION_TYPE, vibrationType)
            putString(KEY_VOICE_SPEED, voiceSpeed)
            apply()
        }
    }

    fun resetToDefaults() {
        darkMode = false
        contrastMode = false
        defaultLanguage = "es"
        vibrationIntensity = "media"
        vibrationType = "continua"
        voiceSpeed = "normal"
    }

    // Getters públicos
    fun isDarkModeEnabled() = darkMode
    fun isContrastModeEnabled() = contrastMode
    fun getVoiceSpeed() = voiceSpeed
    fun getDefaultLanguage() = defaultLanguage
    fun getVibrationIntensity() = vibrationIntensity
    fun getVibrationType() = vibrationType

    // Setters públicos (opcionalmente validables)
    fun setDarkMode(enabled: Boolean) { darkMode = enabled }
    fun setContrastMode(enabled: Boolean) { contrastMode = enabled }
    fun setVoiceSpeed(speed: String) {
        if (speed in listOf("lenta", "normal", "rápida")) voiceSpeed = speed
    }
    fun setDefaultLanguage(lang: String) {
        if (lang in listOf("es", "en")) defaultLanguage = lang
    }
    fun setVibrationIntensity(intensity: String) {
        if (intensity in listOf("suave", "media", "alta")) vibrationIntensity = intensity
    }
    fun setVibrationType(type: String) {
        if (type in listOf("continua", "intermitente", "pulsante")) vibrationType = type
    }

    override fun toString(): String = """
        AppConfig:
        - Modo oscuro: $darkMode
        - Contraste alto: $contrastMode
        - Idioma por defecto: $defaultLanguage
        - Intensidad de vibración: $vibrationIntensity
        - Tipo de vibración: $vibrationType
        - Velocidad de voz: $voiceSpeed
    """.trimIndent()

    fun toSpeechDescription(): String = buildString {
        append("Modo oscuro: ${if (darkMode) "activado" else "desactivado"}. ")
        append("Modo de alto contraste: ${if (contrastMode) "activado" else "desactivado"}. ")
        append("Idioma actual: ${if (defaultLanguage == "es") "Español" else "Inglés"}. ")
        append("Intensidad de vibración: $vibrationIntensity. ")
        append("Tipo de vibración: $vibrationType. ")
        append("Velocidad de voz: $voiceSpeed.")
    }
}
