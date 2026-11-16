package com.sta.datacollector.data

import com.sta.datacollector.data.local.ParticipantDao
import com.sta.datacollector.data.local.ParticipantEntity
import kotlinx.coroutines.flow.Flow

class ParticipantRepository(
    private val participantDao: ParticipantDao,
) {
    fun observeActiveParticipants(): Flow<List<ParticipantEntity>> =
        participantDao.observeActiveParticipants()

    fun observeAllParticipants(): Flow<List<ParticipantEntity>> =
        participantDao.observeAllParticipants()

    suspend fun getById(id: String): ParticipantEntity? = participantDao.getById(id)

    suspend fun insert(participant: ParticipantEntity) {
        participantDao.insert(participant)
    }

    suspend fun update(participant: ParticipantEntity): Boolean =
        participantDao.update(participant) == 1

    suspend fun archive(id: String, updatedAt: Long): Boolean =
        participantDao.archive(id, updatedAt) == 1
}
