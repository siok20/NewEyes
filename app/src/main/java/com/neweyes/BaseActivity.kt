package com.neweyes

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

abstract class BaseActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // Aplica el tema antes de cualquier otra cosa
        if (PreferenceManager.isHighContrast(this)) {
            setTheme(R.style.AppTheme_HighContrast)
        } else {
            setTheme(R.style.AppTheme)
        }
        super.onCreate(savedInstanceState)
    }
}
