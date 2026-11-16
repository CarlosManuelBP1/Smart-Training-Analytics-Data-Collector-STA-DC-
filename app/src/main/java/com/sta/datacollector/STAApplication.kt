package com.sta.datacollector

import android.app.Application
import androidx.room.Room
import com.sta.datacollector.data.ExerciseSetRepository
import com.sta.datacollector.data.ParticipantRepository
import com.sta.datacollector.data.TrainingSessionRepository
import com.sta.datacollector.data.local.MIGRATION_1_2
import com.sta.datacollector.data.local.STADatabase

class STAApplication : Application() {
    private val database: STADatabase by lazy {
        Room.databaseBuilder(
            applicationContext,
            STADatabase::class.java,
            "sta_database",
        )
            .addMigrations(MIGRATION_1_2)
            .build()
    }

    val participantRepository: ParticipantRepository by lazy {
        ParticipantRepository(database.participantDao())
    }

    val trainingSessionRepository: TrainingSessionRepository by lazy {
        TrainingSessionRepository(database.trainingSessionDao())
    }

    val exerciseSetRepository: ExerciseSetRepository by lazy {
        ExerciseSetRepository(database.exerciseSetDao())
    }
}
