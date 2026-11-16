package com.sta.datacollector.sensor

enum class MotionSensorType(val displayName: String, val unit: String) {
    ACCELEROMETER("Acelerómetro", "m/s²"),
    GYROSCOPE("Giroscopio", "rad/s"),
}

data class SensorDescriptor(
    val type: MotionSensorType,
    val available: Boolean,
    val name: String? = null,
    val vendor: String? = null,
    val resolution: Float? = null,
    val maximumRange: Float? = null,
    val minimumDelayUs: Int? = null,
    val maximumDelayUs: Int? = null,
) {
    val lowestSupportedFrequencyHz: Double?
        get() = maximumDelayUs
            ?.takeIf { it > 0 }
            ?.let { 1_000_000.0 / it }

    val highestPermittedFrequencyHz: Double?
        get() = minimumDelayUs
            ?.takeIf { it > 0 }
            ?.let { 1_000_000.0 / it }
}

data class SensorSample(
    val type: MotionSensorType,
    val timestampNanos: Long,
    val x: Float,
    val y: Float,
    val z: Float,
)

data class SensorStatistics(
    val eventCount: Long = 0,
    val effectiveFrequencyHz: Double = 0.0,
    val meanIntervalMillis: Double = 0.0,
    val minimumIntervalMillis: Double = 0.0,
    val maximumIntervalMillis: Double = 0.0,
)

data class SensorCaptureSnapshot(
    val latestSamples: Map<MotionSensorType, SensorSample> = emptyMap(),
    val statistics: Map<MotionSensorType, SensorStatistics> = emptyMap(),
)

enum class CaptureStartResult {
    STARTED,
    ALREADY_RUNNING,
    NO_SENSOR_REGISTERED,
}
