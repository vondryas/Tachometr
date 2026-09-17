package com.example.tachometr

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

@Composable
actual fun SharedMap(
    points: List<LocationPoint>,
    timeRange: ClosedFloatingPointRange<Float>?,
    tripMaxSpeed: Float,
    activeMaxPoint: LocationPoint?,
    clickedPoint: LocationPoint?,
    onMapClick: (lat: Double, lon: Double) -> Unit
) {
    // Prozatím placeholder pro iOS.
    // Abychom zachovali kompilaci a iOS simulátor jel, vložíme zatím jen text.
    // Zde později využijeme UIKitView a MKMapView/GMSMapView
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("Mapa pro iOS bude k dispozici zde.")
    }
}