package com.sta.datacollector.ui.sensors

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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sta.datacollector.sensor.MotionSensorType
import com.sta.datacollector.sensor.SensorDescriptor
import com.sta.datacollector.sensor.SensorSample
import com.sta.datacollector.sensor.SensorStatistics
import java.util.Locale

@Composable
fun SensorDiagnosticScreen(
    viewModel: SensorDiagnosticViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    DisposableEffect(viewModel) {
        onDispose(viewModel::releaseListeners)
    }
    BackHandler(onBack = onBack)
    LaunchedEffect(uiState.message) {
        uiState.message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.consumeMessage()
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = { DiagnosticTopBar(onBack) },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                CaptureControls(
                    uiState = uiState,
                    onStart = viewModel::startCapture,
                    onStop = viewModel::stopCapture,
                )
            }
            items(uiState.sensors, key = { it.type }) { descriptor ->
                SensorCard(
                    descriptor = descriptor,
                    sample = uiState.snapshot.latestSamples[descriptor.type],
                    statistics = uiState.snapshot.statistics[descriptor.type] ?: SensorStatistics(),
                )
            }
            item {
                Text(
                    "El período solicitado es 20 000 μs (50 Hz objetivo). Android y el hardware pueden entregar una frecuencia diferente; la frecuencia efectiva se calcula con SensorEvent.timestamp.",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

@Composable
private fun CaptureControls(
    uiState: SensorDiagnosticUiState,
    onStart: () -> Unit,
    onStop: () -> Unit,
) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Captura experimental", style = MaterialTheme.typography.titleMedium)
            Text(uiState.statusText)
            val progress = (uiState.elapsedMillis.toFloat() / uiState.captureDurationMillis)
                .coerceIn(0f, 1f)
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth(),
            )
            Text("${uiState.elapsedMillis / 1_000} / ${uiState.captureDurationMillis / 1_000} s")
            if (uiState.isCapturing) {
                OutlinedButton(onClick = onStop, modifier = Modifier.fillMaxWidth()) {
                    Text("Detener captura")
                }
            } else {
                Button(
                    onClick = onStart,
                    enabled = uiState.sensors.any(SensorDescriptor::available),
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Iniciar captura de 30 segundos") }
            }
        }
    }
}

@Composable
private fun SensorCard(
    descriptor: SensorDescriptor,
    sample: SensorSample?,
    statistics: SensorStatistics,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (descriptor.available) {
                MaterialTheme.colorScheme.surfaceVariant
            } else {
                MaterialTheme.colorScheme.errorContainer
            }
        ),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(descriptor.type.displayName, style = MaterialTheme.typography.titleMedium)
            if (!descriptor.available) {
                Text("No disponible en este dispositivo.", color = MaterialTheme.colorScheme.error)
                return@Column
            }
            Text("Nombre: ${descriptor.name}")
            Text("Fabricante: ${descriptor.vendor}")
            Text("Resolución: ${descriptor.resolution.formatNumber()} ${descriptor.type.unit}")
            Text("Rango máximo: ${descriptor.maximumRange.formatNumber()} ${descriptor.type.unit}")
            Text("Período mínimo permitido: ${descriptor.minimumDelayUs ?: "No informado"} μs")
            Text("Frecuencia máxima según período mínimo: ${descriptor.highestPermittedFrequencyHz.formatNumber()} Hz")
            Text("Período máximo permitido: ${descriptor.maximumDelayUs ?: "No informado"} μs")
            Text("Frecuencia mínima según período máximo: ${descriptor.lowestSupportedFrequencyHz.formatNumber()} Hz")
            Text("Lectura reciente", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 6.dp))
            if (sample == null) {
                Text("Sin muestras todavía.")
            } else {
                Text("x=${sample.x.formatNumber()}  y=${sample.y.formatNumber()}  z=${sample.z.formatNumber()} ${descriptor.type.unit}")
                Text("timestamp=${sample.timestampNanos} ns", style = MaterialTheme.typography.bodySmall)
            }
            Text("Estadísticas", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 6.dp))
            Text("Eventos: ${statistics.eventCount}")
            Text("Frecuencia efectiva: ${statistics.effectiveFrequencyHz.formatNumber()} Hz")
            Text("Intervalo medio: ${statistics.meanIntervalMillis.formatNumber()} ms")
            Text("Intervalo mínimo: ${statistics.minimumIntervalMillis.formatNumber()} ms")
            Text("Intervalo máximo: ${statistics.maximumIntervalMillis.formatNumber()} ms")
        }
    }
}

@Composable
private fun DiagnosticTopBar(onBack: () -> Unit) {
    Surface(tonalElevation = 3.dp) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(onClick = onBack) { Text("Volver") }
            Text(
                "Diagnóstico de sensores",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(start = 8.dp),
            )
        }
    }
}

private fun Number?.formatNumber(): String = when (this) {
    null -> "No informado"
    else -> String.format(Locale.getDefault(), "%.3f", toDouble())
}
