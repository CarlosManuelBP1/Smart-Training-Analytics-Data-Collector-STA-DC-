package com.sta.datacollector.sensor

import org.junit.Assert.assertEquals
import org.junit.Test

class SensorStatisticsCalculatorTest {
    @Test
    fun `empty and single sample produce zero interval statistics`() {
        val calculator = SensorStatisticsCalculator()
        assertEquals(SensorStatistics(), calculator.snapshot())

        calculator.addTimestamp(10_000_000L)

        assertEquals(1L, calculator.snapshot().eventCount)
        assertEquals(0.0, calculator.snapshot().effectiveFrequencyHz, 0.0)
        assertEquals(0.0, calculator.snapshot().meanIntervalMillis, 0.0)
        assertEquals(0.0, calculator.snapshot().minimumIntervalMillis, 0.0)
    }

    @Test
    fun `calculates frequency mean and maximum from sensor timestamps`() {
        val calculator = SensorStatisticsCalculator()
        calculator.addTimestamp(1_000_000_000L)
        calculator.addTimestamp(1_020_000_000L)
        calculator.addTimestamp(1_040_000_000L)
        calculator.addTimestamp(1_070_000_000L)

        val statistics = calculator.snapshot()
        assertEquals(4L, statistics.eventCount)
        assertEquals(70.0 / 3.0, statistics.meanIntervalMillis, 0.0001)
        assertEquals(20.0, statistics.minimumIntervalMillis, 0.0001)
        assertEquals(30.0, statistics.maximumIntervalMillis, 0.0001)
        assertEquals(3_000.0 / 70.0, statistics.effectiveFrequencyHz, 0.0001)
    }

    @Test
    fun `non increasing timestamp counts event but does not corrupt intervals`() {
        val calculator = SensorStatisticsCalculator()
        calculator.addTimestamp(100_000_000L)
        calculator.addTimestamp(100_000_000L)
        calculator.addTimestamp(120_000_000L)

        val statistics = calculator.snapshot()
        assertEquals(3L, statistics.eventCount)
        assertEquals(20.0, statistics.meanIntervalMillis, 0.0001)
        assertEquals(20.0, statistics.minimumIntervalMillis, 0.0001)
        assertEquals(50.0, statistics.effectiveFrequencyHz, 0.0001)
    }

    @Test
    fun `irregular intervals keep mean minimum and maximum independent`() {
        val calculator = SensorStatisticsCalculator()
        var timestamp = 2_000_000_000L
        calculator.addTimestamp(timestamp)
        listOf(10L, 20L, 40L, 15L).forEach { intervalMillis ->
            timestamp += intervalMillis * 1_000_000L
            calculator.addTimestamp(timestamp)
        }

        val statistics = calculator.snapshot()
        assertEquals(5L, statistics.eventCount)
        assertEquals(21.25, statistics.meanIntervalMillis, 0.0001)
        assertEquals(10.0, statistics.minimumIntervalMillis, 0.0001)
        assertEquals(40.0, statistics.maximumIntervalMillis, 0.0001)
        assertEquals(1_000.0 / 21.25, statistics.effectiveFrequencyHz, 0.0001)
    }

    @Test
    fun `each sensor calculator keeps independent statistics`() {
        val accelerometer = SensorStatisticsCalculator()
        val gyroscope = SensorStatisticsCalculator()
        accelerometer.addTimestamp(0L)
        accelerometer.addTimestamp(10_000_000L)
        gyroscope.addTimestamp(0L)
        gyroscope.addTimestamp(20_000_000L)

        assertEquals(100.0, accelerometer.snapshot().effectiveFrequencyHz, 0.0001)
        assertEquals(50.0, gyroscope.snapshot().effectiveFrequencyHz, 0.0001)
        assertEquals(10.0, accelerometer.snapshot().maximumIntervalMillis, 0.0001)
        assertEquals(20.0, gyroscope.snapshot().maximumIntervalMillis, 0.0001)
    }

    @Test
    fun `sensor delay metadata converts microseconds to the correct frequency bounds`() {
        val descriptor = SensorDescriptor(
            type = MotionSensorType.ACCELEROMETER,
            available = true,
            minimumDelayUs = 5_000,
            maximumDelayUs = 1_000_000,
        )

        assertEquals(200.0, descriptor.highestPermittedFrequencyHz!!, 0.0001)
        assertEquals(1.0, descriptor.lowestSupportedFrequencyHz!!, 0.0001)
    }
}
