package com.sta.datacollector.ui.sessions

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sta.datacollector.data.local.TrainingSessionEntity
import com.sta.datacollector.data.local.TrainingSessionStatus

@Composable
fun SessionHistoryScreen(
    viewModel: SessionViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    BackHandler(onBack = onBack)

    Scaffold(
        modifier = modifier,
        topBar = { SessionTopBar("Historial de grabaciones", onBack) },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Grabaciones de sensores", style = MaterialTheme.typography.titleMedium)
                        Text("Todavía no existen grabaciones de sensores. Esta pantalla muestra únicamente sesiones y series guardadas.")
                    }
                }
            }
            if (uiState.sessions.isEmpty()) {
                item { Text("Todavía no hay sesiones registradas.") }
            } else {
                items(uiState.sessions, key = TrainingSessionEntity::id) { session ->
                    val sets = uiState.allCompletedSets.filter { it.sessionId == session.id }
                    Card(Modifier.fillMaxWidth()) {
                        Column(
                            Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text(formatDateTime(session.startedAt), style = MaterialTheme.typography.titleMedium)
                            Text(sessionStatus(session))
                            session.endedAt?.let { Text("Cierre: ${formatDateTime(it)}") }
                            if (sets.isEmpty()) {
                                Text("Sin series finalizadas.")
                            } else {
                                sets.forEach { set ->
                                    SetCard(set, participantName(set.participantId, uiState.allParticipants))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun sessionStatus(session: TrainingSessionEntity): String = when (session.status) {
    TrainingSessionStatus.ACTIVE -> "Estado: en curso"
    TrainingSessionStatus.COMPLETED -> "Estado: finalizada"
    TrainingSessionStatus.CANCELLED -> "Estado: cancelada"
    else -> "Estado: ${session.status}"
}
