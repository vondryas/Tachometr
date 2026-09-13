package com.example.tachometr

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun TachometerScreen(viewModel: TachometerViewModel) {
    val currentSpeed by viewModel.currentSpeed.collectAsState()
    val maxSpeed by viewModel.maxSpeed.collectAsState()
    val distance by viewModel.distance.collectAsState()
    val isTracking by viewModel.isTracking.collectAsState()

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Aktuální rychlost
        Text(
            text = String.format("%.1f", currentSpeed),
            fontSize = 96.sp,
            fontWeight = FontWeight.Bold
        )
        Text(text = "km/h", fontSize = 24.sp, color = MaterialTheme.colorScheme.secondary)

        Spacer(modifier = Modifier.height(32.dp))

        // Statistiky
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = "MAX", color = MaterialTheme.colorScheme.secondary)
                Text(text = String.format("%.1f km/h", maxSpeed), fontSize = 24.sp)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = "TRASA", color = MaterialTheme.colorScheme.secondary)
                Text(text = FormatUtils.formatDistance(distance), fontSize = 24.sp)
            }
        }

        Spacer(modifier = Modifier.height(48.dp))

        // Ovládací tlačítko
        Button(
            onClick = { viewModel.toggleTracking() },
            modifier = Modifier.size(width = 200.dp, height = 64.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isTracking) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
            )
        ) {
            Text(
                text = if (isTracking) "UKONČIT MĚŘENÍ" else "START",
                fontSize = 20.sp
            )
        }
    }
}
