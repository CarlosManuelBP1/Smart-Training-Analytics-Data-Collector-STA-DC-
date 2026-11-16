package com.sta.datacollector.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ExerciseSetDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(exerciseSet: ExerciseSetEntity)

    @Update
    suspend fun update(exerciseSet: ExerciseSetEntity): Int

    @Delete
    suspend fun delete(exerciseSet: ExerciseSetEntity): Int

    @Query("SELECT * FROM exercise_sets WHERE id = :setId LIMIT 1")
    suspend fun getById(setId: String): ExerciseSetEntity?

    @Query(
        """
        SELECT * FROM exercise_sets
        WHERE completedAt IS NULL
        ORDER BY startedAt DESC
        LIMIT 1
        """
    )
    fun observeActiveSet(): Flow<ExerciseSetEntity?>

    @Query("SELECT COUNT(*) FROM exercise_sets WHERE completedAt IS NULL")
    suspend fun countActiveSets(): Int

    @Query(
        """
        SELECT COUNT(*) FROM training_sessions
        WHERE id = :sessionId AND status = 'active'
        """
    )
    suspend fun isSessionActive(sessionId: String): Int

    @Query(
        """
        SELECT COUNT(*) FROM session_participants
        WHERE sessionId = :sessionId AND participantId = :participantId
        """
    )
    suspend fun isParticipantInSession(sessionId: String, participantId: String): Int

    @Transaction
    suspend fun insertIfNoActive(exerciseSet: ExerciseSetEntity): Boolean {
        if (countActiveSets() > 0) return false
        if (isSessionActive(exerciseSet.sessionId) != 1) return false
        if (isParticipantInSession(exerciseSet.sessionId, exerciseSet.participantId) != 1) {
            return false
        }
        insert(exerciseSet)
        return true
    }

    @Query(
        """
        UPDATE exercise_sets
        SET loadKg = :loadKg,
            repetitions = :repetitions,
            completedAt = :completedAt
        WHERE id = :setId AND completedAt IS NULL
        """
    )
    suspend fun completeActiveSet(
        setId: String,
        loadKg: Float,
        repetitions: Int,
        completedAt: Long,
    ): Int

    @Query("DELETE FROM exercise_sets WHERE id = :setId AND completedAt IS NULL")
    suspend fun cancelActiveSet(setId: String): Int

    @Query(
        """
        SELECT COALESCE(MAX(setOrder), 0) + 1 FROM exercise_sets
        WHERE sessionId = :sessionId
          AND participantId = :participantId
          AND exercise = :exercise COLLATE NOCASE
        """
    )
    suspend fun getNextSetOrder(
        sessionId: String,
        participantId: String,
        exercise: String,
    ): Int

    @Query(
        """
        SELECT loadKg FROM exercise_sets
        WHERE participantId = :participantId
          AND exercise = :exercise COLLATE NOCASE
          AND completedAt IS NOT NULL
        ORDER BY completedAt DESC
        LIMIT 1
        """
    )
    suspend fun getLastCompletedLoad(participantId: String, exercise: String): Float?

    @Query(
        """
        SELECT * FROM exercise_sets
        WHERE sessionId = :sessionId
        ORDER BY startedAt ASC, setOrder ASC
        """
    )
    fun observeForSession(sessionId: String): Flow<List<ExerciseSetEntity>>

    @Query(
        """
        SELECT * FROM exercise_sets
        WHERE completedAt IS NOT NULL
        ORDER BY completedAt DESC
        """
    )
    fun observeAllCompleted(): Flow<List<ExerciseSetEntity>>

    @Query(
        """
        SELECT * FROM exercise_sets
        WHERE sessionId = :sessionId AND participantId = :participantId
        ORDER BY startedAt ASC, setOrder ASC
        """
    )
    fun observeForParticipant(
        sessionId: String,
        participantId: String,
    ): Flow<List<ExerciseSetEntity>>
}
