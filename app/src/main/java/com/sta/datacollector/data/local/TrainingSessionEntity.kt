package com.sta.datacollector.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "training_sessions",
    indices = [
        Index(value = ["startedAt"]),
        Index(value = ["status"]),
    ],
)
data class TrainingSessionEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val startedAt: Long,
    val endedAt: Long? = null,
    val status: String = TrainingSessionStatus.ACTIVE,
)

object TrainingSessionStatus {
    const val ACTIVE = "active"
    const val COMPLETED = "completed"
    const val CANCELLED = "cancelled"
}
