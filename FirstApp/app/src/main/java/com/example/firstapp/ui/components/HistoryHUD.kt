package com.example.firstapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.trackappv2.R

@Composable
fun HistoryHUD(
    // Aici ideal ar fi să trimiți lista de RaceRecord, dar o lăsăm generică pentru moment
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
                    Icon(painter = painterResource(id = R.drawable.ic_close), contentDescription = "Închide", tint = Color.White)
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "Istoricul curselor va apărea aici.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyLarge
            )
            // Aici vei putea adăuga LazyColumn-ul tău cu istoricul exact ca la SavedTracksHUD
        }
    }
}