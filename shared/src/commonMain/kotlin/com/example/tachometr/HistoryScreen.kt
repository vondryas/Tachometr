package com.example.tachometr

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    viewModel: HistoryViewModel,
    onPathClick: (Long) -> Unit,
    onNavigateBack: () -> Unit
) {
    val sessions by viewModel.sessions.collectAsState()
    val dbSize = viewModel.getDatabaseSize()

    var sessionToRename by remember { mutableStateOf<PathSession?>(null) }
    var newNameText by remember { mutableStateOf("") }

    if (sessionToRename != null) {
        AlertDialog(
            onDismissRequest = { sessionToRename = null },
            title = { Text("Přejmenovat trasu") },
            text = {
                OutlinedTextField(
                    value = newNameText,
                    onValueChange = { newNameText = it },
                    label = { Text("Název trasy") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val session = sessionToRename
                        if (session != null && newNameText.isNotBlank()) {
                            viewModel.renameSession(session.id, newNameText)
                        }
                        sessionToRename = null
                    }
                ) {
                    Text("Uložit")
                }
            },
            dismissButton = {
                TextButton(onClick = { sessionToRename = null }) {
                    Text("Zrušit")
                }
            }
        )
    }

    Scaffold(
        modifier = Modifier.systemBarsPadding(),
        topBar = {
            TopAppBar(
                title = { Text("Historie tras") },
                navigationIcon = {
                    Button(
                        onClick = onNavigateBack,
                        modifier = Modifier.padding(start = 8.dp)
                    ) {
                        Text("Zpět")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            Text(
                text = "Celkové zabrané místo databází: $dbSize",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(sessions) { sessionData ->
                    SessionItem(
                        sessionData = sessionData,
                        onClick = { onPathClick(sessionData.session.id) },
                        onRenameClick = {
                            sessionToRename = sessionData.session
                            newNameText = sessionData.session.name
                        },
                        onDeleteClick = { viewModel.deleteSession(sessionData.session.id) }
                    )
                }
            }
        }
    }
}

@Composable
fun SessionItem(
    sessionData: SessionWithUiData,
    onClick: () -> Unit,
    onRenameClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = sessionData.session.name,
                    style = MaterialTheme.typography.titleLarge
                )
                Text(
                    text = "Start: ${sessionData.formattedDate}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Vzdálenost: ${sessionData.formattedDistance} | Čas: ${sessionData.formattedDuration} | Max: ${sessionData.session.maxSpeed.toInt()} km/h",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "Velikost záznamu: ${sessionData.formattedSize}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
            
            Row {
                IconButton(onClick = onRenameClick) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Přejmenovat",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                IconButton(onClick = onDeleteClick) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Smazat",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}