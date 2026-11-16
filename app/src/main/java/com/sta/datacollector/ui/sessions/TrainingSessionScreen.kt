package com.sta.datacollector.ui.sessions

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sta.datacollector.data.local.ExerciseSetEntity
import com.sta.datacollector.data.local.ParticipantEntity
import java.text.DateFormat
import java.util.Date

@Composable
fun TrainingSessionScreen(
    viewModel: SessionViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    BackHandler {
        when {
            uiState.pickerMode != null -> viewModel.dismissParticipantPicker()
            uiState.showFinishSessionConfirmation -> viewModel.dismissFinishSessionConfirmation()
            uiState.recoveredActiveSet == null -> onBack()
        }
    }
    LaunchedEffect(uiState.message) {
        uiState.message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.consumeMessage()
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = { SessionTopBar("Sesión de entrenamiento", onBack) },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        if (uiState.activeSession == null) {
            EmptySession(
                onCreate = viewModel::openCreateSession,
                modifier = Modifier.fillMaxSize().padding(padding),
            )
        } else {
            ActiveSessionContent(
                uiState = uiState,
                viewModel = viewModel,
                modifier = Modifier.fillMaxSize().padding(padding),
            )
        }
    }

    uiState.pickerMode?.let { mode ->
        val existingIds = uiState.sessionParticipants.mapTo(mutableSetOf(), ParticipantEntity::id)
        val candidates = if (mode == ParticipantPickerMode.CREATE_SESSION) {
            uiState.activeParticipants
        } else {
            uiState.activeParticipants.filterNot { it.id in existingIds }
        }
        ParticipantPickerDialog(
            mode = mode,
            candidates = candidates,
            selectedIds = uiState.pickerSelectedParticipantIds,
            isBusy = uiState.isBusy,
            onToggle = viewModel::togglePickerParticipant,
            onConfirm = viewModel::confirmParticipantPicker,
            onDismiss = viewModel::dismissParticipantPicker,
        )
    }

    uiState.recoveredActiveSet?.let { recovered ->
        AlertDialog(
            onDismissRequest = {},
            title = { Text("Serie incompleta recuperada") },
            text = {
                Text("Se encontró una serie de ${participantName(recovered.participantId, uiState.allParticipants)} para ${recovered.exercise}. ¿Deseas continuarla o descartarla?")
            },
            confirmButton = {
                Button(onClick = viewModel::continueRecoveredSet) { Text("Continuar") }
            },
            dismissButton = {
                TextButton(onClick = viewModel::discardRecoveredSet) { Text("Descartar") }
            },
        )
    }

    if (uiState.showFinishSessionConfirmation) {
        AlertDialog(
            onDismissRequest = viewModel::dismissFinishSessionConfirmation,
            title = { Text("Finalizar sesión") },
            text = { Text("La sesión quedará cerrada y no admitirá nuevas series.") },
            confirmButton = {
                Button(onClick = viewModel::confirmFinishSession) { Text("Finalizar") }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissFinishSessionConfirmation) {
                    Text("Cancelar")
                }
            },
        )
    }
}

@Composable
private fun ActiveSessionContent(
    uiState: SessionUiState,
    viewModel: SessionViewModel,
    modifier: Modifier,
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Text(
                "En curso desde ${formatDateTime(uiState.activeSession!!.startedAt)}",
                style = MaterialTheme.typography.titleMedium,
            )
        }
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Participantes", style = MaterialTheme.typography.titleMedium)
                TextButton(onClick = viewModel::openAddParticipants, enabled = !uiState.isBusy) {
                    Text("Añadir")
                }
            }
        }
        items(uiState.sessionParticipants, key = ParticipantEntity::id) { participant ->
            val selected = participant.id == uiState.selectedParticipantId
            OutlinedButton(
                onClick = { viewModel.selectParticipant(participant.id) },
                enabled = uiState.activeSet == null && !uiState.isBusy,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (selected) "${participant.nickname} · Seleccionado" else participant.nickname)
            }
        }
        item {
            HorizontalDivider()
            Column(
                modifier = Modifier.padding(top = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text("Registrar serie", style = MaterialTheme.typography.titleMedium)
                OutlinedTextField(
                    value = uiState.exercise,
                    onValueChange = viewModel::updateExercise,
                    label = { Text("Ejercicio") },
                    isError = uiState.exerciseError != null,
                    supportingText = uiState.exerciseError?.let { error -> { Text(error) } },
                    enabled = uiState.activeSet == null && !uiState.isBusy,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = uiState.loadKg,
                    onValueChange = viewModel::updateLoad,
                    label = { Text("Carga (kg)") },
                    isError = uiState.loadError != null,
                    supportingText = uiState.loadError?.let { error -> { Text(error) } },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    enabled = !uiState.isBusy,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = uiState.repetitions,
                    onValueChange = viewModel::updateRepetitions,
                    label = { Text("Repeticiones") },
                    isError = uiState.repetitionsError != null,
                    supportingText = uiState.repetitionsError?.let { error -> { Text(error) } },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    enabled = !uiState.isBusy,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (uiState.activeSet == null) {
                    Button(
                        onClick = viewModel::startSet,
                        enabled = uiState.selectedParticipantId != null && !uiState.isBusy,
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("Iniciar serie") }
                } else {
                    Text(
                        "Serie activa · iniciada ${formatDateTime(uiState.activeSet.startedAt)}",
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Button(
                        onClick = viewModel::finishSet,
                        enabled = !uiState.isBusy,
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("Finalizar serie") }
                    OutlinedButton(
                        onClick = viewModel::cancelActiveSet,
                        enabled = !uiState.isBusy,
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("Cancelar serie incompleta") }
                }
            }
        }
        item { Text("Series finalizadas", style = MaterialTheme.typography.titleMedium) }
        if (uiState.completedSets.isEmpty()) {
            item { Text("Todavía no hay series finalizadas en esta sesión.") }
        } else {
            items(uiState.completedSets, key = ExerciseSetEntity::id) { set ->
                SetCard(set, participantName(set.participantId, uiState.allParticipants))
            }
        }
        item {
            OutlinedButton(
                onClick = viewModel::requestFinishSession,
                enabled = uiState.activeSet == null && !uiState.isBusy,
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Finalizar sesión") }
        }
    }
}

@Composable
private fun EmptySession(onCreate: () -> Unit, modifier: Modifier) {
    Column(
        modifier = modifier.padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("No hay una sesión en curso.", style = MaterialTheme.typography.titleLarge)
        Text("Crea una sesión y elige uno o varios participantes activos.")
        Button(onClick = onCreate, modifier = Modifier.fillMaxWidth().padding(top = 20.dp)) {
            Text("Crear sesión")
        }
    }
}

@Composable
private fun ParticipantPickerDialog(
    mode: ParticipantPickerMode,
    candidates: List<ParticipantEntity>,
    selectedIds: Set<String>,
    isBusy: Boolean,
    onToggle: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (mode == ParticipantPickerMode.CREATE_SESSION) "Nueva sesión" else "Añadir participantes") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                candidates.forEach { participant ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(
                            checked = participant.id in selectedIds,
                            onCheckedChange = { onToggle(participant.id) },
                            enabled = !isBusy,
                        )
                        Text(participant.nickname)
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onConfirm, enabled = selectedIds.isNotEmpty() && !isBusy) {
                Text(if (mode == ParticipantPickerMode.CREATE_SESSION) "Iniciar" else "Añadir")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isBusy) { Text("Cancelar") }
        },
    )
}

@Composable
internal fun SetCard(set: ExerciseSetEntity, participant: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("$participant · ${set.exercise}", style = MaterialTheme.typography.titleSmall)
            Text("${set.loadKg.toDisplayString()} kg · ${set.repetitions} repeticiones · Serie ${set.setOrder}")
            set.completedAt?.let { Text(formatDateTime(it), style = MaterialTheme.typography.bodySmall) }
        }
    }
}

@Composable
internal fun SessionTopBar(title: String, onBack: () -> Unit) {
    Surface(tonalElevation = 3.dp) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(onClick = onBack) { Text("Volver") }
            Text(title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(start = 8.dp))
        }
    }
}

internal fun participantName(id: String, participants: List<ParticipantEntity>): String =
    participants.firstOrNull { it.id == id }?.nickname ?: "Participante archivado"

internal fun formatDateTime(timestamp: Long): String =
    DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(timestamp))

private fun Float.toDisplayString(): String = if (this % 1f == 0f) toInt().toString() else toString()
