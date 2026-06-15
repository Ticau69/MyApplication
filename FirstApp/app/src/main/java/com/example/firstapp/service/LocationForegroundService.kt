package com.example.firstapp.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import com.example.firstapp.MainActivity
import com.example.firstapp.LocationData
import com.example.firstapp.LocationTracker
import com.example.trackappv2.R
import com.huawei.hms.location.LocationCallback
import com.huawei.hms.location.LocationRequest
import com.huawei.hms.location.LocationResult
import com.huawei.hms.location.LocationServices
import com.huawei.hms.maps.model.LatLng
import kotlin.getValue

class LocationForegroundService : Service() {

    private val fusedLocationClient by lazy { LocationServices.getFusedLocationProviderClient(this) }
    
    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult?) {
            result?.lastLocation?.let { location ->
                // Filtrare de bază pentru acuratețe (similar cu LocationTracker)
                if (location.accuracy > 20f) return
                if (location.speed < 0f) return
                
                val data = LocationData(
                    latLng = LatLng(location.latitude, location.longitude),
                    speed = (location.speed * 3.6).toInt(),
                    bearing = location.bearing,
                    rawLocation = location
                )
                // Emitem locația în flow-ul shared din LocationTracker
                LocationTracker.sharedLocationFlow.tryEmit(data)
            }
        }
    }

    companion object {
        const val CHANNEL_ID = "location_tracking_channel"
        const val NOTIFICATION_ID = 1001

        const val ACTION_START = "ACTION_START"
        const val ACTION_STOP = "ACTION_STOP"

        const val ACTION_MODE_CRUISE = "ACTION_MODE_CRUISE"
        const val ACTION_MODE_RACING = "ACTION_MODE_RACING"
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                // Începem serviciul în foreground
                startForeground(NOTIFICATION_ID, buildNotification())
                // Pornim tracking-ul implicit în modul Cruise
                updateLocationRequest(isRacing = false)
            }
            ACTION_MODE_CRUISE -> updateLocationRequest(isRacing = false)
            ACTION_MODE_RACING -> updateLocationRequest(isRacing = true)
            ACTION_STOP -> stopTracking()
        }
        return START_STICKY
    }

    private fun updateLocationRequest(isRacing: Boolean) {
        val request = LocationRequest.create().apply {
            if (isRacing) {
                // MOD RACING (80-90% stres pe senzor)
                interval = 500L           // O dată la jumătate de secundă
                fastestInterval = 200L    // Maxim o dată la 200ms
                priority = LocationRequest.PRIORITY_HIGH_ACCURACY
            } else {
                // MOD CRUISE (50-60% stres pe senzor)
                interval = 4000L          // O dată la 4 secunde e suficient
                fastestInterval = 2000L
                priority = LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY
            }
        }

        // Aplicăm noile reguli "din mers"
        try {
            fusedLocationClient.requestLocationUpdates(request, locationCallback, Looper.getMainLooper())
        } catch (_: SecurityException) {
            // Permisiuni lipsă
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun stopTracking() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    private fun buildNotification(): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Track App")
            .setContentText("GPS activ — tracking în desfășurare")
            .setSmallIcon(R.drawable.ic_nav_arrow)
            .setContentIntent(pendingIntent)
            .setOngoing(true) // Nu poate fi ștearsă de utilizator
            .setSilent(true)  // Fără sunet la afișare
            .build()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "GPS Tracking",
            NotificationManager.IMPORTANCE_LOW // Low = fără sunet, fără popup
        ).apply {
            description = "Menține GPS-ul activ în background"
            setShowBadge(false)
        }

        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)
    }
}
