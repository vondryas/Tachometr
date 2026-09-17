package com.example.tachometr

import kotlin.test.*

class GpsProcessorTest {

    private lateinit var processor: GpsProcessor

    @BeforeTest
    fun setUp() {
        processor = GpsProcessor()
    }

    @Test
    fun `test initial point sets anchor`() {
        val pt = processor.process(50.0, 14.0, 1000L, 10f, false, 0f)
        assertNotNull(pt)
        assertEquals(0f, pt!!.speedKmh)
        assertEquals(0f, pt.distanceM)
        assertFalse(processor.isMoving)
    }

    @Test
    fun `test walking ignores small jumps in anchor bubble`() {
        processor.process(50.0, 14.0, 1000L, 10f, false, 0f) // Anchor
        
        // Pohneme se jen o malinký kousek (např. drift signálu 1 metr)
        val latDrift = 50.0 + 0.000009 // cca 1 metr
        val pt = processor.process(latDrift, 14.0, 2000L, 10f, false, 0f)
        
        assertNotNull(pt)
        assertEquals(0f, pt!!.speedKmh)
        assertEquals(0f, pt.distanceM)
        assertFalse(processor.isMoving)
    }

    @Test
    fun `test walking starts when leaving anchor bubble`() {
        processor.process(50.0, 14.0, 1000L, 10f, false, 0f)
        
        // Pohneme se zjevně mimo bublinu (např. 10 metrů za 3 vteřiny -> 3.3 m/s = 12 km/h)
        val latMove = 50.0 + 0.00009 // cca 10 metrů
        val pt = processor.process(latMove, 14.0, 4000L, 5f, true, 3.3f)
        
        assertNotNull(pt)
        assertTrue(pt!!.speedKmh > 0f)
        assertTrue(processor.isMoving)
    }

    @Test
    fun `test fast driving correctly logs speed`() {
        processor.process(50.0, 14.0, 1000L, 5f, false, 0f)
        
        // Rozjezd (10 metrů)
        val latMove1 = 50.0 + 0.00009 
        processor.process(latMove1, 14.0, 2000L, 5f, true, 5f)
        
        // Jízda rychle (např. 130 km/h = 36 m/s)
        val distM = 36f
        val latMove2 = latMove1 + (distM / 111320.0) // přibližný převod na stupně
        processor.process(latMove2, 14.0, 3000L, 5f, true, 36f)
        
        // Druhý bod stejnou rychlostí (EMA dosáhne na vyšší hodnotu)
        val latMove3 = latMove2 + (distM / 111320.0)
        val pt = processor.process(latMove3, 14.0, 4000L, 5f, true, 36f)
        
        assertNotNull(pt)
        assertTrue(processor.isMoving)
        assertTrue(pt!!.distanceM > 30f)
        assertTrue("Speed should be high, was ${pt.speedKmh}", pt.speedKmh > 100f)
    }

    @Test
    fun `test braking quickly drops speed`() {
        processor.process(50.0, 14.0, 1000L, 5f, false, 0f)
        
        // Rozjezd
        var currentLat = 50.0 + 0.00009
        processor.process(currentLat, 14.0, 2000L, 5f, true, 5f)
        
        // Jízda rychle 130 km/h
        currentLat += (36f / 111320.0)
        processor.process(currentLat, 14.0, 3000L, 5f, true, 36f)
        
        // Prudké brzdění (10 km/h = 2.7 m/s)
        currentLat += (2.7f / 111320.0)
        val pt = processor.process(currentLat, 14.0, 4000L, 5f, true, 2.7f)
        
        assertNotNull(pt)
        // Očekáváme prudký pokles rychlosti
        assertTrue("Speed should drop rapidly, was ${pt!!.speedKmh}", pt.speedKmh < 50f)
    }

    @Test
    fun `test stop after driving sets zero velocity`() {
        processor.process(50.0, 14.0, 1000L, 5f, false, 0f)
        
        // Rozjezd
        var currentLat = 50.0 + 0.00009
        processor.process(currentLat, 14.0, 2000L, 5f, true, 5f)
        
        // Jízda 50 km/h (13.8 m/s)
        currentLat += (13.8f / 111320.0)
        processor.process(currentLat, 14.0, 3000L, 5f, true, 13.8f)
        
        // Zastavení - posun 0m, rychlost 0 m/s
        val pt = processor.process(currentLat, 14.0, 4000L, 5f, true, 0f)
        
        assertNotNull(pt)
        assertFalse(processor.isMoving)
        assertEquals(0f, pt!!.speedKmh)
        assertEquals(0f, pt.distanceM)
    }
}