package com.example.firstapp.data

import android.content.Context
import androidx.core.content.edit
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
            raceType = draft.raceType,
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
        prefs.edit { putString("tracks_list", gson.toJson(tracks)) }
        return true
    }

    fun getTracks(): List<Track> {
        val json = prefs.getString("tracks_list", null) ?: return emptyList()
        val type = object : TypeToken<List<Track>>() {}.type
        return gson.fromJson(json, type)
    }

    fun deleteTrack(id: String): Boolean {
        val currentTracks = getTracks()
        // Filtrăm lista păstrând doar traseele care NU au ID-ul pe care vrem să-l ștergem
        val filteredTracks = currentTracks.filter { it.id != id }

        // Dacă dimensiunea listei e la fel, înseamnă că nu s-a găsit ID-ul
        if (currentTracks.size == filteredTracks.size) return false

        // Salvăm noua listă (fără traseul șters) înapoi în SharedPreferences
        prefs.edit().putString("tracks_list", gson.toJson(filteredTracks)).apply()
        return true
    }
}