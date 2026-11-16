package com.sta.datacollector.ui.sessions

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class SessionInputValidatorTest {
    @Test
    fun `accepts decimal comma and positive integer repetitions`() {
        val result = SessionInputValidator.validate(" Sentadilla ", "82,5", "6")

        assertNull(result.exerciseError)
        assertNull(result.loadError)
        assertNull(result.repetitionsError)
        assertEquals("Sentadilla", result.value?.exercise)
        assertEquals(82.5f, result.value?.loadKg)
        assertEquals(6, result.value?.repetitions)
    }

    @Test
    fun `rejects empty exercise negative or non finite load and invalid repetitions`() {
        val negative = SessionInputValidator.validate("", "-1", "0")
        assertNotNull(negative.exerciseError)
        assertNotNull(negative.loadError)
        assertNotNull(negative.repetitionsError)

        listOf("NaN", "Infinity", "-Infinity").forEach { invalidLoad ->
            assertNotNull(SessionInputValidator.validate("Press", invalidLoad, "8").loadError)
        }
        assertNotNull(SessionInputValidator.validate("Press", "10", "2.5").repetitionsError)
    }
}
