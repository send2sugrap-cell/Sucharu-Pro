package com.sucharu.sucharupro.data.event

import com.sucharu.sucharupro.data.event.model.RetryConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RetryConfigTest {

    @Test
    fun test01_exponentialDelayCalculation() {
        val config = RetryConfig(
            maxAttempts = 5,
            initialBackoffMs = 1000L,
            maxBackoffMs = 30000L,
            multiplier = 2.0,
            jitterFactor = 0.0
        )

        assertEquals(0L, config.calculateDelayMs(0))
        assertEquals(1000L, config.calculateDelayMs(1))
        assertEquals(2000L, config.calculateDelayMs(2))
        assertEquals(4000L, config.calculateDelayMs(3))
        assertEquals(8000L, config.calculateDelayMs(4))
        assertEquals(16000L, config.calculateDelayMs(5))
        assertEquals(30000L, config.calculateDelayMs(6)) // Capped at maxBackoffMs
    }

    @Test
    fun test02_jitterApplication() {
        val config = RetryConfig(
            maxAttempts = 5,
            initialBackoffMs = 1000L,
            maxBackoffMs = 30000L,
            multiplier = 2.0,
            jitterFactor = 0.2
        )

        val delayWithZeroJitter = config.calculateDelayMs(1, randomFactor = 0.0)
        val delayWithHalfJitter = config.calculateDelayMs(1, randomFactor = 0.5)
        val delayWithFullJitter = config.calculateDelayMs(1, randomFactor = 1.0)

        assertEquals(1000L, delayWithZeroJitter)
        assertEquals(1100L, delayWithHalfJitter)
        assertEquals(1200L, delayWithFullJitter)
        assertTrue(delayWithFullJitter > delayWithZeroJitter)
    }

    @Test(expected = IllegalArgumentException::class)
    fun test03_invalidConfig_rejectsZeroMaxAttempts() {
        RetryConfig(maxAttempts = 0)
    }
}
