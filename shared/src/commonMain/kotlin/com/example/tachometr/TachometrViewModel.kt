package com.example.tachometr

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
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

    private val _elapsedTime = MutableStateFlow(0L)
    val elapsedTime: StateFlow<Long> = _elapsedTime

    private val _averageSpeed = MutableStateFlow(0f)
    val averageSpeed: StateFlow<Float> = _averageSpeed

    private val _speedometerMaxSpeed = MutableStateFlow(100f) // Výchozí maximum pro grafický tachometr
    val speedometerMaxSpeed: StateFlow<Float> = _speedometerMaxSpeed

    private val _isAnalogMode = MutableStateFlow(false)
    val isAnalogMode: StateFlow<Boolean> = _isAnalogMode

    private var currentSessionId: Long = 0L
    private var trackingJob: Job? = null
    private var timerJob: Job? = null

    fun setSpeedometerMaxSpeed(max: Float) {
        _speedometerMaxSpeed.value = max
    }

    fun toggleAnalogMode() {
        _isAnalogMode.value = !_isAnalogMode.value
    }

    fun toggleTracking(onStopped: ((Long) -> Unit)? = null) {
        viewModelScope.launch {
            if (_isTracking.value) {
                // ZASTAVENÍ MĚŘENÍ
                locationTracker.stopTracking()
                trackingJob?.cancel() // Zrušíme aktivní poslech databáze
                timerJob?.cancel()
                _isTracking.value = false

                val stoppedSessionId = currentSessionId
                // Uložení konečných dat do existující relace
                val endTime = System.currentTimeMillis()
                val finalDistance = _distance.value
                val finalMaxSpeed = _maxSpeed.value

                val sessions = sessionDao.getAllSessions().first()
                val currentSession = sessions.find { it.id == currentSessionId }
                if (currentSession != null) {
                    sessionDao.updateSession(
                        currentSession.copy(
                            endTime = endTime,
                            totalDistance = finalDistance,
                            maxSpeed = finalMaxSpeed
                        )
                    )
                }
                
                onStopped?.invoke(stoppedSessionId)
            } else {
                // START MĚŘENÍ
                currentSessionId = System.currentTimeMillis()

                // Vygenerování automatického názvu "Cesta X"
                val sessions = sessionDao.getAllSessions().first()
                val nextNumber = sessions.size + 1
                val newSession = PathSession(
                    id = currentSessionId,
                    name = "Cesta $nextNumber",
                    startTime = currentSessionId
                )

                sessionDao.insertSession(newSession)

                // Nulování předchozích hodnot
                _distance.value = 0f
                _maxSpeed.value = 0f
                _currentSpeed.value = 0f
                _elapsedTime.value = 0L

                locationTracker.startTracking(currentSessionId)
                _isTracking.value = true
                startTimer()
                observeSessionData(currentSessionId)
            }
        }
    }

    private var lastPointTime: Long = 0L

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            val startTime = System.currentTimeMillis()
            while (true) {
                val now = System.currentTimeMillis()
                val elapsed = now - startTime
                _elapsedTime.value = elapsed
                updateAverageSpeed(_distance.value, elapsed)
                // Pokud už víc než 2.5s nepřišel nový bod, rychlost klesne na 0
                if (lastPointTime > 0 && now - lastPointTime > 2500L) {
                    _currentSpeed.value = 0f
                }
                delay(1000L)
            }
        }
    }

    private fun observeSessionData(sessionId: Long) {
        trackingJob?.cancel() // Pro jistotu zrušíme předchozí job
        trackingJob = viewModelScope.launch {
            launch {
                locationDao.getTotalDistance(sessionId).collectLatest { dist ->
                    val totalDist = dist ?: 0f
                    _distance.value = totalDist
                    updateAverageSpeed(totalDist, _elapsedTime.value)
                }
            }

            launch {
                locationDao.getMaxSpeed(sessionId).collectLatest { max ->
                    _maxSpeed.value = max ?: 0f
                }
            }

            launch {
                locationDao.getPointsForSession(sessionId).collectLatest { points ->
                    val lastPoint = points.lastOrNull()
                    if (lastPoint != null) {
                        lastPointTime = lastPoint.timestamp
                        val speed = lastPoint.speedKmh
                        _currentSpeed.value = if (speed < 1.0f) 0f else speed
                    } else {
                        _currentSpeed.value = 0f
                    }
                }
            }
        }
    }

    private fun updateAverageSpeed(distanceMeters: Float, elapsedTimeMs: Long) {
        if (elapsedTimeMs > 0) {
            val hours = elapsedTimeMs / 3600000.0
            val km = distanceMeters / 1000.0
            _averageSpeed.value = (km / hours).toFloat()
        } else {
            _averageSpeed.value = 0f
        }
    }
}