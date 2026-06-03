package com.example.firstapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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

@Composable
fun CreationHUD(
    onAddStart: () -> Unit,
    onAddCheckpoint: () -> Unit,
    onAddFinish: () -> Unit,
    onSaveTrack: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
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