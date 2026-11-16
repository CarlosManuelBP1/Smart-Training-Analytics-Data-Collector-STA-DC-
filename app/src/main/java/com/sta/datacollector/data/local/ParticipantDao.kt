package com.sta.datacollector.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ParticipantDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(participant: ParticipantEntity)

    @Update
    suspend fun update(participant: ParticipantEntity): Int

    @Query("SELECT * FROM participants WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): ParticipantEntity?

    @Query(
        """
        SELECT * FROM participants
        WHERE isActive = 1
        ORDER BY nickname COLLATE NOCASE ASC
        """
    )
    fun observeActiveParticipants(): Flow<List<ParticipantEntity>>

    @Query(
        """
        UPDATE participants
        SET isActive = 0, updatedAt = :updatedAt
        WHERE id = :id AND isActive = 1
        """
    )
    suspend fun archive(id: String, updatedAt: Long): Int
}
