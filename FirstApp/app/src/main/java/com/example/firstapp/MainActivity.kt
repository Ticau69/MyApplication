package com.example.firstapp

import android.Manifest
import android.content.Context
import com.example.trackappv2.R
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.firstapp.map.MapBackground
import com.example.firstapp.sensors.BearingSensorEffect
import com.example.firstapp.service.LocationForegroundService
import com.example.firstapp.ui.ComposeHuaweiMap
import com.example.firstapp.ui.FSMOverlay
import com.example.firstapp.ui.components.GpsDisabledDialog
import com.example.firstapp.ui.components.RaceFinishDialog
import com.huawei.hms.maps.HuaweiMap
import com.huawei.hms.maps.model.MapStyleOptions
import com.huawei.hms.maps.MapsInitializer
import com.huawei.hms.maps.model.BitmapDescriptor
import com.huawei.hms.maps.model.BitmapDescriptorFactory
import android.graphics.Canvas
import com.example.firstapp.data.RaceType

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)

        // 2. Ascundem Bara de Status și Bara de Navigație (Modul Imersiv)
        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)

        // Setăm comportamentul: barele apar doar dacă tragi de margini și dispar singure
        windowInsetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

        // Ascundem tot (și status bar sus, și navigația jos)
        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())

        // 3. Permitem aplicației să deseneze complet sub zona camerei frontale (notch/breton)
        window.attributes = window.attributes.apply {
            layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }

        setContent {
            // Dacă am intrat din notificare, forțăm aplicația în CRUISE
            val shouldStartCruise = intent.getBooleanExtra("START_CRUISE_MODE", false)

            com.example.firstapp.ui.theme.FirstAppTheme(darkTheme = true) {
                RaceTrackerApp(shouldStartCruise = shouldStartCruise)
            }
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            val controller = WindowCompat.getInsetsController(window, window.decorView)
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            controller.hide(WindowInsetsCompat.Type.systemBars())
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        if (isFinishing) {
            stopService(Intent(this, LocationForegroundService::class.java))
        }
    }
}



@Composable
fun RaceTrackerApp(shouldStartCruise: Boolean = false) {
    val context = LocalContext.current
    val viewModel: AppViewModel = viewModel()
    val lifecycleOwner = LocalLifecycleOwner.current

    // State din ViewModel
    val currentState by viewModel.appState.collectAsState()
    val currentLatLng by viewModel.currentLatLng.collectAsState()
    val currentBearing by viewModel.currentBearing.collectAsState()
    val currentSpeed by viewModel.currentSpeed.collectAsState()
    val hasLocationPermission by viewModel.hasLocationPermission.collectAsState()
    val isGpsEnabled by viewModel.isGpsEnabled.collectAsState()
    val selectedTrack by viewModel.selectedTrack.collectAsState()
    val raceFinishData by viewModel.raceFinishData.collectAsState()
    var mapInstance by remember { mutableStateOf<HuaweiMap?>(null) }
    val currentMarkers = remember { mutableListOf<com.huawei.hms.maps.model.Marker>() }

    // Permisiuni
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
        viewModel.onPermissionResult(granted)
    }
    val notificationPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }

    val savedTracks by viewModel.savedTracks.collectAsState()

    val countdownValue by viewModel.countdownValue.collectAsState()
    val isCountingDown by viewModel.isCountingDown.collectAsState()
    val allLaps by viewModel.allLaps.collectAsState()

    // Controlează dacă suntem la prima afișare pe hartă (pentru a evita zborul de la 0,0)
    var isFirstLocationUpdate by remember { mutableStateOf(true) }
    // Controlează dacă harta urmărește mașina sau dacă utilizatorul explorează manual
    var isCameraLockedToCar by remember { mutableStateOf(true) }

    val activity = context as? android.app.Activity
    LaunchedEffect(currentState) {
        activity?.window?.let { window ->
            val controller = WindowCompat.getInsetsController(window, window.decorView)
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            controller.hide(WindowInsetsCompat.Type.systemBars())
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.checkGpsStatus()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(savedTracks, currentState, mapInstance) {
        val map = mapInstance ?: return@LaunchedEffect

        // 1. Curățăm harta de markerele vechi
        currentMarkers.forEach { it.remove() }
        currentMarkers.clear()

        // 2. Desenăm markerele doar în modul CRUISE
        if (currentState == AppState.CRUISE) {
            savedTracks.forEach { track ->

                // Alegem poza corectă direct din folderul drawable
                // (Înlocuiește R.drawable.nume... cu numele reale ale pozelor tale)
                val iconDescriptor = if (track.raceType == RaceType.LAP_RACE) {
                    com.huawei.hms.maps.model.BitmapDescriptorFactory.fromResource(R.drawable.circuit_icon)
                } else {
                    com.huawei.hms.maps.model.BitmapDescriptorFactory.fromResource(R.drawable.sprint_icon)
                }

                // Generăm setările marker-ului
                val markerOptions = com.huawei.hms.maps.model.MarkerOptions()
                    .position(track.start.toLatLng())
                    .title(track.name)
                    .snippet(if (track.raceType == RaceType.LAP_RACE) "Circuit" else "Sprint")
                    .icon(iconDescriptor)
                    // anchor(0.5f, 1.0f) pune "vârful" de jos al pin-ului fix pe coordonată.
                    // Dacă pin-ul tău e perfect rotund, folosește anchor(0.5f, 0.5f)
                    .anchor(0.5f, 1.0f)

                // Adăugăm pe hartă și salvăm referința
                map.addMarker(markerOptions)?.let { marker ->
                    currentMarkers.add(marker)
                }
            }
        }
    }

    // Sincronizăm camera DOAR când primim un update GPS nou

    LaunchedEffect(Unit) {
        val alreadyGranted = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (alreadyGranted) viewModel.onPermissionResult(true)
        else permissionLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        viewModel.checkGpsStatus()
    }

    LaunchedEffect(shouldStartCruise) {
        if (shouldStartCruise && currentState == AppState.CRUISE) {
            viewModel.transitionTo(AppState.CRUISE)
        }
    }

    // Sincronizăm camera hărții cu poziția și direcția reală a mașinii
    LaunchedEffect(currentLatLng, mapInstance) {
        val map = mapInstance
        val latLng = currentLatLng

        if (map != null && latLng != null) {
            // 1. Evităm zborul din "Null Island" (Oceanul Atlantic)
            if (latLng.latitude == 0.0 && latLng.longitude == 0.0) return@LaunchedEffect

            // 2. Mișcăm camera doar dacă NU ești în modul explorare cu degetul
            if (isCameraLockedToCar) {
                val cameraPosition = com.huawei.hms.maps.model.CameraPosition.Builder()
                    .target(latLng)
                    .zoom(17f)
                    .tilt(45f)
                    .bearing(currentBearing)
                    .build()

                if (isFirstLocationUpdate) {
                    // Salt INSTANT la poziția ta curentă (rezolvă zborul animat deranjant)
                    map.moveCamera(com.huawei.hms.maps.CameraUpdateFactory.newCameraPosition(cameraPosition))
                    isFirstLocationUpdate = false
                } else {
                    // Urmărire lină (fără sacadări) în timpul condusului
                    map.animateCamera(com.huawei.hms.maps.CameraUpdateFactory.newCameraPosition(cameraPosition))
                }
            }
        }
    }

    BearingSensorEffect(
        appState = currentState,
        onBearingChanged = { bearing -> viewModel.updateBearing(bearing) }
    )

    if (!isGpsEnabled) {
        GpsDisabledDialog(
            onOpenSettings = { context.startActivity(Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS)) },
            onDismiss = { viewModel.checkGpsStatus() }
        )
    }

    if (hasLocationPermission) {
        // Containerul principal - tot ce e aici se suprapune
        Box(modifier = Modifier.fillMaxSize()) {

            // 1. STRATUL DE BAZĂ: Harta Huawei
            ComposeHuaweiMap(
                modifier = Modifier.fillMaxSize(),
                onMapReady = { readyMap ->
                    mapInstance = readyMap

                    // 1. Încărcăm stilul personalizat din res/raw/
                    try {
                        // ÎNLOCUIEȘTE "map_style" cu numele real al fișierului tău JSON (fără extensia .json)
                        val styleOptions = MapStyleOptions.loadRawResourceStyle(context, R.raw.mapstyle_night)
                        val success = readyMap.setMapStyle(styleOptions)

                        if (!success) {
                            // Fallback de siguranță: dacă JSON-ul are erori, trecem pe Dark Mode default
                            readyMap.mapType = com.huawei.hms.maps.HuaweiMap.MAP_TYPE_NORMAL
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }

                    readyMap.setOnCameraMoveStartedListener { reason ->
                        if (reason == com.huawei.hms.maps.HuaweiMap.OnCameraMoveStartedListener.REASON_GESTURE) {
                            isCameraLockedToCar = false
                            readyMap.stopAnimation() // <--- NOU: Oprim imediat zbaterea hărții
                        }
                    }

                    // 2. Curățăm butoanele default ale hărții
                    readyMap.uiSettings.apply {
                        isZoomControlsEnabled = false
                        isCompassEnabled = false
                        isMyLocationButtonEnabled = false
                        isMapToolbarEnabled = false
                    }

                    // 3. Activăm punctul albastru al utilizatorului
                    try {
                        readyMap.isMyLocationEnabled = true
                    } catch (e: SecurityException) {
                        e.printStackTrace()
                    }
                }
            )

            // 2. STRATUL SUPERIOR: Interfața (Butoane, Vitezometru, Meniuri)
            FSMOverlay(
                viewModel = viewModel,
                state = currentState,
                speed = currentSpeed,
                latLng = currentLatLng,
                huaweiMap = mapInstance,
                selectedTrack = selectedTrack,
                onStateChange = { viewModel.transitionTo(it) }
            )

            // 3. STRATUL DE TOP: Dialoguri și Numărătoare inversă
            CountdownOverlay(value = countdownValue)

            raceFinishData?.let { data ->
                RaceFinishDialog(
                    finishData = data,
                    laps = allLaps,
                    onDismiss = { viewModel.dismissRaceFinish() }
                )
            }
        }
    } else {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Sunt necesare permisiunile de locație.")
        }
    }
}

@Composable
fun CountdownOverlay(value: Int?) {
    if (value != null && value > 0) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = value.toString(),
                style = TextStyle(
                    fontSize = 180.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    shadow = Shadow(
                        color = Color.Black,
                        blurRadius = 10f
                    )
                )
            )
        }
    }
}