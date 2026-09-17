package com.example.tachometr

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. Inicializace databáze a trackeru
        val database = getDatabase(DatabaseBuilder(applicationContext))
        val locationDao = database.locationDao()
        val sessionDao = database.sessionDao()
        val locationTracker = AndroidLocationTracker(applicationContext)

        setContent {
            val darkTheme = isSystemInDarkTheme()
            val dynamicColor = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
            val colorScheme = when {
                dynamicColor && darkTheme -> dynamicDarkColorScheme(applicationContext)
                dynamicColor && !darkTheme -> dynamicLightColorScheme(applicationContext)
                darkTheme -> darkColorScheme()
                else -> lightColorScheme()
            }

            MaterialTheme(colorScheme = colorScheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // 3. Vyžádání runtime oprávnění
                    val permissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
                        contract = ActivityResultContracts.RequestMultiplePermissions()
                    ) { permissions ->
                        // Zde se v budoucnu dá řešit, co dělat, když uživatel oprávnění zamítne
                    }

                    LaunchedEffect(Unit) {
                        val permissionsToRequest = mutableListOf(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        )
                        // Android 13 a vyšší vyžaduje explicitní povolení pro notifikace služby na pozadí
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
                        }
                        permissionLauncher.launch(permissionsToRequest.toTypedArray())
                    }

                    // Nastavíme context pro KMP utils
                    applicationContextForPlatformUtils = applicationContext

                    // 4. Hlavní navigace aplikace
                    AppNavigation(
                        locationDao = locationDao,
                        sessionDao = sessionDao,
                        locationTracker = locationTracker
                    )
                }
            }
        }
    }
}