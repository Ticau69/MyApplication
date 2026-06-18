package com.example.firstapp.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.firstapp.data.SpeedCamera

@Entity(tableName = "speed_cameras")
data class SpeedCameraEntity(
    @PrimaryKey val id: String,
    val name: String,
    val lat: Double,
    val lng: Double,
    val creatorUsername: String,
    val createdAt: Long
) {
    fun toSpeedCamera() = SpeedCamera(id, name, lat, lng, creatorUsername, createdAt)

    companion object {
        fun fromSpeedCamera(c: SpeedCamera) = SpeedCameraEntity(
            id = c.id,
            name = c.name,
            lat = c.lat,
            lng = c.lng,
            creatorUsername = c.creatorUsername,
            createdAt = c.createdAt
        )
    }
}