package com.example.firstapp.managers

import com.example.firstapp.data.LapData
import com.example.firstapp.data.local.RaceHistoryDao
import com.example.firstapp.data.local.RaceHistoryEntity
import com.example.firstapp.racing.RaceRecord
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class HistoryManager(private val raceHistoryDao: RaceHistoryDao) {

    // Flow reactiv pentru UI
    val historyFlow: Flow<List<RaceRecord>> = raceHistoryDao.getAllHistoryFlow()
        .map { entities -> entities.map { it.toRaceRecord() } }

    suspend fun getHistory(): List<RaceRecord> =
        raceHistoryDao.getAllHistory().map { it.toRaceRecord() }

    suspend fun getHistoryForTrack(trackId: String): List<RaceRecord> =
        raceHistoryDao.getHistoryForTrack(trackId).map { it.toRaceRecord() }

    suspend fun getBestRunForTrack(trackId: String): RaceHistoryEntity? =
        raceHistoryDao.getBestRunForTrack(trackId)

    suspend fun saveRace(
        record: RaceRecord,
        trackId: String? = null,
        laps: List<LapData> = emptyList()
    ) {
        raceHistoryDao.insertRace(
            RaceHistoryEntity.fromRaceRecord(record, trackId, laps)
        )
    }

    suspend fun deleteRace(id: String) =
        raceHistoryDao.deleteRaceById(id)

    suspend fun deleteAllForTrack(trackId: String) =
        raceHistoryDao.deleteAllForTrack(trackId)
}