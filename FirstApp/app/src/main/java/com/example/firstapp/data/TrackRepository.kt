package com.example.firstapp.data

import android.app.Application
import com.example.firstapp.data.local.TrackDao
import com.example.firstapp.data.local.TrackEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class TrackRepository(private val trackDao: TrackDao) {

    // Flow reactiv — UI se actualizează automat la orice schimbare
    val tracksFlow: Flow<List<Track>> = trackDao.getAllTracksFlow()
        .map { entities -> entities.map { it.toTrack() } }

    suspend fun getTracks(): List<Track> =
        trackDao.getAllTracks().map { it.toTrack() }

    suspend fun getTrackById(id: String): Track? =
        trackDao.getTrackById(id)?.toTrack()

    suspend fun saveTrack(track: Track) =
        trackDao.insertTrack(TrackEntity.fromTrack(track))

    suspend fun deleteTrack(id: String) =
        trackDao.deleteTrackById(id)
}