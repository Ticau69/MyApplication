package com.example.firstapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.firstapp.AppViewModel
import com.example.firstapp.data.LapData
import com.example.firstapp.ui.theme.BrakeRed
import com.example.firstapp.ui.theme.OutlineSlate
import com.example.firstapp.ui.theme.PrimaryVoltBlue

@Composable
fun RaceFinishDialog(
    finishData: AppViewModel.RaceFinishData,
    laps: List<LapData> = emptyList(),
    onDismiss: () -> Unit
) {
    val mins = finishData.durationSeconds / 60
    val secs = finishData.durationSeconds % 60

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = false // Forțăm utilizatorul să apese OK
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(CutCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceContainer)
                .border(1.5.dp, OutlineSlate.copy(alpha = 0.5f), CutCornerShape(16.dp))
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Titlu
            Text(
                text = "🏁 FINISH!",
                style = MaterialTheme.typography.displayLarge.copy(
                    fontSize = 32.sp,
                    fontStyle = FontStyle.Italic
                ),
                fontWeight = FontWeight.Black,
                color = PrimaryVoltBlue,
                letterSpacing = 2.sp
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Statistici
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                FinishStatItem(
                    label = "TIMP",
                    value = String.format("%02d:%02d", mins, secs),
                    color = Color.White
                )
                FinishStatItem(
                    label = "VITEZĂ MAX",
                    value = "${finishData.maxSpeed} km/h",
                    color = BrakeRed
                )
                FinishStatItem(
                    label = "DISTANȚĂ",
                    value = String.format("%.2f km", finishData.distanceKm),
                    color = PrimaryVoltBlue
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Buton OK
            VelocityPrimaryButton(
                text = "OK",
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }

    if (laps.isNotEmpty()) {
        Spacer(modifier = Modifier.height(16.dp))

        HorizontalDivider(color = OutlineSlate.copy(alpha = 0.3f))

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "TURURI",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            letterSpacing = 2.sp
        )

        Spacer(modifier = Modifier.height(8.dp))

        val bestLapMs = laps.minOf { it.lapTimeMs }

        laps.forEach { lap ->
            val isBest = lap.lapTimeMs == bestLapMs
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Tur ${lap.lapNumber}",
                    color = if (isBest) Color(0xFF00E676)
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = if (isBest) FontWeight.Bold else FontWeight.Normal
                )
                Text(
                    text = lap.formattedTime,
                    color = if (isBest) Color(0xFF00E676)
                    else MaterialTheme.colorScheme.onSurface,
                    fontWeight = if (isBest) FontWeight.Bold else FontWeight.Normal
                )
                Text(
                    text = "${lap.maxSpeedKmh} km/h",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun FinishStatItem(
    label: String,
    value: String,
    color: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.ExtraBold,
            color = color,
            fontSize = 18.sp
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 10.sp,
            letterSpacing = 1.sp
        )
    }
}