package com.sta.datacollector.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TrainingSessionDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertSession(session: TrainingSessionEntity)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertSessionParticipants(participants: List<SessionParticipantEntity>)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun addParticipant(participant: SessionParticipantEntity)

    @Update
    suspend fun updateSession(session: TrainingSessionEntity): Int

    @Transaction
    suspend fun insertSessionWithParticipants(
        session: TrainingSessionEntity,
        participants: List<SessionParticipantEntity>,
    ) {
        insertSession(session)
        insertSessionParticipants(participants)
    }

    @Query("SELECT * FROM training_sessions WHERE id = :sessionId LIMIT 1")
    suspend fun getSessionById(sessionId: String): TrainingSessionEntity?

    @Query(
        """
        SELECT * FROM training_sessions
        ORDER BY startedAt DESC
        """
    )
    fun observeSessions(): Flow<List<TrainingSessionEntity>>

    @Query(
        """
        SELECT participants.* FROM participants
        INNER JOIN session_participants
            ON participants.id = session_participants.participantId
        WHERE session_participants.sessionId = :sessionId
        ORDER BY participants.nickname COLLATE NOCASE ASC
        """
    )
    suspend fun getParticipantsForSession(sessionId: String): List<ParticipantEntity>

    @Query(
        """
        UPDATE training_sessions
        SET status = :status, endedAt = :endedAt
        WHERE id = :sessionId
        """
    )
    suspend fun updateSessionStatus(
        sessionId: String,
        status: String,
        endedAt: Long?,
    ): Int
}
