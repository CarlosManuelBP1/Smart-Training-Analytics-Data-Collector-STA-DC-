package com.sta.datacollector.sensor

class SensorStatisticsCalculator {
    private var eventCount = 0L
    private var previousTimestampNanos: Long? = null
    private var validIntervalCount = 0L
    private var totalIntervalNanos = 0L
    private var minimumIntervalNanos = Long.MAX_VALUE
    private var maximumIntervalNanos = 0L

    fun addTimestamp(timestampNanos: Long) {
        eventCount++
        val previous = previousTimestampNanos
        if (previous == null) {
            previousTimestampNanos = timestampNanos
        } else {
            val interval = timestampNanos - previous
            if (interval > 0L) {
                validIntervalCount++
                totalIntervalNanos += interval
                minimumIntervalNanos = minOf(minimumIntervalNanos, interval)
                maximumIntervalNanos = maxOf(maximumIntervalNanos, interval)
                previousTimestampNanos = timestampNanos
            }
        }
    }

    fun snapshot(): SensorStatistics {
        val meanIntervalNanos = if (validIntervalCount == 0L) {
            0.0
        } else {
            totalIntervalNanos.toDouble() / validIntervalCount
        }
        return SensorStatistics(
            eventCount = eventCount,
            effectiveFrequencyHz = if (meanIntervalNanos == 0.0) {
                0.0
            } else {
                NANOS_PER_SECOND / meanIntervalNanos
            },
            meanIntervalMillis = meanIntervalNanos / NANOS_PER_MILLISECOND,
            minimumIntervalMillis = if (validIntervalCount == 0L) {
                0.0
            } else {
                minimumIntervalNanos / NANOS_PER_MILLISECOND
            },
            maximumIntervalMillis = maximumIntervalNanos / NANOS_PER_MILLISECOND,
        )
    }

    private companion object {
        const val NANOS_PER_SECOND = 1_000_000_000.0
        const val NANOS_PER_MILLISECOND = 1_000_000.0
    }
}
