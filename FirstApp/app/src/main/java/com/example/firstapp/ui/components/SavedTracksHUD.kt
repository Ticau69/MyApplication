package com.example.firstapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview
import com.example.firstapp.data.RaceType
import com.example.firstapp.data.SerializableLatLng
import com.example.firstapp.data.Track
import com.example.firstapp.ui.theme.BrakeRed
import com.example.firstapp.ui.theme.FirstAppTheme
import com.example.firstapp.ui.theme.OutlineSlate
import com.example.trackappv2.R

@Composable
fun SavedTracksHUD(
    tracks: List<Track>,
    onCloseClick: () -> Unit,
    onTrackClick: (Track) -> Unit,
    onDeleteClick: (Track) -> Unit,
    onStartRaceClick: (Track) -> Unit, // Adăugat
    modifier: Modifier = Modifier
) {
    // Fundalul principal: translucid (Glassmorphism) pentru a lăsa harta să se vadă
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background.copy(alpha = 0.85f))
            .padding(16.dp)
            .padding(top = 24.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // --- HEADER-ul COREECTAT ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "TRASEE SALVATE",
                    style = MaterialTheme.typography.displayLarge.copy(fontSize = 28.sp),
                    color = MaterialTheme.colorScheme.primary
                )

                // AICI vine pus X-ul, în dreapta titlului
                IconButton(onClick = onCloseClick) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_close), // Vectorul tău modern
                        contentDescription = "Închide",
                        tint = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Lista propriu-zisă
            if (tracks.isEmpty()) {
                Text(
                    text = "Niciun traseu salvat încă.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyLarge
                )
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(tracks) { track ->
                        TrackCard(
                            track = track,
                            onClick = { onTrackClick(track) },
                            onDelete = { onDeleteClick(track) },
                            onStartRace = { onStartRaceClick(track) } // Pasăm lambda corect
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 800, heightDp = 400)
@Composable
fun PreviewSavedTracksHUD() {
    val mockTracks = listOf(
        Track(
            id = "1",
            name = "Transfăgărășan Section",
            createdAt = "2024-05-20 14:30",
            start = SerializableLatLng(44.4268, 26.1025),
            checkpoints = listOf(SerializableLatLng(44.4300, 26.1100)),
            finish = SerializableLatLng(44.4400, 26.1200),
            raceType = RaceType.SPRINT
        ),
        Track(
            id = "2",
            name = "Motorpark Circuit",
            createdAt = "2024-05-21 10:15",
            start = SerializableLatLng(44.5000, 26.2000),
            checkpoints = listOf(SerializableLatLng(44.5100, 26.2100), SerializableLatLng(44.5200, 26.2200)),
            finish = SerializableLatLng(44.5000, 26.2000),
            raceType = RaceType.LAP_RACE
        )
    )

    FirstAppTheme(darkTheme = true) {
        SavedTracksHUD(
            tracks = mockTracks,
            onCloseClick = {},
            onTrackClick = {},
            onDeleteClick = {},
            onStartRaceClick = {}
        )
    }
}

@Composable
fun TrackCard(
    track: Track,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onStartRace: () -> Unit  // ← adaugă
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(CutCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .border(1.5.dp, OutlineSlate.copy(alpha = 0.5f), CutCornerShape(12.dp))
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = track.name.uppercase(),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${track.createdAt} | ${track.checkpoints.size} CP",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = if (track.raceType == RaceType.LAP_RACE)
                    "🔄 Circuit" else "➡️ Sprint",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                fontSize = 11.sp
            )
        }

        // Buton Start Cursă
        IconButton(onClick = onStartRace) {
            Icon(
                painter = painterResource(id = R.drawable.ic_start_race),
                contentDescription = "Start cursă",
                tint = Color(0xFF00E676)
            )
        }

        // Buton Ștergere
        IconButton(onClick = onDelete) {
            Icon(
                painter = painterResource(id = R.drawable.ic_delete),
                contentDescription = "Șterge",
                tint = BrakeRed
            )
        }
    }
}