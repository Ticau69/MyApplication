package com.example.firstapp

import android.annotation.SuppressLint
import android.content.Context
import android.os.Looper
import com.huawei.hms.location.*
import com.huawei.hms.maps.model.LatLng

data class LocationData(
    val latLng: LatLng,
    val speed: Int,
    val bearing: Float
)

class LocationTracker(context: Context) {
    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)
    
    private var locationCallback: LocationCallback? = null

    @SuppressLint("MissingPermission")
    fun startTracking(onLocationUpdate: (LocationData) -> Unit) {
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
                            bearing = location.bearing
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
