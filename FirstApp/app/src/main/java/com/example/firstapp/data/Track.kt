package com.example.firstapp.data

import com.huawei.hms.maps.model.LatLng

data class Track(
    val id: String,
    val name: String,
    val createdAt: String,
    val start: SerializableLatLng,
    val checkpoints: List<SerializableLatLng>,
    val finish: SerializableLatLng,
    val routedPoints: List<SerializableLatLng> = emptyList()
)

// LatLng nu e serializabil direct cu Gson, folosim wrapper
data class SerializableLatLng(
    val latitude: Double,
    val longitude: Double
) {
    fun toLatLng() = LatLng(latitude, longitude)

    companion object {
        fun from(latLng: LatLng) = SerializableLatLng(latLng.latitude, latLng.longitude)
        fun from(waypoint: Waypoint) = SerializableLatLng(
            waypoint.position.latitude,
            waypoint.position.longitude
        )
    }
}