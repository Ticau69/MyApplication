package com.example.firstapp

import android.os.Bundle
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.huawei.hms.maps.CameraUpdateFactory
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.core.content.ContextCompat
import com.huawei.hms.maps.HuaweiMap
import com.huawei.hms.maps.MapView
import com.huawei.hms.maps.model.BitmapDescriptorFactory
import com.huawei.hms.maps.model.CameraPosition
import com.huawei.hms.maps.model.Marker
import com.huawei.hms.maps.model.MarkerOptions
import com.example.trackappv2.R

@Composable
fun MapWithTracking() {
    val context = LocalContext.current
    val mapView = remember { MapView(context) }
    var huaweiMapInstance by remember { mutableStateOf<HuaweiMap?>(null) }
    var userMarker by remember { mutableStateOf<Marker?>(null) }
    var currentSpeed by remember { mutableIntStateOf(0) }

    val arrowIcon = remember(context) {
        val drawable = ContextCompat.getDrawable(context, R.drawable.ic_direction_arrow)
        val bitmap = Bitmap.createBitmap(
            drawable!!.intrinsicWidth,
            drawable.intrinsicHeight,
            Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        BitmapDescriptorFactory.fromBitmap(bitmap)
    }

    // Lifecycle Observer for MapView
    MapLifecycleObserver(mapView)

    // Location Tracking Logic
    DisposableEffect(Unit) {
        val tracker = LocationTracker(context)
        tracker.startTracking { data ->
            currentSpeed = data.speed
            huaweiMapInstance?.let { map ->
                // Update or create user marker
                if (userMarker == null) {
                    userMarker = map.addMarker(
                        MarkerOptions()
                            .position(data.latLng)
                            .anchor(0.5f, 0.5f)
                            .icon(arrowIcon)
                            .flat(true)
                    )
                } else {
                    userMarker?.position = data.latLng
                    userMarker?.rotation = data.bearing
                }

                val cameraPosition = CameraPosition.Builder()
                    .target(data.latLng)
                    .zoom(18f)
                    .bearing(data.bearing)
                    .tilt(45f)
                    .build()
                
                map.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition))
            }
        }

        onDispose {
            tracker.stopTracking()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = { mapView },
            modifier = Modifier.fillMaxSize()
        ) { mv ->
            mv.getMapAsync { hMap ->
                huaweiMapInstance = hMap
                // Hide default blue dot to use our custom arrow marker
                try {
                    hMap.isMyLocationEnabled = false
                } catch (e: SecurityException) { }
                hMap.uiSettings.isZoomControlsEnabled = false
                hMap.uiSettings.isCompassEnabled = true
            }
        }

        // Speedometer Overlay
        Box(modifier = Modifier.fillMaxSize()) {
            SpeedometerOverlay(currentSpeed)
        }
    }
}

@Composable
fun BoxScope.SpeedometerOverlay(speed: Int) {
    Column(
        modifier = Modifier
            .padding(bottom = 48.dp, start = 24.dp)
            .align(Alignment.BottomStart)
            .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Text("VITEZĂ", color = Color.Gray, style = MaterialTheme.typography.labelSmall)
        Text("$speed km/h", color = Color(0xFFDEFF9A), style = MaterialTheme.typography.headlineLarge)
    }
}

@Composable
fun MapLifecycleObserver(mapView: MapView) {
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle) {
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
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }
}
