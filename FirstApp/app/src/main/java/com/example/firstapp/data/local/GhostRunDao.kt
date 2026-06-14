package com.example.firstapp.data.local

import androidx.room.*

@Dao
interface GhostRunDao {

    @Query("SELECT * FROM ghost_runs WHERE trackId = :trackId")
    suspend fun getGhostForTrack(trackId: String): GhostRunEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGhostRun(ghostRun: GhostRunEntity)

    @Query("DELETE FROM ghost_runs WHERE trackId = :trackId")
    suspend fun deleteGhostForTrack(trackId: String)
}