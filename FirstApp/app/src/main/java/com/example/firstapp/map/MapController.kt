package com.example.firstapp.map

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.graphics.createBitmap
import com.example.firstapp.AppState
import com.example.firstapp.ui.theme.FirstAppTheme
import com.example.trackappv2.R
import com.huawei.hms.maps.CameraUpdateFactory
import com.huawei.hms.maps.HuaweiMap
import com.huawei.hms.maps.model.BitmapDescriptorFactory
import com.huawei.hms.maps.model.CameraPosition
import com.huawei.hms.maps.model.LatLng
import com.huawei.hms.maps.model.MapStyleOptions
import com.huawei.hms.maps.model.Marker
import com.huawei.hms.maps.model.MarkerOptions
import kotlinx.coroutines.delay

private const val REFOLLOW_DELAY_MS = 12_000L // 12 secunde de inactivitate
// Prag de viteză pentru comutarea modului
private const val MOVING_SPEED_THRESHOLD_KMH = 5

@Composable
fun MapController(
    huaweiMap: HuaweiMap?,
    latLng: LatLng?,
    bearing: Float,
    speed: Int,
    appState: AppState,
    onCameraMoveStarted: (Int) -> Unit = {} // ← adaugă callback pentru a evita conflictele de listenere
) {
    val context = LocalContext.current

    // ── Variabile de stare ────────────────────────────────────────────
    var playerMarker    by remember { mutableStateOf<Marker?>(null) }
    var isFollowingUser by remember { mutableStateOf(true) }
    var isFirstLoad     by remember { mutableStateOf(true) }
    val lastInteraction = remember { mutableLongStateOf(0L) }
    val lastBearing     = remember { mutableFloatStateOf(0f) }

    // ── Mod hibrid ────────────────────────────────────────────────────
    val isMoving        = speed >= MOVING_SPEED_THRESHOLD_KMH
    val effectiveBearing = if (isMoving) bearing else 0f
    val effectiveTilt    = if (isMoving) 60f else 0f

    // ── rememberUpdatedState — pentru LaunchedEffect ──
    val updatedBearing  = rememberUpdatedState(effectiveBearing)
    val updatedTilt     = rememberUpdatedState(effectiveTilt)
    val updatedOnCameraMoveStarted = rememberUpdatedState(onCameraMoveStarted)

    // ── Configurare hartă — o singură dată ───────────────────────
    LaunchedEffect(huaweiMap) {
        val map = huaweiMap ?: return@LaunchedEffect

        try {
            val style = MapStyleOptions.loadRawResourceStyle(context, R.raw.mapstyle_night)
            map.setMapStyle(style)
        } catch (e: Exception) {
            android.util.Log.e("MapController", "Eroare stil: ${e.message}")
        }

        map.uiSettings.apply {
            isZoomControlsEnabled     = false
            isCompassEnabled          = false
            isMyLocationButtonEnabled = false
            isMapToolbarEnabled       = false
        }
        map.isMyLocationEnabled = false

        // Detectăm când utilizatorul atinge harta
        map.setOnCameraMoveStartedListener { reason ->
            if (reason == HuaweiMap.OnCameraMoveStartedListener.REASON_GESTURE) {
                isFollowingUser = false
                map.stopAnimation()
                lastInteraction.longValue = System.currentTimeMillis()
            }
            // Apelăm și callback-ul extern
            updatedOnCameraMoveStarted.value(reason)
        }
    }

    // ── Timer reactivare după inactivitate ───────────────────────
    LaunchedEffect(lastInteraction.longValue) {
        if (lastInteraction.longValue == 0L) return@LaunchedEffect
        delay(REFOLLOW_DELAY_MS)
        // Verificăm că nu a mai atins harta în intervalul de așteptare
        val elapsed = System.currentTimeMillis() - lastInteraction.longValue
        if (elapsed >= REFOLLOW_DELAY_MS) {
            isFollowingUser = true
        }
    }

    // ── Player marker ─────────────────────────────────────────────
    LaunchedEffect(huaweiMap, latLng, bearing) {
        val map = huaweiMap ?: return@LaunchedEffect
        val pos = latLng ?: return@LaunchedEffect
        if (pos.latitude == 0.0 && pos.longitude == 0.0) return@LaunchedEffect

        if (playerMarker == null) {
            val icon = bitmapFromVector(context, R.drawable.ic_nav_arrow)
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

    // ── Cameră ────────────────────────────────────────────────────
    LaunchedEffect(huaweiMap, latLng, appState, isFollowingUser, isMoving) {
        val map = huaweiMap ?: return@LaunchedEffect
        val pos = latLng   ?: return@LaunchedEffect
        if (pos.latitude == 0.0 && pos.longitude == 0.0) return@LaunchedEffect
        
        // Dacă nu urmărim userul, nu facem nimic cu camera aici
        if (!isFollowingUser) return@LaunchedEffect
        
        // Nu mișcăm camera în anumite ecrane de meniu
        if (appState == AppState.RACE_CREATION ||
            appState == AppState.SAVED_TRACKS  ||
            appState == AppState.HISTORY       ||
            appState == AppState.SETTINGS) return@LaunchedEffect

        // Citim valorile curente din rememberUpdatedState
        val camBearing = updatedBearing.value
        val camTilt    = updatedTilt.value

        val camTarget = map.cameraPosition.target
        val results = FloatArray(1)
        android.location.Location.distanceBetween(
            camTarget.latitude, camTarget.longitude,
            pos.latitude, pos.longitude, results
        )

        val bearingDelta = kotlin.math.abs(camBearing - lastBearing.floatValue)
        val shouldUpdateBearing = bearingDelta > 5f && bearingDelta < 355f

        if (isFirstLoad) {
            map.moveCamera(
                CameraUpdateFactory.newCameraPosition(
                    CameraPosition.Builder()
                        .target(pos)
                        .zoom(18.5f)
                        .bearing(camBearing)
                        .tilt(camTilt)
                        .build()
                )
            )
            isFirstLoad = false
            lastBearing.floatValue = camBearing
            return@LaunchedEffect
        }

        val isModeSwitch = (isMoving && map.cameraPosition.tilt == 0f) ||
                (!isMoving && map.cameraPosition.tilt > 0f)

        // Evităm animații inutile dacă suntem deja aproape și direcția e similară
        if (results[0] < 20f && !shouldUpdateBearing && !isModeSwitch) return@LaunchedEffect

        lastBearing.floatValue = camBearing

        val animDuration = if (isModeSwitch) 1200 else 500

        map.animateCamera(
            CameraUpdateFactory.newCameraPosition(
                CameraPosition.Builder()
                    .target(pos)
                    .zoom(if (isMoving) 18.5f else 17f)
                    .bearing(camBearing)
                    .tilt(camTilt)
                    .build()
            ),
            animDuration,
            object : HuaweiMap.CancelableCallback {
                override fun onFinish() {
                    // Animația s-a terminat normal. RĂMÂNEM pe true pentru a continua urmărirea!
                    // isFollowingUser = true // (E deja true)
                }

                override fun onCancel() {
                    // Atingerea ecranului a anulat animația GPS. Afișăm butonul!
                    isFollowingUser = false
                    lastInteraction.longValue = System.currentTimeMillis()
                }
            }
        )
    }

    // ── Buton recentrare manual ───────────────────────────────────
    if (!isFollowingUser) {
        Box(modifier = Modifier.fillMaxSize()) {
            FloatingActionButton(
                onClick = {
                    isFollowingUser = true
                    lastInteraction.longValue = 0L
                },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(bottom = 32.dp, end = 16.dp),
                containerColor = MaterialTheme.colorScheme.surfaceContainer,
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(
                    painter = painterResource(R.drawable.recenter_position_icon),
                    contentDescription = "Recentrează",
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}

private fun bitmapFromVector(
    context: Context,
    drawableRes: Int
): com.huawei.hms.maps.model.BitmapDescriptor {
    val drawable = ContextCompat.getDrawable(context, drawableRes)
        ?: return BitmapDescriptorFactory.defaultMarker()
    val bitmap = createBitmap(
        drawable.intrinsicWidth,
        drawable.intrinsicHeight,
        Bitmap.Config.ARGB_8888
    )
    val canvas = Canvas(bitmap)
    drawable.setBounds(0, 0, canvas.width, canvas.height)
    drawable.draw(canvas)
    return BitmapDescriptorFactory.fromBitmap(bitmap)
}

@Preview(
    name = "Recenter Button",
    showBackground = true,
    backgroundColor = 0xFF12121D,
    widthDp = 800,
    heightDp = 360
)
@Composable
fun PreviewRecenterButton() {
    FirstAppTheme(darkTheme = true) {
        Box(modifier = Modifier.fillMaxSize()) {
            FloatingActionButton(
                onClick = {},
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(bottom = 32.dp, end = 16.dp),
                containerColor = MaterialTheme.colorScheme.surfaceContainer,
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(
                    painter = painterResource(R.drawable.recenter_position_icon),
                    contentDescription = "Recentrează",
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}
