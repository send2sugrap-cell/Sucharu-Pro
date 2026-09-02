package com.sucharu.sucharupro.domain.validation

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.common.Money
import com.sucharu.sucharupro.domain.model.returns.ReturnAnalyticsPeriod
import com.sucharu.sucharupro.domain.model.returns.ReturnAnalyticsSummary
import com.sucharu.sucharupro.domain.model.returns.ReturnException
import com.sucharu.sucharupro.domain.model.returns.ReturnExceptionStatus
import com.sucharu.sucharupro.domain.model.returns.ReturnExceptionType
import com.sucharu.sucharupro.domain.validation.returns.ReturnAnalyticsValidator
import com.sucharu.sucharupro.domain.validation.returns.ReturnGovernanceValidator
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Domain validation unit tests for Return Analytics & Governance (Module 11 Step 06 Chunk 03).
 */
class ReturnAnalyticsValidationTest {

    @Test
    fun `test validateAnalyticsRequest valid`() {
        val res = ReturnAnalyticsValidator.validateAnalyticsRequest("PRJ-01", ReturnAnalyticsPeriod.THIS_MONTH)
        assertTrue(res is DomainResult.Success)
    }

    @Test
    fun `test validateAnalyticsRequest blank project throws error`() {
        val res = ReturnAnalyticsValidator.validateAnalyticsRequest("  ", ReturnAnalyticsPeriod.TODAY)
        assertTrue(res is DomainResult.Error)
    }

    @Test
    fun `test validateStateTransition from OPEN to ACKNOWLEDGED succeeds`() {
        val ex = createException(ReturnExceptionStatus.OPEN)
        val res = ReturnGovernanceValidator.validateStateTransition(ex, ReturnExceptionStatus.ACKNOWLEDGED)
        assertTrue(res is DomainResult.Success)
    }

    @Test
    fun `test validateStateTransition from RESOLVED to OPEN fails`() {
        val ex = createException(ReturnExceptionStatus.RESOLVED)
        val res = ReturnGovernanceValidator.validateStateTransition(ex, ReturnExceptionStatus.OPEN)
        assertTrue(res is DomainResult.Error)
    }

    @Test
    fun `test validateResolution blank notes fails`() {
        val res = ReturnGovernanceValidator.validateResolution("ACTOR-1", "   ")
        assertTrue(res is DomainResult.Error)
    }

    private fun createException(status: ReturnExceptionStatus) = ReturnException(
        exceptionId = "EX-01",
        projectId = "PRJ-01",
        exceptionType = ReturnExceptionType.AGING_UNINSPECTED,
        status = status,
        description = "Test exception",
        resolvedBy = if (status.isTerminal) "ADMIN" else null,
        idempotencyKey = "KEY-01"
    )
}
