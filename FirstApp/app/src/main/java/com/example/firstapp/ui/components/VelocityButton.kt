package com.example.firstapp.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.firstapp.ui.theme.BrakeRed
import com.example.firstapp.ui.theme.OutlineSlate
import com.example.firstapp.ui.theme.PrimaryVoltBlue
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback

// --- THE VELOCITY SHAPE ---
// Creează un paralelogram înclinat la un unghi specific pentru senzația de viteză
val SlantedShape = GenericShape { size, _ ->
    val slantOffset = size.height * 0.35f // Gradul de înclinare
    moveTo(slantOffset, 0f)
    lineTo(size.width, 0f)
    lineTo(size.width - slantOffset, size.height)
    lineTo(0f, size.height)
    close()
}

@Composable
fun VelocityPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isDestructive: Boolean = false
) {
    val containerColor = if (isDestructive) BrakeRed else MaterialTheme.colorScheme.primary
    val contentColor = if (isDestructive) Color.White else MaterialTheme.colorScheme.onPrimary

    val haptics = LocalHapticFeedback.current

    Button(
        onClick = {
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            onClick()
        },
        modifier = modifier
            .padding(4.dp)
            .shadow(
                elevation = 8.dp,
                shape = SlantedShape,
                ambientColor = containerColor,
                spotColor = containerColor
            ),
        shape = SlantedShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor
        ),
        // Adăugăm padding extra pe orizontală pentru a compensa colțurile ascuțite
        contentPadding = PaddingValues(horizontal = 32.dp, vertical = 16.dp)
    ) {
        // Înclinăm și textul puțin (italic) pentru a se potrivi cu forma
        Text(
            text = text.uppercase(),
            style = MaterialTheme.typography.labelLarge.copy(fontStyle = androidx.compose.ui.text.font.FontStyle.Italic),
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 1.sp
        )
    }
}

@Composable
fun VelocityGhostButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptics = LocalHapticFeedback.current

    OutlinedButton(
        onClick = {
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            onClick()
        },
        modifier = modifier.padding(4.dp),
        shape = SlantedShape,
        border = BorderStroke(1.5.dp, OutlineSlate),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        contentPadding = PaddingValues(horizontal = 32.dp, vertical = 16.dp)
    ) {
        Text(
            text = text.uppercase(),
            style = MaterialTheme.typography.labelLarge.copy(fontStyle = androidx.compose.ui.text.font.FontStyle.Italic),
            fontWeight = FontWeight.Bold
        )
    }
}