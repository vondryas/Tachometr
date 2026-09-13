package com.example.tachometr

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.Query
import androidx.room3.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface SessionDao {
    @Insert
    suspend fun insertSession(session: PathSession)

    @Update
    suspend fun updateSession(session: PathSession)

    @Query("SELECT * FROM path_sessions ORDER BY startTime DESC")
    fun getAllSessions(): Flow<List<PathSession>>

    @Query("SELECT * FROM path_sessions WHERE id = :sessionId LIMIT 1")
    suspend fun getSessionById(sessionId: Long): PathSession?

    @Query("DELETE FROM path_sessions WHERE id = :sessionId")
    suspend fun deleteSession(sessionId: Long)
    
    @Query("SELECT COUNT(*) FROM path_sessions")
    suspend fun getSessionCount(): Int
}