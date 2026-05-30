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

class LocationTracker(private val context: Context) {
    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)
    
    private var locationCallback: LocationCallback? = null

    @SuppressLint("MissingPermission")
    fun startTracking(onLocationUpdate: (LocationData) -> Unit) {
        val locationRequest = LocationRequest.create().apply {
            interval = 500
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult?) {
                locationResult?.lastLocation?.let { location ->
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

        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.getMainLooper()
        )
    }

    fun stopTracking() {
        locationCallback?.let {
            fusedLocationClient.removeLocationUpdates(it)
        }
    }
}
