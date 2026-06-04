package com.example.firstapp.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.firstapp.AppViewModel
import com.example.firstapp.data.LapData
import com.example.firstapp.data.RaceType
import com.example.firstapp.data.SplitData
import com.example.firstapp.data.Track
import com.example.firstapp.ui.theme.BrakeRed
import com.huawei.hms.maps.model.LatLng

@Composable
fun RacingHUD(
    speed: Int,
    currentLatLng: LatLng?,
    selectedTrack: Track?,
    onStopClick: () -> Unit,
    modifier: Modifier = Modifier,
    currentLapTimeMs: Long = 0L,
    lastLapNotification: AppViewModel.LapNotification? = null,
    allLaps: List<LapData> = emptyList(),
    currentSplit: SplitData? = null,
    sprintProgress: Float = 0f
) {
    // 1. Calculăm constant dacă mașina a ieșit de pe traseu
    val isOffTrack by remember(currentLatLng, selectedTrack) {
        derivedStateOf {
            calculateOffTrackStatus(currentLatLng, selectedTrack, thresholdMeters = 50f)
        }
    }

    // 2. Animația pentru alerta vizuală (Edge Glow)
    val infiniteTransition = rememberInfiniteTransition(label = "glow")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(400, easing = LinearEasing), // Pulsare rapidă, agresivă
            repeatMode = RepeatMode.Reverse
        ), label = "alpha"
    )

    Box(modifier = modifier.fillMaxSize()) {
        // --- ALERTĂ EDGE GLOW ---
        if (isOffTrack) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .border(12.dp, BrakeRed.copy(alpha = glowAlpha)) // O margine groasă, translucidă
            )

            // Text de avertizare plasat sus
            Text(
                text = "OFF TRACK",
                color = BrakeRed.copy(alpha = glowAlpha),
                fontWeight = FontWeight.Black,
                fontSize = 24.sp,
                letterSpacing = 4.sp,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 16.dp)
            )
        }

        if (selectedTrack?.raceType == RaceType.SPRINT) {
            Column(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 16.dp)
                    .fillMaxWidth(0.7f)
                    .clip(CutCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp))
                    .background(
                        MaterialTheme.colorScheme.surfaceContainerLowest.copy(alpha = 0.85f)
                    )
                    .padding(horizontal = 24.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Timp total
                Text(
                    text = formatLapTime(currentLapTimeMs),
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Black,
                    fontStyle = FontStyle.Italic,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Progress bar
                LinearProgressIndicator(
                    progress = { sprintProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "${(sprintProgress * 100).toInt()}% completat",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (selectedTrack?.raceType == RaceType.LAP_RACE) {
            Column(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 16.dp)
                    .clip(CutCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerLowest.copy(alpha = 0.85f))
                    .padding(horizontal = 24.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Număr tur curent
                Text(
                    text = "TUR ${(allLaps.size + 1)}",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 2.sp
                )

                // Timp tur curent
                Text(
                    text = formatLapTime(currentLapTimeMs),
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Black,
                    fontStyle = FontStyle.Italic,
                    color = MaterialTheme.colorScheme.primary
                )

                // Delta față de best lap
                val bestLapMs = allLaps.minByOrNull { it.lapTimeMs }?.lapTimeMs
                if (bestLapMs != null && allLaps.isNotEmpty()) {
                    val delta = currentLapTimeMs - bestLapMs
                    val deltaColor = if (delta <= 0) Color(0xFF00E676) else BrakeRed
                    val deltaPrefix = if (delta <= 0) "-" else "+"

                    Text(
                        text = "$deltaPrefix${formatLapTime(kotlin.math.abs(delta))}",
                        style = MaterialTheme.typography.labelLarge,
                        color = deltaColor,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        lastLapNotification?.let { notification ->
            LapCompletedBanner(
                notification = notification,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(top = 16.dp, start = 16.dp)
            )
        }

        currentSplit?.let { split ->
            SplitBanner(
                split = split,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(top = 16.dp, start = 16.dp)
            )
        }

        if (allLaps.isNotEmpty()) {
            LazyColumn(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 16.dp, end = 16.dp)
                    .widthIn(max = 160.dp)
                    .heightIn(max = 200.dp)
                    .clip(CutCornerShape(8.dp))
                    .background(
                        MaterialTheme.colorScheme.surfaceContainerLowest.copy(alpha = 0.7f)
                    )
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(allLaps.reversed()) { lap ->
                    val isBest = lap.lapTimeMs == allLaps.minOf { it.lapTimeMs }
                    LapRowItem(lap = lap, isBest = isBest)
                }
            }
        }

        // --- TELEMETRIE (Vitezometru) ---
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = if (isOffTrack) 64.dp else 48.dp) // Împingem puțin în jos dacă e alertă
                .clip(CutCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerLowest.copy(alpha = 0.85f))
                .padding(horizontal = 48.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = speed.toString(),
                fontSize = 80.sp,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.primary,
                fontStyle = FontStyle.Italic // Respectăm designul "Velocity"
            )
            Text(
                text = "KM/H",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // --- BUTON OPRIRE ---
        VelocityPrimaryButton(
            text = "OPREȘTE CURSA",
            onClick = onStopClick,
            isDestructive = true,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 48.dp)
                .fillMaxWidth(0.8f)
        )
    }
}

@Composable
fun SplitBanner(
    split: SplitData,
    modifier: Modifier = Modifier
) {
    val deltaColor = when {
        split.deltaVsBestMs == null -> MaterialTheme.colorScheme.primary
        split.deltaVsBestMs <= 0 -> Color(0xFF00E676)  // Verde — mai rapid
        else -> BrakeRed                                 // Roșu — mai lent
    }

    val deltaText = split.deltaVsBestMs?.let { delta ->
        val prefix = if (delta <= 0) "-" else "+"
        "$prefix${formatLapTime(kotlin.math.abs(delta))}"
    }

    Column(
        modifier = modifier
            .clip(CutCornerShape(topEnd = 16.dp, bottomEnd = 16.dp))
            .background(deltaColor.copy(alpha = 0.9f))
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = split.checkpointName.uppercase(),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.ExtraBold,
            color = Color.Black,
            letterSpacing = 1.sp
        )
        Text(
            text = formatLapTime(split.splitTimeMs),
            fontSize = 20.sp,
            fontWeight = FontWeight.Black,
            fontStyle = FontStyle.Italic,
            color = Color.Black
        )
        deltaText?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.labelLarge,
                color = Color.Black.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
fun LapCompletedBanner(
    notification: AppViewModel.LapNotification,
    modifier: Modifier = Modifier
) {
    val bannerColor = if (notification.isBestLap)
        Color(0xFF00E676) else MaterialTheme.colorScheme.primary

    Column(
        modifier = modifier
            .clip(CutCornerShape(topEnd = 16.dp, bottomEnd = 16.dp))
            .background(bannerColor.copy(alpha = 0.9f))
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = if (notification.isBestLap) "🏆 BEST LAP!" else "TUR ${notification.lap.lapNumber}",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.ExtraBold,
            color = Color.Black
        )
        Text(
            text = notification.lap.formattedTime,
            fontSize = 20.sp,
            fontWeight = FontWeight.Black,
            fontStyle = FontStyle.Italic,
            color = Color.Black
        )
    }
}

@Composable
fun LapRowItem(lap: LapData, isBest: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "L${lap.lapNumber}",
            style = MaterialTheme.typography.bodyMedium,
            color = if (isBest) Color(0xFF00E676)
            else MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = if (isBest) FontWeight.ExtraBold else FontWeight.Normal
        )
        Text(
            text = lap.formattedTime,
            style = MaterialTheme.typography.bodyMedium,
            color = if (isBest) Color(0xFF00E676)
            else MaterialTheme.colorScheme.onSurface,
            fontWeight = if (isBest) FontWeight.ExtraBold else FontWeight.Normal
        )
    }
}

private fun formatLapTime(ms: Long): String {
    val mins = ms / 60000
    val secs = (ms % 60000) / 1000
    val millis = (ms % 1000) / 10
    return String.format(java.util.Locale.getDefault(), "%02d:%02d.%02d", mins, secs, millis)
}

/**
 * Funcție matematică pentru a calcula dacă locația curentă este prea departe de traseu.
 * Folosim Android Location API pentru calcularea precisă a distanței geospațiale (Haversine).
 */
private fun calculateOffTrackStatus(userPos: LatLng?, track: Track?, thresholdMeters: Float): Boolean {
    if (userPos == null || track == null) return false

    // Folosim metoda ta corectă .toLatLng() pentru a extrage coordonatele din Track
    val pointsToCheck = if (track.routedPoints.isNotEmpty()) {
        track.routedPoints.map { it.toLatLng() }
    } else {
        val list = mutableListOf<LatLng>()
        list.add(track.start.toLatLng())
        list.addAll(track.checkpoints.map { it.toLatLng() })
        list.add(track.finish.toLatLng())
        list
    }

    if (pointsToCheck.isEmpty()) return false

    val results = FloatArray(1)
    var minDistance = Float.MAX_VALUE

    // Găsim distanța până la cel mai apropiat punct de pe traseu
    for (point in pointsToCheck) {
        android.location.Location.distanceBetween(
            userPos.latitude, userPos.longitude,
            point.latitude, point.longitude,
            results
        )
        if (results[0] < minDistance) {
            minDistance = results[0]
        }
    }

    // Dacă cel mai apropiat punct este la peste limita stabilită, ești ieșit de pe traseu
    return minDistance > thresholdMeters
}