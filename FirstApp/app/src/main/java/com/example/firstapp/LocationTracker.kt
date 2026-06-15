package com.example.firstapp

import android.annotation.SuppressLint
import android.content.Context
import android.os.Looper
import com.huawei.hms.location.FusedLocationProviderClient
import com.huawei.hms.location.LocationCallback
import com.huawei.hms.location.LocationRequest
import com.huawei.hms.location.LocationResult
import com.huawei.hms.location.LocationServices
import com.huawei.hms.maps.model.LatLng
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow

data class LocationData(
    val latLng: LatLng,
    val speed: Int,
    val bearing: Float,
    val rawLocation: android.location.Location
)

class LocationTracker(context: Context) {
    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)
    
    private var locationCallback: LocationCallback? = null

    companion object {
        // Flow shared între Service și ViewModel
        val sharedLocationFlow = MutableSharedFlow<LocationData>(
            replay = 1,
            extraBufferCapacity = 10,
            onBufferOverflow = BufferOverflow.DROP_OLDEST
        )
    }

    @SuppressLint("MissingPermission")
    fun getLastKnownLocation(onResult: (LocationData?) -> Unit) {
        try {
            // Încearcă mai întâi locația din cache HMS (instantanee)
            fusedLocationClient.lastLocation
                .addOnSuccessListener { location ->
                    if (location != null && location.accuracy < 100f) {
                        onResult(
                            LocationData(
                                latLng = LatLng(location.latitude, location.longitude),
                                speed = 0,
                                bearing = location.bearing,
                                rawLocation = location
                            )
                        )
                    } else {
                        // Cache gol sau prea imprecis — cerem un fix rapid o singură dată
                        requestSingleFix(onResult)
                    }
                }
                .addOnFailureListener {
                    requestSingleFix(onResult)
                }
        } catch (e: SecurityException) {
            onResult(null)
        }
    }

    @SuppressLint("MissingPermission")
    private fun requestSingleFix(onResult: (LocationData?) -> Unit) {
        val request = LocationRequest.create().apply {
            numUpdates = 1  // Un singur update
            priority = LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY // Folosește Wi-Fi + Cell
            interval = 0
            fastestInterval = 0
        }

        val singleCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult?) {
                result?.lastLocation?.let { location ->
                    onResult(
                        LocationData(
                            latLng = LatLng(location.latitude, location.longitude),
                            speed = 0,
                            bearing = location.bearing,
                             rawLocation = location
                        )
                    )
                }
                fusedLocationClient.removeLocationUpdates(this)
            }
        }

        try {
            fusedLocationClient.requestLocationUpdates(
                request,
                singleCallback,
                Looper.getMainLooper()
            )
        } catch (e: SecurityException) {
            onResult(null)
        }
    }

    @SuppressLint("MissingPermission")
    fun startTracking(onLocationUpdate: (LocationData) -> Unit) {
        try {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    val initialData = LocationData(
                        latLng = LatLng(location.latitude, location.longitude),
                        speed = (location.speed * 3.6f).toInt(), // Conversie m/s în km/h dacă ai nevoie
                        bearing = location.bearing,
                        rawLocation = location
                    )
                    onLocationUpdate(initialData) // Trimitem locația orientativă către hartă imediat!
                }
            }
        } catch (e: SecurityException) {
            // Ignorăm eroarea de permisiuni aici, va fi tratată în fluxul principal
        }

        val locationRequest = LocationRequest.create().apply {
            interval = 500
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }

        val callback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult?) {
                locationResult?.lastLocation?.let { location ->
                    if (location.accuracy > 20f) return
                    if (location.speed < 0f) return
                    onLocationUpdate(
                        LocationData(
                            latLng = LatLng(location.latitude, location.longitude),
                            speed = (location.speed * 3.6).toInt(),
                            bearing = location.bearing,
                            rawLocation = location
                        )
                    )
                }
            }
        }
        locationCallback = callback

        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            callback,
            Looper.getMainLooper()
        )
    }

    fun stopTracking() {
        locationCallback?.let {
            fusedLocationClient.removeLocationUpdates(it)
            locationCallback = null
        }
    }
}
