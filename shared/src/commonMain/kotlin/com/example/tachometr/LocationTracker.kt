package com.example.tachometr

interface LocationTracker {
    fun startTracking(sessionId: Long)
    fun stopTracking()
}