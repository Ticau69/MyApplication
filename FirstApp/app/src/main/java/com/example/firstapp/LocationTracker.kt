package com.example.firstapp

import android.annotation.SuppressLint
import android.content.Context
import android.os.Looper
import com.huawei.hms.location.*
import com.huawei.hms.maps.model.LatLng
import com.huawei.hms.location.LocationServices

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
        try {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    val initialData = LocationData(
                        latLng = LatLng(location.latitude, location.longitude),
                        speed = (location.speed * 3.6f).toInt(), // Conversie m/s în km/h dacă ai nevoie
                        bearing = location.bearing
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
