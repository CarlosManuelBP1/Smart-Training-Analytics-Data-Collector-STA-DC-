package com.sta.datacollector

import android.app.Application
import androidx.room.Room
import com.sta.datacollector.data.ParticipantRepository
import com.sta.datacollector.data.local.STADatabase

class STAApplication : Application() {
    private val database: STADatabase by lazy {
        Room.databaseBuilder(
            applicationContext,
            STADatabase::class.java,
            "sta_database",
        ).build()
    }

    val participantRepository: ParticipantRepository by lazy {
        ParticipantRepository(database.participantDao())
    }
}
