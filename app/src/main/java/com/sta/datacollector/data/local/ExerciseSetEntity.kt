package com.sta.datacollector.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "exercise_sets",
    foreignKeys = [
        ForeignKey(
            entity = SessionParticipantEntity::class,
            parentColumns = ["sessionId", "participantId"],
            childColumns = ["sessionId", "participantId"],
            onUpdate = ForeignKey.CASCADE,
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["sessionId", "participantId"]),
        Index(value = ["sessionId", "startedAt"]),
        Index(value = ["participantId"]),
    ],
)
data class ExerciseSetEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val sessionId: String,
    val participantId: String,
    val exercise: String,
    val loadKg: Float,
    val repetitions: Int,
    val setOrder: Int,
    val startedAt: Long,
    val completedAt: Long? = null,
)
