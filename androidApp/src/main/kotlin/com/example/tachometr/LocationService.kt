package com.example.tachometr

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
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class LocationService : Service() {
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private lateinit var locationDao: LocationDao

    private var lastLocation: Location? = null
    private var currentSessionId: Long = 0L
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // Inicializace sdílené KMP databáze pro Android
        val db = getDatabase(DatabaseBuilder(this))
        locationDao = db.locationDao()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { location ->
                    saveLocationToDatabase(location)
                    lastLocation = location
                }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            "ACTION_START" -> {
                currentSessionId = intent.getLongExtra("EXTRA_SESSION_ID", System.currentTimeMillis())
                lastLocation = null // Reset last location for new session
                startForegroundService()
                startLocationUpdates()
            }
            "ACTION_STOP" -> {
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
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Měření trasy aktivní")
            .setContentText("Probíhá záznam GPS na pozadí")
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setOngoing(true)
            .build()

        startForeground(1, notification)
    }

    private fun startLocationUpdates() {
        // Nastavení požadavku na maximální přesnost každou 1 vteřinu
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1000)
            .setMinUpdateIntervalMillis(1000)
            .build()

        try {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )
        } catch (e: SecurityException) {
            // Zde by mělo být ošetření, pokud uživatel nedal oprávnění k poloze
        }
    }

    private fun saveLocationToDatabase(location: Location) {
        // Výpočet vzdálenosti od předchozího bodu v metrech
        val distance = if (lastLocation != null) location.distanceTo(lastLocation!!) else 0f

        // Přepočet rychlosti z m/s na km/h (pokud senzor rychlost nevrátí, uložíme 0)
        val speedKmh = if (location.hasSpeed()) location.speed * 3.6f else 0f

        val point = LocationPoint(
            sessionId = currentSessionId,
            timestamp = System.currentTimeMillis(),
            latitude = location.latitude,
            longitude = location.longitude,
            speedKmh = speedKmh,
            distanceSinceLast = distance
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