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
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
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
import com.example.firstapp.ui.FSMOverlay
import com.example.firstapp.ui.components.GpsDisabledDialog
import com.example.firstapp.ui.components.RaceFinishDialog
import com.huawei.hms.maps.HuaweiMap
import com.huawei.hms.maps.MapsInitializer

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // Forțăm aplicația să deseneze SUB decupajul camerei frontale
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.attributes = window.attributes.apply {
                layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
        }

        // Forțăm ecranul complet (Edge-to-Edge)
        window.navigationBarColor = android.graphics.Color.TRANSPARENT
        WindowCompat.setDecorFitsSystemWindows(window, false)

        MapsInitializer.setApiKey(com.example.trackappv2.BuildConfig.HUAWEI_MAP_API_KEY)
        MapsInitializer.initialize(this)

        setContent {
            com.example.firstapp.ui.theme.FirstAppTheme(darkTheme = true) {
                RaceTrackerApp()
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
fun RaceTrackerApp() {
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

    // Permisiuni
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
        viewModel.onPermissionResult(granted)
    }
    val notificationPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.checkGpsStatus()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(Unit) {
        val alreadyGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (alreadyGranted) viewModel.onPermissionResult(true)
        else permissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        viewModel.checkGpsStatus()
    }

    // NOU: Senzorul de direcție este acum perfect izolat într-o componentă curată
    BearingSensorEffect(onBearingChanged = { bearing ->
        viewModel.updateBearing(bearing)
    })

    if (!isGpsEnabled) {
        GpsDisabledDialog(
            onOpenSettings = { context.startActivity(Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS)) },
            onDismiss = { viewModel.checkGpsStatus() }
        )
    }

    if (hasLocationPermission) {
        val huaweiMapRef = remember { mutableStateOf<HuaweiMap?>(null) }

        Box(modifier = Modifier.fillMaxSize()) {
            MapBackground(
                latLng = currentLatLng,
                bearing = currentBearing,
                appState = currentState,
                onMapReady = { huaweiMapRef.value = it }
            )

            FSMOverlay(
                viewModel = viewModel,
                state = currentState,
                speed = currentSpeed,
                latLng = currentLatLng,
                huaweiMap = huaweiMapRef.value,
                selectedTrack = selectedTrack,
                onStateChange = { viewModel.transitionTo(it) }
            )
        }

        raceFinishData?.let { data ->
            RaceFinishDialog(finishData = data, onDismiss = { viewModel.dismissRaceFinish() })
        }
    } else {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Sunt necesare permisiunile de locație.")
        }
    }
}