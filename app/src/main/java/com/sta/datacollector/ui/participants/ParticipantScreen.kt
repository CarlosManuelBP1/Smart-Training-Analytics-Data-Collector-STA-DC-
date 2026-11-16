package com.sta.datacollector.ui.participants

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.MaterialTheme
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
import com.sta.datacollector.data.local.ParticipantEntity

@Composable
fun ParticipantScreen(
    viewModel: ParticipantViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    BackHandler {
        when {
            uiState.participantPendingArchive != null -> viewModel.dismissArchiveConfirmation()
            uiState.editor != null -> viewModel.dismissEditor()
            else -> onBack()
        }
    }

    LaunchedEffect(uiState.message) {
        uiState.message?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.consumeMessage()
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            Surface(tonalElevation = 3.dp) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(onClick = onBack) {
                        Text("Volver")
                    }
                    Text(
                        text = "Participantes",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = viewModel::startCreating,
            ) {
                Text("Nuevo participante")
            }
        },
    ) { padding ->
        ParticipantList(
            uiState = uiState,
            onSelect = viewModel::selectParticipant,
            onEdit = viewModel::startEditing,
            onArchive = viewModel::requestArchive,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        )
    }

    uiState.editor?.let { editor ->
        ParticipantEditorDialog(
            editor = editor,
            onNicknameChange = viewModel::updateNickname,
            onHeightChange = viewModel::updateHeight,
            onWeightChange = viewModel::updateWeight,
            onExperienceChange = viewModel::updateExperienceLevel,
            onSave = viewModel::saveEditor,
            onDismiss = viewModel::dismissEditor,
        )
    }

    uiState.participantPendingArchive?.let { participant ->
        AlertDialog(
            onDismissRequest = viewModel::dismissArchiveConfirmation,
            title = { Text("Archivar participante") },
            text = {
                Text("¿Deseas archivar el perfil de ${participant.nickname}? Ya no aparecerá en la lista activa.")
            },
            confirmButton = {
                Button(onClick = viewModel::confirmArchive) {
                    Text("Archivar")
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissArchiveConfirmation) {
                    Text("Cancelar")
                }
            },
        )
    }
}

@Composable
private fun ParticipantList(
    uiState: ParticipantUiState,
    onSelect: (ParticipantEntity) -> Unit,
    onEdit: (ParticipantEntity) -> Unit,
    onArchive: (ParticipantEntity) -> Unit,
    modifier: Modifier = Modifier,
) {
    when {
        uiState.isLoading -> {
            Column(
                modifier = modifier,
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                CircularProgressIndicator()
                Spacer(Modifier.height(12.dp))
                Text("Cargando participantes…")
            }
        }

        uiState.participants.isEmpty() -> {
            Column(
                modifier = modifier.padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = "Aún no hay participantes activos.",
                    style = MaterialTheme.typography.titleMedium,
                )
                Spacer(Modifier.height(8.dp))
                Text("Crea el primer perfil con el botón Nuevo participante.")
            }
        }

        else -> {
            LazyColumn(
                modifier = modifier,
                contentPadding = PaddingValues(
                    start = 16.dp,
                    top = 16.dp,
                    end = 16.dp,
                    bottom = 96.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(uiState.participants, key = ParticipantEntity::id) { participant ->
                    ParticipantCard(
                        participant = participant,
                        isSelected = participant.id == uiState.selectedParticipantId,
                        onSelect = { onSelect(participant) },
                        onEdit = { onEdit(participant) },
                        onArchive = { onArchive(participant) },
                    )
                }
            }
        }
    }
}

@Composable
private fun ParticipantCard(
    participant: ParticipantEntity,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onEdit: () -> Unit,
    onArchive: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        ),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = participant.nickname,
                    style = MaterialTheme.typography.titleMedium,
                )
                if (isSelected) {
                    Text(
                        text = "Seleccionado",
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }

            val details = buildList {
                participant.heightCm?.let { add("${it.toDisplayString()} cm") }
                participant.weightKg?.let { add("${it.toDisplayString()} kg") }
                participant.experienceLevel?.takeIf(String::isNotBlank)?.let(::add)
            }
            Text(
                text = details.ifEmpty { listOf("Sin datos adicionales") }.joinToString(" · "),
                style = MaterialTheme.typography.bodyMedium,
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(onClick = onSelect, enabled = !isSelected) {
                    Text(if (isSelected) "Seleccionado" else "Seleccionar")
                }
                TextButton(onClick = onEdit) {
                    Text("Editar")
                }
                TextButton(onClick = onArchive) {
                    Text("Archivar")
                }
            }
        }
    }
}

@Composable
private fun ParticipantEditorDialog(
    editor: ParticipantEditorState,
    onNicknameChange: (String) -> Unit,
    onHeightChange: (String) -> Unit,
    onWeightChange: (String) -> Unit,
    onExperienceChange: (String) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (editor.isEditing) "Editar participante" else "Nuevo participante")
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedTextField(
                    value = editor.nickname,
                    onValueChange = onNicknameChange,
                    label = { Text("Apodo *") },
                    singleLine = true,
                    isError = editor.nicknameError != null,
                    supportingText = editor.nicknameError?.let { error -> { Text(error) } },
                    enabled = !editor.isSaving,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = editor.heightCm,
                    onValueChange = onHeightChange,
                    label = { Text("Estatura (cm)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    isError = editor.heightError != null,
                    supportingText = editor.heightError?.let { error -> { Text(error) } },
                    enabled = !editor.isSaving,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = editor.weightKg,
                    onValueChange = onWeightChange,
                    label = { Text("Peso (kg)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    isError = editor.weightError != null,
                    supportingText = editor.weightError?.let { error -> { Text(error) } },
                    enabled = !editor.isSaving,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = editor.experienceLevel,
                    onValueChange = onExperienceChange,
                    label = { Text("Nivel de experiencia") },
                    singleLine = true,
                    enabled = !editor.isSaving,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            Button(onClick = onSave, enabled = !editor.isSaving) {
                Text(if (editor.isSaving) "Guardando…" else "Guardar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !editor.isSaving) {
                Text("Cancelar")
            }
        },
    )
}

private fun Float.toDisplayString(): String =
    if (this % 1f == 0f) toInt().toString() else toString()
