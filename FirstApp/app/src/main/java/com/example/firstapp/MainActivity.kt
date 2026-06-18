package com.example.firstapp

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
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
import com.example.trackappv2.BuildConfig
import com.example.firstapp.sensors.BearingSensorEffect
import com.example.firstapp.service.LocationForegroundService
import com.example.firstapp.ui.ComposeHuaweiMap
import com.example.firstapp.ui.FSMOverlay
import com.example.firstapp.ui.components.GpsDisabledDialog
import com.example.firstapp.ui.components.RaceFinishDialog
import com.huawei.hms.maps.HuaweiMap
import com.huawei.hms.maps.MapsInitializer

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // MapsInitializer — O SINGURĂ DATĂ, înainte de orice
        MapsInitializer.setApiKey(BuildConfig.HUAWEI_MAP_API_KEY)
        MapsInitializer.initialize(this)

        WindowCompat.setDecorFitsSystemWindows(window, false)
        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())

        window.attributes = window.attributes.apply {
            layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        enableEdgeToEdge()

        setContent {
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
    val context      = LocalContext.current
    val viewModel: AppViewModel = viewModel()
    val lifecycleOwner = LocalLifecycleOwner.current

    val currentState  by viewModel.appState.collectAsState()
    val currentLatLng by viewModel.currentLatLng.collectAsState()
    val currentBearing by viewModel.currentBearing.collectAsState()
    val currentSpeed  by viewModel.currentSpeed.collectAsState()
    val hasLocationPermission by viewModel.hasLocationPermission.collectAsState()
    val isGpsEnabled  by viewModel.isGpsEnabled.collectAsState()
    val selectedTrack by viewModel.selectedTrack.collectAsState()
    val raceFinishData by viewModel.raceFinishData.collectAsState()
    val countdownValue by viewModel.countdownValue.collectAsState()
    val allLaps       by viewModel.allLaps.collectAsState()

    var mapInstance by remember { mutableStateOf<HuaweiMap?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
        viewModel.onPermissionResult(granted)
    }
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    // Permisiuni la pornire
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
            // Logica pentru pornire automată Cruise dacă este cazul
            android.util.Log.d("MainActivity", "Starting Cruise from intent")
        }
    }

    // Lifecycle GPS check
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.checkGpsStatus()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    BearingSensorEffect(
        appState = currentState,
        currentSpeed = currentSpeed, // <--- ADAUGĂ ACEASTĂ LINIE
        onBearingChanged = { viewModel.updateBearing(it) }
    )

    if (!isGpsEnabled) {
        GpsDisabledDialog(
            onOpenSettings = {
                context.startActivity(
                    Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                )
            },
            onDismiss = { viewModel.checkGpsStatus() }
        )
    }

    if (hasLocationPermission) {
        Box(modifier = Modifier.fillMaxSize()) {

            // 1. Harta — lifecycle gestionat intern
            ComposeHuaweiMap(
                modifier = Modifier.fillMaxSize()
            ) { readyMap ->
                mapInstance = readyMap
            }

            // 2. HUD-ul aplicației (Conține și MapController + TrackMarkersOverlay)
            FSMOverlay(
                state = currentState,
                speed = currentSpeed,
                latLng = currentLatLng,
                bearing = currentBearing,
                huaweiMap = mapInstance,
                selectedTrack = selectedTrack,
                onStateChange = { viewModel.transitionTo(it) },
                viewModel = viewModel
            )

            // 5. Countdown + Dialog finish
            CountdownOverlay(value = countdownValue)

            if (raceFinishData != null) {
                RaceFinishDialog(
                    finishData = raceFinishData!!,
                    laps       = allLaps,
                    onDismiss  = { viewModel.dismissRaceFinish() }
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
    if (value != null && (value > 0)) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
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