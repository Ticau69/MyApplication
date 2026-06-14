package com.example.firstapp.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface RaceHistoryDao {

    @Query("SELECT * FROM race_history ORDER BY date DESC")
    fun getAllHistoryFlow(): Flow<List<RaceHistoryEntity>>

    @Query("SELECT * FROM race_history ORDER BY date DESC")
    suspend fun getAllHistory(): List<RaceHistoryEntity>

    @Query("SELECT * FROM race_history WHERE trackId = :trackId ORDER BY date DESC")
    suspend fun getHistoryForTrack(trackId: String): List<RaceHistoryEntity>

    @Query("SELECT * FROM race_history WHERE trackId = :trackId ORDER BY durationSeconds ASC LIMIT 1")
    suspend fun getBestRunForTrack(trackId: String): RaceHistoryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRace(race: RaceHistoryEntity)

    @Query("DELETE FROM race_history WHERE id = :raceId")
    suspend fun deleteRaceById(raceId: String)

    @Query("DELETE FROM race_history WHERE trackId = :trackId")
    suspend fun deleteAllForTrack(trackId: String)
}