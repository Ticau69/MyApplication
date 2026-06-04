package com.example.firstapp.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.firstapp.data.RaceType
import com.example.firstapp.ui.theme.BrakeRed

enum class CreationMode { MANUAL, DRIVE }

@Composable
fun CreationHUD(
    onAddStart: () -> Unit,
    onAddCheckpoint: () -> Unit,
    onAddFinish: () -> Unit,
    onSaveTrack: () -> Unit,
    onCancel: () -> Unit,
    selectedRaceType: RaceType,
    onRaceTypeChanged: (RaceType) -> Unit,
    modifier: Modifier = Modifier
) {
    var currentCreationMode by remember { mutableStateOf(CreationMode.MANUAL) }

    Box(modifier = modifier.fillMaxSize()) {

        // ==========================================
        // 1. ZONA SUS: Mod Creare (Rămâne neschimbată)
        // ==========================================
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

        // ==========================================
        // 2. ZONA DREAPTA: Pini Hartă + Buton Salvează
        // ==========================================
        Column(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.End
        ) {
            // Uneltele pentru traseu
            MapSideButton("START", textColor = MaterialTheme.colorScheme.onPrimary, bgColor = MaterialTheme.colorScheme.primary, onClick = onAddStart)
            MapSideButton("+ CP", textColor = MaterialTheme.colorScheme.onSurface, bgColor = MaterialTheme.colorScheme.surfaceContainerLowest, hasBorder = true, onClick = onAddCheckpoint)
            MapSideButton("FINISH", textColor = MaterialTheme.colorScheme.onPrimary, bgColor = MaterialTheme.colorScheme.primary, onClick = onAddFinish)

            // O mică bară despărțitoare vizuală pentru a separa logica de sistem (Salvare)
            Spacer(modifier = Modifier.height(4.dp))
            Box(modifier = Modifier.width(40.dp).height(2.dp).background(MaterialTheme.colorScheme.outlineVariant))
            Spacer(modifier = Modifier.height(4.dp))

            // Butonul Salvează (aceeași dimensiune)
            MapSideButton("SALVEAZĂ", textColor = MaterialTheme.colorScheme.onPrimary, bgColor = MaterialTheme.colorScheme.primary, onClick = onSaveTrack)
        }

        // ==========================================
        // 3. ZONA JOS-STÂNGA: Buton Anulează
        // ==========================================
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 50.dp, bottom = 35.dp)
        ) {
            MapSideButton("ANULEAZĂ", textColor = BrakeRed, bgColor = MaterialTheme.colorScheme.surfaceContainerLowest, hasBorder = true, onClick = onCancel)
        }

        // ==========================================
        // 4. ZONA JOS-CENTRU: Sprint / Circuit (Comutator compact)
        // ==========================================
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp)
                .height(38.dp) // Design micuț și integrat
                .background(MaterialTheme.colorScheme.surfaceContainerLowest.copy(alpha = 0.85f), RoundedCornerShape(19.dp))
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(19.dp)),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier
                .fillMaxHeight()
                .clip(RoundedCornerShape(topStart = 19.dp, bottomStart = 19.dp))
                .clickable { onRaceTypeChanged(RaceType.SPRINT) }
                .background(if (selectedRaceType == RaceType.SPRINT) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else Color.Transparent)
                .padding(horizontal = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("SPRINT", color = if (selectedRaceType == RaceType.SPRINT) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Black, fontSize = 10.sp, letterSpacing = 1.sp)
            }

            Box(modifier = Modifier.width(1.dp).fillMaxHeight(0.5f).background(MaterialTheme.colorScheme.outlineVariant))

            Box(modifier = Modifier
                .fillMaxHeight()
                .clip(RoundedCornerShape(topEnd = 19.dp, bottomEnd = 19.dp))
                .clickable { onRaceTypeChanged(RaceType.LAP_RACE) }
                .background(if (selectedRaceType == RaceType.LAP_RACE) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else Color.Transparent)
                .padding(horizontal = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("CIRCUIT", color = if (selectedRaceType == RaceType.LAP_RACE) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Black, fontSize = 10.sp, letterSpacing = 1.sp)
            }
        }
    }
}

// --- Componentă modificată pentru a accepta culori și a fi reutilizabilă ---
@Composable
private fun MapSideButton(
    text: String,
    textColor: Color,
    bgColor: Color,
    hasBorder: Boolean = false,
    onClick: () -> Unit
) {
    val border = if (hasBorder) BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant) else null

    Button(
        onClick = onClick,
        shape = CutCornerShape(topStart = 8.dp, bottomEnd = 8.dp),
        colors = ButtonDefaults.buttonColors(containerColor = bgColor, contentColor = textColor),
        border = border,
        contentPadding = PaddingValues(0.dp), // Eliminăm padding-ul intern
        modifier = Modifier.size(width = 90.dp, height = 44.dp) // Dimensiuni fixe pentru TOATE butoanele
    ) {
        Text(text, fontWeight = FontWeight.Black, fontSize = 11.sp, letterSpacing = 1.sp)
    }
}