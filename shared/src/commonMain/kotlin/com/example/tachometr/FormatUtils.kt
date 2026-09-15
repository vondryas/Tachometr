package com.example.tachometr

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp

object FormatUtils {
    fun formatDistance(distanceMeters: Float): String {
        return if (distanceMeters < 1000f) {
            "${distanceMeters.toInt()} m"
        } else {
            val km = distanceMeters / 1000f
            val rounded = (km * 100).toInt() / 100f
            "$rounded km"
        }
    }

    fun formatDuration(durationMs: Long): String {
        val totalSec = (durationMs / 1000).coerceAtLeast(0)
        val sec = totalSec % 60
        val min = (totalSec / 60) % 60
        val hrs = totalSec / 3600

        val secStr = if (sec < 10) "0$sec" else "$sec"
        val minStr = if (min < 10) "0$min" else "$min"
        val hrsStr = if (hrs < 10) "0$hrs" else "$hrs"

        return if (hrs > 0) {
            "$hrsStr:$minStr:$secStr"
        } else {
            "$minStr:$secStr"
        }
    }

    // Odhad velikosti: cca 60 bytů na jeden uložený bod LocationPoint
    fun calculateSessionSizeString(pointsCount: Int): String {
        val bytes = pointsCount * 60f
        val kb = bytes / 1024f
        return if (kb < 1024f) {
            val rounded = (kb * 10).toInt() / 10f
            "$rounded kB"
        } else {
            val mb = kb / 1024f
            val rounded = (mb * 100).toInt() / 100f
            "$rounded MB"
        }
    }

    fun getSpeedColor(speedKmh: Float, maxSpeedKmh: Float): Color {
        val max = maxSpeedKmh.coerceAtLeast(10f)
        val rawFraction = (speedKmh / max).coerceIn(0f, 1f)
        
        // Rozdělení gradientu na 25 diskrétních odstínů
        val steps = 25
        val fraction = (rawFraction * steps).toInt().toFloat() / steps

        return when {
            fraction < 0.25f -> {
                val t = fraction / 0.25f
                lerp(Color(0xFF00B0FF), Color(0xFF4CAF50), t) // Modrá -> Zelená
            }
            fraction < 0.5f -> {
                val t = (fraction - 0.25f) / 0.25f
                lerp(Color(0xFF4CAF50), Color(0xFFFFEB3B), t) // Zelená -> Žlutá
            }
            fraction < 0.75f -> {
                val t = (fraction - 0.5f) / 0.25f
                lerp(Color(0xFFFFEB3B), Color(0xFFFF9800), t) // Žlutá -> Oranžová
            }
            else -> {
                val t = (fraction - 0.75f) / 0.25f
                lerp(Color(0xFFFF9800), Color(0xFFF44336), t) // Oranžová -> Červená
            }
        }
    }
}