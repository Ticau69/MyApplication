package com.example.firstapp.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Vom folosi FontFamily.Default momentan.
// După ce descarci Sora, vom înlocui cu FontFamily(Font(R.font.sora_bold))
val Typography = Typography(
    // Folosit pentru alertele critice (Full-screen edge glow text)
    displayLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 57.sp,
        lineHeight = 64.sp,
        letterSpacing = (-0.25).sp
    ),
    // Folosit pentru butoanele Primary și Navigation
    labelLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 16.sp, // Mărit pentru ergonomie HUD
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    ),
    // Text standard pentru liste și telemetrie
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    )
)