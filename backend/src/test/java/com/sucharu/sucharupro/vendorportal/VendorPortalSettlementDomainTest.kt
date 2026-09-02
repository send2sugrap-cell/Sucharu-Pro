package com.sucharu.sucharupro.vendorportal

import com.sucharu.sucharupro.domain.model.common.Money
import com.sucharu.sucharupro.domain.model.vendor.SettlementMethod
import com.sucharu.sucharupro.domain.model.vendor.VendorSettlementStatus
import com.sucharu.sucharupro.domain.model.vendorportal.*
import org.junit.Assert.*
import org.junit.Test
import java.math.BigDecimal

class VendorPortalSettlementDomainTest {

    @Test
    fun testSettlementSummaryCreationAndMathInvariants() {
        val summary = VendorPortalSettlementSummary(
            settlementId = "SETTL-001",
            tenantId = "TENANT-001",
            projectId = "PRJ-001",
            vendorId = "VND-001",
            settlementNumber = "SETTL-2026-001",
            settlementDate = 1756291200000L,
            currency = "BDT",
            grossAmount = Money(BigDecimal("150000.00")),
            deductions = Money(BigDecimal("5000.00")),
            credits = Money(BigDecimal("2000.00")),
            netPayable = Money(BigDecimal("147000.00")),
            status = VendorSettlementStatus.SETTLED,
            settlementMethod = SettlementMethod.BANK_TRANSFER,
            maskedPaymentReference = "****4821",
            notes = "Monthly consolidated settlement",
            approvedAt = 1756291300000L,
            settledAt = 1756291400000L,
            allocationCount = 3,
            acknowledgementStatus = VendorPortalSettlementViewStatus.ACKNOWLEDGED
        )

        assertEquals("SETTL-001", summary.settlementId)
        assertEquals("****4821", summary.maskedPaymentReference)
        assertEquals(VendorSettlementStatus.SETTLED, summary.status)
        assertEquals(VendorPortalSettlementViewStatus.ACKNOWLEDGED, summary.acknowledgementStatus)
        assertEquals(3, summary.allocationCount)
        assertTrue(summary.netPayable.amount > BigDecimal.ZERO)
    }

    @Test
    fun testReconciliationCaseVarianceCalculation() {
        val claimed = Money(BigDecimal("50000.00"))
        val system = Money(BigDecimal("42000.00"))
        val variance = Money((claimed.amount - system.amount).abs())

        val case = VendorPortalReconciliationCase(
            caseId = "REC-001",
            tenantId = "TENANT-001",
            projectId = "PRJ-001",
            vendorId = "VND-001",
            caseNumber = "REC-1001",
            subject = "Invoice #1042 discrepancy",
            status = VendorPortalReconciliationCaseStatus.OPEN,
            claimedAmount = claimed,
            systemAmount = system,
            varianceAmount = variance,
            currency = "BDT",
            notes = "Claiming additional delivery fees",
            createdBy = "vendor_user"
        )

        assertEquals(BigDecimal("8000.00"), case.varianceAmount.amount)
        assertEquals(VendorPortalReconciliationCaseStatus.OPEN, case.status)
    }

    @Test
    fun testFinancialDisputeCreationAndStatusEnum() {
        val dispute = VendorPortalFinancialDispute(
            disputeId = "DISP-001",
            tenantId = "TENANT-001",
            projectId = "PRJ-001",
            vendorId = "VND-001",
            disputeNumber = "DISP-501",
            category = "UNEXPECTED_DEDUCTION",
            priority = "HIGH",
            status = VendorPortalFinancialDisputeStatus.SUBMITTED,
            disputedAmount = Money(BigDecimal("12500.00")),
            reason = "Late delivery penalty applied incorrectly despite force majeure notice",
            createdBy = "vendor_admin"
        )

        assertEquals(VendorPortalFinancialDisputeStatus.SUBMITTED, dispute.status)
        assertEquals("HIGH", dispute.priority)
        assertEquals(BigDecimal("12500.00"), dispute.disputedAmount.amount)
        assertNull(dispute.resolvedBy)
        assertNull(dispute.resolvedAt)
    }

    @Test
    fun testAcknowledgementIdempotencyModel() {
        val ack = VendorPortalSettlementAcknowledgement(
            acknowledgementId = "ACK-001",
            settlementId = "SETTL-001",
            tenantId = "TENANT-001",
            projectId = "PRJ-001",
            vendorId = "VND-001",
            acknowledgedBy = "vendor_user",
            status = VendorPortalSettlementViewStatus.ACKNOWLEDGED_WITH_DISCREPANCY,
            idempotencyKey = "IDEM-SETTL-001-VND001",
            discrepancyFlag = true,
            discrepancyNotes = "Amount credited is less than agreed invoice total"
        )

        assertEquals("IDEM-SETTL-001-VND001", ack.idempotencyKey)
        assertTrue(ack.discrepancyFlag)
        assertEquals(VendorPortalSettlementViewStatus.ACKNOWLEDGED_WITH_DISCREPANCY, ack.status)
    }
}
