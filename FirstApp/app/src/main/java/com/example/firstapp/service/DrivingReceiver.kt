package com.example.firstapp.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.example.firstapp.MainActivity
import com.huawei.hms.location.ActivityIdentificationData
import com.huawei.hms.location.ActivityIdentificationResponse

class DrivingReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == "com.example.firstapp.ACTION_DRIVING_DETECTED") {
            val response = ActivityIdentificationResponse.getDataFromIntent(intent) ?: return
            val mostProbableActivity = response.mostActivityIdentification ?: return

            // Dacă telefonul e 75% sigur că ești într-un vehicul în mișcare
            if (mostProbableActivity.identificationActivity == ActivityIdentificationData.VEHICLE &&
                mostProbableActivity.possibility >= 75) {
                sendDrivingNotification(context)
            }
        }
    }

    private fun sendDrivingNotification(context: Context) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "driving_detection_channel"

        // Creăm canalul de notificare (obligatoriu pe Android 8+)
        val channel = NotificationChannel(
            channelId,
            "Detectare Auto-Start",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Notificări care te invită să pornești aplicația la volan"
        }
        notificationManager.createNotificationChannel(channel)

        // Intent-ul care prinde CLICK-ul tău pe notificare și deschide aplicația
        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            // Aici punem secretul: o "cheie" care îi spune MainActivity ce să facă!
            putExtra("START_CRUISE_MODE", true)
        }

        val pendingIntent = PendingIntent.getActivity(
            context, 0, openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Construim notificarea vizuală
        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_map) // Poți înlocui cu R.mipmap.ic_launcher_round
            .setContentTitle("🚗 Ai urcat la volan?")
            .setContentText("Atinge aici pentru a porni modul Cruise și radarul de trasee.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true) // Notificarea dispare după ce dai click pe ea
            .setContentIntent(pendingIntent)
            .build()

        // Afișăm notificarea (ID-ul 1001 e ales aleator, ca să nu se suprapună cu altele)
        notificationManager.notify(1001, notification)
    }
}