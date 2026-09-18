package com.example.tachometr

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.system.getTimeMillis

class HistoryViewModel(
    private val sessionDao: SessionDao,
    private val locationDao: LocationDao
) : ViewModel() {

    // Přibližný odhad velikosti jednoho bodu (v bytech)
    private val BYTES_PER_POINT = 64

    val sessions: StateFlow<List<SessionWithUiData>> = sessionDao.getAllSessions()
        .map { sessions ->
            sessions.map { session ->
                val pointsCount = locationDao.getPointCountForSession(session.id)
                val sizeBytes = pointsCount * BYTES_PER_POINT
                
                val durationMs = (session.endTime ?: getTimeMillis()) - session.startTime
                val formattedDate = formatDateTime(session.startTime)

                SessionWithUiData(
                    session = session,
                    formattedDate = formattedDate,
                    formattedDistance = FormatUtils.formatDistance(session.totalDistance),
                    formattedDuration = FormatUtils.formatDuration(durationMs),
                    formattedSize = formatSize(sizeBytes.toLong())
                )
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun getDatabaseSize(): String {
        return getDatabaseSizeStr()
    }

    fun deleteSession(sessionId: Long) {
        viewModelScope.launch {
            locationDao.deletePointsForSession(sessionId)
            sessionDao.deleteSession(sessionId)
        }
    }

    fun renameSession(sessionId: Long, newName: String) {
        viewModelScope.launch {
            val session = sessionDao.getSessionById(sessionId)
            if (session != null && newName.isNotBlank()) {
                sessionDao.updateSession(session.copy(name = newName.trim()))
            }
        }
    }

    private fun formatSize(bytes: Long): String {
        val kb = bytes / 1024.0
        val mb = kb / 1024.0
        return when {
            mb >= 1.0 -> formatFloat(mb.toFloat(), 2) + " MB"
            kb >= 1.0 -> formatFloat(kb.toFloat(), 2) + " kB"
            else -> "$bytes B"
        }
    }
}

data class SessionWithUiData(
    val session: PathSession,
    val formattedDate: String,
    val formattedDistance: String,
    val formattedDuration: String,
    val formattedSize: String
)