package com.example.firstapp.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.firstapp.ui.theme.BrakeRed
import com.example.firstapp.ui.theme.PrimaryVoltBlue

@Composable
fun CountdownOverlay(value: Int?) {
    if (value == null) return

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.6f)),
        contentAlignment = Alignment.Center
    ) {
        AnimatedContent(
            targetState = value,
            transitionSpec = {
                (scaleIn(
                    initialScale = 2f,
                    animationSpec = tween(400)
                ) + fadeIn(tween(200))) togetherWith
                        (scaleOut(
                            targetScale = 0.5f,
                            animationSpec = tween(300)
                        ) + fadeOut(tween(200)))
            },
            label = "countdown_anim"
        ) { count ->
            Text(
                text = if (count == 0) "GO!" else count.toString(),
                fontSize = if (count == 0) 96.sp else 120.sp,
                fontWeight = FontWeight.Black,
                fontStyle = FontStyle.Italic,
                color = when (count) {
                    0 -> Color(0xFF00E676)   // Verde — GO!
                    1 -> BrakeRed            // Roșu — tensiune maximă
                    else -> PrimaryVoltBlue  // Albastru — 3, 2
                },
                letterSpacing = if (count == 0) 4.sp else 0.sp
            )
        }
    }
}