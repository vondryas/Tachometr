package com.example.tachometr

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class PathDetailViewModel(
    private val sessionId: Long,
    private val sessionDao: SessionDao,
    private val locationDao: LocationDao
) : ViewModel() {

    private val _session = MutableStateFlow<PathSession?>(null)
    val session: StateFlow<PathSession?> = _session

    private val _points = MutableStateFlow<List<LocationPoint>>(emptyList())
    val points: StateFlow<List<LocationPoint>> = _points

    // Výběr úseku
    private val _selectedTimeRange = MutableStateFlow<ClosedFloatingPointRange<Float>?>(null)
    val selectedTimeRange: StateFlow<ClosedFloatingPointRange<Float>?> = _selectedTimeRange

    private val _selectedDistance = MutableStateFlow(0f)
    val selectedDistance: StateFlow<Float> = _selectedDistance

    private val _selectedMaxSpeed = MutableStateFlow(0f)
    val selectedMaxSpeed: StateFlow<Float> = _selectedMaxSpeed

    private val _selectedAvgSpeed = MutableStateFlow(0f)
    val selectedAvgSpeed: StateFlow<Float> = _selectedAvgSpeed

    private val _selectedDuration = MutableStateFlow(0L)
    val selectedDuration: StateFlow<Long> = _selectedDuration

    private val _selectedStartTimeStr = MutableStateFlow("")
    val selectedStartTimeStr: StateFlow<String> = _selectedStartTimeStr

    private val _selectedEndTimeStr = MutableStateFlow("")
    val selectedEndTimeStr: StateFlow<String> = _selectedEndTimeStr

    init {
        viewModelScope.launch {
            _session.value = sessionDao.getSessionById(sessionId)
            locationDao.getPointsForSession(sessionId).collect { allPoints ->
                _points.value = allPoints
            }
        }
    }

    fun updateSelection(timeRange: ClosedFloatingPointRange<Float>?) {
        _selectedTimeRange.value = timeRange
        
        if (timeRange == null || _points.value.isEmpty()) {
            _selectedDistance.value = 0f
            _selectedMaxSpeed.value = 0f
            _selectedAvgSpeed.value = 0f
            return
        }

        val firstTime = _points.value.first().timestamp
        val selectedPoints = _points.value.filter { point ->
            val relTime = (point.timestamp - firstTime).toFloat() / 1000f // čas v sekundách od začátku
            relTime in timeRange
        }

        if (selectedPoints.isNotEmpty()) {
            _selectedDistance.value = selectedPoints.sumOf { it.distanceSinceLast.toDouble() }.toFloat()
            _selectedMaxSpeed.value = selectedPoints.maxOf { it.speedKmh }
            _selectedAvgSpeed.value = selectedPoints.sumOf { it.speedKmh.toDouble() }.toFloat() / selectedPoints.size
            _selectedDuration.value = selectedPoints.last().timestamp - selectedPoints.first().timestamp
            
            val startRel = selectedPoints.first().timestamp - firstTime
            val endRel = selectedPoints.last().timestamp - firstTime
            _selectedStartTimeStr.value = FormatUtils.formatDuration(startRel)
            _selectedEndTimeStr.value = FormatUtils.formatDuration(endRel)
        } else {
            _selectedDistance.value = 0f
            _selectedMaxSpeed.value = 0f
            _selectedAvgSpeed.value = 0f
            _selectedDuration.value = 0L
            _selectedStartTimeStr.value = ""
            _selectedEndTimeStr.value = ""
        }
    }

    fun renameSession(newName: String) {
        viewModelScope.launch {
            val current = _session.value
            if (current != null && newName.isNotBlank()) {
                val updated = current.copy(name = newName.trim())
                sessionDao.updateSession(updated)
                _session.value = updated
            }
        }
    }
}