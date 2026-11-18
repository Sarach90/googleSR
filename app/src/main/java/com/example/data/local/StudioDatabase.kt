package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [ProjectEntity::class, ProjectVersionEntity::class],
    version = 1,
    exportSchema = false
)
abstract class MotionStudioDatabase : RoomDatabase() {
    abstract fun studioDao(): StudioDao

    companion object {
        @Volatile
        private var INSTANCE: MotionStudioDatabase? = null

        fun getInstance(context: Context): MotionStudioDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    MotionStudioDatabase::class.java,
                    "motion_studio_ai.db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
