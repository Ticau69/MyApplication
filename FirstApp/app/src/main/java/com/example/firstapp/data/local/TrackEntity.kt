package com.example.firstapp.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.huawei.hms.maps.model.LatLng

@Entity(tableName = "tracks")
data class TrackEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val isCircuit: Boolean, // Diferențierea pe care am făcut-o pentru iconițe (Sprint vs Circuit)
    val startLatLng: LatLng, // Coordonata de start unde punem markerul pin
    val createdAt: Long = System.currentTimeMillis()
)