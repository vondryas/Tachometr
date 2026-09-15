package com.example.tachometr

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.round

@Composable
fun TachometerScreen(
    viewModel: TachometerViewModel,
    onNavigateToHistory: () -> Unit,
    onNavigateToDetail: (Long) -> Unit
) {
    val currentSpeed by viewModel.currentSpeed.collectAsState()
    val maxSpeed by viewModel.maxSpeed.collectAsState()
    val distance by viewModel.distance.collectAsState()
    val elapsedTime by viewModel.elapsedTime.collectAsState()
    val isTracking by viewModel.isTracking.collectAsState()
    val averageSpeed by viewModel.averageSpeed.collectAsState()
    val speedometerMaxSpeed by viewModel.speedometerMaxSpeed.collectAsState()
    val isAnalogMode by viewModel.isAnalogMode.collectAsState()

    var showMaxSpeedDialog by remember { mutableStateOf(false) }
    var customMaxSpeedText by remember { mutableStateOf("") }
    var isCustomError by remember { mutableStateOf(false) }

    if (showMaxSpeedDialog) {
        AlertDialog(
            onDismissRequest = { showMaxSpeedDialog = false },
            title = { Text("Max. rychlost tachometru") },
            text = {
                Column {
                    Text("Přednastavené rychlosti:")
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        listOf(30f, 50f, 100f).forEach { speed ->
                            OutlinedButton(onClick = {
                                viewModel.setSpeedometerMaxSpeed(speed)
                                showMaxSpeedDialog = false
                            }) {
                                Text("${speed.toInt()}")
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        listOf(160f, 220f, 300f).forEach { speed ->
                            OutlinedButton(onClick = {
                                viewModel.setSpeedometerMaxSpeed(speed)
                                showMaxSpeedDialog = false
                            }) {
                                Text("${speed.toInt()}")
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Nebo vlastní (10 - 400 km/h, krok 5):")
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = customMaxSpeedText,
                        onValueChange = { 
                            customMaxSpeedText = it
                            isCustomError = false
                        },
                        label = { Text("Vlastní maximum") },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number
                        ),
                        singleLine = true,
                        isError = isCustomError,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (customMaxSpeedText.isNotBlank()) {
                        val parsed = customMaxSpeedText.toFloatOrNull()
                        if (parsed != null && parsed in 10f..400f) {
                            // Zaokrouhlení na nejbližší násobek 5
                            val rounded = round(parsed / 5f) * 5f
                            viewModel.setSpeedometerMaxSpeed(rounded)
                            showMaxSpeedDialog = false
                        } else {
                            isCustomError = true
                        }
                    } else {
                        showMaxSpeedDialog = false
                    }
                }) {
                    Text("Uložit")
                }
            },
            dismissButton = {
                TextButton(onClick = { showMaxSpeedDialog = false }) {
                    Text("Zrušit")
                }
            }
        )
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            TextButton(onClick = { viewModel.toggleAnalogMode() }) {
                Text(if (isAnalogMode) "Zobrazit digitálně" else "Zobrazit ručičkově")
            }
            if (isAnalogMode) {
                TextButton(onClick = { showMaxSpeedDialog = true }) {
                    Text("Max: ${speedometerMaxSpeed.toInt()}")
                }
            }
        }

        Box(
            modifier = Modifier.weight(1f),
            contentAlignment = Alignment.Center
        ) {
            if (isAnalogMode) {
                AnalogSpeedometer(
                    currentSpeed = currentSpeed,
                    maxSpeed = speedometerMaxSpeed,
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "${currentSpeed.toInt()}",
                        fontSize = 120.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(text = "km/h", fontSize = 24.sp, color = MaterialTheme.colorScheme.secondary)
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Statistiky
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = "MAX", color = MaterialTheme.colorScheme.secondary)
                Text(text = "${maxSpeed.toInt()} km/h", fontSize = 20.sp)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = "PRŮMĚR", color = MaterialTheme.colorScheme.secondary)
                Text(text = "${averageSpeed.toInt()} km/h", fontSize = 20.sp)
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = "TRASA", color = MaterialTheme.colorScheme.secondary)
                Text(text = FormatUtils.formatDistance(distance), fontSize = 20.sp)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = "ČAS", color = MaterialTheme.colorScheme.secondary)
                Text(text = FormatUtils.formatDuration(elapsedTime), fontSize = 20.sp)
            }
        }

        Spacer(modifier = Modifier.height(48.dp))

        // Ovládací tlačítko
        Button(
            onClick = {
                viewModel.toggleTracking(onStopped = { sessionId ->
                    onNavigateToDetail(sessionId)
                })
            },
            modifier = Modifier.size(width = 220.dp, height = 64.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isTracking) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
            )
        ) {
            Text(
                text = if (isTracking) "UKONČIT MĚŘENÍ" else "START",
                fontSize = 20.sp
            )
        }
        // Tlačítko pro historii
        Spacer(modifier = Modifier.height(16.dp))
        TextButton(onClick = onNavigateToHistory) {
            Text("HISTORIE TRAS")
        }
    }
}
