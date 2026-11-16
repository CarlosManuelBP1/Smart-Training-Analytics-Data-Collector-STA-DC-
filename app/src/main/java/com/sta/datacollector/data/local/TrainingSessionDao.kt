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

    @Query("SELECT COUNT(*) FROM training_sessions WHERE status = 'active'")
    suspend fun countActiveSessions(): Int

    @Transaction
    suspend fun insertSessionWithParticipantsIfNoneActive(
        session: TrainingSessionEntity,
        participants: List<SessionParticipantEntity>,
    ): Boolean {
        if (countActiveSessions() > 0) return false
        insertSessionWithParticipants(session, participants)
        return true
    }

    @Query("SELECT * FROM training_sessions WHERE id = :sessionId LIMIT 1")
    suspend fun getSessionById(sessionId: String): TrainingSessionEntity?

    @Query(
        """
        SELECT * FROM training_sessions
        WHERE status = 'active'
        ORDER BY startedAt DESC
        LIMIT 1
        """
    )
    fun observeActiveSession(): Flow<TrainingSessionEntity?>

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
        SELECT participants.* FROM participants
        INNER JOIN session_participants
            ON participants.id = session_participants.participantId
        WHERE session_participants.sessionId = :sessionId
        ORDER BY participants.nickname COLLATE NOCASE ASC
        """
    )
    fun observeParticipantsForSession(sessionId: String): Flow<List<ParticipantEntity>>

    @Query(
        """
        SELECT COUNT(*) FROM training_sessions
        WHERE id = :sessionId AND status = 'active'
        """
    )
    suspend fun isSessionActive(sessionId: String): Int

    @Transaction
    suspend fun addParticipantsToActiveSession(
        sessionId: String,
        participants: List<SessionParticipantEntity>,
    ): Boolean {
        if (isSessionActive(sessionId) != 1) return false
        insertSessionParticipants(participants)
        return true
    }

    @Query(
        """
        UPDATE training_sessions
        SET status = :status, endedAt = :endedAt
        WHERE id = :sessionId AND status = 'active'
        """
    )
    suspend fun updateSessionStatus(
        sessionId: String,
        status: String,
        endedAt: Long?,
    ): Int
}
