package com.example.tachometr

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.ComposeUIViewController

fun MainViewController() = ComposeUIViewController { 
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("iOS implementation in progress...")
    }
}