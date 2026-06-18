package com.example.firstapp.map

import androidx.compose.runtime.*
import com.example.firstapp.AppState
import com.example.firstapp.data.RaceType
import com.example.firstapp.data.Track
import com.example.trackappv2.R
import com.huawei.hms.maps.HuaweiMap
import com.huawei.hms.maps.model.BitmapDescriptorFactory
import com.huawei.hms.maps.model.Marker
import com.huawei.hms.maps.model.MarkerOptions

@Composable
fun TrackMarkersOverlay(
    huaweiMap: HuaweiMap?,
    savedTracks: List<Track>,
    appState: AppState
) {
    val activeMarkers = remember { mutableListOf<Marker>() }

    LaunchedEffect(huaweiMap, savedTracks, appState) {
        val map = huaweiMap ?: return@LaunchedEffect

        // Curățăm mereu înainte să redesenăm
        activeMarkers.forEach { it.remove() }
        activeMarkers.clear()

        // Markerii apar DOAR în Cruise
        if (appState != AppState.CRUISE) return@LaunchedEffect

        savedTracks.forEach { track ->
            val icon = when (track.raceType) {
                RaceType.LAP_RACE -> BitmapDescriptorFactory.fromResource(R.drawable.circuit_icon)
                RaceType.SPEED_ZONE -> BitmapDescriptorFactory.fromResource(R.drawable.speedzone_icon)
                RaceType.SPEED_TRAP -> BitmapDescriptorFactory.fromResource(R.drawable.speedcamera_icon)// Iconița ta pentru Zone
                else -> BitmapDescriptorFactory.fromResource(R.drawable.sprint_icon)
            }

            val markerOptions = MarkerOptions()
                .position(track.start.toLatLng())
                .title(track.name)
                .snippet(if (track.raceType == RaceType.LAP_RACE) "Circuit" else "Sprint")
                .icon(icon)
                .anchor(0.5f, 1.0f)

            map.addMarker(markerOptions)?.let { marker ->
                activeMarkers.add(marker)
            }
        }
    }

    // Cleanup la distrugere
    DisposableEffect(Unit) {
        onDispose {
            activeMarkers.forEach { it.remove() }
            activeMarkers.clear()
        }
    }
}