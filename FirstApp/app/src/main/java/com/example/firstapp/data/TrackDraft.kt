package com.example.firstapp.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.huawei.hms.maps.model.LatLng

enum class WaypointType { START, CHECKPOINT, FINISH }

data class Waypoint(
    val position: LatLng,
    val type: WaypointType
)

class TrackDraft {
    var start by mutableStateOf<Waypoint?>(null)
    val checkpoints = mutableStateListOf<Waypoint>()
    var finish by mutableStateOf<Waypoint?>(null)
    var raceType by mutableStateOf(RaceType.SPRINT)

    val isValid: Boolean
        get() = start != null && finish != null

    fun addOrReplace(waypoint: Waypoint) {
        when (waypoint.type) {
            WaypointType.START -> start = waypoint
            WaypointType.CHECKPOINT -> checkpoints.add(waypoint)
            WaypointType.FINISH -> finish = waypoint
        }
    }

    fun clear() {
        start = null
        checkpoints.clear()
        finish = null
    }
}
