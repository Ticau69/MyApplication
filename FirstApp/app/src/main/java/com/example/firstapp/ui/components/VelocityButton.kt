package com.example.firstapp.ui.components

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.firstapp.ui.theme.BrakeRed
import com.example.firstapp.ui.theme.OutlineSlate
import com.example.firstapp.ui.theme.PrimaryVoltBlue

// --- THE VELOCITY SHAPE ---
val SlantedShape = GenericShape { size, _ ->
    val slantOffset = size.height * 0.35f
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

    // Extragem contextul real al aplicației
    val context = LocalContext.current

    Button(
        onClick = {
            // Declanșăm vibrația direct pe componenta hardware
            performHardwareClick(context)
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
        contentPadding = PaddingValues(horizontal = 32.dp, vertical = 16.dp)
    ) {
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
    // Extragem contextul real al aplicației
    val context = LocalContext.current

    OutlinedButton(
        onClick = {
            // Declanșăm vibrația hardware
            performHardwareClick(context)
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

// --- HARDWARE VIBRATION UTILITY ---
private fun performHardwareClick(context: Context) {
    try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            val vibrator = vibratorManager.defaultVibrator
            vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))
        } else {
            @Suppress("DEPRECATION")
            val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(40) // Un impuls fix de 40 de milisecunde
            }
        }
    } catch (e: Exception) {
        // Ignorăm erorile silențios
    }
}