package com.example.tachometr

expect fun formatDateTime(timestamp: Long): String
expect fun formatFloat(value: Float, decimals: Int): String
expect fun getDatabaseSizeStr(): String