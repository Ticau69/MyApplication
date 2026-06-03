package com.example.firstapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.trackappv2.R

@Composable
fun CruiseHUD(
    onSavedTracksClick: () -> Unit,
    onHistoryClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onCreateTrackClick: () -> Unit,
    onQuickRaceClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Stări pentru ambele meniuri (Stânga și Dreapta)
    var mainMenuExpanded by remember { mutableStateOf(false) }
    var settingsExpanded by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .padding(top = 24.dp)
    ) {
        // ========================================================
        // --- COLȚUL STÂNGA-SUS: Meniul Principal (Sandwich) ---
        // ========================================================
        Box(modifier = Modifier.align(Alignment.TopStart)) {
            IconButton(onClick = { mainMenuExpanded = true }) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_menu),
                    contentDescription = "Meniu",
                    tint = Color.White
                )
            }

            DropdownMenu(
                expanded = mainMenuExpanded,
                onDismissRequest = { mainMenuExpanded = false },
                modifier = Modifier.background(MaterialTheme.colorScheme.surfaceContainer)
            ) {
                DropdownMenuItem(
                    text = { Text("ÎNCHIDE", color = com.example.firstapp.ui.theme.BrakeRed, fontWeight = FontWeight.Bold) },
                    trailingIcon = {
                        Icon(painter = painterResource(id = R.drawable.ic_close), contentDescription = null, tint = com.example.firstapp.ui.theme.BrakeRed)
                    },
                    onClick = { mainMenuExpanded = false }
                )
                HorizontalDivider(color = com.example.firstapp.ui.theme.OutlineVariant)
                DropdownMenuItem(
                    text = { Text("Trasee Salvate", color = MaterialTheme.colorScheme.onSurface) },
                    onClick = {
                        mainMenuExpanded = false
                        onSavedTracksClick()
                    }
                )
                DropdownMenuItem(
                    text = { Text("Istoric Curse", color = MaterialTheme.colorScheme.onSurface) },
                    onClick = {
                        mainMenuExpanded = false
                        onHistoryClick()
                    }
                )
            }
        }

        // ========================================================
        // --- COLȚUL DREAPTA-SUS: Meniul de Setări (Gear) ---
        // ========================================================
        Box(modifier = Modifier.align(Alignment.TopEnd)) {
            IconButton(onClick = { settingsExpanded = true }) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_settings), // Iconița nouă
                    contentDescription = "Setări",
                    tint = Color.White
                )
            }

            DropdownMenu(
                expanded = settingsExpanded,
                onDismissRequest = { settingsExpanded = false },
                modifier = Modifier.background(MaterialTheme.colorScheme.surfaceContainer)
            ) {
                DropdownMenuItem(
                    text = { Text("ÎNCHIDE", color = com.example.firstapp.ui.theme.BrakeRed, fontWeight = FontWeight.Bold) },
                    trailingIcon = {
                        Icon(painter = painterResource(id = R.drawable.ic_close), contentDescription = null, tint = com.example.firstapp.ui.theme.BrakeRed)
                    },
                    onClick = { settingsExpanded = false }
                )
                HorizontalDivider(color = com.example.firstapp.ui.theme.OutlineVariant)

                // Opțiunea ta de creare traseu s-a mutat aici!
                DropdownMenuItem(
                    text = { Text("Creare Traseu Nou", color = MaterialTheme.colorScheme.onSurface) },
                    onClick = {
                        settingsExpanded = false
                        onCreateTrackClick()
                    }
                )
                DropdownMenuItem(
                    text = { Text("Setări Aplicație", color = MaterialTheme.colorScheme.onSurface) },
                    onClick = {
                        settingsExpanded = false
                        onSettingsClick()
                    }
                )
            }
        }

        // ========================================================
        // --- JOS-CENTRU: Quick Race ---
        // ========================================================
        VelocityPrimaryButton(
            text = "QUICK RACE",
            onClick = onQuickRaceClick,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp)
        )
    }
}