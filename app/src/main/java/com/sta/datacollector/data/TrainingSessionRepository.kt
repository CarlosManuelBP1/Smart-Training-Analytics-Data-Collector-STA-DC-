package com.sta.datacollector.data

import com.sta.datacollector.data.local.ParticipantEntity
import com.sta.datacollector.data.local.SessionParticipantEntity
import com.sta.datacollector.data.local.TrainingSessionDao
import com.sta.datacollector.data.local.TrainingSessionEntity
import kotlinx.coroutines.flow.Flow

class TrainingSessionRepository(
    private val trainingSessionDao: TrainingSessionDao,
) {
    fun observeSessions(): Flow<List<TrainingSessionEntity>> =
        trainingSessionDao.observeSessions()

    suspend fun getSessionById(sessionId: String): TrainingSessionEntity? =
        trainingSessionDao.getSessionById(sessionId)

    suspend fun getParticipants(sessionId: String): List<ParticipantEntity> =
        trainingSessionDao.getParticipantsForSession(sessionId)

    suspend fun createSharedSession(
        session: TrainingSessionEntity,
        participantIds: Collection<String>,
    ) {
        val uniqueParticipantIds = participantIds.distinct()
        require(uniqueParticipantIds.isNotEmpty()) {
            "Una sesión debe tener al menos un participante."
        }
        trainingSessionDao.insertSessionWithParticipants(
            session = session,
            participants = uniqueParticipantIds.map { participantId ->
                SessionParticipantEntity(
                    sessionId = session.id,
                    participantId = participantId,
                )
            },
        )
    }

    suspend fun addParticipant(sessionId: String, participantId: String) {
        trainingSessionDao.addParticipant(
            SessionParticipantEntity(
                sessionId = sessionId,
                participantId = participantId,
            )
        )
    }

    suspend fun updateSession(session: TrainingSessionEntity): Boolean =
        trainingSessionDao.updateSession(session) == 1

    suspend fun updateStatus(
        sessionId: String,
        status: String,
        endedAt: Long?,
    ): Boolean = trainingSessionDao.updateSessionStatus(sessionId, status, endedAt) == 1
}
