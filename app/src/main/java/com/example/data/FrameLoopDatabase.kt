package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [MediaSlot::class],
    version = 1,
    exportSchema = false
)
abstract class FrameLoopDatabase : RoomDatabase() {
    abstract fun mediaSlotDao(): MediaSlotDao

    companion object {
        @Volatile
        private var INSTANCE: FrameLoopDatabase? = null

        fun getInstance(context: Context): FrameLoopDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    FrameLoopDatabase::class.java,
                    "frameloop_slots.db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
