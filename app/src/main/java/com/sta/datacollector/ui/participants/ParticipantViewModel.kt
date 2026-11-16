package com.sta.datacollector.ui.participants

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.sta.datacollector.data.ParticipantRepository
import com.sta.datacollector.data.local.ParticipantEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

data class ParticipantEditorState(
    val participantId: String? = null,
    val nickname: String = "",
    val heightCm: String = "",
    val weightKg: String = "",
    val experienceLevel: String = "",
    val nicknameError: String? = null,
    val heightError: String? = null,
    val weightError: String? = null,
    val isSaving: Boolean = false,
) {
    val isEditing: Boolean
        get() = participantId != null
}

data class ParticipantUiState(
    val participants: List<ParticipantEntity> = emptyList(),
    val selectedParticipantId: String? = null,
    val editor: ParticipantEditorState? = null,
    val participantPendingArchive: ParticipantEntity? = null,
    val isLoading: Boolean = true,
    val message: String? = null,
)

class ParticipantViewModel(
    private val repository: ParticipantRepository,
    private val currentTimeMillis: () -> Long = System::currentTimeMillis,
) : ViewModel() {
    private val _uiState = MutableStateFlow(ParticipantUiState())
    val uiState: StateFlow<ParticipantUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.observeActiveParticipants()
                .catch { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        message = error.message ?: "No se pudieron cargar los participantes.",
                    )
                }
                .collect { participants ->
                    val selectedId = _uiState.value.selectedParticipantId
                        ?.takeIf { id -> participants.any { it.id == id } }
                    _uiState.value = _uiState.value.copy(
                        participants = participants,
                        selectedParticipantId = selectedId,
                        isLoading = false,
                    )
                }
        }
    }

    fun startCreating() {
        _uiState.value = _uiState.value.copy(editor = ParticipantEditorState())
    }

    fun startEditing(participant: ParticipantEntity) {
        _uiState.value = _uiState.value.copy(
            editor = ParticipantEditorState(
                participantId = participant.id,
                nickname = participant.nickname,
                heightCm = participant.heightCm?.toEditableString().orEmpty(),
                weightKg = participant.weightKg?.toEditableString().orEmpty(),
                experienceLevel = participant.experienceLevel.orEmpty(),
            )
        )
    }

    fun dismissEditor() {
        if (_uiState.value.editor?.isSaving != true) {
            _uiState.value = _uiState.value.copy(editor = null)
        }
    }

    fun updateNickname(value: String) = updateEditor {
        copy(nickname = value, nicknameError = null)
    }

    fun updateHeight(value: String) = updateEditor {
        copy(heightCm = value, heightError = null)
    }

    fun updateWeight(value: String) = updateEditor {
        copy(weightKg = value, weightError = null)
    }

    fun updateExperienceLevel(value: String) = updateEditor {
        copy(experienceLevel = value)
    }

    fun saveEditor() {
        val editor = _uiState.value.editor ?: return
        val nickname = editor.nickname.trim()
        val height = parseOptionalPositiveFloat(editor.heightCm)
        val weight = parseOptionalPositiveFloat(editor.weightKg)
        val validatedEditor = editor.copy(
            nicknameError = if (nickname.isEmpty()) "El apodo es obligatorio." else null,
            heightError = if (height.isInvalid) "Ingresa una estatura válida mayor que 0." else null,
            weightError = if (weight.isInvalid) "Ingresa un peso válido mayor que 0." else null,
        )

        if (
            validatedEditor.nicknameError != null ||
            validatedEditor.heightError != null ||
            validatedEditor.weightError != null
        ) {
            _uiState.value = _uiState.value.copy(editor = validatedEditor)
            return
        }

        _uiState.value = _uiState.value.copy(editor = validatedEditor.copy(isSaving = true))
        viewModelScope.launch {
            try {
                val now = currentTimeMillis()
                val experienceLevel = editor.experienceLevel.trim().ifEmpty { null }
                val participantId = editor.participantId
                if (participantId == null) {
                    repository.insert(
                        ParticipantEntity(
                            nickname = nickname,
                            heightCm = height.value,
                            weightKg = weight.value,
                            experienceLevel = experienceLevel,
                            createdAt = now,
                            updatedAt = now,
                        )
                    )
                    _uiState.value = _uiState.value.copy(
                        editor = null,
                        message = "Participante creado.",
                    )
                } else {
                    val existing = repository.getById(participantId)
                    checkNotNull(existing) { "El participante ya no existe." }
                    check(
                        repository.update(
                            existing.copy(
                                nickname = nickname,
                                heightCm = height.value,
                                weightKg = weight.value,
                                experienceLevel = experienceLevel,
                                updatedAt = now,
                            )
                        )
                    ) { "No se pudo actualizar el participante." }
                    _uiState.value = _uiState.value.copy(
                        editor = null,
                        message = "Participante actualizado.",
                    )
                }
            } catch (error: Exception) {
                _uiState.value = _uiState.value.copy(
                    editor = _uiState.value.editor?.copy(isSaving = false),
                    message = error.message ?: "No se pudo guardar el participante.",
                )
            }
        }
    }

    fun selectParticipant(participant: ParticipantEntity) {
        if (_uiState.value.participants.any { it.id == participant.id }) {
            _uiState.value = _uiState.value.copy(
                selectedParticipantId = participant.id,
                message = "${participant.nickname} seleccionado.",
            )
        }
    }

    fun requestArchive(participant: ParticipantEntity) {
        _uiState.value = _uiState.value.copy(participantPendingArchive = participant)
    }

    fun dismissArchiveConfirmation() {
        _uiState.value = _uiState.value.copy(participantPendingArchive = null)
    }

    fun confirmArchive() {
        val participant = _uiState.value.participantPendingArchive ?: return
        _uiState.value = _uiState.value.copy(participantPendingArchive = null)
        viewModelScope.launch {
            try {
                check(repository.archive(participant.id, currentTimeMillis())) {
                    "El participante ya estaba archivado."
                }
                _uiState.value = _uiState.value.copy(
                    selectedParticipantId = _uiState.value.selectedParticipantId
                        ?.takeUnless { it == participant.id },
                    message = "Participante archivado.",
                )
            } catch (error: Exception) {
                _uiState.value = _uiState.value.copy(
                    message = error.message ?: "No se pudo archivar el participante.",
                )
            }
        }
    }

    fun consumeMessage() {
        _uiState.value = _uiState.value.copy(message = null)
    }

    private fun updateEditor(transform: ParticipantEditorState.() -> ParticipantEditorState) {
        val editor = _uiState.value.editor ?: return
        if (!editor.isSaving) {
            _uiState.value = _uiState.value.copy(editor = editor.transform())
        }
    }

    private fun parseOptionalPositiveFloat(rawValue: String): ParsedFloat {
        if (rawValue.isBlank()) return ParsedFloat(value = null, isInvalid = false)
        val value = rawValue.trim().replace(',', '.').toFloatOrNull()
        return ParsedFloat(
            value = value?.takeIf { it.isFinite() && it > 0f },
            isInvalid = value == null || !value.isFinite() || value <= 0f,
        )
    }

    private data class ParsedFloat(
        val value: Float?,
        val isInvalid: Boolean,
    )
}

class ParticipantViewModelFactory(
    private val repository: ParticipantRepository,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(ParticipantViewModel::class.java)) {
            "ViewModel no compatible: ${modelClass.name}"
        }
        return ParticipantViewModel(repository) as T
    }
}

private fun Float.toEditableString(): String =
    if (this % 1f == 0f) toInt().toString() else toString()
