package com.example.firstapp.data

import com.huawei.hms.maps.model.LatLng

enum class WaypointType { START, CHECKPOINT, FINISH }

data class Waypoint(
    val position: LatLng,
    val type: WaypointType
)

data class TrackDraft(
    var start: Waypoint? = null,
    val checkpoints: MutableList<Waypoint> = mutableListOf(),
    var finish: Waypoint? = null
) {
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