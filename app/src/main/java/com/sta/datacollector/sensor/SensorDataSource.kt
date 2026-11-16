package com.sta.datacollector.sensor

interface SensorDataSource {
    val sensors: List<SensorDescriptor>
    val isCapturing: Boolean

    fun startCapture(
        samplingPeriodUs: Int = DEFAULT_SAMPLING_PERIOD_US,
        onSample: (SensorSample) -> Unit = {},
    ): CaptureStartResult
    fun snapshot(): SensorCaptureSnapshot
    fun stopCapture(): SensorCaptureSnapshot

    companion object {
        const val DEFAULT_SAMPLING_PERIOD_US = 20_000
    }
}
