package com.sta.datacollector

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sta.datacollector.sensor.AndroidSensorDataSource
import com.sta.datacollector.ui.participants.ParticipantScreen
import com.sta.datacollector.ui.participants.ParticipantViewModel
import com.sta.datacollector.ui.participants.ParticipantViewModelFactory
import com.sta.datacollector.ui.sessions.SessionHistoryScreen
import com.sta.datacollector.ui.sessions.SessionViewModel
import com.sta.datacollector.ui.sessions.SessionViewModelFactory
import com.sta.datacollector.ui.sessions.TrainingSessionScreen
import com.sta.datacollector.ui.sensors.SensorDiagnosticScreen
import com.sta.datacollector.ui.sensors.SensorDiagnosticViewModel
import com.sta.datacollector.ui.sensors.SensorDiagnosticViewModelFactory
import com.sta.datacollector.ui.theme.STADataCollectorTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            STADataCollectorTheme {
                var destination by rememberSaveable { mutableStateOf(AppDestination.HOME) }
                when (destination) {
                    AppDestination.HOME -> STAHomeScreen(
                        onNewSessionClick = { destination = AppDestination.SESSION },
                        onParticipantsClick = { destination = AppDestination.PARTICIPANTS },
                        onHistoryClick = { destination = AppDestination.HISTORY },
                        onSensorDiagnosticClick = { destination = AppDestination.SENSORS },
                    )

                    AppDestination.PARTICIPANTS -> {
                        val staApplication = application as STAApplication
                        val factory = remember(staApplication) {
                            ParticipantViewModelFactory(staApplication.participantRepository)
                        }
                        val participantViewModel: ParticipantViewModel = viewModel(factory = factory)
                        ParticipantScreen(
                            viewModel = participantViewModel,
                            onBack = { destination = AppDestination.HOME },
                        )
                    }

                    AppDestination.SESSION,
                    AppDestination.HISTORY,
                    -> {
                        val staApplication = application as STAApplication
                        val factory = remember(staApplication) {
                            SessionViewModelFactory(
                                staApplication.participantRepository,
                                staApplication.trainingSessionRepository,
                                staApplication.exerciseSetRepository,
                            )
                        }
                        val sessionViewModel: SessionViewModel = viewModel(factory = factory)
                        if (destination == AppDestination.SESSION) {
                            TrainingSessionScreen(
                                viewModel = sessionViewModel,
                                onBack = { destination = AppDestination.HOME },
                            )
                        } else {
                            SessionHistoryScreen(
                                viewModel = sessionViewModel,
                                onBack = { destination = AppDestination.HOME },
                            )
                        }
                    }

                    AppDestination.SENSORS -> {
                        val factory = remember(applicationContext) {
                            SensorDiagnosticViewModelFactory(
                                AndroidSensorDataSource(applicationContext)
                            )
                        }
                        val sensorViewModel: SensorDiagnosticViewModel = viewModel(factory = factory)
                        SensorDiagnosticScreen(
                            viewModel = sensorViewModel,
                            onBack = { destination = AppDestination.HOME },
                        )
                    }
                }
            }
        }
    }
}

private enum class AppDestination {
    HOME,
    PARTICIPANTS,
    SESSION,
    HISTORY,
    SENSORS,
}

@Composable
fun STAHomeScreen(
    onNewSessionClick: () -> Unit,
    onParticipantsClick: () -> Unit,
    onHistoryClick: () -> Unit,
    onSensorDiagnosticClick: () -> Unit,
) {
    Scaffold(
        topBar = {
            Surface(tonalElevation = 3.dp) {
                Text(
                    text = "Smart Training Analytics",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "STA Data Collector",
                style = MaterialTheme.typography.headlineMedium,
            )

            Text("Sistema de recopilación de datos de entrenamiento")

            Button(
                onClick = onNewSessionClick,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Nueva sesión")
            }

            OutlinedButton(
                onClick = onParticipantsClick,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Participantes")
            }

            OutlinedButton(
                onClick = onHistoryClick,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Historial de grabaciones")
            }

            OutlinedButton(
                onClick = onSensorDiagnosticClick,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Diagnóstico de sensores")
            }
        }
    }
}
