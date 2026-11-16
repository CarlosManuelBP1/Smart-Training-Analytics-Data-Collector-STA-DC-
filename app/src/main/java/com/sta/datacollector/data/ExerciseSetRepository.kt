package com.sta.datacollector.data

import com.sta.datacollector.data.local.ExerciseSetDao
import com.sta.datacollector.data.local.ExerciseSetEntity
import kotlinx.coroutines.flow.Flow

class ExerciseSetRepository(
    private val exerciseSetDao: ExerciseSetDao,
) {
    fun observeForSession(sessionId: String): Flow<List<ExerciseSetEntity>> =
        exerciseSetDao.observeForSession(sessionId)

    fun observeActiveSet(): Flow<ExerciseSetEntity?> = exerciseSetDao.observeActiveSet()

    fun observeAllCompleted(): Flow<List<ExerciseSetEntity>> =
        exerciseSetDao.observeAllCompleted()

    fun observeForParticipant(
        sessionId: String,
        participantId: String,
    ): Flow<List<ExerciseSetEntity>> =
        exerciseSetDao.observeForParticipant(sessionId, participantId)

    suspend fun getById(setId: String): ExerciseSetEntity? = exerciseSetDao.getById(setId)

    suspend fun insert(exerciseSet: ExerciseSetEntity) {
        exerciseSetDao.insert(exerciseSet)
    }

    suspend fun startIfNoActive(exerciseSet: ExerciseSetEntity): Boolean =
        exerciseSetDao.insertIfNoActive(exerciseSet)

    suspend fun completeActiveSet(
        setId: String,
        loadKg: Float,
        repetitions: Int,
        completedAt: Long,
    ): Boolean = exerciseSetDao.completeActiveSet(
        setId = setId,
        loadKg = loadKg,
        repetitions = repetitions,
        completedAt = completedAt,
    ) == 1

    suspend fun cancelActiveSet(setId: String): Boolean =
        exerciseSetDao.cancelActiveSet(setId) == 1

    suspend fun getNextSetOrder(
        sessionId: String,
        participantId: String,
        exercise: String,
    ): Int = exerciseSetDao.getNextSetOrder(sessionId, participantId, exercise)

    suspend fun getLastCompletedLoad(participantId: String, exercise: String): Float? =
        exerciseSetDao.getLastCompletedLoad(participantId, exercise)

    suspend fun update(exerciseSet: ExerciseSetEntity): Boolean =
        exerciseSetDao.update(exerciseSet) == 1

    suspend fun delete(exerciseSet: ExerciseSetEntity): Boolean =
        exerciseSetDao.delete(exerciseSet) == 1
}
