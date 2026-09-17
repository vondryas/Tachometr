package com.example.tachometr

import androidx.compose.runtime.Composable

@Composable
expect fun SharedMap(
    points: List<LocationPoint>,
    timeRange: ClosedFloatingPointRange<Float>?,
    tripMaxSpeed: Float,
    activeMaxPoint: LocationPoint?,
    clickedPoint: LocationPoint?,
    onMapClick: (lat: Double, lon: Double) -> Unit
)
