package com.sucharu.sucharupro.businessreconciliation

import com.sucharu.sucharupro.domain.model.businessreconciliation.*
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.validation.businessreconciliation.BusinessFinancialReconciliationValidators
import org.junit.Assert.*
import org.junit.Test
import java.math.BigDecimal

class BusinessFinancialReconciliationDomainTest {

    @Test
    fun testReconciliationRunStatusTransitions() {
        val created = ReconciliationRunStatus.CREATED
        assertFalse(created.isTerminal)
        assertFalse(created.canBeApproved)

        val completed = ReconciliationRunStatus.COMPLETED
        assertFalse(completed.isTerminal)
        assertTrue(completed.canBeApproved)

        val underReview = ReconciliationRunStatus.UNDER_REVIEW
        assertFalse(underReview.isTerminal)
        assertTrue(underReview.canBeApproved)

        val approved = ReconciliationRunStatus.APPROVED
        assertTrue(approved.isTerminal)
        assertFalse(approved.canBeApproved)
    }

    @Test
    fun testDiscrepancyStatusAndSeverity() {
        val open = DiscrepancyStatus.OPEN
        assertTrue(open.isOpenOrInvestigating)
        assertFalse(open.isClosed)

        val resolved = DiscrepancyStatus.RESOLVED
        assertFalse(resolved.isOpenOrInvestigating)
        assertTrue(resolved.isClosed)

        val waived = DiscrepancyStatus.WAIVED
        assertTrue(waived.isClosed)

        val critical = DiscrepancySeverity.CRITICAL
        assertTrue(critical.isBlockingForPeriodClose)

        val warning = DiscrepancySeverity.WARNING
        assertFalse(warning.isBlockingForPeriodClose)
    }

    @Test
    fun testValidatePrecisionAndCurrency() {
        val validAmount = BigDecimal("1234.5678")
        val precSuccess = BusinessFinancialReconciliationValidators.validatePrecision(validAmount)
        assertTrue(precSuccess is DomainResult.Success)

        val invalidAmount = BigDecimal("1234.56789")
        val precFail = BusinessFinancialReconciliationValidators.validatePrecision(invalidAmount)
        assertTrue(precFail is DomainResult.Error)

        val validCurr = BusinessFinancialReconciliationValidators.validateCurrency("BDT")
        assertTrue(validCurr is DomainResult.Success)

        val invalidCurr = BusinessFinancialReconciliationValidators.validateCurrency("INVALID")
        assertTrue(invalidCurr is DomainResult.Error)
    }

    @Test
    fun testValidateRunCreation() {
        val valid = BusinessFinancialReconciliationValidators.validateRunCreation(
            periodId = "PER-2026-08",
            runNumber = "REC-001",
            tenantId = "TENANT-001",
            projectId = "PRJ-001",
            createdBy = "USR-001"
        )
        assertTrue(valid is DomainResult.Success)

        val blankPeriod = BusinessFinancialReconciliationValidators.validateRunCreation(
            periodId = "",
            runNumber = "REC-001",
            tenantId = "TENANT-001",
            projectId = "PRJ-001",
            createdBy = "USR-001"
        )
        assertTrue(blankPeriod is DomainResult.Error)
    }
}
