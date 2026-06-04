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
import com.example.firstapp.data.Track
import com.example.firstapp.ui.theme.BrakeRed
import com.example.firstapp.ui.theme.OutlineSlate
import com.example.trackappv2.R

@Composable
fun SavedTracksHUD(
    tracks: List<Track>,
    onCloseClick: () -> Unit,
    onTrackClick: (Track) -> Unit,
    onDeleteClick: (Track) -> Unit,
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
                            onDelete = { onDeleteClick(track) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TrackCard(
    track: Track,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    // Un card cu Octagon Influence (tăiat la colțuri) și contur subtil
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
        // Detalii cursă (Stânga)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = track.name.uppercase(),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Data: ${track.createdAt} | Puncte: ${track.checkpoints.size}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

        }

        // Buton de ștergere (Dreapta) cu nuanța Brake Red
        IconButton(onClick = onDelete) {
            Icon(
                painter = painterResource(id = R.drawable.ic_delete), // Iconița ta de delete creată anterior
                contentDescription = "Șterge",
                tint = BrakeRed
            )
        }
    }
}