package com.example.tachometr

import android.content.Context
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

var applicationContextForPlatformUtils: Context? = null

actual fun formatDateTime(timestamp: Long): String {
    if (timestamp <= 0L) return ""
    val sdf = SimpleDateFormat("d. M. yyyy HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

actual fun formatFloat(value: Float, decimals: Int): String {
    return String.format(Locale.US, "%.${decimals}f", value)
}

actual fun getDatabaseSizeStr(): String {
    val ctx = applicationContextForPlatformUtils ?: return "0 B"
    val dbFile = ctx.getDatabasePath("tachometr_database.db")
    if (dbFile.exists()) {
        val bytes = dbFile.length()
        val kb = bytes / 1024.0
        val mb = kb / 1024.0
        return when {
            mb >= 1.0 -> formatFloat(mb.toFloat(), 2) + " MB"
            kb >= 1.0 -> formatFloat(kb.toFloat(), 2) + " kB"
            else -> "$bytes B"
        }
    }
    return "0 B"
}