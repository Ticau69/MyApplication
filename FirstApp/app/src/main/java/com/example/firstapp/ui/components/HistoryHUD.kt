package com.example.firstapp.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.example.firstapp.data.RaceType
import com.example.firstapp.racing.RaceRecord
import com.example.firstapp.ui.theme.BrakeRed
import com.example.firstapp.ui.theme.OutlineSlate
import com.example.trackappv2.R

import java.util.Locale

@Composable
fun HistoryHUD(
    raceHistory: List<RaceRecord>,
    activeFilter: RaceType?,
    onFilterChanged: (RaceType?) -> Unit,
    onCloseClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background.copy(alpha = 0.85f))
            .padding(16.dp)
            .padding(top = 24.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            // Header Original
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "ISTORIC CURSE",
                    style = MaterialTheme.typography.displayLarge.copy(fontSize = 28.sp),
                    color = MaterialTheme.colorScheme.primary
                )
                IconButton(onClick = onCloseClick) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_close),
                        contentDescription = "Închide",
                        tint = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Bara de Filtrare (Integrată cu designul tău)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(36.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f), RoundedCornerShape(18.dp))
                    .border(1.dp, OutlineSlate.copy(alpha = 0.3f), RoundedCornerShape(18.dp)),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilterTab("TOATE", activeFilter == null, modifier = Modifier.weight(1f).clip(RoundedCornerShape(topStart = 18.dp, bottomStart = 18.dp))) { onFilterChanged(null) }
                Box(modifier = Modifier.width(1.dp).fillMaxHeight(0.5f).background(OutlineSlate.copy(alpha = 0.3f)))
                FilterTab("SPRINT", activeFilter == RaceType.SPRINT, modifier = Modifier.weight(1f)) { onFilterChanged(RaceType.SPRINT) }
                Box(modifier = Modifier.width(1.dp).fillMaxHeight(0.5f).background(OutlineSlate.copy(alpha = 0.3f)))
                FilterTab("CIRCUIT", activeFilter == RaceType.LAP_RACE, modifier = Modifier.weight(1f)) { onFilterChanged(RaceType.LAP_RACE) }
                Box(modifier = Modifier.width(1.dp).fillMaxHeight(0.5f).background(OutlineSlate.copy(alpha = 0.3f)))
                FilterTab("CAMERE", activeFilter == RaceType.SPEED_TRAP, modifier = Modifier.weight(1.2f).clip(RoundedCornerShape(topEnd = 18.dp, bottomEnd = 18.dp)), isRed = true) { onFilterChanged(RaceType.SPEED_TRAP) }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Statistici sumare (Se actualizează automat la filtrare)
            if (raceHistory.isNotEmpty()) {
                val bestSpeed = raceHistory.maxOf { it.maxSpeed }
                val totalRaces = raceHistory.size
                val totalDistanceKm = raceHistory.sumOf { it.distanceKm }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(CutCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    StatChip(label = "CURSE", value = "$totalRaces")
                    StatChip(label = "VITEZĂ MAX", value = "$bestSpeed km/h")
                    StatChip(label = "DISTANȚĂ", value = String.format(Locale.getDefault(), "%.1f km", totalDistanceKm))
                }

                Spacer(modifier = Modifier.height(16.dp))
            }

            // Lista curse
            if (raceHistory.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Nicio înregistrare găsită.", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyLarge)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Modifică filtrul sau pornește o sesiune nouă.", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f), style = MaterialTheme.typography.bodyMedium)
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(raceHistory) { record ->
                        RaceRecordCard(record = record)
                    }
                }
            }
        }
    }
}

@Composable
private fun FilterTab(text: String, isSelected: Boolean, modifier: Modifier = Modifier, isRed: Boolean = false, onClick: () -> Unit) {
    val activeColor = if (isRed) BrakeRed else MaterialTheme.colorScheme.primary
    Box(
        modifier = modifier
            .fillMaxHeight()
            .clickable { onClick() }
            .background(if (isSelected) activeColor.copy(alpha = 0.2f) else Color.Transparent),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = if (isSelected) activeColor else MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Black, fontSize = 10.sp, letterSpacing = 0.5.sp)
    }
}

@Composable
fun StatChip(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 10.sp,
            letterSpacing = 1.sp
        )
    }
}

@Composable
fun RaceRecordCard(record: RaceRecord) {
    val mins = record.durationSeconds / 60
    val secs = record.durationSeconds % 60

    // Mapăm iconița în funcție de tipul recordului (dacă RaceRecord are raceType)
    val iconRes = when (record.raceType) {
        RaceType.LAP_RACE -> R.drawable.circuit_icon
        RaceType.SPEED_TRAP -> R.drawable.speedcamera_icon
        RaceType.SPEED_ZONE -> R.drawable.speedzone_icon
        else -> R.drawable.sprint_icon
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(CutCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .border(1.5.dp, OutlineSlate.copy(alpha = 0.5f), CutCornerShape(12.dp))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Iconița Vizuală pentru tipul cursei
        Box(
            modifier = Modifier.size(36.dp).clip(RoundedCornerShape(8.dp)).background(MaterialTheme.colorScheme.background.copy(alpha = 0.5f)),
            contentAlignment = Alignment.Center
        ) {
            Image(painter = painterResource(id = iconRes), contentDescription = null, modifier = Modifier.size(20.dp))
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Coloana stânga — dată și durată
        Column(modifier = Modifier.weight(1f)) {
            Text(text = record.date, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.ExtraBold, color = Color.White)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = String.format(Locale.getDefault(), "%02d:%02d", mins, secs), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        // Coloana mijloc — distanță
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
            Text(text = String.format(Locale.getDefault(), "%.2f", record.distanceKm), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
            Text(text = "KM", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp, letterSpacing = 1.sp)
        }

        // Coloana dreapta — viteză max
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
            Text(text = "${record.maxSpeed}", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.ExtraBold, color = BrakeRed)
            Text(text = "KM/H MAX", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp, letterSpacing = 1.sp)
        }
    }
}