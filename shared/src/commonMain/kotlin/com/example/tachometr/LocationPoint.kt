package com.example.tachometr

import androidx.room3.Entity
import androidx.room3.PrimaryKey


@Entity(tableName = "location_points")
data class LocationPoint(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val sessionId: Long,
    val timestamp: Long,
    val latitude: Double,
    val longitude: Double,
    val speedKmh: Float,
    val distanceSinceLast: Float
)