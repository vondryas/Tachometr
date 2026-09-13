package com.example.tachometr

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface LocationDao {
    @Insert
    suspend fun insertPoint(point: LocationPoint)

    @Query("SELECT * FROM location_points WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    fun getPointsForSession(sessionId: Long): Flow<List<LocationPoint>>

    @Query("SELECT SUM(distanceSinceLast) FROM location_points WHERE sessionId = :sessionId")
    fun getTotalDistance(sessionId: Long): Flow<Float?>

    @Query("SELECT MAX(speedKmh) FROM location_points WHERE sessionId = :sessionId")
    fun getMaxSpeed(sessionId: Long): Flow<Float?>

    @Query("DELETE FROM location_points WHERE sessionId = :sessionId")
    suspend fun deletePointsForSession(sessionId: Long)

    @Query("SELECT COUNT(*) FROM location_points WHERE sessionId = :sessionId")
    suspend fun getPointCountForSession(sessionId: Long): Int
}