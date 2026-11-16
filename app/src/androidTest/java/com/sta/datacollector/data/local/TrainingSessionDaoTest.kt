package com.sta.datacollector.data.local

import android.content.Context
import android.database.sqlite.SQLiteConstraintException
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TrainingSessionDaoTest {
    private lateinit var database: STADatabase
    private lateinit var participantDao: ParticipantDao
    private lateinit var sessionDao: TrainingSessionDao
    private lateinit var exerciseSetDao: ExerciseSetDao

    @Before
    fun createDatabase() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, STADatabase::class.java)
            .allowMainThreadQueries()
            .build()
        participantDao = database.participantDao()
        sessionDao = database.trainingSessionDao()
        exerciseSetDao = database.exerciseSetDao()
    }

    @After
    fun closeDatabase() {
        database.close()
    }

    @Test
    fun multipleParticipantsCanAlternateSetsInSameSession() = runBlocking {
        val ana = participant(id = "participant-ana", nickname = "Ana")
        val bruno = participant(id = "participant-bruno", nickname = "Bruno")
        participantDao.insert(ana)
        participantDao.insert(bruno)

        val session = TrainingSessionEntity(
            id = "session-shared",
            startedAt = 1_000L,
        )
        sessionDao.insertSessionWithParticipants(
            session = session,
            participants = listOf(
                SessionParticipantEntity(session.id, ana.id),
                SessionParticipantEntity(session.id, bruno.id),
            ),
        )

        val sets = listOf(
            exerciseSet(
                id = "set-ana-1",
                sessionId = session.id,
                participantId = ana.id,
                setOrder = 1,
                startedAt = 1_100L,
            ),
            exerciseSet(
                id = "set-bruno-1",
                sessionId = session.id,
                participantId = bruno.id,
                setOrder = 1,
                startedAt = 1_200L,
            ),
            exerciseSet(
                id = "set-ana-2",
                sessionId = session.id,
                participantId = ana.id,
                setOrder = 2,
                startedAt = 1_300L,
            ),
        )
        sets.forEach { exerciseSetDao.insert(it) }

        assertEquals(
            listOf(ana.id, bruno.id),
            sessionDao.getParticipantsForSession(session.id).map(ParticipantEntity::id),
        )
        assertEquals(
            sets.map(ExerciseSetEntity::id),
            exerciseSetDao.observeForSession(session.id).first().map(ExerciseSetEntity::id),
        )
        assertEquals(
            listOf("set-ana-1", "set-ana-2"),
            exerciseSetDao.observeForParticipant(session.id, ana.id)
                .first()
                .map(ExerciseSetEntity::id),
        )
    }

    @Test
    fun unregisteredParticipantCannotOwnSet() = runBlocking {
        val registered = participant(id = "registered", nickname = "Registrado")
        val outsider = participant(id = "outsider", nickname = "No inscrito")
        participantDao.insert(registered)
        participantDao.insert(outsider)

        val session = TrainingSessionEntity(id = "session", startedAt = 1_000L)
        sessionDao.insertSessionWithParticipants(
            session = session,
            participants = listOf(SessionParticipantEntity(session.id, registered.id)),
        )

        var rejectedByForeignKey = false
        try {
            exerciseSetDao.insert(
                exerciseSet(
                    id = "invalid-set",
                    sessionId = session.id,
                    participantId = outsider.id,
                    setOrder = 1,
                    startedAt = 1_100L,
                )
            )
        } catch (_: SQLiteConstraintException) {
            rejectedByForeignKey = true
        }

        assertTrue(rejectedByForeignKey)
        assertTrue(exerciseSetDao.observeForSession(session.id).first().isEmpty())
    }

    private fun participant(id: String, nickname: String) = ParticipantEntity(
        id = id,
        nickname = nickname,
        createdAt = 100L,
        updatedAt = 100L,
    )

    private fun exerciseSet(
        id: String,
        sessionId: String,
        participantId: String,
        setOrder: Int,
        startedAt: Long,
    ) = ExerciseSetEntity(
        id = id,
        sessionId = sessionId,
        participantId = participantId,
        exercise = "Sentadilla",
        loadKg = 80f,
        repetitions = 5,
        setOrder = setOrder,
        startedAt = startedAt,
        completedAt = startedAt + 30_000L,
    )
}
