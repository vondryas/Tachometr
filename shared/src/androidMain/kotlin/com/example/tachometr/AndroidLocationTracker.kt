package com.example.tachometr

import android.content.Context
import android.content.Intent

class AndroidLocationTracker(private val context: Context) : LocationTracker {
    override fun startTracking(sessionId: Long) {
        val intent = Intent().apply {
            setClassName(context, "com.example.tachometr.LocationService")
            action = "ACTION_START"
            putExtra("EXTRA_SESSION_ID", sessionId)
        }
        // V moderním Androidu musíme službu startovat tímto speciálním příkazem
        context.startForegroundService(intent)
    }

    override fun stopTracking() {
        val intent = Intent().apply {
            setClassName(context, "com.example.tachometr.LocationService")
            action = "ACTION_STOP"
        }
        context.startService(intent)
    }
}