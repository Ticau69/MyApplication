package com.example.firstapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.firstapp.managers.TelemetryManager
import com.example.firstapp.data.LapData
import com.example.firstapp.data.RaceType
import com.example.firstapp.data.SplitData

@Composable
fun RaceFinishDialog(
    finishData: TelemetryManager.RaceFinishData,
    laps: List<LapData> = emptyList(),
    onDismiss: () -> Unit
) {
    val mins = finishData.durationSeconds / 60
    val secs = finishData.durationSeconds % 60

    // Culori fallback sigure în caz că cele din temă lipsesc
    val voltBlue = MaterialTheme.colorScheme.primary
    val brakeRed = Color(0xFFE53935)
    val outlineSlate = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(CutCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surface) // REPARAT: Folosim 'surface' standard
                .border(1.5.dp, outlineSlate, CutCornerShape(16.dp))
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "🏁 FINISH!",
                style = MaterialTheme.typography.headlineLarge.copy( // REPARAT: Folosim headlineLarge (mai compatibil)
                    fontSize = 32.sp,
                    fontStyle = FontStyle.Italic
                ),
                fontWeight = FontWeight.Black,
                color = voltBlue,
                letterSpacing = 2.sp
            )

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                FinishStatItem("TIMP", String.format("%02d:%02d", mins, secs), MaterialTheme.colorScheme.onSurface)
                FinishStatItem("VITEZĂ MAX", "${finishData.maxSpeed} km/h", brakeRed)
                FinishStatItem("DISTANȚĂ", String.format("%.2f km", finishData.distanceKm), voltBlue)
            }

            if (laps.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                Divider(color = outlineSlate) // REPARAT: Divider simplu compatibil cu versiuni mai vechi de Compose
                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "TURURI",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 2.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                val bestLapMs = laps.minOfOrNull { it.lapTimeMs } ?: 0L

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 160.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    laps.forEach { lap ->
                        val isBest = lap.lapTimeMs == bestLapMs

                        // REPARAT: Calculăm formatul timpului pe loc în caz că proprietatea lipsește din model
                        val lMins = (lap.lapTimeMs / 1000) / 60
                        val lSecs = (lap.lapTimeMs / 1000) % 60
                        val formattedTime = String.format("%02d:%02d", lMins, lSecs)

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Tur ${lap.lapNumber}", color = if (isBest) Color(0xFF00E676) else MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(formattedTime, color = if (isBest) Color(0xFF00E676) else MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                            Text("${lap.maxSpeedKmh} km/h", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                shape = CutCornerShape(8.dp)
            ) {
                Text("OK", fontWeight = FontWeight.Black)
            }
        }
    }
}

@Composable
fun FinishStatItem(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = value, fontWeight = FontWeight.ExtraBold, color = color, fontSize = 18.sp)
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = label, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp, letterSpacing = 1.sp)
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF12121D)
@Composable
fun PreviewRaceFinishDialog() {
    // Notă: Dacă aceste clase (RaceFinishData, LapData) nu sunt structurate exact așa în proiect,
    // acest Preview va trebui comentat sau adaptat conform constructorilor reali.
    val mockFinishData = TelemetryManager.RaceFinishData(
        durationSeconds = 418,
        maxSpeed = 156,
        distanceKm = 12.45,
        splits = emptyList(),
        raceType = RaceType.LAP_RACE
    )

    val mockLaps = listOf(
        LapData(lapNumber = 1, lapTimeMs = 142000L, maxSpeedKmh = 145, distanceKm = 4.2),
        LapData(lapNumber = 2, lapTimeMs = 138000L, maxSpeedKmh = 156, distanceKm = 4.2)
    )

    MaterialTheme(colorScheme = darkColorScheme()) {
        RaceFinishDialog(finishData = mockFinishData, laps = mockLaps, onDismiss = {})
    }
}