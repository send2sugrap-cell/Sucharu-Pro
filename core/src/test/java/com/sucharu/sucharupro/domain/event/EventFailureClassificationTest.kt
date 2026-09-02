package com.sucharu.sucharupro.domain.event

import com.sucharu.sucharupro.domain.event.consumer.EventConsumerResult
import com.sucharu.sucharupro.domain.event.consumer.EventFailureClassification
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EventFailureClassificationTest {

    @Test
    fun test01_retryableAndNonRetryableFailureSeparation() {
        // Transient is retryable
        assertTrue(EventFailureClassification.TRANSIENT.isRetryable)

        // All permanent/security/validation/duplicate/stale errors are non-retryable
        assertFalse(EventFailureClassification.NON_RETRYABLE.isRetryable)
        assertFalse(EventFailureClassification.SECURITY.isRetryable)
        assertFalse(EventFailureClassification.VALIDATION.isRetryable)
        assertFalse(EventFailureClassification.DUPLICATE.isRetryable)
        assertFalse(EventFailureClassification.STALE_VERSION.isRetryable)
    }

    @Test
    fun test02_consumerResultProperties() {
        val success = EventConsumerResult.Success("Processed cleanly")
        val failure = EventConsumerResult.Failure("Network dropped", EventFailureClassification.TRANSIENT)
        val skipped = EventConsumerResult.Skipped("Duplicate", EventFailureClassification.DUPLICATE)

        assertTrue(success.isSuccess)
        assertFalse(success.isFailure)

        assertTrue(failure.isFailure)
        assertTrue(failure.isRetryable)
        assertEquals(EventFailureClassification.TRANSIENT, failure.classification)

        assertTrue(skipped.isSkipped)
        assertFalse(skipped.isFailure)
    }
}
