package com.example.firstapp.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SpeedCameraDao {
    @Query("SELECT * FROM speed_cameras")
    fun getAllSpeedCameras(): Flow<List<SpeedCameraEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCamera(camera: SpeedCameraEntity)
}