package com.sta.datacollector.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [ParticipantEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class STADatabase : RoomDatabase() {
    abstract fun participantDao(): ParticipantDao
}
