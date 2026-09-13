package com.example.tachometr

object FormatUtils {
    fun formatDistance(distanceMeters: Float): String {
        return if (distanceMeters < 1000f) {
            "${distanceMeters.toInt()} m"
        } else {
            val km = distanceMeters / 1000f
            "${(km * 100).toInt() / 100f} km"
        }
    }
}