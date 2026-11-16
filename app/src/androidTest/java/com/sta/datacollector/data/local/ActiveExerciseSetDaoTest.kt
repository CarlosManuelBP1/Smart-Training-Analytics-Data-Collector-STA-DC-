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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ActiveExerciseSetDaoTest {
    private lateinit var context: Context
    private lateinit var database: STADatabase

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        context.deleteDatabase(DATABASE_NAME)
        database = openDatabase()
    }

    @After
    fun tearDown() {
        database.close()
        context.deleteDatabase(DATABASE_NAME)
    }

    @Test
    fun onlyOneActiveSetExistsAndParticipantsCanAlternateAfterCompletion() = runBlocking {
        val ana = participant("ana", "Ana")
        val bruno = participant("bruno", "Bruno")
        database.participantDao().insert(ana)
        database.participantDao().insert(bruno)
        createSession("session", ana, bruno)

        val first = activeSet("set-1", "session", ana.id, 1)
        val simultaneous = activeSet("set-2", "session", bruno.id, 1)
        assertTrue(database.exerciseSetDao().insertIfNoActive(first))
        assertFalse(database.exerciseSetDao().insertIfNoActive(simultaneous))
        assertEquals("set-1", database.exerciseSetDao().observeActiveSet().first()?.id)

        assertEquals(1, database.exerciseSetDao().completeActiveSet("set-1", 82.5f, 6, 2_000L))
        assertTrue(database.exerciseSetDao().insertIfNoActive(simultaneous))
        assertEquals(bruno.id, database.exerciseSetDao().observeActiveSet().first()?.participantId)
    }

    @Test
    fun cancellationDeletesIncompleteSetAndFinishedSessionRejectsNewSets() = runBlocking {
        val ana = participant("ana", "Ana")
        database.participantDao().insert(ana)
        createSession("session", ana)
        val incomplete = activeSet("incomplete", "session", ana.id, 1)

        assertTrue(database.exerciseSetDao().insertIfNoActive(incomplete))
        assertEquals(1, database.exerciseSetDao().cancelActiveSet(incomplete.id))
        assertNull(database.exerciseSetDao().observeActiveSet().first())
        assertTrue(database.exerciseSetDao().observeAllCompleted().first().isEmpty())

        assertEquals(
            1,
            database.trainingSessionDao().updateSessionStatus(
                "session",
                TrainingSessionStatus.COMPLETED,
                3_000L,
            ),
        )
        assertFalse(database.exerciseSetDao().insertIfNoActive(activeSet("late", "session", ana.id, 2)))
    }

    @Test
    fun activeSessionAndIncompleteSetSurviveDatabaseReopen() = runBlocking {
        val ana = participant("ana", "Ana")
        database.participantDao().insert(ana)
        createSession("persisted-session", ana)
        assertTrue(
            database.exerciseSetDao().insertIfNoActive(
                activeSet("persisted-set", "persisted-session", ana.id, 1)
            )
        )

        database.close()
        database = openDatabase()

        assertEquals(
            "persisted-session",
            database.trainingSessionDao().observeActiveSession().first()?.id,
        )
        assertNotNull(database.exerciseSetDao().observeActiveSet().first())
        assertEquals("persisted-set", database.exerciseSetDao().observeActiveSet().first()?.id)
    }

    private suspend fun createSession(id: String, vararg participants: ParticipantEntity) {
        database.trainingSessionDao().insertSessionWithParticipants(
            TrainingSessionEntity(id = id, startedAt = 1_000L),
            participants.map { SessionParticipantEntity(id, it.id) },
        )
    }

    private fun openDatabase(): STADatabase = Room.databaseBuilder(
        context,
        STADatabase::class.java,
        DATABASE_NAME,
    ).addMigrations(MIGRATION_1_2).allowMainThreadQueries().build()

    private fun participant(id: String, nickname: String) = ParticipantEntity(
        id = id,
        nickname = nickname,
        createdAt = 100L,
        updatedAt = 100L,
    )

    private fun activeSet(id: String, sessionId: String, participantId: String, order: Int) =
        ExerciseSetEntity(
            id = id,
            sessionId = sessionId,
            participantId = participantId,
            exercise = "Sentadilla",
            loadKg = 80f,
            repetitions = 5,
            setOrder = order,
            startedAt = 1_500L + order,
        )

    private companion object {
        const val DATABASE_NAME = "active-series-test.db"
    }
}
