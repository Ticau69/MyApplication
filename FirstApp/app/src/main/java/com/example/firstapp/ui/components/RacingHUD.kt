package com.example.firstapp.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.firstapp.AppViewModel
import com.example.firstapp.data.LapData
import com.example.firstapp.data.RaceType
import com.example.firstapp.data.SplitData
import com.example.firstapp.data.Track
import com.example.firstapp.ui.theme.BrakeRed
import com.example.firstapp.ui.theme.FirstAppTheme
import com.huawei.hms.maps.model.LatLng

val maxG = 2f



@Composable
fun RacingHUD(
    speed: Int,
    currentLatLng: LatLng?,
    selectedTrack: Track?,
    currentLapTimeMs: Long,
    lastLapNotification: com.example.firstapp.managers.TelemetryManager.LapNotification?,
    allLaps: List<LapData>,
    currentSplit: SplitData?,
    sprintProgress: Float,
    hasRaceStarted: Boolean = true,
    onStopClick: () -> Unit,
    gForceX: Float = 0f, // NOU
    gForceY: Float = 0f, // NOU
    modifier: Modifier = Modifier,
    ghostDeltaMs: Long? = null
) {
    // Formatăm timpul în MM:SS.CC
    val mins = currentLapTimeMs / 60000
    val secs = (currentLapTimeMs % 60000) / 1000
    val millis = (currentLapTimeMs % 1000) / 10
    val timeString = String.format("%02d:%02d.%02d", mins, secs, millis)

    val raceType = selectedTrack?.raceType ?: RaceType.SPRINT
    val currentLap = (allLaps.size + 1)



    Box(modifier = modifier.fillMaxSize()) {

        // ==========================================
        // 1. ZONA SUS-STÂNGA: Cronometrul (Timp Total)
        // ==========================================
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(top = 32.dp, start = 16.dp)
                .background(
                    MaterialTheme.colorScheme.surfaceContainerLowest.copy(alpha = 0.85f),
                    CutCornerShape(bottomEnd = 16.dp)
                )
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CutCornerShape(bottomEnd = 16.dp))
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            Column(horizontalAlignment = Alignment.Start) {
                if (hasRaceStarted) {
                    Text("TIME", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                    Text(timeString, color = MaterialTheme.colorScheme.onSurface, fontSize = 24.sp, fontWeight = FontWeight.Black, fontStyle = FontStyle.Italic)
                } else {
                    Text("STATUS", color = MaterialTheme.colorScheme.primary, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                    Text("READY...", color = Color(0xFFFFD700), fontSize = 24.sp, fontWeight = FontWeight.Black, fontStyle = FontStyle.Italic)
                }
            }
        }

        //GHOST
        ghostDeltaMs?.let { delta ->
            val isFaster = delta <= 0
            val absMs = kotlin.math.abs(delta)
            val secs = absMs / 1000
            val millis = (absMs % 1000) / 10
            val prefix = if (isFaster) "▲ -" else "▼ +"
            val deltaColor = when {
                delta < -1000 -> Color(0xFF00E676)  // Verde intens — >1s avantaj
                delta < 0     -> Color(0xFF69F0AE)  // Verde deschis
                delta < 1000  -> Color(0xFFFFD740)  // Galben — aproape egal
                else          -> BrakeRed           // Roșu — în urmă
            }

            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(top = 100.dp, start = 16.dp)
                    .background(
                        MaterialTheme.colorScheme.surfaceContainerLowest.copy(alpha = 0.85f),
                        CutCornerShape(bottomEnd = 12.dp)
                    )
                    .border(
                        1.dp,
                        deltaColor.copy(alpha = 0.5f),
                        CutCornerShape(bottomEnd = 12.dp)
                    )
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Column(horizontalAlignment = Alignment.Start) {
                    Text(
                        text = "GHOST",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp
                    )
                    Text(
                        text = "$prefix${secs}.${String.format("%02d", millis)}",
                        color = deltaColor,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black,
                        fontStyle = FontStyle.Italic
                    )
                }
            }
        }

        // ==========================================
        // 2. ZONA SUS-CENTRU: Vitezometrul Tactic
        // ==========================================
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 24.dp)
                .background(
                    MaterialTheme.colorScheme.surfaceContainerLowest.copy(alpha = 0.9f),
                    CutCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp)
                )
                .border(
                    1.dp,
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                    CutCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp)
                )
                .padding(horizontal = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = speed.toString(),
                color = MaterialTheme.colorScheme.primary, // Folosim VoltBlue pentru vizibilitate agresivă
                fontSize = 50.sp,
                fontWeight = FontWeight.Black,
                fontStyle = FontStyle.Italic,
                letterSpacing = (2).sp
            )
            Text(
                text = "KM/H",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 10.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 2.sp
            )
        }

        // ==========================================
        // 3. ZONA SUS-DREAPTA: Progres Traseu / Lap-uri
        // ==========================================
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 32.dp, end = 16.dp)
                .background(
                    MaterialTheme.colorScheme.surfaceContainerLowest.copy(alpha = 0.85f),
                    CutCornerShape(bottomStart = 16.dp)
                )
                .border(
                    1.dp,
                    MaterialTheme.colorScheme.outlineVariant,
                    CutCornerShape(bottomStart = 16.dp)
                )
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            Column(horizontalAlignment = Alignment.End) {
                if (raceType == RaceType.LAP_RACE) {
                    Text("LAP", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                    Text(currentLap.toString(), color = MaterialTheme.colorScheme.onSurface, fontSize = 24.sp, fontWeight = FontWeight.Black, fontStyle = FontStyle.Italic)
                } else {
                    val percentage = (sprintProgress * 100).toInt().coerceIn(0, 100)
                    Text("PROGRESS", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                    Text("$percentage%", color = MaterialTheme.colorScheme.onSurface, fontSize = 24.sp, fontWeight = FontWeight.Black, fontStyle = FontStyle.Italic)
                }
            }
        }

        // ==========================================
        // 4. ZONA JOS-STÂNGA: Buton Oprire Urgență/Finish
        // ==========================================
        if(selectedTrack == null) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(start = 0.dp, bottom = 32.dp)
            ) {
                Button(
                    onClick = onStopClick,
                    shape = CutCornerShape(topStart = 56.dp, bottomEnd = 56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = BrakeRed,
                        contentColor = Color.White
                    ),
                    contentPadding = PaddingValues(horizontal = 64.dp, vertical = 16.dp)
                ) {
                    Text(
                        "OPREȘTE CURSA",
                        fontWeight = FontWeight.Black,
                        fontSize = 12.sp,
                        letterSpacing = 1.sp
                    )
                }
            }
        }
        // ==========================================
        // 5. ZONA JOS-DREAPTA: Grafic G-Meter (Placeholder vizual)
        // ==========================================
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = 32.dp)
                .size(100.dp) // Dimensiunea cadranului G-Meter
                .background(
                    MaterialTheme.colorScheme.surfaceContainerLowest.copy(alpha = 0.85f),
                    CircleShape
                )
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                .clip(CircleShape),
            contentAlignment = Alignment.Center
        ) {
            // Desenăm interfața G-Meter-ului direct cu Canvas
            val gridColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
            val accentColor = MaterialTheme.colorScheme.primary

            Canvas(modifier = Modifier.fillMaxSize()) {
                val center = Offset(size.width / 2, size.height / 2)
                val radius = size.width / 2

                val clampedX = gForceX.coerceIn(-maxG, maxG)
                val clampedY = gForceY.coerceIn(-maxG, maxG)

                val dotCenter = Offset(
                    x = center.x + (clampedX / maxG) * radius,
                    y = center.y - (clampedY / maxG) * radius
                )

                // Cercurile concentrice (1G, 2G etc.)
                drawCircle(color = gridColor, radius = radius * 0.33f, center = center, style = Stroke(width = 2f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f))))
                drawCircle(color = gridColor, radius = radius * 0.66f, center = center, style = Stroke(width = 2f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f))))

                // Axele X și Y
                drawLine(color = gridColor, start = Offset(0f, center.y), end = Offset(size.width, center.y), strokeWidth = 2f)
                drawLine(color = gridColor, start = Offset(center.x, 0f), end = Offset(center.x, size.height), strokeWidth = 2f)

                drawCircle(color = accentColor, radius = 8.dp.toPx(), center = dotCenter)
                drawCircle(color = BrakeRed, radius = 4.dp.toPx(), center = dotCenter)
            }

            // Etichete mici pentru G-Meter
            Text("G", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 16.dp, end = 24.dp))
        }

        // ==========================================
        // BANNER AUTO-START (CENTRU)
        // ==========================================
        if (!hasRaceStarted) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 100.dp)
                    .background(BrakeRed.copy(alpha = 0.9f), CutCornerShape(8.dp))
                    .padding(horizontal = 24.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "ACCELEREAZĂ PESTE 10 KM/H LA START!",
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    fontSize = 14.sp
                )
            }
        }

        // ==========================================
        // 6. NOTIFICĂRI TUR ȘI SPLIT (CENTRU)
        // ==========================================
        Column(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(top = 120.dp, start = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            lastLapNotification?.let { notification ->
                Box(
                    modifier = Modifier
                        .background(
                            if (notification.isBestLap) Color(0xFFFFD700).copy(alpha = 0.9f) // Gold for best lap
                            else MaterialTheme.colorScheme.secondary.copy(alpha = 0.9f),
                            CutCornerShape(8.dp)
                        )
                        .padding(horizontal = 24.dp, vertical = 8.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = if (notification.isBestLap) "NEW BEST LAP!" else "LAP ${notification.lap.lapNumber} COMPLETED",
                            color = if (notification.isBestLap) Color.Black else Color.White,
                            fontWeight = FontWeight.Black,
                            fontSize = 14.sp
                        )
                        Text(
                            text = notification.lap.formattedTime,
                            color = if (notification.isBestLap) Color.Black else Color.White,
                            fontWeight = FontWeight.Black,
                            fontSize = 24.sp,
                            fontStyle = FontStyle.Italic
                        )
                    }
                }
            }

            if (lastLapNotification != null && currentSplit != null) {
                Spacer(modifier = Modifier.height(16.dp))
            }

            currentSplit?.let { split ->
                Box(
                    modifier = Modifier
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f),
                            CutCornerShape(8.dp)
                        )
                        .border(1.dp, MaterialTheme.colorScheme.outline, CutCornerShape(8.dp))
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column {
                            Text(
                                text = String.format("%02d:%02d.%01d", split.splitTimeMs / 60000, (split.splitTimeMs % 60000) / 1000, (split.splitTimeMs % 1000) / 100),
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                        }
                        split.deltaVsBestMs?.let { delta ->
                            Spacer(modifier = Modifier.width(16.dp))
                            val isFaster = delta <= 0
                            Text(
                                text = (if (isFaster) "-" else "+") + String.format("%.2f", kotlin.math.abs(delta) / 1000.0),
                                color = if (isFaster) Color(0xFF00C853) else Color(0xFFD50000),
                                fontWeight = FontWeight.Black,
                                fontSize = 18.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Preview(
    name = "Racing HUD Preview",
    showBackground = true,
    backgroundColor = 0xFF12121D, // Fundalul nostru SurfaceDark
    widthDp = 800, // Dimensiuni orientative de ecran (Landscape)
    heightDp = 360
)
@Composable
fun PreviewRacingHUD() {
    // Apelăm tema pentru culori
    FirstAppTheme(darkTheme = true) {
        // Apelăm componenta cu date statice (dummy data)
        RacingHUD(
            speed = 145,
            currentLatLng = LatLng(44.4268, 26.1025),
            selectedTrack = null, // Defaults to SPRINT
            currentLapTimeMs = 418000L, // Aprox 6 minute și 58 secunde
            lastLapNotification = null,
            allLaps = emptyList(),
            currentSplit = SplitData(1, "Checkpoint 1", 120000L, -500L),
            sprintProgress = 0.65f,
            onStopClick = {}
        )
    }
}
