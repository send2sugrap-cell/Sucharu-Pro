package com.sucharu.sucharupro.businessfinancialadjustment

import com.sucharu.sucharupro.domain.model.businessfinancialadjustment.*
import org.junit.Assert.*
import org.junit.Test
import java.math.BigDecimal

class BusinessFinancialAdjustmentDomainTest {

    @Test
    fun testAdjustmentStatusTransitions() {
        val draft = AdjustmentStatus.DRAFT
        assertFalse(draft.isTerminal)
        assertTrue(draft.isEditable)
        assertFalse(draft.canBeReviewed)
        assertFalse(draft.canBeApproved)
        assertFalse(draft.canBePosted)
        assertFalse(draft.canBeReversed)

        val submitted = AdjustmentStatus.SUBMITTED
        assertTrue(submitted.canBeReviewed)
        assertTrue(submitted.canBeApproved)

        val underReview = AdjustmentStatus.UNDER_REVIEW
        assertTrue(underReview.canBeApproved)

        val approved = AdjustmentStatus.APPROVED
        assertTrue(approved.canBePosted)

        val posted = AdjustmentStatus.POSTED
        assertTrue(posted.canBeReversed)

        val reversed = AdjustmentStatus.REVERSED
        assertTrue(reversed.isTerminal)
    }

    @Test
    fun testRefundAndWriteOffStatuses() {
        val refundReq = RefundStatus.REQUESTED
        assertFalse(refundReq.isTerminal)
        assertTrue(refundReq.canBeApproved)

        val refundApproved = RefundStatus.APPROVED
        assertTrue(refundApproved.canBePosted)

        val woReq = WriteOffStatus.REQUESTED
        assertTrue(woReq.canBeApproved)

        val woApp = WriteOffStatus.APPROVED
        assertTrue(woApp.canBePosted)
    }

    @Test
    fun testAdjustmentTypeCategories() {
        assertEquals("EXPENSE", BusinessFinancialAdjustmentType.EXPENSE_CORRECTION.category)
        assertEquals("PAYABLE", BusinessFinancialAdjustmentType.VENDOR_PAYABLE_ADJUSTMENT.category)
        assertEquals("REFUND", BusinessFinancialAdjustmentType.CUSTOMER_REFUND.category)
        assertEquals("WRITE_OFF", BusinessFinancialAdjustmentType.BAD_DEBT_WRITE_OFF.category)
        assertEquals("REVERSAL", BusinessFinancialAdjustmentType.REVERSAL_REQUEST.category)
    }

    @Test
    fun testBusinessFinancialAdjustmentInstantiation() {
        val adj = BusinessFinancialAdjustment(
            id = "ADJ-001",
            tenantId = "TENANT-1",
            projectId = "PRJ-1",
            adjustmentNumber = "ADJ-2026-0001",
            adjustmentType = BusinessFinancialAdjustmentType.EXPENSE_CORRECTION,
            sourceType = AdjustmentSourceType.BUSINESS_EXPENSE,
            sourceId = "EXP-101",
            originalAmount = BigDecimal("10000.0000"),
            adjustmentAmount = BigDecimal("-1000.0000"),
            effectiveAmount = BigDecimal("9000.0000"),
            currency = "BDT",
            reason = "Vendor discount applied post-invoice",
            justification = "Approved by accounts manager with credit memo attachment",
            periodId = "PER-2026-08",
            createdBy = "USR-ACCOUNTS"
        )

        assertEquals("ADJ-001", adj.id)
        assertEquals(BigDecimal("9000.0000"), adj.effectiveAmount)
        assertEquals(AdjustmentStatus.DRAFT, adj.status)
    }
}
