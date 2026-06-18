package com.example.firstapp.racing

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale
import kotlin.math.abs

class VoiceCopilot(context: Context) : TextToSpeech.OnInitListener {
    private var tts: TextToSpeech? = null
    private var isReady = false

    // Legat de setările aplicației (din TelemetryManager)
    var isEnabled = true

    init {
        tts = TextToSpeech(context, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            // Setăm co-pilotul în Limba Română
            val locale = Locale("ro", "RO")
            val result = tts?.setLanguage(locale)

            // Fallback la engleză dacă pachetul RO nu este instalat pe telefon
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                tts?.setLanguage(Locale.US)
            } else {
                isReady = true
            }
        }
    }

    fun speak(text: String) {
        if (!isEnabled || !isReady || text.isBlank()) return

        // QUEUE_ADD adaugă mesajele la rând pentru a nu se întrerupe unul pe celălalt
        tts?.speak(text, TextToSpeech.QUEUE_ADD, null, null)
    }

    // ── NOU: Funcții inteligente pentru TelemetryManager ──

    fun speakCheckpoint(checkpointName: String, deltaVsBestMs: Long?) {
        if (deltaVsBestMs == null) {
            // Nu avem un record anterior (Ghost), deci anunțăm doar numele punctului
            speak(checkpointName)
            return
        }

        // Convertim milisecundele în secunde cu o zecimală (ex: 1.5)
        val sec = abs(deltaVsBestMs) / 1000f

        // Folosim formatarea US (cu punct, nu cu virgulă) pentru că motoarele TTS
        // știu să citească punctul ca pe o zecimală naturală ("unu virgulă cinci")
        val formattedSec = String.format(Locale.US, "%.1f", sec)

        val message = when {
            deltaVsBestMs < 0 -> "$checkpointName, minus $formattedSec secunde"
            deltaVsBestMs > 0 -> "$checkpointName, plus $formattedSec secunde"
            else -> "$checkpointName, timp egal"
        }

        speak(message)
    }

    fun speakLap(lapNumber: Int, lapTimeMs: Long) {
        val totalSeconds = lapTimeMs / 1000
        val mins = totalSeconds / 60
        val secs = totalSeconds % 60

        val timeText = if (mins > 0) {
            if (mins == 1L) "un minut și $secs secunde"
            else "$mins minute și $secs secunde"
        } else {
            "$secs secunde"
        }

        speak("Turul $lapNumber, $timeText")
    }

    // ── Funcții de curățare ──

    // Alias pentru TelemetryManager
    fun destroy() {
        shutdown()
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
    }
}