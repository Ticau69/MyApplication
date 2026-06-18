package com.example.firstapp.data

import java.util.UUID

// Această clasă va deveni tabelul 'speed_cameras' în Supabase
data class SpeedCamera(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val lat: Double,
    val lng: Double,
    val creatorUsername: String = "LocalPlayer", // Când avem online, aici va fi numele tău/Jimmy
    val createdAt: Long = System.currentTimeMillis()
)

// Această clasă va deveni tabelul 'camera_records' în Supabase
data class SpeedRecord(
    val id: String = UUID.randomUUID().toString(),
    val cameraId: String,
    val username: String = "LocalPlayer",
    val topSpeedKmh: Int,
    val timestamp: Long = System.currentTimeMillis()
)