package com.example.tachometr

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class TachometerViewModel(
    private val locationDao: LocationDao,
    private val sessionDao: SessionDao,
    private val locationTracker: LocationTracker
) : ViewModel() {

    private val _currentSpeed = MutableStateFlow(0f)
    val currentSpeed: StateFlow<Float> = _currentSpeed

    private val _maxSpeed = MutableStateFlow(0f)
    val maxSpeed: StateFlow<Float> = _maxSpeed

    private val _distance = MutableStateFlow(0f)
    val distance: StateFlow<Float> = _distance

    private val _isTracking = MutableStateFlow(false)
    val isTracking: StateFlow<Boolean> = _isTracking

    private var currentSessionId: Long = 0L
    private var trackingJob: Job? = null

    fun toggleTracking() {
        if (_isTracking.value) {
            locationTracker.stopTracking()
            _isTracking.value = false
            trackingJob?.cancel()
            trackingJob = null
            
            // Ukončit session
            viewModelScope.launch {
                val session = sessionDao.getSessionById(currentSessionId)
                if (session != null) {
                    sessionDao.updateSession(
                        session.copy(
                            endTime = System.currentTimeMillis(),
                            totalDistance = _distance.value,
                            maxSpeed = _maxSpeed.value
                        )
                    )
                }
            }
        } else {
            currentSessionId = System.currentTimeMillis()
            
            // Vytvořit novou session
            viewModelScope.launch {
                val count = sessionDao.getSessionCount()
                val session = PathSession(
                    id = currentSessionId,
                    name = "Cesta ${count + 1}",
                    startTime = currentSessionId
                )
                sessionDao.insertSession(session)
            }

            locationTracker.startTracking(currentSessionId)
            _isTracking.value = true
            observeSessionData(currentSessionId)
        }
    }

    private fun observeSessionData(sessionId: Long) {
        trackingJob?.cancel()
        trackingJob = viewModelScope.launch {
            // Sledujeme celkovou ujetou vzdálenost
            launch {
                locationDao.getTotalDistance(sessionId).collectLatest { dist ->
                    _distance.value = dist ?: 0f
                }
            }

            // Sledujeme maximální rychlost
            launch {
                locationDao.getMaxSpeed(sessionId).collectLatest { max ->
                    _maxSpeed.value = max ?: 0f
                }
            }

            // Sledujeme aktuální rychlost (poslední vložený bod)
            launch {
                locationDao.getPointsForSession(sessionId).collectLatest { points ->
                    val lastPoint = points.lastOrNull()
                    _currentSpeed.value = lastPoint?.speedKmh ?: 0f
                }
            }
        }
    }
}