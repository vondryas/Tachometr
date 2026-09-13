package com.example.tachometr

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform