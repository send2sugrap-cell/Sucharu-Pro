package com.sucharu.sucharupro.businesscostcontrol

import com.sucharu.sucharupro.domain.model.businesscostcontrol.*
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.validation.businesscostcontrol.BusinessCostControlValidators
import org.junit.Assert.*
import org.junit.Test
import java.math.BigDecimal
import java.math.RoundingMode

class BusinessCostCommitmentDomainTest {

    @Test
    fun testValidCommitmentCreation() {
        val res = BusinessCostControlValidators.validateCommitment(
            commitmentNumber = "CMT-2026-001",
            committedAmount = BigDecimal("150000.0000"),
            currency = "BDT",
            costCategoryId = "CAT-PAPER",
            description = "Bulk paper procurement for magazine print run",
            tenantId = "TENANT-001",
            projectId = "PRJ-001",
            createdBy = "USR-001"
        )
        assertTrue(res is DomainResult.Success)
    }

    @Test
    fun testNegativeOrZeroCommittedAmountFails() {
        val resZero = BusinessCostControlValidators.validateCommitment(
            commitmentNumber = "CMT-001",
            committedAmount = BigDecimal.ZERO,
            currency = "BDT",
            costCategoryId = "CAT-PAPER",
            description = "Test description",
            tenantId = "TENANT-001",
            projectId = "PRJ-001",
            createdBy = "USR-001"
        )
        assertTrue(resZero is DomainResult.Error)
        assertEquals("Committed amount must be strictly greater than zero.", (resZero as DomainResult.Error).message)

        val resNeg = BusinessCostControlValidators.validateCommitment(
            commitmentNumber = "CMT-001",
            committedAmount = BigDecimal("-50.0000"),
            currency = "BDT",
            costCategoryId = "CAT-PAPER",
            description = "Test description",
            tenantId = "TENANT-001",
            projectId = "PRJ-001",
            createdBy = "USR-001"
        )
        assertTrue(resNeg is DomainResult.Error)
    }

    @Test
    fun testInvalidCommitmentNumberOrDescriptionFails() {
        val resNum = BusinessCostControlValidators.validateCommitment(
            commitmentNumber = "A",
            committedAmount = BigDecimal("100.0000"),
            currency = "BDT",
            costCategoryId = "CAT-PAPER",
            description = "Valid description here",
            tenantId = "TENANT-001",
            projectId = "PRJ-001",
            createdBy = "USR-001"
        )
        assertTrue(resNum is DomainResult.Error)

        val resDesc = BusinessCostControlValidators.validateCommitment(
            commitmentNumber = "CMT-001",
            committedAmount = BigDecimal("100.0000"),
            currency = "BDT",
            costCategoryId = "CAT-PAPER",
            description = "No",
            tenantId = "TENANT-001",
            projectId = "PRJ-001",
            createdBy = "USR-001"
        )
        assertTrue(resDesc is DomainResult.Error)
    }

    @Test
    fun testCommitmentRemainingAmountCalculation() {
        val committed = BigDecimal("50000.0000")
        val consumed = BigDecimal("18500.0000")
        val commitment = BusinessCostCommitment(
            id = "CMT-01",
            tenantId = "TENANT-001",
            projectId = "PRJ-001",
            commitmentNumber = "CMT-01",
            costCategoryId = "CAT-PAPER",
            description = "Paper Purchase Order",
            committedAmount = committed,
            consumedAmount = consumed,
            remainingAmount = (committed - consumed).setScale(4, RoundingMode.HALF_UP),
            currency = "BDT",
            status = BusinessCostCommitmentStatus.PARTIALLY_CONSUMED,
            sourceType = BusinessCostCommitmentSourceType.PURCHASE_COMMITMENT,
            sourceId = "PO-1001",
            createdBy = "USR-01"
        )
        assertEquals(BigDecimal("31500.0000"), commitment.remainingAmount)
        assertEquals(BigDecimal("18500.0000"), commitment.consumedAmount)
        assertFalse(commitment.status.isTerminal)
        assertFalse(commitment.status.isEditable)
    }

    @Test
    fun testCommitmentStatusTerminalAndEditableProperties() {
        assertTrue(BusinessCostCommitmentStatus.DRAFT.isEditable)
        assertFalse(BusinessCostCommitmentStatus.SUBMITTED.isEditable)
        assertFalse(BusinessCostCommitmentStatus.APPROVED.isEditable)
        assertFalse(BusinessCostCommitmentStatus.ACTIVE.isEditable)

        assertTrue(BusinessCostCommitmentStatus.FULLY_CONSUMED.isTerminal)
        assertTrue(BusinessCostCommitmentStatus.CANCELLED.isTerminal)
        assertTrue(BusinessCostCommitmentStatus.EXPIRED.isTerminal)
        assertTrue(BusinessCostCommitmentStatus.CLOSED.isTerminal)
        assertFalse(BusinessCostCommitmentStatus.ACTIVE.isTerminal)
    }
}
