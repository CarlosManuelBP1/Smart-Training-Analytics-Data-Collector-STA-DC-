package com.sta.datacollector.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        ParticipantEntity::class,
        TrainingSessionEntity::class,
        SessionParticipantEntity::class,
        ExerciseSetEntity::class,
    ],
    version = 2,
    exportSchema = true,
)
abstract class STADatabase : RoomDatabase() {
    abstract fun participantDao(): ParticipantDao
    abstract fun trainingSessionDao(): TrainingSessionDao
    abstract fun exerciseSetDao(): ExerciseSetDao
}
