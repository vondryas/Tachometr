package com.example.tachometr

import android.location.Location
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PathDetailScreen(
    viewModel: PathDetailViewModel,
    onNavigateBack: () -> Unit
) {
    val session by viewModel.session.collectAsState()
    val points by viewModel.points.collectAsState()
    val timeRange by viewModel.selectedTimeRange.collectAsState()
    val selDist by viewModel.selectedDistance.collectAsState()
    val selMax by viewModel.selectedMaxSpeed.collectAsState()
    val selAvg by viewModel.selectedAvgSpeed.collectAsState()
    val selDur by viewModel.selectedDuration.collectAsState()
    val selStart by viewModel.selectedStartTimeStr.collectAsState()
    val selEnd by viewModel.selectedEndTimeStr.collectAsState()

    val totalDurationMs = remember(session, points) {
        val start = session?.startTime ?: points.firstOrNull()?.timestamp ?: 0L
        val end = session?.endTime ?: points.lastOrNull()?.timestamp ?: start
        (end - start).coerceAtLeast(0L)
    }

    val tripMaxSpeed = remember(session, points) {
        val sMax = session?.maxSpeed ?: 0f
        val pMax = points.maxOfOrNull { it.speedKmh } ?: 0f
        maxOf(sMax, pMax).coerceAtLeast(10f)
    }

    // Bod s maximální rychlostí (reaguje na výběr úseku)
    val activeMaxPoint = remember(points, timeRange) {
        val range = timeRange
        if (points.isEmpty()) null
        else if (range != null) {
            val firstTime = points.first().timestamp
            points.filter { pt ->
                val relSec = (pt.timestamp - firstTime).toFloat() / 1000f
                relSec in range
            }.maxByOrNull { it.speedKmh }
        } else {
            points.maxByOrNull { it.speedKmh }
        }
    }

    var isRenaming by remember { mutableStateOf(false) }
    var renameText by remember { mutableStateOf("") }

    if (isRenaming) {
        AlertDialog(
            onDismissRequest = { isRenaming = false },
            title = { Text("Přejmenovat trasu") },
            text = {
                OutlinedTextField(
                    value = renameText,
                    onValueChange = { renameText = it },
                    label = { Text("Název trasy") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (renameText.isNotBlank()) {
                            viewModel.renameSession(renameText)
                        }
                        isRenaming = false
                    }
                ) {
                    Text("Uložit")
                }
            },
            dismissButton = {
                TextButton(onClick = { isRenaming = false }) {
                    Text("Zrušit")
                }
            }
        )
    }

    val formattedStartDate = remember(session) {
        val start = session?.startTime ?: 0L
        if (start > 0L) {
            val sdf = SimpleDateFormat("d. M. yyyy HH:mm", Locale.getDefault())
            sdf.format(Date(start))
        } else ""
    }

    Scaffold(
        modifier = Modifier.systemBarsPadding(),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(session?.name ?: "Detail cesty")
                        if (formattedStartDate.isNotEmpty()) {
                            Text(
                                text = "Start: $formattedStartDate",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Zpět")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        renameText = session?.name ?: ""
                        isRenaming = true
                    }) {
                        Icon(Icons.Default.Edit, contentDescription = "Přejmenovat")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // MAPA (horní část) s plovoucí legendou a značkou max rychlosti
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1.1f)
            ) {
                if (points.isNotEmpty()) {
                    var clickedPoint by remember { mutableStateOf<LocationPoint?>(null) }
                    
                    SharedMap(
                        points = points,
                        timeRange = timeRange,
                        tripMaxSpeed = tripMaxSpeed,
                        activeMaxPoint = activeMaxPoint,
                        clickedPoint = clickedPoint,
                        onMapClick = { lat, lon ->
                            if (points.isNotEmpty()) {
                                // Najde nejbližší bod k místu kliknutí (zjednodušená vzdálenost)
                                val closest = points.minByOrNull {
                                    val dLat = it.latitude - lat
                                    val dLon = it.longitude - lon
                                    dLat * dLat + dLon * dLon
                                }
                                if (closest != null) {
                                    // Zjednodušený výpočet v metrech
                                    val earthRadius = 6371000.0
                                    val dLat = (lat - closest.latitude) * PI / 180.0
                                    val dLon = (lon - closest.longitude) * PI / 180.0
                                    val a = sin(dLat / 2) * sin(dLat / 2) +
                                            cos(closest.latitude * PI / 180.0) * cos(lat * PI / 180.0) *
                                            sin(dLon / 2) * sin(dLon / 2)
                                    val c = 2 * atan2(sqrt(a), sqrt(1 - a))
                                    val distance = earthRadius * c
                                    
                                    if (distance < 60f) {
                                        clickedPoint = closest
                                    } else {
                                        clickedPoint = null
                                    }
                                }
                            }
                        }
                    )

                    // Plovoucí legenda pro barvy rychlosti (25 odstínů)
                    Card(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(12.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Barevné odlišení (25 odstínů)",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Box(
                                modifier = Modifier
                                    .width(160.dp)
                                    .height(12.dp)
                                    .background(
                                        brush = Brush.horizontalGradient(
                                            listOf(
                                                Color(0xFF00B0FF), // Modrá
                                                Color(0xFF4CAF50), // Zelená
                                                Color(0xFFFFEB3B), // Žlutá
                                                Color(0xFFFF9800), // Oranžová
                                                Color(0xFFF44336)  // Červená
                                            )
                                        ),
                                        shape = RoundedCornerShape(6.dp)
                                    )
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Row(
                                modifier = Modifier.width(160.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("0", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("${(tripMaxSpeed * 0.25).toInt()}", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("${(tripMaxSpeed * 0.5).toInt()}", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("${(tripMaxSpeed * 0.75).toInt()}", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("${tripMaxSpeed.toInt()}", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                } else {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Načítám trasu...")
                    }
                }
            }

            // SPODNÍ ČÁST: Statistiky a Graf rychlosti
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(16.dp)
            ) {
                val displayDist = if (timeRange != null) selDist else session?.totalDistance ?: 0f
                val displayMax = if (timeRange != null) selMax else session?.maxSpeed ?: 0f
                val displayDur = if (timeRange != null) selDur else totalDurationMs
                
                // Průměrná rychlost pro celou trasu (pokud není výsek)
                val displayAvg = if (timeRange != null) {
                    selAvg
                } else {
                    if (totalDurationMs > 0) (session?.totalDistance ?: 0f) / (totalDurationMs / 1000f) * 3.6f else 0f
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = if (timeRange == null) "Statistika trasy" else "Vybraný úsek",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        if (timeRange != null && selStart.isNotEmpty()) {
                            Text(
                                text = "Časový úsek: $selStart – $selEnd",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                    if (timeRange != null) {
                        TextButton(
                            onClick = { viewModel.updateSelection(null) },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                        ) {
                            Text("Zrušit výběr", fontSize = 12.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Karty statistik v celých číslech
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("VZDÁLENOST", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                        Text(FormatUtils.formatDistance(displayDist), style = MaterialTheme.typography.bodyMedium)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("ČAS", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                        Text(FormatUtils.formatDuration(displayDur), style = MaterialTheme.typography.bodyMedium)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("MAX", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                        Text("${displayMax.toInt()} km/h", style = MaterialTheme.typography.bodyMedium)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("PRŮMĚR", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                        Text("${displayAvg.toInt()} km/h", style = MaterialTheme.typography.bodyMedium)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // GRAF RYCHLOSTI S NÁPISY ČASU NA SPODNÍ LEGENDĚ
                Text("Graf rychlosti v čase (tažením vybereš úsek):", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                Spacer(modifier = Modifier.height(4.dp))

                if (points.isNotEmpty()) {
                    var dragStartX by remember { mutableStateOf<Float?>(null) }
                    var dragEndX by remember { mutableStateOf<Float?>(null) }

                    LaunchedEffect(timeRange) {
                        if (timeRange == null) {
                            dragStartX = null
                            dragEndX = null
                        }
                    }

                    // Maximální rychlost pro Y-osu
                    val maxSpeedCanvas = remember(points) {
                        points.maxOfOrNull { it.speedKmh }?.coerceAtLeast(10f) ?: 50f
                    }

                    Row(modifier = Modifier.fillMaxWidth().weight(1f)) {
                        // LEVÁ LEGENDA Y-OSY (RYCHLOST)
                        Column(
                            modifier = Modifier.fillMaxHeight().padding(end = 4.dp),
                            verticalArrangement = Arrangement.SpaceBetween,
                            horizontalAlignment = Alignment.End
                        ) {
                            Text("${maxSpeedCanvas.toInt()} km/h", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("${(maxSpeedCanvas / 2).toInt()}", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("0", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }

                        // GRAF
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .weight(1f)
                        ) {
                            Canvas(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .pointerInput(Unit) {
                                        detectDragGestures(
                                            onDragStart = { offset ->
                                                dragStartX = offset.x
                                                dragEndX = offset.x
                                            },
                                            onDrag = { change, _ ->
                                                dragEndX = change.position.x
                                            },
                                            onDragEnd = {
                                                if (dragStartX != null && dragEndX != null) {
                                                    val minX = minOf(dragStartX!!, dragEndX!!)
                                                    val maxX = maxOf(dragStartX!!, dragEndX!!)

                                                    val canvasWidth = size.width
                                                    val firstTime = points.first().timestamp
                                                    val lastTime = points.last().timestamp
                                                    val durationSec = ((lastTime - firstTime) / 1000f).coerceAtLeast(1f)

                                                    val tStart = (minX / canvasWidth) * durationSec
                                                    val tEnd = (maxX / canvasWidth) * durationSec

                                                    viewModel.updateSelection(tStart..tEnd)
                                                }
                                            },
                                            onDragCancel = {
                                                dragStartX = null
                                                dragEndX = null
                                                viewModel.updateSelection(null)
                                            }
                                        )
                                    }
                            ) {
                                val firstTime = points.first().timestamp
                                val lastTime = points.last().timestamp
                                val durationSec = ((lastTime - firstTime) / 1000f).coerceAtLeast(1f)

                                // Mřížka (3 vodorovné linky)
                                val gridLineColor = Color.LightGray.copy(alpha = 0.4f)
                                drawLine(
                                    color = gridLineColor,
                                    start = Offset(0f, 0f),
                                    end = Offset(size.width, 0f),
                                    strokeWidth = 1f
                                )
                                drawLine(
                                    color = gridLineColor,
                                    start = Offset(0f, size.height * 0.5f),
                                    end = Offset(size.width, size.height * 0.5f),
                                    strokeWidth = 1f
                                )
                                drawLine(
                                    color = gridLineColor,
                                    start = Offset(0f, size.height),
                                    end = Offset(size.width, size.height),
                                    strokeWidth = 1f
                                )

                                // Vykreslení křivky rychlosti
                                val path = Path()
                                points.forEachIndexed { index, point ->
                                    val relTimeSec = (point.timestamp - firstTime).toFloat() / 1000f
                                    val x = (relTimeSec / durationSec) * size.width
                                    val y = size.height - ((point.speedKmh / maxSpeedCanvas) * size.height)

                                    if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
                                }

                                drawPath(
                                    path = path,
                                    color = Color(0xFF1E88E5),
                                    style = Stroke(width = 4f)
                                )

                                // Zvýraznění výběru
                                if (dragStartX != null && dragEndX != null) {
                                    val minX = minOf(dragStartX!!, dragEndX!!)
                                    val maxX = maxOf(dragStartX!!, dragEndX!!)
                                    drawRect(
                                        color = Color(0xFF1E88E5).copy(alpha = 0.25f),
                                        topLeft = Offset(x = minX, y = 0f),
                                        size = Size(width = maxX - minX, height = size.height)
                                    )
                                }
                            }
                        }
                    }

                    // Spodní legenda časové osy pod grafem
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 2.dp, start = 32.dp), // odsadíme zleva o šířku Y-osy
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("00:00", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(FormatUtils.formatDuration(totalDurationMs / 2), fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(FormatUtils.formatDuration(totalDurationMs), fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

