package com.sta.datacollector.ui.sensors

import android.os.SystemClock
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.sta.datacollector.sensor.CaptureStartResult
import com.sta.datacollector.sensor.SensorCaptureSnapshot
import com.sta.datacollector.sensor.SensorDataSource
import com.sta.datacollector.sensor.SensorDescriptor
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SensorDiagnosticUiState(
    val sensors: List<SensorDescriptor> = emptyList(),
    val isCapturing: Boolean = false,
    val elapsedMillis: Long = 0L,
    val captureDurationMillis: Long = CAPTURE_DURATION_MILLIS,
    val snapshot: SensorCaptureSnapshot = SensorCaptureSnapshot(),
    val statusText: String = "Listo para iniciar una captura experimental.",
    val message: String? = null,
)

class SensorDiagnosticViewModel(
    private val dataSource: SensorDataSource,
    private val elapsedRealtime: () -> Long = SystemClock::elapsedRealtime,
    private val captureDurationMillis: Long = CAPTURE_DURATION_MILLIS,
    private val visualRefreshMillis: Long = VISUAL_REFRESH_MILLIS,
) : ViewModel() {
    private val _uiState = MutableStateFlow(
        SensorDiagnosticUiState(
            sensors = dataSource.sensors,
            captureDurationMillis = captureDurationMillis,
        )
    )
    val uiState: StateFlow<SensorDiagnosticUiState> = _uiState.asStateFlow()

    private var captureJob: Job? = null

    fun startCapture() {
        when (dataSource.startCapture(SensorDataSource.DEFAULT_SAMPLING_PERIOD_US)) {
            CaptureStartResult.ALREADY_RUNNING -> return
            CaptureStartResult.NO_SENSOR_REGISTERED -> {
                _uiState.value = _uiState.value.copy(
                    message = "No se pudo registrar ningún sensor de movimiento.",
                    statusText = "Captura no iniciada.",
                )
            }

            CaptureStartResult.STARTED -> beginCaptureTimer()
        }
    }

    fun stopCapture() {
        if (!dataSource.isCapturing) return
        captureJob?.cancel()
        captureJob = null
        publishStoppedCapture("Captura detenida por el usuario.")
    }

    fun releaseListeners() {
        captureJob?.cancel()
        captureJob = null
        if (dataSource.isCapturing) {
            val finalSnapshot = dataSource.stopCapture()
            _uiState.value = _uiState.value.copy(
                isCapturing = false,
                snapshot = finalSnapshot,
                statusText = "Captura detenida al salir de la pantalla.",
            )
        }
    }

    fun consumeMessage() {
        _uiState.value = _uiState.value.copy(message = null)
    }

    override fun onCleared() {
        releaseListeners()
        super.onCleared()
    }

    private fun beginCaptureTimer() {
        captureJob?.cancel()
        val startedAt = elapsedRealtime()
        _uiState.value = _uiState.value.copy(
            isCapturing = true,
            elapsedMillis = 0L,
            snapshot = SensorCaptureSnapshot(),
            statusText = "Capturando a 50 Hz objetivo…",
            message = null,
        )
        captureJob = viewModelScope.launch {
            while (dataSource.isCapturing) {
                delay(visualRefreshMillis)
                val elapsed = (elapsedRealtime() - startedAt).coerceAtLeast(0L)
                _uiState.value = _uiState.value.copy(
                    elapsedMillis = elapsed.coerceAtMost(captureDurationMillis),
                    snapshot = dataSource.snapshot(),
                )
                if (elapsed >= captureDurationMillis) {
                    captureJob = null
                    publishStoppedCapture("Captura de 30 segundos completada.")
                    break
                }
            }
        }
    }

    private fun publishStoppedCapture(status: String) {
        val finalSnapshot = dataSource.stopCapture()
        _uiState.value = _uiState.value.copy(
            isCapturing = false,
            snapshot = finalSnapshot,
            statusText = status,
        )
    }
}

class SensorDiagnosticViewModelFactory(
    private val dataSource: SensorDataSource,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(SensorDiagnosticViewModel::class.java))
        return SensorDiagnosticViewModel(dataSource) as T
    }
}

internal const val CAPTURE_DURATION_MILLIS = 30_000L
private const val VISUAL_REFRESH_MILLIS = 200L
