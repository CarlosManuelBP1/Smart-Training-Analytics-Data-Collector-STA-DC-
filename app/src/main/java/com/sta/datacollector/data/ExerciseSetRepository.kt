package com.sta.datacollector.data

import com.sta.datacollector.data.local.ExerciseSetDao
import com.sta.datacollector.data.local.ExerciseSetEntity
import kotlinx.coroutines.flow.Flow

class ExerciseSetRepository(
    private val exerciseSetDao: ExerciseSetDao,
) {
    fun observeForSession(sessionId: String): Flow<List<ExerciseSetEntity>> =
        exerciseSetDao.observeForSession(sessionId)

    fun observeForParticipant(
        sessionId: String,
        participantId: String,
    ): Flow<List<ExerciseSetEntity>> =
        exerciseSetDao.observeForParticipant(sessionId, participantId)

    suspend fun getById(setId: String): ExerciseSetEntity? = exerciseSetDao.getById(setId)

    suspend fun insert(exerciseSet: ExerciseSetEntity) {
        exerciseSetDao.insert(exerciseSet)
    }

    suspend fun update(exerciseSet: ExerciseSetEntity): Boolean =
        exerciseSetDao.update(exerciseSet) == 1

    suspend fun delete(exerciseSet: ExerciseSetEntity): Boolean =
        exerciseSetDao.delete(exerciseSet) == 1
}
