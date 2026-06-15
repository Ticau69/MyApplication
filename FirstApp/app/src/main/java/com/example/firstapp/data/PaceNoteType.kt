package com.example.firstapp.data

import androidx.compose.ui.graphics.Color

enum class PaceNoteType(
    val minAngle: Float,
    val maxAngle: Float,
    val color: Color,
    val label: String,
    val ttsText: String,
    val priority: Int       // Pentru TTS — anunțăm doar severitate >= 2
) {
    STRAIGHT(
        minAngle = 0f,
        maxAngle = 10f,
        color    = Color(0xFF00E676),   // Verde neon
        label    = "DREPT",
        ttsText  = "",                  // Nu anunțăm dreptele
        priority = 0
    ),
    EASY_CORNER(
        minAngle = 10f,
        maxAngle = 25f,
        color    = Color(0xFFFFD740),   // Galben
        label    = "CURBĂ UȘOARĂ",
        ttsText  = "curbă ușoară",
        priority = 1
    ),
    TIGHT_CORNER(
        minAngle = 25f,
        maxAngle = 50f,
        color    = Color(0xFFFF6D00),   // Portocaliu
        label    = "CURBĂ STRÂNSĂ",
        ttsText  = "curbă strânsă",
        priority = 2
    ),
    HAIRPIN(
        minAngle = 50f,
        maxAngle = 180f,
        color    = Color(0xFFD50000),   // Roșu aprins
        label    = "AC DE PĂR",
        ttsText  = "ac de păr, frânează",
        priority = 3
    );

    companion object {
        fun fromAngle(angle: Float): PaceNoteType = entries.first { note ->
            angle >= note.minAngle && angle < note.maxAngle
        }
    }
}