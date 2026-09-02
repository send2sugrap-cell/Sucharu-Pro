package com.sucharu.sucharupro.domain.model.returns

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Domain model validation unit tests for Return Governance Exceptions (Module 11 Step 06 Chunk 01).
 */
class ReturnGovernanceModelTest {

    @Test
    fun `test valid ReturnException construction`() {
        val exception = ReturnException(
            exceptionId = "EX-01",
            projectId = "PRJ-01",
            returnId = "RET-01",
            exceptionType = ReturnExceptionType.AGING_UNINSPECTED,
            severity = "HIGH",
            status = ReturnExceptionStatus.OPEN,
            description = "Return RET-01 uninspected for over 48h",
            idempotencyKey = "IDEMP-EX-01"
        )
        assertEquals("EX-01", exception.exceptionId)
        assertEquals("PRJ-01", exception.projectId)
        assertEquals(ReturnExceptionType.AGING_UNINSPECTED, exception.exceptionType)
        assertEquals(ReturnExceptionStatus.OPEN, exception.status)
        assertFalse(exception.status.isTerminal)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `test ReturnException blank exceptionId throws`() {
        ReturnException(
            exceptionId = "",
            projectId = "PRJ-01",
            exceptionType = ReturnExceptionType.HIGH_VALUE_RETURN,
            description = "High value alert",
            idempotencyKey = "KEY-01"
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun `test ReturnException acknowledged status requires acknowledgedBy`() {
        ReturnException(
            exceptionId = "EX-02",
            projectId = "PRJ-01",
            exceptionType = ReturnExceptionType.UNSETTLED_PROCESSED,
            status = ReturnExceptionStatus.ACKNOWLEDGED,
            description = "Unsettled processed return",
            acknowledgedBy = null,
            idempotencyKey = "KEY-02"
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun `test ReturnException resolved status requires resolvedBy`() {
        ReturnException(
            exceptionId = "EX-03",
            projectId = "PRJ-01",
            exceptionType = ReturnExceptionType.SLA_BREACH,
            status = ReturnExceptionStatus.RESOLVED,
            description = "SLA breach resolved",
            resolvedBy = null,
            idempotencyKey = "KEY-03"
        )
    }

    @Test
    fun `test ReturnExceptionStatus terminal semantics`() {
        assertFalse(ReturnExceptionStatus.OPEN.isTerminal)
        assertFalse(ReturnExceptionStatus.ACKNOWLEDGED.isTerminal)
        assertTrue(ReturnExceptionStatus.RESOLVED.isTerminal)
        assertTrue(ReturnExceptionStatus.DISMISSED.isTerminal)
    }
}
