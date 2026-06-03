package com.example.firstapp.ui.components

import android.location.Location
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.firstapp.data.Track
import com.example.firstapp.ui.theme.BrakeRed
import com.huawei.hms.maps.model.LatLng

@Composable
fun RacingHUD(
    speed: Int,
    currentLatLng: LatLng?,
    selectedTrack: Track?, // Poate fi null dacă e un Quick Race (fără traseu predefinit)
    onStopClick: () -> Unit,
    modifier: Modifier = Modifier
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