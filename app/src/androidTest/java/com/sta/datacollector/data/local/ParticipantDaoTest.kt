package com.sta.datacollector.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ParticipantDaoTest {
    private lateinit var database: STADatabase
    private lateinit var dao: ParticipantDao

    @Before
    fun createDatabase() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, STADatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = database.participantDao()
    }

    @After
    fun closeDatabase() {
        database.close()
    }

    @Test
    fun insertObserveAndArchiveParticipant() = runBlocking {
        val createdAt = 1_700_000_000_000L
        val participant = ParticipantEntity(
            nickname = "RunnerOne",
            heightCm = 175f,
            weightKg = 68.5f,
            experienceLevel = "intermediate",
            createdAt = createdAt,
            updatedAt = createdAt,
        )

        dao.insert(participant)

        val stored = dao.getById(participant.id)
        assertNotNull(stored)
        assertEquals(participant, stored)
        assertEquals(listOf(participant), dao.observeActiveParticipants().first())

        val archivedAt = createdAt + 1_000L
        assertEquals(1, dao.archive(participant.id, archivedAt))

        val archived = dao.getById(participant.id)
        assertNotNull(archived)
        assertFalse(archived!!.isActive)
        assertEquals(archivedAt, archived.updatedAt)
        assertTrue(dao.observeActiveParticipants().first().isEmpty())
    }
}
