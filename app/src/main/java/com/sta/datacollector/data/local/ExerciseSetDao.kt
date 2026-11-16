package com.sta.datacollector.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
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
        WHERE sessionId = :sessionId
        ORDER BY startedAt ASC, setOrder ASC
        """
    )
    fun observeForSession(sessionId: String): Flow<List<ExerciseSetEntity>>

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
