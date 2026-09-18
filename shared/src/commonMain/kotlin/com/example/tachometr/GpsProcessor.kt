package com.example.tachometr

import kotlin.math.*

/**
 * Zpracovává surová data z GPS a filtruje šum.
 * Funguje čistě s primitivy, aby se dal snadno testovat bez Android frameworku.
 */
class GpsProcessor {

    var isMoving: Boolean = false
        private set

    var lastSpeedKmh: Float = 0f
        private set

    var anchorLat: Double? = null
        private set
    var anchorLon: Double? = null
        private set
    var lastValidTimeMs: Long = 0
        private set
        
    private var lastLat: Double? = null
    private var lastLon: Double? = null

    // Konfigurace
    private val MIN_ACCURACY_M = 20f
    private val STATIONARY_SPEED_THRESHOLD_KMH = 1.0f
    private val STATIONARY_RADIUS_M = 3.5f
    private val MOVING_SPEED_THRESHOLD_KMH = 2.0f
    private val TELEPORT_SPEED_KMH = 300f // Letadla/Vlaky mohou jet rychleji než 150 km/h
    private val TELEPORT_DISTANCE_M = 50f
    private val EMA_ALPHA = 0.85f // Vyhlazování rychlosti - zvýšeno z 0.6 na 0.85 pro rychlejší odezvu!

    /**
     * Zpracuje nový bod.
     * Vrátí zpracovaný výsledek nebo null, pokud byl bod zahozen (např. kvůli špatné přesnosti).
     */
    fun process(
        lat: Double,
        lon: Double,
        timeMs: Long,
        accuracy: Float,
        hasHwSpeed: Boolean,
        hwSpeedMps: Float
    ): ProcessedPoint? {
        // 1. Zahození příliš nepřesných bodů
        if (accuracy > MIN_ACCURACY_M) {
            return null
        }

        // Inicializace prvního bodu
        if (anchorLat == null || lastLat == null) {
            anchorLat = lat
            anchorLon = lon
            lastLat = lat
            lastLon = lon
            lastValidTimeMs = timeMs
            isMoving = false
            lastSpeedKmh = 0f
            return ProcessedPoint(lat, lon, timeMs, 0f, 0f)
        }

        val timeDeltaSec = ((timeMs - lastValidTimeMs) / 1000f).coerceAtLeast(0.1f)
        val rawDistance = haversine(lastLat!!, lastLon!!, lat, lon)

        val hwSpeedKmh = if (hasHwSpeed) hwSpeedMps * 3.6f else 0f
        val calcSpeedKmh = (rawDistance / timeDeltaSec) * 3.6f

        val currentSpeedKmh = if (hasHwSpeed) hwSpeedKmh else calcSpeedKmh

        // 2. Teleportace - ignorovat obrovské skoky nereálnou rychlostí
        if (currentSpeedKmh > TELEPORT_SPEED_KMH && rawDistance > TELEPORT_DISTANCE_M) {
            // Ignorujeme posun, jen aktualizujeme pozici pro další výpočty (pokud to nebyl jen glitch, ustálí se to)
            lastLat = lat
            lastLon = lon
            lastValidTimeMs = timeMs
            return null
        }

        // 3. Detekce Zastavení (Auto-Pause)
        val distFromAnchor = haversine(anchorLat!!, anchorLon!!, lat, lon)
        
        val isZeroVelocity = if (hasHwSpeed) {
            hwSpeedKmh < STATIONARY_SPEED_THRESHOLD_KMH
        } else {
            calcSpeedKmh < 2.5f && distFromAnchor < STATIONARY_RADIUS_M
        }

        if (isZeroVelocity) {
            if (isMoving) {
                isMoving = false
                anchorLat = lat
                anchorLon = lon
            }
            
            // Stojíme
            lastSpeedKmh = 0f
            lastValidTimeMs = timeMs
            // Neaktualizujeme lastLat/lastLon, abychom odstínili šum kolem kotvy
            return ProcessedPoint(lat, lon, timeMs, 0f, 0f)
        }

        // 4. Rozjezd z místa
        if (!isMoving) {
            val requiredDistance = if (hasHwSpeed) 1.5f else 4.0f
            
            if (distFromAnchor > requiredDistance || (hasHwSpeed && hwSpeedKmh > MOVING_SPEED_THRESHOLD_KMH)) {
                isMoving = true
                // Záměrně ustrihneme první vzdálenost z bubliny (nepočítejme posun 0)
                lastLat = lat
                lastLon = lon
                lastValidTimeMs = timeMs
                
                // OPRAVA: Zabráníme vystřelení rychlosti při opuštění bubliny.
                // calcSpeedKmh je tady nepřesná, protože čas (timeDeltaSec) mohl být od posledního updatu
                // uvnitř bubliny (např. 0.5s), ale překonaná vzdálenost se počítá od kotvy (např. 4.1m).
                // To vygenerovalo nesmyslný spike (např. 4.1m / 0.5s = 29 km/h).
                // Místo coerceAtLeast se spolehneme na HW rychlost. Pokud není, omezíme počáteční rychlost na max 10 km/h.
                val startSpeed = if (hasHwSpeed) hwSpeedKmh else calcSpeedKmh.coerceAtMost(10f)
                
                lastSpeedKmh = startSpeed
                return ProcessedPoint(lat, lon, timeMs, lastSpeedKmh, 0f)
            } else {
                // Jsme pořád v bublině kotvy, bereme jako šum
                lastValidTimeMs = timeMs
                return ProcessedPoint(lat, lon, timeMs, 0f, 0f)
            }
        }

        // 5. Plynulý pohyb
        var smoothedSpeed = (currentSpeedKmh * EMA_ALPHA) + (lastSpeedKmh * (1 - EMA_ALPHA))
        
        // Prudké brzdění - nechceme tahat starou rychlost
        if (currentSpeedKmh < 5f && lastSpeedKmh > 10f) {
            smoothedSpeed = currentSpeedKmh
        }

        lastLat = lat
        lastLon = lon
        anchorLat = lat
        anchorLon = lon
        lastValidTimeMs = timeMs
        lastSpeedKmh = smoothedSpeed

        return ProcessedPoint(lat, lon, timeMs, smoothedSpeed, rawDistance)
    }

    /**
     * Vrátí nulový bod (při zastavení, ztrátě signálu - watchdog).
     */
    fun createZeroPoint(timeMs: Long): ProcessedPoint? {
        if (lastLat == null) return null
        isMoving = false
        lastSpeedKmh = 0f
        lastValidTimeMs = timeMs
        return ProcessedPoint(lastLat!!, lastLon!!, timeMs, 0f, 0f)
    }

    /**
     * Výpočet vzdálenosti ve volném prostoru (Haversine formule)
     */
    private fun haversine(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Float {
        val r = 6371000.0 // Země v metrech
        val phi1 = lat1 * PI / 180.0
        val phi2 = lat2 * PI / 180.0
        val deltaPhi = (lat2 - lat1) * PI / 180.0
        val deltaLambda = (lon2 - lon1) * PI / 180.0

        val a = sin(deltaPhi / 2) * sin(deltaPhi / 2) +
                cos(phi1) * cos(phi2) *
                sin(deltaLambda / 2) * sin(deltaLambda / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))

        return (r * c).toFloat()
    }
}

data class ProcessedPoint(
    val lat: Double,
    val lon: Double,
    val timeMs: Long,
    val speedKmh: Float,
    val distanceM: Float
)
