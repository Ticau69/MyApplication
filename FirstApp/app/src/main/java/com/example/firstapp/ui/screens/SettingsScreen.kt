package com.example.firstapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    isTtsEnabled: Boolean,
    proximityRadius: Int,
    onTtsToggle: (Boolean) -> Unit,
    onRadiusChange: (Int) -> Unit,
    onBackClick: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "SETĂRI APLICAȚIE",
                        fontWeight = FontWeight.Black,
                        fontStyle = FontStyle.Italic,
                        letterSpacing = 1.sp
                    )
                },
                navigationIcon = {
                    Text(
                        text = "← ÎNAPOI",
                        modifier = Modifier
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .clickable { onBackClick() },
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.surface)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {

            // ==========================================
            // CATEGORIA 1: SETĂRI GENERALE
            // ==========================================
            SettingsSection(title = "SETĂRI GENERALE") {
                SettingsClickableItem(
                    title = "Unitate de măsură viteză",
                    subtitle = "Metric (km/h)",
                    onClick = { /* Viitoare implementare */ }
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                SettingsClickableItem(
                    title = "Temă aplicație",
                    subtitle = "Întunecat (Motorsport Dark)",
                    onClick = { /* Viitoare implementare */ }
                )
            }

            // ==========================================
            // CATEGORIA 2: COPILOT AUDIO & SUNTET
            // ==========================================
            SettingsSection(title = "COPILOT AUDIO & TELEMETRIE") {
                SettingsToggleItem(
                    title = "Copilot Vocal (TTS)",
                    subtitle = "Anunță timpii delta și checkpoint-urile prin difuzoare",
                    checked = isTtsEnabled,
                    onCheckedChange = onTtsToggle
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                SettingsClickableItem(
                    title = "Limbă ghidaj vocal",
                    subtitle = "Română (RO)",
                    enabled = isTtsEnabled,
                    onClick = { /* Viitoare implementare */ }
                )
            }

            // ==========================================
            // CATEGORIA 3: ECRAN CRUISE & RADAR
            // ==========================================
            SettingsSection(title = "MOD CRUISE (DETECTARE)") {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Rază scanare evenimente: $proximityRadius metri",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Text(
                        text = "Distanța de la care telefonul îți arată cardul de aliniere pentru o cursă apropiată",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Slider(
                        value = proximityRadius.toFloat(),
                        onValueChange = { onRadiusChange(it.toInt()) },
                        valueRange = 50f..500f,
                        steps = 8 // Salturi din 50 în 50 de metri
                    )
                }
            }

            // Versiune App la bază
            Text(
                text = "VelocityApp v2.5 • Build Motorsport",
                modifier = Modifier.align(Alignment.CenterHorizontally),
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                fontSize = 12.sp
            )
        }
    }
}

// ==========================================
// COMPONENTE REUTILIZABILE (DESIGN SYSTEM)
// ==========================================

@Composable
fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = title,
            fontSize = 11.sp,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.primary,
            letterSpacing = 2.sp,
            modifier = Modifier.padding(start = 4.dp)
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(CutCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), CutCornerShape(8.dp))
        ) {
            content()
        }
    }
}

@Composable
fun SettingsToggleItem(title: String, subtitle: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
            Text(text = title, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Text(text = subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
fun SettingsClickableItem(title: String, subtitle: String, enabled: Boolean = true, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled) { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
            )
            Text(
                text = subtitle,
                color = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                fontSize = 12.sp
            )
        }
        Text(text = "›", fontSize = 20.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

// ==========================================
// PREVIEW
// ==========================================
@Preview(showBackground = true)
@Composable
fun PreviewSettingsScreen() {
    MaterialTheme(colorScheme = darkColorScheme()) {
        SettingsScreen(
            isTtsEnabled = true,
            proximityRadius = 200,
            onTtsToggle = {},
            onRadiusChange = {},
            onBackClick = {}
        )
    }
}