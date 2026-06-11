package com.example.firstapp.ui

import android.os.Bundle
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.trackappv2.BuildConfig
import com.huawei.hms.maps.HuaweiMap
import com.huawei.hms.maps.MapView
import com.huawei.hms.maps.MapsInitializer

@Composable
fun ComposeHuaweiMap(
    modifier: Modifier = Modifier,
    onMapReady: (HuaweiMap) -> Unit
) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle

    // 1. Forțăm inițializarea SDK-ului Huawei și creăm harta o singură dată
    val mapView = remember {
        // Citim valoarea generată de Gradle din local.properties
        val apiKey = BuildConfig.HUAWEI_MAP_API_KEY

        if (apiKey.isNotEmpty()) {
            MapsInitializer.setApiKey(apiKey)
        }

        MapsInitializer.initialize(context)
        MapView(context).apply {
            onCreate(Bundle())
        }
    }

    // 2. Sincronizăm perfect ciclul de viață
    DisposableEffect(lifecycle, mapView) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> mapView.onStart()
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                Lifecycle.Event.ON_STOP -> mapView.onStop()
                Lifecycle.Event.ON_DESTROY -> mapView.onDestroy()
                else -> {}
            }
        }

        lifecycle.addObserver(observer)

        // Secretul nr. 2: Dacă aplicația este deja pe ecran (RESUMED) când harta abia se creează,
        // forțăm harta să treacă direct în modul activ.
        if (lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
            mapView.onResume()
        } else if (lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
            mapView.onStart()
        }

        onDispose {
            lifecycle.removeObserver(observer)
        }
    }

    // 3. Conectăm View-ul la interfața Jetpack Compose
    AndroidView(
        modifier = modifier,
        factory = {
            mapView.apply {
                getMapAsync { map ->
                    onMapReady(map)
                }
            }
        }
    )
}