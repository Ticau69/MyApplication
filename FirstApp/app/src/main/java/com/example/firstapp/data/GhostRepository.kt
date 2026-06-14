package com.example.firstapp.data

import com.example.firstapp.data.local.GhostRunDao
import com.example.firstapp.data.local.GhostRunEntity

class GhostRepository(private val ghostRunDao: GhostRunDao) {

    suspend fun getBestRun(trackId: String): GhostRun? =
        ghostRunDao.getGhostForTrack(trackId)?.toGhostRun()

    suspend fun saveBestRun(run: GhostRun) {
        val existing = ghostRunDao.getGhostForTrack(run.trackId)

        // Salvăm doar dacă e mai bun decât cel existent
        if (existing == null || run.totalTimeMs < existing.totalTimeMs) {
            ghostRunDao.insertGhostRun(GhostRunEntity.fromGhostRun(run))
        }
    }

    suspend fun deleteGhostForTrack(trackId: String) =
        ghostRunDao.deleteGhostForTrack(trackId)
}