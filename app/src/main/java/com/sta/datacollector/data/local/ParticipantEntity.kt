package com.sta.datacollector.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "participants")
data class ParticipantEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val nickname: String,
    val heightCm: Float? = null,
    val weightKg: Float? = null,
    val experienceLevel: String? = null,
    val isActive: Boolean = true,
    val createdAt: Long,
    val updatedAt: Long,
)
