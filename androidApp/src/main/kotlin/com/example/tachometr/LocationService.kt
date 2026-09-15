package com.example.tachometr

import android.R
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.location.Location
import android.os.Build
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs

class LocationService : Service() {
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private lateinit var locationDao: LocationDao

    private var currentSessionId: Long = 0L
    private var watchdogJob: Job? = null
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val gpsProcessor = GpsProcessor()

    override fun onCreate() {
        super.onCreate()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        val db = getDatabase(DatabaseBuilder(this))
        locationDao = db.locationDao()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { location ->
                    processLocation(location)
                }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            "ACTION_START" -> {
                currentSessionId = intent.getLongExtra("EXTRA_SESSION_ID", System.currentTimeMillis())
                startForegroundService()
                startLocationUpdates()
            }
            "ACTION_STOP" -> {
                val finalPoint = gpsProcessor.createZeroPoint(System.currentTimeMillis())
                if (finalPoint != null) {
                    insertPointToDb(finalPoint)
                }
                watchdogJob?.cancel()
                stopLocationUpdates()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
        return START_STICKY
    }

    private fun startForegroundService() {
        val channelId = "tachometr_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "GPS Záznam trasy",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Měření trasy aktivní")
            .setContentText("Probíhá záznam GPS na pozadí")
            .setSmallIcon(R.drawable.ic_menu_mylocation)
            .setOngoing(true)
            .build()

        startForeground(1, notification)
    }

    private fun startLocationUpdates() {
        // Požádáme o lokaci ideálně každých 500 ms (minimum 250 ms)
        // Běžné telefony umí jen 1 Hz (1000 ms), ale moderní telefony (nebo dual-frequency GPS)
        // umí 2 Hz a více, což zajistí bleskovou aktualizaci
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 500)
            .setMinUpdateIntervalMillis(250)
            .setMinUpdateDistanceMeters(0f)
            .build()

        try {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )
        } catch (e: SecurityException) {
            // Ošetření oprávnění
        }
    }

    private fun processLocation(location: Location) {
        resetWatchdog()

        val processedPoint = gpsProcessor.process(
            lat = location.latitude,
            lon = location.longitude,
            timeMs = location.time,
            accuracy = if (location.hasAccuracy()) location.accuracy else 999f,
            hasHwSpeed = location.hasSpeed(),
            hwSpeedMps = if (location.hasSpeed()) location.speed else 0f
        )

        if (processedPoint != null) {
            insertPointToDb(processedPoint)
        }
    }

    private fun resetWatchdog() {
        watchdogJob?.cancel()
        watchdogJob = serviceScope.launch {
            // Pokud po dobu 3 sekund nepřijde od GPS žádná zpráva (stání v tunelu / vypnutí emulátoru)
            delay(3000L)
            if (gpsProcessor.isMoving || gpsProcessor.lastSpeedKmh > 0f) {
                val pt = gpsProcessor.createZeroPoint(System.currentTimeMillis())
                if (pt != null) {
                    insertPointToDb(pt)
                }
            }
        }
    }

    private fun insertPointToDb(pt: ProcessedPoint) {
        val point = LocationPoint(
            sessionId = currentSessionId,
            timestamp = pt.timeMs,
            latitude = pt.lat,
            longitude = pt.lon,
            speedKmh = pt.speedKmh,
            distanceSinceLast = pt.distanceM
        )

        serviceScope.launch {
            locationDao.insertPoint(point)
        }
    }

    private fun stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    override fun onBind(intent: Intent?): IBinder? = null
}