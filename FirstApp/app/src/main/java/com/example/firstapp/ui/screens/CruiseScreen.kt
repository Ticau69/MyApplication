package com.example.firstapp.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.zIndex
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.firstapp.AppState
import com.example.firstapp.data.Track
import com.example.firstapp.ui.components.CruiseHUD
import com.example.trackappv2.R // Asigură-te că pachetul tău de resurse este corect

@Composable
fun CruiseScreen(
    nearbyTrack: Track?, // NOU: Traseul din proximitate
    distanceToTrack: Int?, // NOU: Distanța în metri
    onStateChange: (AppState) -> Unit,
    onStartCountdown: (onFinished: () -> Unit) -> Unit,
    onStartNearbyRace: (Track) -> Unit // NOU: Acțiunea de conectare la cursă
) {
    Box(modifier = Modifier.fillMaxSize()) {

        // HUD-ul principal de Cruise rămâne neatins la bază
        CruiseHUD(
            onSavedTracksClick = { onStateChange(AppState.SAVED_TRACKS) },
            onHistoryClick = { onStateChange(AppState.HISTORY) },
            onSettingsClick = { onStateChange(AppState.SETTINGS) },
            onCreateTrackClick = { onStateChange(AppState.RACE_CREATION) },
            onQuickRaceClick = {
                onStartCountdown {
                    onStateChange(AppState.RACING)
                }
            }
        )

        // Popup-ul lateral animat (Apare doar când avem un traseu < 100m/200m)
        // MUTAT DUPĂ CruiseHUD pentru a fi deasupra (Z-order)
        AnimatedVisibility(
            visible = nearbyTrack != null && distanceToTrack != null,
            enter = slideInHorizontally(
                initialOffsetX = { -it },
                animationSpec = tween(600)
            ) + fadeIn(tween(600)),
            exit = slideOutHorizontally(
                targetOffsetX = { -it },
                animationSpec = tween(500)
            ) + fadeOut(tween(500)),
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(bottom = 60.dp) // Ridicat puțin ca să nu se bată cu alte butoane din HUD
                .zIndex(10f) // Forțăm Z-order-ul
        ) {
            nearbyTrack?.let { track ->
                Row(
                    modifier = Modifier
                        .clip(CutCornerShape(topEnd = 16.dp, bottomEnd = 16.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.95f))
                        .padding(start = 12.dp, end = 20.dp, top = 16.dp, bottom = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Am refolosit iconița de finish/start ca placeholder vizual
                    Icon(
                        painter = painterResource(id = R.drawable.ic_start_marker),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(36.dp)
                    )

                    Spacer(modifier = Modifier.width(16.dp))

                    // Info Traseu
                    Column {
                        Text("EVENIMENT", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, letterSpacing = 2.sp)
                        Text(track.name, fontSize = 18.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurface)
                        Text("La ${distanceToTrack}m distanță", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }

                    Spacer(modifier = Modifier.width(24.dp))

                    // Buton Aliniere
                    Button(
                        onClick = { onStartNearbyRace(track) },
                        shape = CutCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text("ALINIAZĂ-TE", fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                    }
                }
            }
        }
    }
}
