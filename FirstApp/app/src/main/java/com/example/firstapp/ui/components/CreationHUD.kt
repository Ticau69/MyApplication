package com.example.firstapp.ui.components

import androidx.compose.ui.text.font.FontStyle
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.firstapp.data.RaceType

@Composable
fun CreationHUD(
    onAddStart: () -> Unit,
    onAddCheckpoint: () -> Unit,
    onAddFinish: () -> Unit,
    onSaveTrack: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
    selectedRaceType: RaceType = RaceType.SPRINT,
    onRaceTypeChanged: (RaceType) -> Unit,
) {
    Box(
        modifier = modifier.fillMaxSize()
    ) {
        // Butonul de Anulare (Ghost Button) în stânga sus, ca să fie rapid de accesat
        VelocityGhostButton(
            text = "Anulează",
            onClick = onCancel,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(top = 40.dp, start = 16.dp) // Spațiu pentru status bar
        )

        // Panoul principal de control lipit de marginea de jos
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(16.dp)
                .clip(RoundedCornerShape(24.dp))
                // Glassmorphism: Un dark surface transparent
                .background(MaterialTheme.colorScheme.surfaceContainerLowest.copy(alpha = 0.85f))
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "MOD CREARE TRASEU",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 2.sp,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Rândul 1: Start și Finish
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                VelocityPrimaryButton(text = "Start", onClick = onAddStart)
                VelocityPrimaryButton(text = "Finish", onClick = onAddFinish)
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(CutCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                RaceTypeChip(
                    label = "SPRINT",
                    selected = selectedRaceType == RaceType.SPRINT,
                    onClick = { onRaceTypeChanged(RaceType.SPRINT) }
                )
                RaceTypeChip(
                    label = "LAP RACE",
                    selected = selectedRaceType == RaceType.LAP_RACE,
                    onClick = { onRaceTypeChanged(RaceType.LAP_RACE) }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Rândul 2: Checkpoints și Salvare
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                VelocityGhostButton(text = "+ Checkpoint", onClick = onAddCheckpoint)
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Butonul final de salvare (mare, pe tot lățimea)
            VelocityPrimaryButton(
                text = "SALVEAZĂ TRASEU",
                onClick = onSaveTrack,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun RaceTypeChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(CutCornerShape(6.dp))
            .background(
                if (selected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.surface
            )
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            fontStyle = FontStyle.Italic,
            fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.Normal,
            color = if (selected) MaterialTheme.colorScheme.onPrimary
            else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}