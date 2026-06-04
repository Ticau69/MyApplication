package com.example.firstapp.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.firstapp.MainActivity
import com.example.trackappv2.R

class LocationForegroundService : Service() {

    companion object {
        const val CHANNEL_ID = "location_tracking_channel"
        const val NOTIFICATION_ID = 1001

        const val ACTION_START = "ACTION_START"
        const val ACTION_STOP = "ACTION_STOP"

        // Flow static — AppViewModel îl ascultă
        val locationFlow = com.example.firstapp.LocationTracker.sharedLocationFlow
    }

    private var locationTracker: com.example.firstapp.LocationTracker? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> startTracking()
            ACTION_STOP -> stopTracking()
        }
        return START_STICKY // Repornește automat dacă sistemul îl omoară
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun startTracking() {
        startForeground(NOTIFICATION_ID, buildNotification())

        locationTracker = com.example.firstapp.LocationTracker(this).also {
            it.startTracking { data ->
                // Emitem locația în flow-ul shared
                com.example.firstapp.LocationTracker.sharedLocationFlow.tryEmit(data)
            }
        }
    }

    private fun stopTracking() {
        locationTracker?.stopTracking()
        locationTracker = null
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        locationTracker?.stopTracking()
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

        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)
    }
}