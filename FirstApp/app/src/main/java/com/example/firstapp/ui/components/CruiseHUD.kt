package com.example.firstapp.ui.components

import android.app.Activity
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
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
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
    val context = LocalContext.current
    var mainMenuExpanded by remember { mutableStateOf(false) }
    var settingsExpanded by remember { mutableStateOf(false) }

    // Helper pentru a ascunde status bar-ul
    fun hideSystemBars() {
        val window = (context as? Activity)?.window ?: return
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        window.navigationBarColor = android.graphics.Color.TRANSPARENT
        val controller = WindowCompat.getInsetsController(window, window.decorView)
        controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller.hide(WindowInsetsCompat.Type.systemBars())
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .padding(top = 24.dp)
    ) {
        // ── STÂNGA-SUS: Meniul Principal ─────────────────────────
        Box(modifier = Modifier.align(Alignment.TopStart)) {
            IconButton(onClick = {
                mainMenuExpanded = true
                hideSystemBars()
            }) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_menu),
                    contentDescription = "Meniu",
                    tint = Color.White
                )
            }

            DropdownMenu(
                expanded = mainMenuExpanded,
                onDismissRequest = {
                    mainMenuExpanded = false
                    hideSystemBars()
                },
                modifier = Modifier.background(MaterialTheme.colorScheme.surfaceContainer)
            ) {
                DropdownMenuItem(
                    text = { Text("Trasee Salvate", color = MaterialTheme.colorScheme.onSurface) },
                    onClick = {
                        mainMenuExpanded = false
                        hideSystemBars()
                        onSavedTracksClick()
                    }
                )
                HorizontalDivider(color = com.example.firstapp.ui.theme.OutlineVariant)
                DropdownMenuItem(
                    text = { Text("Istoric Curse", color = MaterialTheme.colorScheme.onSurface) },
                    onClick = {
                        mainMenuExpanded = false
                        hideSystemBars()
                        onHistoryClick()
                    }
                )
            }
        }

        // ── DREAPTA-SUS: Meniul Setări ───────────────────────────
        Box(modifier = Modifier.align(Alignment.TopEnd)) {
            IconButton(onClick = {
                settingsExpanded = true
                hideSystemBars()
            }) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_settings),
                    contentDescription = "Setări",
                    tint = Color.White
                )
            }

            DropdownMenu(
                expanded = settingsExpanded,
                onDismissRequest = {
                    settingsExpanded = false
                    hideSystemBars()
                },
                modifier = Modifier.background(MaterialTheme.colorScheme.surfaceContainer)
            ) {
                DropdownMenuItem(
                    text = { Text("Creare Activitate Noua", color = MaterialTheme.colorScheme.onSurface) },
                    onClick = {
                        settingsExpanded = false
                        hideSystemBars()
                        onCreateTrackClick()
                    }
                )
                HorizontalDivider(color = com.example.firstapp.ui.theme.OutlineVariant)
                DropdownMenuItem(
                    text = { Text("Setări Aplicație", color = MaterialTheme.colorScheme.onSurface) },
                    onClick = {
                        settingsExpanded = false
                        hideSystemBars()
                        onSettingsClick()
                    }
                )
            }
        }

        // ── JOS-CENTRU: Quick Race ───────────────────────────────
        VelocityPrimaryButton(
            text = "QUICK RACE",
            onClick = onQuickRaceClick,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp)
        )
    }
}

@Preview(
    showBackground = true,
    backgroundColor = 0xFF121212, // Un fundal închis pentru a simula harta de noapte
    name = "Cruise HUD Preview"
)
@Composable
fun CruiseHUDPreview() {
    // Îmbrăcăm previzualizarea într-o temă întunecată (Dark Theme)
    MaterialTheme(colorScheme = darkColorScheme()) {
        CruiseHUD(
            onSavedTracksClick = { /* Acțiune simulată */ },
            onHistoryClick = { /* Acțiune simulată */ },
            onSettingsClick = { /* Acțiune simulată */ },
            onCreateTrackClick = { /* Acțiune simulată */ },
            onQuickRaceClick = { /* Acțiune simulată */ },
            modifier = Modifier.fillMaxSize()
        )
    }
}