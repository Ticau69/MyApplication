package com.example.firstapp.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.firstapp.data.RaceType
import com.example.firstapp.ui.theme.BrakeRed
import com.example.firstapp.ui.theme.FirstAppTheme
import com.example.firstapp.data.WaypointType

enum class CreationMode { MANUAL, DRIVE }

@Composable
fun CreationHUD(
    activeMode: WaypointType,
    onAddStart: () -> Unit,
    onAddCheckpoint: () -> Unit,
    onAddFinish: () -> Unit,
    onSaveTrack: () -> Unit,
    onCancel: () -> Unit,
    selectedRaceType: RaceType,
    onRaceTypeChanged: (RaceType) -> Unit,
    isRecording: Boolean,
    onStartDriveRecording: () -> Unit,
    onRecordDriveCheckpoint: () -> Unit,
    onStopDriveRecording: () -> Unit,
    // NOU: Callback pentru salvarea radarului
    onSaveSpeedCamera: () -> Unit,
    hasStart: Boolean = false,
    hasFinish: Boolean = false,
    modifier: Modifier = Modifier
) {
    var currentCreationMode by remember { mutableStateOf(CreationMode.MANUAL) }

    Box(modifier = modifier.fillMaxSize()) {

        // ==========================================
        // 1. ZONA SUS: Banner Info / Mod Creare
        // ==========================================
        if (selectedRaceType == RaceType.SPEED_TRAP) {
            val bannerText = when {
                hasStart && hasFinish -> "DETECTAT: SPEED ZONE (VITEZĂ MEDIE)"
                hasStart -> "DETECTAT: SPEED TRAP (RADAR FIX) | ADAUGĂ PUNCTUL 2 PENTRU ZONE"
                else -> "CONFIGURARE CAMERĂ: PLASEAZĂ PUNCTUL 1"
            }
            val bannerColor = when {
                hasStart && hasFinish -> Color(0xFF00E676) // Verde
                hasStart -> BrakeRed
                else -> MaterialTheme.colorScheme.onSurfaceVariant
            }

            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 40.dp)
                    .background(MaterialTheme.colorScheme.surfaceContainerLowest.copy(alpha = 0.9f), RoundedCornerShape(12.dp))
                    .border(1.dp, bannerColor.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 24.dp, vertical = 10.dp)
            ) {
                Text(
                    text = bannerText,
                    color = bannerColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp
                )
            }
        } else if (!isRecording) {
            Row(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 40.dp)
                    .height(44.dp)
                    .background(MaterialTheme.colorScheme.surfaceContainerLowest.copy(alpha = 0.85f), RoundedCornerShape(22.dp))
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(22.dp))
                    .padding(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(18.dp))
                    .clickable { currentCreationMode = CreationMode.MANUAL }
                    .background(if (currentCreationMode == CreationMode.MANUAL) MaterialTheme.colorScheme.primary else Color.Transparent)
                    .padding(horizontal = 20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("MANUAL", color = if (currentCreationMode == CreationMode.MANUAL) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Black, fontSize = 11.sp, letterSpacing = 1.sp)
                }

                Box(modifier = Modifier
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(18.dp))
                    .clickable { currentCreationMode = CreationMode.DRIVE }
                    .background(if (currentCreationMode == CreationMode.DRIVE) BrakeRed else Color.Transparent)
                    .padding(horizontal = 20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("ÎN MERS", color = if (currentCreationMode == CreationMode.DRIVE) Color.White else MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Black, fontSize = 11.sp, letterSpacing = 1.sp)
                }
            }
        }

        // ==========================================
        // 2. CONȚINUT DINAMIC (DREAPTA)
        // ==========================================
        if (selectedRaceType == RaceType.SPEED_TRAP) {
            Column(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.End
            ) {
                // Modificăm vizual butoanele laterale dacă punctele sunt setate
                val startColor  = if (hasStart) Color(0xFF00E676) else MaterialTheme.colorScheme.primary
                val finishColor = if (hasFinish) Color(0xFF00E676) else MaterialTheme.colorScheme.primary

                MapSideButton(if (hasStart) "PUNCT 1 ✓" else "PUNCT 1", textColor = MaterialTheme.colorScheme.onPrimary, bgColor = startColor, isActive = activeMode == WaypointType.START, onClick = onAddStart)
                MapSideButton(if (hasFinish) "PUNCT 2 ✓" else "PUNCT 2", textColor = MaterialTheme.colorScheme.onPrimary, bgColor = finishColor, isActive = activeMode == WaypointType.FINISH, onClick = onAddFinish)
            }

            Box(modifier = Modifier.align(Alignment.BottomEnd).padding(bottom = 15.dp, end = 16.dp)){
                // Schimbăm eticheta butonului de salvare
                val btnActionText = if (hasStart && hasFinish) "CREAZĂ ZONE" else "CREAZĂ TRAP"
                MapSideButton(btnActionText, textColor = MaterialTheme.colorScheme.onPrimary, bgColor = MaterialTheme.colorScheme.primary, onClick = onSaveSpeedCamera)
            }

        } else if (currentCreationMode == CreationMode.MANUAL) {
            Column(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.End
            ) {
                MapSideButton("START", textColor = MaterialTheme.colorScheme.onPrimary, bgColor = MaterialTheme.colorScheme.primary, isActive = activeMode == WaypointType.START, onClick = onAddStart)
                MapSideButton("+ CP", textColor = MaterialTheme.colorScheme.onSurface, bgColor = MaterialTheme.colorScheme.surfaceContainerLowest, hasBorder = true, isActive = activeMode == WaypointType.CHECKPOINT, onClick = onAddCheckpoint)

                if (selectedRaceType == RaceType.SPRINT) {
                    MapSideButton("FINISH", textColor = MaterialTheme.colorScheme.onPrimary, bgColor = MaterialTheme.colorScheme.primary, isActive = activeMode == WaypointType.FINISH, onClick = onAddFinish)
                }
            }

            Box(modifier = Modifier.align(Alignment.BottomEnd).padding(bottom = 15.dp, end = 16.dp)){
                MapSideButton("SALVEAZĂ", textColor = MaterialTheme.colorScheme.onPrimary, bgColor = MaterialTheme.colorScheme.primary, onClick = onSaveTrack)
            }
        } else {
            Column(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.End
            ) {
                if (!isRecording) {
                    MapSideButton("START REC", textColor = Color.White, bgColor = BrakeRed, onClick = onStartDriveRecording)
                } else {
                    MapSideButton("+ CP", textColor = MaterialTheme.colorScheme.onSurface, bgColor = MaterialTheme.colorScheme.surfaceContainerLowest, hasBorder = true, onClick = onRecordDriveCheckpoint)
                    MapSideButton("STOP & SAVE", textColor = Color.White, bgColor = MaterialTheme.colorScheme.primary, onClick = onStopDriveRecording)
                }
            }
        }

        // ==========================================
        // 3. BUTON ANULEAZĂ
        // ==========================================
        Box(modifier = Modifier.align(Alignment.BottomStart).padding(start = 42.dp, bottom = 15.dp)) {
            MapSideButton("ANULEAZĂ", textColor = BrakeRed, bgColor = MaterialTheme.colorScheme.surfaceContainerLowest, hasBorder = true, onClick = onCancel)
        }

        // ==========================================
        // 4. ZONA JOS-CENTRU: 3 TAB-URI
        // ==========================================
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 15.dp)
                .height(32.dp)
                .width(284.dp)
                .background(MaterialTheme.colorScheme.surfaceContainerLowest.copy(alpha = 0.85f), RoundedCornerShape(16.dp))
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp)),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .clip(RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp))
                .clickable { onRaceTypeChanged(RaceType.SPRINT) }
                .background(if (selectedRaceType == RaceType.SPRINT) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else Color.Transparent),
                contentAlignment = Alignment.Center
            ) {
                Text("SPRINT", color = if (selectedRaceType == RaceType.SPRINT) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Black, fontSize = 10.sp, letterSpacing = 1.sp)
            }

            Box(modifier = Modifier.width(1.dp).fillMaxHeight(0.5f).background(MaterialTheme.colorScheme.outlineVariant))

            Box(modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .clickable { onRaceTypeChanged(RaceType.LAP_RACE) }
                .background(if (selectedRaceType == RaceType.LAP_RACE) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else Color.Transparent),
                contentAlignment = Alignment.Center
            ) {
                Text("CIRCUIT", color = if (selectedRaceType == RaceType.LAP_RACE) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Black, fontSize = 10.sp, letterSpacing = 1.sp)
            }

            Box(modifier = Modifier.width(1.dp).fillMaxHeight(0.5f).background(MaterialTheme.colorScheme.outlineVariant))

            Box(modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .clip(RoundedCornerShape(topEnd = 16.dp, bottomEnd = 16.dp))
                .clickable { onRaceTypeChanged(RaceType.SPEED_TRAP) }
                .background(if (selectedRaceType == RaceType.SPEED_TRAP) BrakeRed.copy(alpha = 0.2f) else Color.Transparent),
                contentAlignment = Alignment.Center
            ) {
                Text("CAMERA", color = if (selectedRaceType == RaceType.SPEED_TRAP) BrakeRed else MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Black, fontSize = 10.sp, letterSpacing = 1.sp)
            }
        }
    }
}

@Composable
private fun MapSideButton(text: String, textColor: Color, bgColor: Color, hasBorder: Boolean = false, isActive: Boolean = false, onClick: () -> Unit) {
    val border = if (hasBorder) BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant) else null
    val infiniteTransition = rememberInfiniteTransition(label = "pulse_transition")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isActive) 0.3f else 1f,
        animationSpec = infiniteRepeatable(animation = tween(600, easing = LinearEasing), repeatMode = RepeatMode.Reverse),
        label = "pulse_alpha"
    )

    Button(
        onClick = onClick,
        shape = CutCornerShape(topStart = 8.dp, bottomEnd = 8.dp),
        colors = ButtonDefaults.buttonColors(containerColor = bgColor.copy(alpha = alpha), contentColor = textColor.copy(alpha = alpha)),
        border = border,
        contentPadding = PaddingValues(0.dp),
        modifier = Modifier.size(width = 90.dp, height = 44.dp)
    ) {
        Text(text, fontWeight = FontWeight.Black, fontSize = 11.sp, letterSpacing = 1.sp)
    }
}

@Composable
fun WaypointContextMneu(
    selectedRaceType: RaceType,
    onSelect: (WaypointType) -> Unit,
    onDismiss: () -> Unit
){
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ){ onDismiss() },
        contentAlignment = Alignment.Center
    ){
        Card(
            modifier = Modifier.width(220.dp).wrapContentHeight(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ){
            Column(
                modifier = Modifier.padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    "PLASEAZĂ PUNCT",
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    fontWeight = FontWeight.Black,
                    fontSize = 11.sp,
                    letterSpacing = 1.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                ContextMenuItem(
                    icon = "🚩",
                    label = "START",
                    onClick = { onSelect(WaypointType.START) }
                )

                ContextMenuItem(
                    icon = "📍",
                    label = "CHECKPOINT",
                    onClick = { onSelect(WaypointType.CHECKPOINT) }
                )

                if (selectedRaceType == RaceType.SPRINT) {
                    ContextMenuItem(
                        icon = "🏁",
                        label = "FINISH",
                        onClick = { onSelect(WaypointType.FINISH) }
                    )
                }
            }
        }
    }
}

@Composable
fun ContextMenuItem(icon: String, label: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ){
        Text(icon, fontSize = 18.sp)
        Text(
            label,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurface
        )

    }
}

// ==========================================
// 5. PREVIEW PENTRU ANDROID STUDIO
// ==========================================
@Preview(
    name = "Creation HUD - Circuit Mode",
    showBackground = true,
    backgroundColor = 0xFF12121D, // Fundalul întunecat al aplicației
    widthDp = 800, // Dimensiune Landscape orientativă
    heightDp = 360
)
@Composable
fun PreviewCreationHUD() {
    var mockRaceType by remember { mutableStateOf(RaceType.LAP_RACE) }

    FirstAppTheme(darkTheme = true) {
        CreationHUD(
            activeMode = WaypointType.START,
            onAddStart = {},
            onAddCheckpoint = {},
            onAddFinish = {},
            onSaveTrack = {},
            onCancel = {},
            selectedRaceType = mockRaceType,
            onRaceTypeChanged = { mockRaceType = it },
            isRecording = false,
            onStartDriveRecording = {},
            onRecordDriveCheckpoint = {},
            onStopDriveRecording = {},
            onSaveSpeedCamera = {}
        )
    }
}