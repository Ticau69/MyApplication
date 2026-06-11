package com.example.firstapp.racing

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale
import kotlin.math.abs

class VoiceCopilot(context: Context) : TextToSpeech.OnInitListener {
    private var tts: TextToSpeech? = null
    private var isReady = false

    init {
        tts = TextToSpeech(context, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            // Încercăm să setăm limba pe Română, altfel folosim limba sistemului
            val locale = Locale("ro", "RO")
            val result = tts?.setLanguage(locale)

            if (result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED) {
                isReady = true
            } else {
                tts?.setLanguage(Locale.getDefault())
                isReady = true
            }
        }
    }

    fun speakCheckpoint(checkpointName: String, deltaMs: Long?) {
        if (!isReady) return

        val text = buildString {
            append("$checkpointName. ")

            if (deltaMs != null && deltaMs != 0L) {
                val seconds = abs(deltaMs) / 1000.0
                // Formatăm cu o singură zecimală (ex: 0.5)
                val formattedSeconds = String.format(Locale.getDefault(), "%.1f", seconds)

                if (deltaMs < 0) {
                    append("Minus $formattedSeconds secunde.")
                } else {
                    append("Plus $formattedSeconds secunde.")
                }
            } else if (deltaMs == 0L) {
                append("Timp egal.")
            }
        }

        tts?.speak(text, TextToSpeech.QUEUE_ADD, null, null)
    }

    fun speakLap(lapNumber: Int, lapTimeMs: Long) {
        if (!isReady) return
        val mins = (lapTimeMs / 1000) / 60
        val secs = (lapTimeMs / 1000) % 60

        val text = "Turul $lapNumber complet. Timp: $mins minute și $secs secunde."
        tts?.speak(text, TextToSpeech.QUEUE_ADD, null, null)
    }

    fun destroy() {
        tts?.stop()
        tts?.shutdown()
    }
}