package com.example.firstapp.data

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.huawei.hms.maps.model.LatLng

class TrackRepository(context: Context) {
    private val prefs = context.getSharedPreferences("saved_tracks", Context.MODE_PRIVATE)
    private val gson = Gson()

    fun saveTrack(
        draft: TrackDraft,
        name: String,
        routedPoints: List<LatLng> = emptyList()
    ): Boolean {
        if (!draft.isValid) return false

        val track = Track(
            id = java.util.UUID.randomUUID().toString(),
            name = name,
            createdAt = java.text.SimpleDateFormat(
                "yyyy-MM-dd HH:mm",
                java.util.Locale.getDefault()
            ).format(java.util.Date()),
            start = SerializableLatLng.from(draft.start!!),
            checkpoints = draft.checkpoints.map { SerializableLatLng.from(it) },
            finish = SerializableLatLng.from(draft.finish!!),
            routedPoints = routedPoints.map { SerializableLatLng(it.latitude, it.longitude) }
        )

        val tracks = getTracks().toMutableList()
        tracks.add(0, track)
        prefs.edit().putString("tracks_list", gson.toJson(tracks)).apply()
        return true
    }

    fun getTracks(): List<Track> {
        val json = prefs.getString("tracks_list", null) ?: return emptyList()
        val type = object : TypeToken<List<Track>>() {}.type
        return gson.fromJson(json, type)
    }

    fun deleteTrack(id: String) {
        val tracks = getTracks().filter { it.id != id }
        prefs.edit().putString("tracks_list", gson.toJson(tracks)).apply()
    }
}