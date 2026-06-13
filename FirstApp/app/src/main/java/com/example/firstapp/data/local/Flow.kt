package com.example.firstapp.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface TrackDao {
    @Query("SELECT * FROM tracks ORDER BY createdAt DESC")
    fun getAllTracksFlow(): Flow<List<TrackEntity>> // Actualizare reactivă automată în UI

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrack(track: TrackEntity): Long

    @Delete
    suspend fun deleteTrack(track: TrackEntity)
}

@Dao
interface RaceHistoryDao {
    @Query("SELECT * FROM race_history WHERE trackId = :trackId ORDER BY date DESC")
    fun getHistoryForTrack(trackId: Long): Flow<List<RaceHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRaceRecord(race: RaceHistoryEntity): Long
}