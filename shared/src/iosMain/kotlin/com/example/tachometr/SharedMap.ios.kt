package com.example.tachometr

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.interop.UIKitView
import cocoapods.GoogleMaps.GMSCameraPosition
import cocoapods.GoogleMaps.GMSMapView
import cocoapods.GoogleMaps.GMSMarker
import cocoapods.GoogleMaps.GMSMutablePath
import cocoapods.GoogleMaps.GMSPolyline
import kotlinx.cinterop.ExperimentalForeignApi
import platform.CoreLocation.CLLocationCoordinate2DMake
import platform.UIKit.UIColor

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun SharedMap(
    points: List<LocationPoint>,
    timeRange: ClosedFloatingPointRange<Float>?,
    tripMaxSpeed: Float,
    activeMaxPoint: LocationPoint?,
    clickedPoint: LocationPoint?,
    onMapClick: (lat: Double, lon: Double) -> Unit
) {
    val mapView = remember { GMSMapView() }
    val polyline = remember { GMSPolyline() }
    
    // Convert Compose Color (0x1E88E5) to UIColor
    val uiColor = UIColor.colorWithRed(
        red = 0.1176, // 0x1E
        green = 0.5333, // 0x88
        blue = 0.8980, // 0xE5
        alpha = 1.0
    )

    LaunchedEffect(points, timeRange) {
        if (points.isNotEmpty()) {
            val path = GMSMutablePath()
            val filteredPoints = if (timeRange != null) {
                val maxTime = points.last().timestamp
                points.filter { 
                    it.timestamp >= timeRange.start * maxTime && it.timestamp <= timeRange.endInclusive * maxTime 
                }
            } else {
                points
            }
            
            filteredPoints.forEach { point ->
                path.addCoordinate(CLLocationCoordinate2DMake(point.latitude, point.longitude))
            }
            
            polyline.path = path
            polyline.strokeWidth = 10.0
            polyline.strokeColor = uiColor
            polyline.map = mapView

            // Move camera to the last point
            val lastPoint = points.last()
            val camera = GMSCameraPosition.cameraWithLatitude(
                latitude = lastPoint.latitude,
                longitude = lastPoint.longitude,
                zoom = 15f
            )
            mapView.camera = camera
        } else {
            polyline.map = null
        }
    }

    LaunchedEffect(clickedPoint) {
        mapView.clear()
        polyline.map = mapView // re-add polyline since clear() removes everything
        
        clickedPoint?.let { pt ->
            val marker = GMSMarker().apply {
                position = CLLocationCoordinate2DMake(pt.latitude, pt.longitude)
                title = "Rychlost: ${pt.speedKmh.toInt()} km/h"
                snippet = "Vzdálenost: ${FormatUtils.formatDistance(pt.distanceSinceLast)}"
                map = mapView
            }
            mapView.selectedMarker = marker
        }
    }

    UIKitView(
        factory = { mapView },
        modifier = Modifier.fillMaxSize(),
        update = {
            // Further updates if needed
        }
    )
}