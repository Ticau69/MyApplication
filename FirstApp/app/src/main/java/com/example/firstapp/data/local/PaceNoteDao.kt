package com.example.firstapp.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface PaceNoteDao {

    @Query("SELECT * FROM pace_notes WHERE trackId = :trackId ORDER BY segmentIndex ASC")
    fun getPaceNotesForTrackFlow(trackId: String): Flow<List<PaceNoteEntity>>

    @Query("SELECT * FROM pace_notes WHERE trackId = :trackId ORDER BY segmentIndex ASC")
    suspend fun getPaceNotesForTrack(trackId: String): List<PaceNoteEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(notes: List<PaceNoteEntity>)

    @Query("DELETE FROM pace_notes WHERE trackId = :trackId")
    suspend fun deleteForTrack(trackId: String)
}