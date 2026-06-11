package com.example.firstapp.managers

import android.content.Context
import android.location.LocationManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class GpsMonitor(
    context: Context,
    private val scope: CoroutineScope
) {
    private val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    private val _isGpsEnabled = MutableStateFlow(checkGpsStatus())
    val isGpsEnabled = _isGpsEnabled.asStateFlow()

    private var gpsMonitorJob: Job? = null

    /**
     * O verificare manuală, instantanee, a statusului.
     */
    private fun checkGpsStatus(): Boolean {
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
    }

    /**
     * Pornește bucla infinită care verifică din 5 în 5 secunde dacă utilizatorul a oprit GPS-ul din bara de stare.
     */
    fun startMonitoring() {
        gpsMonitorJob?.cancel()
        gpsMonitorJob = scope.launch {
            while (isActive) {
                _isGpsEnabled.value = checkGpsStatus()
                delay(5000)
            }
        }
    }

    /**
     * Oprește monitorizarea (util când aplicația intră în background sau e închisă).
     */
    fun stopMonitoring() {
        gpsMonitorJob?.cancel()
        gpsMonitorJob = null
    }

    fun forceCheck() {
        _isGpsEnabled.value = checkGpsStatus()
    }
}