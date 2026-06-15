package com.example.firstapp.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [
        TrackEntity::class,
        RaceHistoryEntity::class,
        GhostRunEntity::class,
        PaceNoteEntity::class   // ← adaugă
    ],
    version = 3,                // ← incrementat
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun trackDao(): TrackDao
    abstract fun raceHistoryDao(): RaceHistoryDao
    abstract fun ghostRunDao(): GhostRunDao
    abstract fun paceNoteDao(): PaceNoteDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "race_tracker_db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}