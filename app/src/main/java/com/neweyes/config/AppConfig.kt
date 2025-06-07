import android.content.Context

object AppConfig {
    var isDarkModeEnabled: Boolean = false
    var isContrastModeEnabled: Boolean = false
    var voiceSpeed: String = "normal" // "lenta", "rápida"
    var defaultLanguage: String = "es" // "en"
    var vibrationIntensity: String = "media" // "suave", "alta"
    var vibrationType: String = "continua" // "intermitente", "pulsante"

    private const val PREFS_NAME = "AppSettings"

    // Cargar desde SharedPreferences
    fun load(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        isDarkModeEnabled = prefs.getBoolean("darkMode", true)
        isContrastModeEnabled = prefs.getBoolean("contrastMode", false)
        defaultLanguage = prefs.getString("language", "es") ?: "es"
        vibrationIntensity = prefs.getString("vibrationIntensity", "media") ?: "media"
        vibrationType = prefs.getString("vibrationType", "continua") ?: "continua"
        voiceSpeed = prefs.getString("voiceSpeed", "normal") ?: "normal"
    }

    // Guardar a SharedPreferences (todos los valores)
    fun save(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().apply {
            putBoolean("darkMode", isDarkModeEnabled)
            putBoolean("contrastMode", isContrastModeEnabled)
            putString("language", defaultLanguage)
            putString("vibrationIntensity", vibrationIntensity)
            putString("vibrationType", vibrationType)
            putString("voiceSpeed", voiceSpeed)
            apply()
        }
    }
}
