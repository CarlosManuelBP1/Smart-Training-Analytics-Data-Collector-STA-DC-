package com.sta.datacollector.ui.sensors

import com.sta.datacollector.sensor.CaptureStartResult
import com.sta.datacollector.sensor.MotionSensorType
import com.sta.datacollector.sensor.SensorCaptureSnapshot
import com.sta.datacollector.sensor.SensorDataSource
import com.sta.datacollector.sensor.SensorDescriptor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SensorDiagnosticViewModelTest {
    private val scheduler = TestCoroutineScheduler()
    private val dispatcher = StandardTestDispatcher(scheduler)

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `rapid starts register listeners only once and manual stop releases them`() =
        runTest(dispatcher) {
            val dataSource = FakeSensorDataSource()
            val viewModel = viewModel(dataSource)

            viewModel.startCapture()
            viewModel.startCapture()

            assertEquals(1, dataSource.startCalls)
            assertTrue(viewModel.uiState.value.isCapturing)

            viewModel.stopCapture()
            viewModel.stopCapture()

            assertEquals(1, dataSource.stopCalls)
            assertFalse(viewModel.uiState.value.isCapturing)
        }

    @Test
    fun `capture stops automatically at configured duration`() = runTest(dispatcher) {
        val dataSource = FakeSensorDataSource()
        val viewModel = viewModel(dataSource)

        viewModel.startCapture()
        advanceTimeBy(TEST_DURATION_MILLIS)
        runCurrent()

        assertEquals(1, dataSource.stopCalls)
        assertFalse(viewModel.uiState.value.isCapturing)
        assertEquals(TEST_DURATION_MILLIS, viewModel.uiState.value.elapsedMillis)
    }

    @Test
    fun `leaving screen releases active listener once`() = runTest(dispatcher) {
        val dataSource = FakeSensorDataSource()
        val viewModel = viewModel(dataSource)
        viewModel.startCapture()

        viewModel.releaseListeners()
        viewModel.releaseListeners()

        assertEquals(1, dataSource.stopCalls)
        assertFalse(dataSource.isCapturing)
    }

    private fun viewModel(dataSource: SensorDataSource) = SensorDiagnosticViewModel(
        dataSource = dataSource,
        elapsedRealtime = { scheduler.currentTime },
        captureDurationMillis = TEST_DURATION_MILLIS,
        visualRefreshMillis = REFRESH_MILLIS,
    )

    private companion object {
        const val TEST_DURATION_MILLIS = 1_000L
        const val REFRESH_MILLIS = 100L
    }
}

private class FakeSensorDataSource : SensorDataSource {
    override val sensors = listOf(
        SensorDescriptor(MotionSensorType.ACCELEROMETER, available = true)
    )
    override var isCapturing = false
        private set
    var startCalls = 0
    var stopCalls = 0

    override fun startCapture(
        samplingPeriodUs: Int,
        onSample: (com.sta.datacollector.sensor.SensorSample) -> Unit,
    ): CaptureStartResult {
        if (isCapturing) return CaptureStartResult.ALREADY_RUNNING
        startCalls++
        isCapturing = true
        return CaptureStartResult.STARTED
    }

    override fun snapshot() = SensorCaptureSnapshot()

    override fun stopCapture(): SensorCaptureSnapshot {
        stopCalls++
        isCapturing = false
        return SensorCaptureSnapshot()
    }
}
