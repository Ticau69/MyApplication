package com.example.firstapp.managers

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class SettingsManager(context: Context) {

    // Inițializăm fișierul SharedPreferences dedicat setărilor
    private val prefs = context.getSharedPreferences("velocity_settings", Context.MODE_PRIVATE)

    // Starea reactivă pentru Copilotul Audio (Implicit: TRUE)
    private val _isTtsEnabled = MutableStateFlow(prefs.getBoolean("tts_enabled", true))
    val isTtsEnabled = _isTtsEnabled.asStateFlow()

    // Starea reactivă pentru raza radarului de proximitate (Implicit: 200 metri)
    private val _proximityRadius = MutableStateFlow(prefs.getInt("proximity_radius", 200))
    val proximityRadius = _proximityRadius.asStateFlow()

    /**
     * Activează sau dezactivează Copilotul Vocal (TTS) și salvează starea în memorie.
     */
    fun setTtsEnabled(enabled: Boolean) {
        _isTtsEnabled.value = enabled
        prefs.edit().putBoolean("tts_enabled", enabled).apply()
    }

    /**
     * Modifică raza de scanare a curselor din apropiere și o persistează.
     */
    fun setProximityRadius(radius: Int) {
        _proximityRadius.value = radius
        prefs.edit().putInt("proximity_radius", radius).apply()
    }
}