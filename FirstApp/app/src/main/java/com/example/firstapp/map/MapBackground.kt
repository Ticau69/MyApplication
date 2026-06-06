package com.example.firstapp.map

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Bundle
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.graphics.createBitmap
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.firstapp.AppState
import com.example.firstapp.data.RaceType
import com.example.firstapp.data.TrackRepository
import com.example.trackappv2.R
import com.huawei.hms.maps.CameraUpdateFactory
import com.huawei.hms.maps.HuaweiMap
import com.huawei.hms.maps.MapView
import com.huawei.hms.maps.model.BitmapDescriptor
import com.huawei.hms.maps.model.BitmapDescriptorFactory
import com.huawei.hms.maps.model.CameraPosition
import com.huawei.hms.maps.model.LatLng
import com.huawei.hms.maps.model.MapStyleOptions
import com.huawei.hms.maps.model.Marker
import com.huawei.hms.maps.model.MarkerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlin.time.Duration.Companion.seconds

@Composable
fun MapBackground(
    latLng: LatLng?,
    bearing: Float,
    appState: AppState,
    onMapReady: (HuaweiMap) -> Unit = {}
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val mapView = remember { MapView(context) }
    var huaweiMapInstance by remember { mutableStateOf<HuaweiMap?>(null) }
    var playerMarker by remember { mutableStateOf<Marker?>(null) }

    var isFollowingUser by remember { mutableStateOf(true) }
    var isFirstLoad by remember { mutableStateOf(true) }
    // Folosim un Ref în loc de State — lambda-urile din HMS pot citi
    // mereu valoarea curentă fără să fie recapturate
    val isFollowingRef = remember { mutableStateOf(true) }
    val lastInteractionRef = remember { mutableLongStateOf(0L) }

    // Smoothing pentru bearing — reducem numărul de triggere
    // Actualizăm camera doar când bearing-ul s-a schimbat cu mai mult de 2 grade
    val lastAppliedBearing = remember { mutableFloatStateOf(0f) }
    val bearingDelta = kotlin.math.abs(bearing - lastAppliedBearing.floatValue)
    val shouldUpdateBearing = (bearingDelta > 5f) && (bearingDelta < 355f)

    val savedTrackMarkers = remember { mutableListOf<Marker>() }
    val savedTrackIcon = vectorToBitmap(context, R.drawable.ic_saved_track)

    // Gestionăm lifecycle-ul MapView-ului manual pentru a ne asigura că este inițializat corect.
    // Compose poate recomune MapBackground după ce ON_CREATE a trecut, deci sincronizăm starea.
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_CREATE -> mapView.onCreate(Bundle())
                Lifecycle.Event.ON_START -> mapView.onStart()
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                Lifecycle.Event.ON_STOP -> mapView.onStop()
                Lifecycle.Event.ON_DESTROY -> mapView.onDestroy()
                else -> {}
            }
        }
        val currentState = lifecycleOwner.lifecycle.currentState
        if (currentState.isAtLeast(Lifecycle.State.CREATED)) {
            try { mapView.onCreate(Bundle()) } catch (e: Exception) {}
        }
        if (currentState.isAtLeast(Lifecycle.State.STARTED)) mapView.onStart()
        if (currentState.isAtLeast(Lifecycle.State.RESUMED)) mapView.onResume()

        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Timer reactivare urmărire
    LaunchedEffect(lastInteractionRef.longValue) {
        if (lastInteractionRef.longValue == 0L) return@LaunchedEffect
        isFollowingRef.value = false
        isFollowingUser = false
        delay(5.seconds)
        isFollowingRef.value = true
        isFollowingUser = true
    }

    LaunchedEffect(appState, huaweiMapInstance) {
        val map = huaweiMapInstance ?: return@LaunchedEffect

        // Dacă ieșim din modul Cruise (ex: intrăm în Editare sau în Cursă), curățăm harta
        if (appState != AppState.CRUISE) {
            savedTrackMarkers.forEach { it.remove() }
            savedTrackMarkers.clear()
            return@LaunchedEffect
        }

        // Dacă suntem în Cruise, aducem cursele pe hartă
        try {
            // Ștergem pinii vechi înainte să punem unii noi, ca să nu se dubleze
            savedTrackMarkers.forEach { it.remove() }
            savedTrackMarkers.clear()

            // Extragem traseele pe un thread de background pentru a nu bloca UI-ul
            val tracks: List<com.example.firstapp.data.Track> = withContext(Dispatchers.IO) {
                val repo = TrackRepository(context)
                // AICI: Înlocuiește getTracks() cu numele exact al funcției tale din repo!
                // Dacă nu ai încă o funcție care returnează lista, va trebui să o creăm.
                repo.getTracks()
            }



            tracks.forEach { track ->
                val marker = map.addMarker(
                    MarkerOptions()
                        .position(track.start.toLatLng())
                        .icon(savedTrackIcon)
                        .title(track.name) // Se va afișa numele traseului la click
                        .snippet(if (track.raceType == RaceType.LAP_RACE) "Circuit (Ture)" else "Sprint (A → B)")
                        .anchorMarker(0.5f, 1f)
                )
                if (marker != null) {
                    marker.tag = track.id // Ascundem ID-ul în tag-ul markerului pentru viitor
                    savedTrackMarkers.add(marker)
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("MapBackground", "Eroare la adăugarea pinilor de start: ${e.message}")
        }
    }

    // Actualizare marker — SEPARAT de cameră, rulează la fiecare locație nouă
    LaunchedEffect(latLng, bearing) {
        val map = huaweiMapInstance ?: return@LaunchedEffect
        val pos = latLng ?: return@LaunchedEffect
        if (pos.latitude == 0.0 && pos.longitude == 0.0) return@LaunchedEffect

        if (playerMarker == null) {
            val icon = vectorToBitmap(context, R.drawable.ic_nav_arrow)
            playerMarker = map.addMarker(
                MarkerOptions()
                    .position(pos)
                    .icon(icon)
                    .anchorMarker(0.5f, 0.5f)
                    .flat(true)
            )
        } else {
            playerMarker?.position = pos
            playerMarker?.rotation = bearing
        }
    }

    // Camera — SEPARAT de marker, cu throttle pe bearing
    LaunchedEffect(latLng, shouldUpdateBearing, huaweiMapInstance, appState, isFollowingUser) {
        val map = huaweiMapInstance ?: return@LaunchedEffect
        val pos = latLng ?: return@LaunchedEffect
        if (pos.latitude == 0.0 && pos.longitude == 0.0) return@LaunchedEffect
        if (!isFollowingUser) return@LaunchedEffect
        if (appState == AppState.RACE_CREATION ||
            appState == AppState.SAVED_TRACKS ||
            appState == AppState.HISTORY) return@LaunchedEffect

        val currentCameraPos = map.cameraPosition.target
        val results = FloatArray(1)
        android.location.Location.distanceBetween(
            currentCameraPos.latitude, currentCameraPos.longitude,
            pos.latitude, pos.longitude, results
        )
        val distance = results[0]

        if (isFirstLoad) {
            val cameraPosition = CameraPosition.Builder()
                .target(pos)
                .zoom(18.5f)
                .bearing(bearing)
                .tilt(60f)
                .build()
            map.moveCamera(CameraUpdateFactory.newCameraPosition(cameraPosition))
            isFirstLoad = false
            lastAppliedBearing.floatValue = bearing
            return@LaunchedEffect
        }

        if (distance < 20f && !shouldUpdateBearing) return@LaunchedEffect

        lastAppliedBearing.floatValue = bearing

        val cameraPosition = CameraPosition.Builder()
            .target(pos)
            .zoom(18.5f)
            .bearing(bearing)
            .tilt(60f)
            .build()

        map.animateCamera(
            CameraUpdateFactory.newCameraPosition(cameraPosition),
            500,
            null
        )
    }

    AndroidView(
        factory = {
            mapView.getMapAsync { hMap ->
                huaweiMapInstance = hMap
                onMapReady(hMap)

                hMap.setOnCameraMoveStartedListener { reason ->
                    if (reason == HuaweiMap.OnCameraMoveStartedListener.REASON_GESTURE) {
                        lastInteractionRef.longValue = System.currentTimeMillis()
                    }
                }

                try {
                    val style = MapStyleOptions.loadRawResourceStyle(context, R.raw.mapstyle_night)
                    hMap.setMapStyle(style)

                    hMap.isMyLocationEnabled = false
                    hMap.uiSettings.isMyLocationButtonEnabled = false
                    hMap.uiSettings.isZoomControlsEnabled = false
                    hMap.uiSettings.isCompassEnabled = false

                } catch (_: SecurityException) {}
            }
            mapView
        },
        modifier = Modifier.fillMaxSize()
    )

    if (!isFollowingUser) {
        Box(modifier = Modifier.fillMaxSize()) {
            FloatingActionButton(
                onClick = {
                    isFollowingRef.value = true
                    isFollowingUser = true
                    lastInteractionRef.longValue = 0L
                },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(bottom = 120.dp, end = 16.dp),
                containerColor = MaterialTheme.colorScheme.surfaceContainer,
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_recenter),
                    contentDescription = "Recentrează"
                )
            }
        }
    }
}

// Funcție ajutătoare pentru a converti vectorul .xml al săgeții într-un Bitmap pe care Harta îl poate înțelege
private fun vectorToBitmap(context: Context, drawableRes: Int): BitmapDescriptor {
    val drawable = ContextCompat.getDrawable(context, drawableRes) ?: return BitmapDescriptorFactory.defaultMarker()
    // Folosim aceiași parametri pentru a asigura dimensiunea corectă
    val bitmap = createBitmap(
        drawable.intrinsicWidth,
        drawable.intrinsicHeight,
        Bitmap.Config.ARGB_8888,
    )
    val canvas = Canvas(bitmap)
    drawable.setBounds(0, 0, canvas.width, canvas.height)
    drawable.draw(canvas)
    return BitmapDescriptorFactory.fromBitmap(bitmap)
}