package com.sta.datacollector.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager

class AndroidSensorDataSource(context: Context) : SensorDataSource, SensorEventListener {
    private val sensorManager = context.applicationContext
        .getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val platformSensors = mapOf(
        MotionSensorType.ACCELEROMETER to sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
        MotionSensorType.GYROSCOPE to sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE),
    )
    private val lock = Any()
    private var capturing = false
    private var latestSamples = mutableMapOf<MotionSensorType, SensorSample>()
    private var calculators = newCalculators()
    private var sampleConsumer: (SensorSample) -> Unit = {}

    override val sensors: List<SensorDescriptor> = MotionSensorType.entries.map { type ->
        platformSensors[type].toDescriptor(type)
    }

    override val isCapturing: Boolean
        get() = synchronized(lock) { capturing }

    override fun startCapture(
        samplingPeriodUs: Int,
        onSample: (SensorSample) -> Unit,
    ): CaptureStartResult = synchronized(lock) {
        require(samplingPeriodUs > 0) { "El período de muestreo debe ser positivo." }
        if (capturing) return CaptureStartResult.ALREADY_RUNNING

        latestSamples = mutableMapOf()
        calculators = newCalculators()
        sampleConsumer = onSample
        val registered = platformSensors.values.filterNotNull().count { sensor ->
            sensorManager.registerListener(this, sensor, samplingPeriodUs)
        }
        if (registered == 0) {
            sensorManager.unregisterListener(this)
            sampleConsumer = {}
            return CaptureStartResult.NO_SENSOR_REGISTERED
        }
        capturing = true
        CaptureStartResult.STARTED
    }

    override fun snapshot(): SensorCaptureSnapshot = synchronized(lock) {
        snapshotLocked()
    }

    override fun stopCapture(): SensorCaptureSnapshot {
        sensorManager.unregisterListener(this)
        return synchronized(lock) {
            capturing = false
            sampleConsumer = {}
            snapshotLocked()
        }
    }

    override fun onSensorChanged(event: SensorEvent) {
        val type = event.sensor.toMotionSensorType() ?: return
        if (event.values.size < AXIS_COUNT) return
        val sample = SensorSample(
            type = type,
            timestampNanos = event.timestamp,
            x = event.values[0],
            y = event.values[1],
            z = event.values[2],
        )
        val consumer = synchronized(lock) {
            if (!capturing) return
            latestSamples[type] = sample
            calculators.getValue(type).addTimestamp(sample.timestampNanos)
            sampleConsumer
        }
        consumer(sample)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    private fun snapshotLocked() = SensorCaptureSnapshot(
        latestSamples = latestSamples.toMap(),
        statistics = calculators.mapValues { (_, calculator) -> calculator.snapshot() },
    )

    private fun newCalculators() = MotionSensorType.entries.associateWith {
        SensorStatisticsCalculator()
    }.toMutableMap()

    private fun Sensor?.toDescriptor(type: MotionSensorType): SensorDescriptor =
        if (this == null) {
            SensorDescriptor(type = type, available = false)
        } else {
            SensorDescriptor(
                type = type,
                available = true,
                name = name,
                vendor = vendor,
                resolution = resolution,
                maximumRange = maximumRange,
                minimumDelayUs = minDelay.takeIf { it >= 0 },
                maximumDelayUs = maxDelay.takeIf { it > 0 },
            )
        }

    private fun Sensor.toMotionSensorType(): MotionSensorType? = when (type) {
        Sensor.TYPE_ACCELEROMETER -> MotionSensorType.ACCELEROMETER
        Sensor.TYPE_GYROSCOPE -> MotionSensorType.GYROSCOPE
        else -> null
    }

    private companion object {
        const val AXIS_COUNT = 3
    }
}
