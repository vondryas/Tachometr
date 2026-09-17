package com.example.tachometr

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.CameraPositionState
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberMarkerState

@Composable
actual fun SharedMap(
    points: List<LocationPoint>,
    timeRange: ClosedFloatingPointRange<Float>?,
    tripMaxSpeed: Float,
    activeMaxPoint: LocationPoint?,
    clickedPoint: LocationPoint?,
    onMapClick: (lat: Double, lon: Double) -> Unit
) {
    if (points.isEmpty()) return

    val firstPt = LatLng(points.first().latitude, points.first().longitude)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(firstPt, 15f)
    }

    val mapSegments = mutableListOf<ColoredSegment>()
    val firstTime = points.first().timestamp
    
    var currentColor: Color? = null
    var currentIsSelected: Boolean? = null
    var currentSegment = mutableListOf<LatLng>()
    
    for (i in 0 until points.size - 1) {
        val p1 = points[i]
        val p2 = points[i + 1]
        val relTime = (p1.timestamp - firstTime).toFloat() / 1000f
        val isSelected = timeRange?.contains(relTime) ?: true
        val color = if (isSelected) FormatUtils.getSpeedColor(p2.speedKmh, tripMaxSpeed) else Color.Gray.copy(alpha = 0.4f)
        
        if (color != currentColor || isSelected != currentIsSelected) {
            if (currentSegment.isNotEmpty()) {
                mapSegments.add(ColoredSegment(currentColor!!, currentSegment, currentIsSelected!!, if (currentIsSelected) 10f else 5f))
            }
            currentSegment = mutableListOf(LatLng(p1.latitude, p1.longitude), LatLng(p2.latitude, p2.longitude))
            currentColor = color
            currentIsSelected = isSelected
        } else {
            currentSegment.add(LatLng(p2.latitude, p2.longitude))
        }
    }
    if (currentSegment.isNotEmpty() && currentColor != null) {
        mapSegments.add(ColoredSegment(currentColor, currentSegment, currentIsSelected!!, if (currentIsSelected) 10f else 5f))
    }

    GoogleMap(
        modifier = Modifier.fillMaxSize(),
        cameraPositionState = cameraPositionState,
        onMapClick = { latLng ->
            onMapClick(latLng.latitude, latLng.longitude)
        }
    ) {
        mapSegments.forEach { segment ->
            Polyline(
                points = segment.points,
                color = segment.color,
                width = segment.width
            )
        }

        // Bod s maximální rychlostí
        if (activeMaxPoint != null) {
            key("max_${activeMaxPoint.timestamp}") {
                val markerState = rememberMarkerState(position = LatLng(activeMaxPoint.latitude, activeMaxPoint.longitude))
                LaunchedEffect(Unit) {
                    markerState.showInfoWindow()
                }
                Marker(
                    state = markerState,
                    title = "MAX: ${activeMaxPoint.speedKmh.toInt()} km/h",
                    snippet = "Nejvyšší rychlost"
                )
            }
        }
        
        // Zobrazení rychlosti po kliknutí na trasu
        if (clickedPoint != null) {
            key("clicked_${clickedPoint.timestamp}") {
                val pt = clickedPoint
                val markerState = rememberMarkerState(position = LatLng(pt.latitude, pt.longitude))
                LaunchedEffect(Unit) {
                    markerState.showInfoWindow()
                }
                Marker(
                    state = markerState,
                    title = "Rychlost: ${pt.speedKmh.toInt()} km/h",
                    snippet = "Vzdálenost: ${FormatUtils.formatDistance(pt.distanceSinceLast)}"
                )
            }
        }
    }
}

data class ColoredSegment(
    val color: Color,
    val points: List<LatLng>,
    val isSelected: Boolean,
    val width: Float
)