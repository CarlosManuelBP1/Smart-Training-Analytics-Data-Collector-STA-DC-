package com.sta.datacollector.ui.sessions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.sta.datacollector.data.ExerciseSetRepository
import com.sta.datacollector.data.ParticipantRepository
import com.sta.datacollector.data.TrainingSessionRepository
import com.sta.datacollector.data.local.ExerciseSetEntity
import com.sta.datacollector.data.local.ParticipantEntity
import com.sta.datacollector.data.local.TrainingSessionEntity
import com.sta.datacollector.data.local.TrainingSessionStatus
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

enum class ParticipantPickerMode {
    CREATE_SESSION,
    ADD_TO_SESSION,
}

data class SessionUiState(
    val activeParticipants: List<ParticipantEntity> = emptyList(),
    val allParticipants: List<ParticipantEntity> = emptyList(),
    val sessions: List<TrainingSessionEntity> = emptyList(),
    val activeSession: TrainingSessionEntity? = null,
    val sessionParticipants: List<ParticipantEntity> = emptyList(),
    val completedSets: List<ExerciseSetEntity> = emptyList(),
    val allCompletedSets: List<ExerciseSetEntity> = emptyList(),
    val activeSet: ExerciseSetEntity? = null,
    val selectedParticipantId: String? = null,
    val exercise: String = "",
    val loadKg: String = "",
    val repetitions: String = "",
    val exerciseError: String? = null,
    val loadError: String? = null,
    val repetitionsError: String? = null,
    val pickerMode: ParticipantPickerMode? = null,
    val pickerSelectedParticipantIds: Set<String> = emptySet(),
    val recoveredActiveSet: ExerciseSetEntity? = null,
    val showFinishSessionConfirmation: Boolean = false,
    val isBusy: Boolean = false,
    val message: String? = null,
)

data class ValidatedSetInput(
    val exercise: String,
    val loadKg: Float,
    val repetitions: Int,
)

data class SetInputValidation(
    val value: ValidatedSetInput? = null,
    val exerciseError: String? = null,
    val loadError: String? = null,
    val repetitionsError: String? = null,
)

object SessionInputValidator {
    fun validate(exercise: String, loadKg: String, repetitions: String): SetInputValidation {
        val cleanExercise = exercise.trim()
        val parsedLoad = loadKg.trim().replace(',', '.').toFloatOrNull()
        val parsedRepetitions = repetitions.trim().toIntOrNull()
        val exerciseError = if (cleanExercise.isEmpty()) {
            "El ejercicio es obligatorio."
        } else {
            null
        }
        val loadError = if (
            parsedLoad == null || !parsedLoad.isFinite() || parsedLoad < 0f
        ) {
            "Ingresa una carga válida mayor o igual a 0."
        } else {
            null
        }
        val repetitionsError = if (parsedRepetitions == null || parsedRepetitions <= 0) {
            "Ingresa repeticiones enteras mayores que 0."
        } else {
            null
        }
        return SetInputValidation(
            value = if (
                exerciseError == null && loadError == null && repetitionsError == null
            ) {
                ValidatedSetInput(cleanExercise, parsedLoad!!, parsedRepetitions!!)
            } else {
                null
            },
            exerciseError = exerciseError,
            loadError = loadError,
            repetitionsError = repetitionsError,
        )
    }
}

class SessionViewModel(
    private val participantRepository: ParticipantRepository,
    private val trainingSessionRepository: TrainingSessionRepository,
    private val exerciseSetRepository: ExerciseSetRepository,
    private val currentTimeMillis: () -> Long = System::currentTimeMillis,
) : ViewModel() {
    private val _uiState = MutableStateFlow(SessionUiState())
    val uiState: StateFlow<SessionUiState> = _uiState.asStateFlow()

    private val handledActiveSetIds = mutableSetOf<String>()
    private var sessionParticipantsJob: Job? = null
    private var sessionSetsJob: Job? = null
    private var loadLookupJob: Job? = null

    init {
        viewModelScope.launch {
            participantRepository.observeActiveParticipants().collect { participants ->
                _uiState.value = _uiState.value.copy(activeParticipants = participants)
            }
        }
        viewModelScope.launch {
            participantRepository.observeAllParticipants().collect { participants ->
                _uiState.value = _uiState.value.copy(allParticipants = participants)
            }
        }
        viewModelScope.launch {
            trainingSessionRepository.observeSessions().collect { sessions ->
                _uiState.value = _uiState.value.copy(sessions = sessions)
            }
        }
        viewModelScope.launch {
            trainingSessionRepository.observeActiveSession().collectLatest { session ->
                observeActiveSession(session)
            }
        }
        viewModelScope.launch {
            exerciseSetRepository.observeActiveSet().collect { activeSet ->
                val recoveredSet = activeSet?.takeIf { it.id !in handledActiveSetIds }
                _uiState.value = _uiState.value.copy(
                    activeSet = activeSet,
                    recoveredActiveSet = recoveredSet ?: _uiState.value.recoveredActiveSet
                        ?.takeIf { it.id == activeSet?.id },
                )
            }
        }
        viewModelScope.launch {
            exerciseSetRepository.observeAllCompleted().collect { sets ->
                _uiState.value = _uiState.value.copy(allCompletedSets = sets)
            }
        }
    }

    fun openCreateSession() {
        if (_uiState.value.activeSession != null) {
            _uiState.value = _uiState.value.copy(message = "Ya existe una sesión en curso.")
            return
        }
        if (_uiState.value.activeParticipants.isEmpty()) {
            _uiState.value = _uiState.value.copy(
                message = "Crea al menos un participante activo antes de iniciar una sesión."
            )
            return
        }
        _uiState.value = _uiState.value.copy(
            pickerMode = ParticipantPickerMode.CREATE_SESSION,
            pickerSelectedParticipantIds = emptySet(),
        )
    }

    fun openAddParticipants() {
        val session = _uiState.value.activeSession ?: return
        val currentIds = _uiState.value.sessionParticipants.mapTo(mutableSetOf(), ParticipantEntity::id)
        val candidates = _uiState.value.activeParticipants.filterNot { it.id in currentIds }
        if (candidates.isEmpty()) {
            _uiState.value = _uiState.value.copy(
                message = "No hay participantes activos pendientes por añadir."
            )
            return
        }
        if (session.status != TrainingSessionStatus.ACTIVE) return
        _uiState.value = _uiState.value.copy(
            pickerMode = ParticipantPickerMode.ADD_TO_SESSION,
            pickerSelectedParticipantIds = emptySet(),
        )
    }

    fun togglePickerParticipant(participantId: String) {
        val selected = _uiState.value.pickerSelectedParticipantIds
        _uiState.value = _uiState.value.copy(
            pickerSelectedParticipantIds = if (participantId in selected) {
                selected - participantId
            } else {
                selected + participantId
            }
        )
    }

    fun dismissParticipantPicker() {
        if (!_uiState.value.isBusy) {
            _uiState.value = _uiState.value.copy(
                pickerMode = null,
                pickerSelectedParticipantIds = emptySet(),
            )
        }
    }

    fun confirmParticipantPicker() {
        val mode = _uiState.value.pickerMode ?: return
        val participantIds = _uiState.value.pickerSelectedParticipantIds
        if (participantIds.isEmpty()) {
            _uiState.value = _uiState.value.copy(
                message = "Selecciona al menos un participante."
            )
            return
        }
        launchOperation {
            when (mode) {
                ParticipantPickerMode.CREATE_SESSION -> {
                    val created = trainingSessionRepository.createSharedSessionIfNoneActive(
                        session = TrainingSessionEntity(startedAt = currentTimeMillis()),
                        participantIds = participantIds,
                    )
                    check(created) { "Ya existe una sesión en curso." }
                    "Sesión iniciada."
                }

                ParticipantPickerMode.ADD_TO_SESSION -> {
                    val sessionId = checkNotNull(_uiState.value.activeSession?.id)
                    val added = trainingSessionRepository.addParticipants(sessionId, participantIds)
                    check(added) { "La sesión ya no está activa." }
                    "Participantes añadidos."
                }
            }.also { message ->
                _uiState.value = _uiState.value.copy(
                    pickerMode = null,
                    pickerSelectedParticipantIds = emptySet(),
                    message = message,
                )
            }
        }
    }

    fun selectParticipant(participantId: String) {
        if (_uiState.value.activeSet != null) {
            _uiState.value = _uiState.value.copy(
                message = "Finaliza o cancela la serie antes de cambiar de participante."
            )
            return
        }
        if (_uiState.value.sessionParticipants.none { it.id == participantId }) return
        _uiState.value = _uiState.value.copy(
            selectedParticipantId = participantId,
            loadKg = "",
            loadError = null,
            repetitionsError = null,
        )
        lookupLastLoad()
    }

    fun updateExercise(value: String) {
        if (_uiState.value.activeSet != null) return
        _uiState.value = _uiState.value.copy(
            exercise = value,
            exerciseError = null,
            loadKg = "",
            loadError = null,
        )
        lookupLastLoad()
    }

    fun updateLoad(value: String) {
        _uiState.value = _uiState.value.copy(loadKg = value, loadError = null)
    }

    fun updateRepetitions(value: String) {
        _uiState.value = _uiState.value.copy(
            repetitions = value,
            repetitionsError = null,
        )
    }

    fun startSet() {
        val state = _uiState.value
        val session = state.activeSession ?: return
        val participantId = state.selectedParticipantId
        if (participantId == null) {
            _uiState.value = state.copy(message = "Selecciona un participante.")
            return
        }
        if (state.activeSet != null) {
            _uiState.value = state.copy(message = "Ya hay una serie activa en este dispositivo.")
            return
        }
        val validation = validateCurrentInput()
        val input = validation.value ?: return
        launchOperation {
            val setOrder = exerciseSetRepository.getNextSetOrder(
                sessionId = session.id,
                participantId = participantId,
                exercise = input.exercise,
            )
            val exerciseSet = ExerciseSetEntity(
                sessionId = session.id,
                participantId = participantId,
                exercise = input.exercise,
                loadKg = input.loadKg,
                repetitions = input.repetitions,
                setOrder = setOrder,
                startedAt = currentTimeMillis(),
            )
            handledActiveSetIds += exerciseSet.id
            val started = exerciseSetRepository.startIfNoActive(exerciseSet)
            if (!started) handledActiveSetIds -= exerciseSet.id
            check(started) {
                "No se pudo iniciar: ya hay una serie activa o la sesión dejó de estar disponible."
            }
            _uiState.value = _uiState.value.copy(message = "Serie iniciada.")
        }
    }

    fun finishSet() {
        val activeSet = _uiState.value.activeSet ?: return
        val validation = validateCurrentInput()
        val input = validation.value ?: return
        launchOperation {
            val completedAt = currentTimeMillis()
            check(completedAt >= activeSet.startedAt) { "La hora de cierre no es válida." }
            check(
                exerciseSetRepository.completeActiveSet(
                    setId = activeSet.id,
                    loadKg = input.loadKg,
                    repetitions = input.repetitions,
                    completedAt = completedAt,
                )
            ) { "La serie ya no estaba activa." }
            _uiState.value = _uiState.value.copy(
                loadKg = input.loadKg.toEditableString(),
                repetitions = "",
                message = "Serie finalizada.",
            )
        }
    }

    fun cancelActiveSet() {
        val activeSet = _uiState.value.activeSet ?: return
        launchOperation {
            check(exerciseSetRepository.cancelActiveSet(activeSet.id)) {
                "La serie ya no estaba activa."
            }
            handledActiveSetIds += activeSet.id
            _uiState.value = _uiState.value.copy(
                recoveredActiveSet = null,
                message = "Serie incompleta descartada.",
            )
        }
    }

    fun continueRecoveredSet() {
        val recovered = _uiState.value.recoveredActiveSet ?: return
        handledActiveSetIds += recovered.id
        _uiState.value = _uiState.value.copy(
            recoveredActiveSet = null,
            selectedParticipantId = recovered.participantId,
            exercise = recovered.exercise,
            loadKg = recovered.loadKg.toEditableString(),
            repetitions = recovered.repetitions.toString(),
            message = "Serie recuperada. Puedes finalizarla o cancelarla.",
        )
    }

    fun discardRecoveredSet() {
        val recovered = _uiState.value.recoveredActiveSet ?: return
        handledActiveSetIds += recovered.id
        _uiState.value = _uiState.value.copy(recoveredActiveSet = null)
        cancelActiveSet()
    }

    fun requestFinishSession() {
        if (_uiState.value.activeSet != null) {
            _uiState.value = _uiState.value.copy(
                message = "Finaliza o cancela la serie activa antes de cerrar la sesión."
            )
            return
        }
        if (_uiState.value.activeSession != null) {
            _uiState.value = _uiState.value.copy(showFinishSessionConfirmation = true)
        }
    }

    fun dismissFinishSessionConfirmation() {
        _uiState.value = _uiState.value.copy(showFinishSessionConfirmation = false)
    }

    fun confirmFinishSession() {
        val session = _uiState.value.activeSession ?: return
        if (_uiState.value.activeSet != null) return
        _uiState.value = _uiState.value.copy(showFinishSessionConfirmation = false)
        launchOperation {
            check(
                trainingSessionRepository.updateStatus(
                    sessionId = session.id,
                    status = TrainingSessionStatus.COMPLETED,
                    endedAt = currentTimeMillis(),
                )
            ) { "La sesión ya no estaba activa." }
            _uiState.value = _uiState.value.copy(message = "Sesión finalizada.")
        }
    }

    fun consumeMessage() {
        _uiState.value = _uiState.value.copy(message = null)
    }

    private fun observeActiveSession(session: TrainingSessionEntity?) {
        sessionParticipantsJob?.cancel()
        sessionSetsJob?.cancel()
        _uiState.value = _uiState.value.copy(
            activeSession = session,
            sessionParticipants = emptyList(),
            completedSets = emptyList(),
            selectedParticipantId = if (session == null) null else _uiState.value.selectedParticipantId,
        )
        if (session == null) return

        sessionParticipantsJob = viewModelScope.launch {
            trainingSessionRepository.observeParticipants(session.id).collect { participants ->
                val selectedId = _uiState.value.selectedParticipantId
                    ?.takeIf { id -> participants.any { it.id == id } }
                    ?: participants.firstOrNull()?.id
                _uiState.value = _uiState.value.copy(
                    sessionParticipants = participants,
                    selectedParticipantId = selectedId,
                )
                lookupLastLoad()
            }
        }
        sessionSetsJob = viewModelScope.launch {
            exerciseSetRepository.observeForSession(session.id).collect { sets ->
                _uiState.value = _uiState.value.copy(
                    completedSets = sets.filter { it.completedAt != null }.asReversed()
                )
            }
        }
    }

    private fun lookupLastLoad() {
        loadLookupJob?.cancel()
        val participantId = _uiState.value.selectedParticipantId ?: return
        val exercise = _uiState.value.exercise.trim()
        if (exercise.isEmpty() || _uiState.value.activeSet != null) return
        loadLookupJob = viewModelScope.launch {
            delay(200)
            val lastLoad = exerciseSetRepository.getLastCompletedLoad(participantId, exercise)
            val current = _uiState.value
            if (
                lastLoad != null &&
                current.selectedParticipantId == participantId &&
                current.exercise.trim().equals(exercise, ignoreCase = true) &&
                current.loadKg.isBlank() &&
                current.activeSet == null
            ) {
                _uiState.value = current.copy(loadKg = lastLoad.toEditableString())
            }
        }
    }

    private fun validateCurrentInput(): SetInputValidation {
        val state = _uiState.value
        val validation = SessionInputValidator.validate(
            exercise = state.exercise,
            loadKg = state.loadKg,
            repetitions = state.repetitions,
        )
        _uiState.value = state.copy(
            exerciseError = validation.exerciseError,
            loadError = validation.loadError,
            repetitionsError = validation.repetitionsError,
        )
        return validation
    }

    private fun launchOperation(operation: suspend () -> Unit) {
        if (_uiState.value.isBusy) return
        _uiState.value = _uiState.value.copy(isBusy = true)
        viewModelScope.launch {
            try {
                operation()
            } catch (error: Exception) {
                _uiState.value = _uiState.value.copy(
                    message = error.message ?: "La operación no pudo completarse."
                )
            } finally {
                _uiState.value = _uiState.value.copy(isBusy = false)
            }
        }
    }
}

class SessionViewModelFactory(
    private val participantRepository: ParticipantRepository,
    private val trainingSessionRepository: TrainingSessionRepository,
    private val exerciseSetRepository: ExerciseSetRepository,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(SessionViewModel::class.java))
        return SessionViewModel(
            participantRepository,
            trainingSessionRepository,
            exerciseSetRepository,
        ) as T
    }
}

private fun Float.toEditableString(): String =
    if (this % 1f == 0f) toInt().toString() else toString()
