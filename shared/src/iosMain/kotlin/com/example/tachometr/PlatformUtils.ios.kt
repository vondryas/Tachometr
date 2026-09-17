package com.example.tachometr

import platform.Foundation.NSDate
import platform.Foundation.NSDateFormatter
import platform.Foundation.NSString
import platform.Foundation.stringWithFormat

actual fun formatDateTime(timestamp: Long): String {
    if (timestamp <= 0L) return ""
    return "1. 1. 2026 12:00"
}

actual fun formatFloat(value: Float, decimals: Int): String {
    return NSString.stringWithFormat("%.${decimals}f", value.toDouble())
}

actual fun getDatabaseSizeStr(): String {
    return "N/A" // Pro teď, iOS DB size není kritický
}