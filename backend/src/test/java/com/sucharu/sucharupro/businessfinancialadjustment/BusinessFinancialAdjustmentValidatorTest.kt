package com.sucharu.sucharupro.businessfinancialadjustment

import com.sucharu.sucharupro.domain.model.businessfinancialadjustment.*
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.validation.businessfinancialadjustment.BusinessFinancialAdjustmentValidators
import org.junit.Assert.*
import org.junit.Test
import java.math.BigDecimal

class BusinessFinancialAdjustmentValidatorTest {

    @Test
    fun testValidateAdjustmentCreationSuccess() {
        val res = BusinessFinancialAdjustmentValidators.validateAdjustmentCreation(
            tenantId = "TENANT-1",
            projectId = "PRJ-1",
            adjustmentNumber = "ADJ-001",
            adjustmentType = BusinessFinancialAdjustmentType.EXPENSE_CORRECTION,
            sourceType = AdjustmentSourceType.BUSINESS_EXPENSE,
            sourceId = "EXP-1",
            originalAmount = BigDecimal("5000.0000"),
            adjustmentAmount = BigDecimal("-500.0000"),
            currency = "BDT",
            reason = "Invoice discrepancy correction",
            justification = "Vendor agreed on 10% rebate for bulk quantity",
            periodId = "PER-2026-08",
            createdBy = "USR-01"
        )
        assertTrue(res is DomainResult.Success)
    }

    @Test
    fun testValidateAdjustmentCreationValidationFailures() {
        // Zero adjustment amount
        val zeroAdj = BusinessFinancialAdjustmentValidators.validateAdjustmentCreation(
            tenantId = "TENANT-1",
            projectId = "PRJ-1",
            adjustmentNumber = "ADJ-001",
            adjustmentType = BusinessFinancialAdjustmentType.EXPENSE_CORRECTION,
            sourceType = AdjustmentSourceType.BUSINESS_EXPENSE,
            sourceId = "EXP-1",
            originalAmount = BigDecimal("5000.0000"),
            adjustmentAmount = BigDecimal.ZERO,
            currency = "BDT",
            reason = "Invoice discrepancy correction",
            justification = "Vendor agreed on 10% rebate for bulk quantity",
            periodId = "PER-2026-08",
            createdBy = "USR-01"
        )
        assertTrue(zeroAdj is DomainResult.Error)

        // Short justification (< 10 chars)
        val shortJust = BusinessFinancialAdjustmentValidators.validateAdjustmentCreation(
            tenantId = "TENANT-1",
            projectId = "PRJ-1",
            adjustmentNumber = "ADJ-001",
            adjustmentType = BusinessFinancialAdjustmentType.EXPENSE_CORRECTION,
            sourceType = AdjustmentSourceType.BUSINESS_EXPENSE,
            sourceId = "EXP-1",
            originalAmount = BigDecimal("5000.0000"),
            adjustmentAmount = BigDecimal("-500.0000"),
            currency = "BDT",
            reason = "Correction",
            justification = "Short",
            periodId = "PER-2026-08",
            createdBy = "USR-01"
        )
        assertTrue(shortJust is DomainResult.Error)
    }

    @Test
    fun testValidateSeparationOfDuties() {
        val sameUser = BusinessFinancialAdjustmentValidators.validateSeparationOfDuties(
            creatorId = "USR-001",
            actorId = "USR-001",
            actionName = "approve"
        )
        assertTrue(sameUser is DomainResult.Error)

        val diffUser = BusinessFinancialAdjustmentValidators.validateSeparationOfDuties(
            creatorId = "USR-001",
            actorId = "USR-002",
            actionName = "approve"
        )
        assertTrue(diffUser is DomainResult.Success)
    }

    @Test
    fun testValidateStateTransitions() {
        // Legal: DRAFT -> SUBMITTED
        val legal1 = BusinessFinancialAdjustmentValidators.validateAdjustmentStateTransition(
            AdjustmentStatus.DRAFT,
            AdjustmentStatus.SUBMITTED
        )
        assertTrue(legal1 is DomainResult.Success)

        // Illegal: DRAFT -> POSTED
        val illegal1 = BusinessFinancialAdjustmentValidators.validateAdjustmentStateTransition(
            AdjustmentStatus.DRAFT,
            AdjustmentStatus.POSTED
        )
        assertTrue(illegal1 is DomainResult.Error)

        // Legal: POSTED -> REVERSAL_REQUESTED
        val legal2 = BusinessFinancialAdjustmentValidators.validateAdjustmentStateTransition(
            AdjustmentStatus.POSTED,
            AdjustmentStatus.REVERSAL_REQUESTED
        )
        assertTrue(legal2 is DomainResult.Success)
    }
}
