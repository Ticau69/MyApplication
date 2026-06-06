package com.example.firstapp.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.firstapp.ui.theme.BrakeRed
import com.example.trackappv2.R // Verifică dacă importul de R este cel corect din proiectul tău

@Composable
fun GpsDisabledDialog(
    onOpenSettings: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(painter = painterResource(id = R.drawable.ic_gps_off), contentDescription = null, tint = BrakeRed, modifier = Modifier.size(24.dp))
                Text("GPS Dezactivat", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Text("Aplicația necesită GPS activ pentru localizare precisă și funcționalități de curse. Fără GPS, harta și tracking-ul nu vor funcționa corect.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        },
        confirmButton = { VelocityPrimaryButton(text = "Activează", onClick = onOpenSettings) },
        dismissButton = { VelocityGhostButton(text = "Ignoră", onClick = onDismiss) }
    )
}