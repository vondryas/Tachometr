package com.example.tachometr

import androidx.room3.Entity
import androidx.room3.PrimaryKey

@Entity(tableName = "path_sessions")
data class PathSession(
    @PrimaryKey val id: Long, // Použijeme rovnou sessionId (timestamp)
    val name: String,
    val startTime: Long,
    val endTime: Long? = null,
    val totalDistance: Float = 0f,
    val maxSpeed: Float = 0f
)