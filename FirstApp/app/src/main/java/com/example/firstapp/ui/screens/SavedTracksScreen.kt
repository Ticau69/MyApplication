package com.example.firstapp.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.firstapp.AppState
import com.example.firstapp.AppViewModel
import com.example.firstapp.data.Track
import com.example.firstapp.history.SavedTracksState
import com.example.firstapp.ui.components.SavedTracksHUD
import androidx.compose.runtime.DisposableEffect

@Composable
fun SavedTracksScreen(
    savedTracks: List<Track>,
    savedTracksLogic: SavedTracksState,
    viewModel: AppViewModel,
    onStartRaceClick: (Track) -> Unit,
    onStateChange: (AppState) -> Unit
) {
    // Ascultăm selecția curentă
    val selectedTrack by viewModel.selectedTrack.collectAsState()

    DisposableEffect(Unit) {
        onDispose {
            // 1. Curățăm harta GARANTAT, indiferent cum ieșim din ecran
            savedTracksLogic.clearDrawnObjects()

            // 2. Resetăm traseul selectat doar dacă NU am plecat ca să începem o cursă
            // (Dacă am dat click pe "START CURSĂ", trebuie să păstrăm traseul în memorie)
            val nextState = viewModel.appState.value
            if (nextState != AppState.TRACK_RACING && nextState != AppState.RACING) {
                viewModel.selectTrack(null)
            }
        }
    }

    if (selectedTrack != null) {
        // ── MODUL FOCUS: Traseul pe hartă fără listă ──
        Box(modifier = Modifier.fillMaxSize()) {

            // 1. Numele Traseului (Stânga Sus)
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF121212).copy(alpha = 0.85f)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(top = 48.dp, start = 16.dp)
            ) {
                Text(
                    text = selectedTrack!!.name,
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                )
            }

            // 2. Butonul "X" de închidere (Dreapta Sus)
            FilledIconButton(
                onClick = {
                    viewModel.selectTrack(null) // Resetăm selecția
                    savedTracksLogic.clearDrawnObjects() // Ștergem linia colorată de pe hartă
                },
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = Color(0xFFFF3B30).copy(alpha = 0.9f),
                    contentColor = Color.White
                ),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 48.dp, end = 16.dp)
                    .size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Înapoi",
                    modifier = Modifier.size(24.dp)
                )
            }

            // 3. Buton de start direct din previzualizare
            Button(
                onClick = { onStartRaceClick(selectedTrack!!) },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 32.dp)
                    .fillMaxWidth(0.6f)
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E676)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    text = "START CURSĂ",
                    color = Color.Black,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 18.sp
                )
            }
        }
    } else {
        // ── MODUL NORMAL: Lista de Trasee ──
        SavedTracksHUD(
            tracks = savedTracks,
            onCloseClick = {
                savedTracksLogic.clearDrawnObjects()
                onStateChange(AppState.CRUISE)
            },
            onTrackClick = { track ->
                viewModel.selectTrack(track)
                savedTracksLogic.drawTrack(track)
            },
            onDeleteClick = { track ->
                viewModel.deleteTrack(track)
                savedTracksLogic.clearDrawnObjects()
            },
            onStartRaceClick = onStartRaceClick
        )
    }
}