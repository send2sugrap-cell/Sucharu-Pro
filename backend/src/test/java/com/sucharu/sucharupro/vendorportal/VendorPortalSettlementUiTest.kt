package com.sucharu.sucharupro.vendorportal

import com.sucharu.sucharupro.data.api.model.*
import org.junit.Assert.*
import org.junit.Test

class VendorPortalSettlementUiTest {

    @Test
    fun testWorkspaceDtoMappingForUiState() {
        val workspaceDto = VendorPortalFinancialWorkspaceDto(
            settlementOverview = listOf(
                VendorPortalSettlementSummaryDto(
                    settlementId = "SETTL-1",
                    tenantId = "TENANT-001",
                    projectId = "PRJ-001",
                    vendorId = "VND-1",
                    settlementNumber = "SETTL-2026-001",
                    settlementDate = 1756291200000L,
                    currency = "BDT",
                    grossAmount = 100000.0,
                    deductions = 0.0,
                    credits = 0.0,
                    netPayable = 100000.0,
                    status = "SETTLED",
                    settlementMethod = "BANK_TRANSFER",
                    maskedPaymentReference = "****5544",
                    allocationCount = 2,
                    acknowledgementStatus = "ACKNOWLEDGED"
                )
            ),
            outstandingBalance = 25000.0,
            pendingReconciliations = emptyList(),
            openDisputes = emptyList(),
            recentActivity = emptyList(),
            analytics = VendorPortalSettlementAnalyticsSummaryDto(
                vendorId = "VND-1",
                currency = "BDT",
                totalSettledAmount = 100000.0,
                totalOutstandingAmount = 25000.0,
                totalDisputedAmount = 0.0,
                totalReconciledAmount = 100000.0,
                activeDisputeCount = 0,
                pendingReconciliationCount = 0,
                averageSettlementCycleDays = 14.5,
                disputeResolutionRate = 100.0
            )
        )

        assertEquals(1, workspaceDto.settlementOverview.size)
        assertEquals(25000.0, workspaceDto.outstandingBalance, 0.001)
        assertEquals(14.5, workspaceDto.analytics.averageSettlementCycleDays, 0.001)
        assertEquals(100.0, workspaceDto.analytics.disputeResolutionRate, 0.001)
        assertEquals("****5544", workspaceDto.settlementOverview.first().maskedPaymentReference)
    }

    @Test
    fun testReconciliationCaseDtoDataIntegrity() {
        val caseDto = VendorPortalReconciliationCaseDto(
            caseId = "REC-1",
            tenantId = "TENANT-001",
            projectId = "PRJ-001",
            vendorId = "VND-1",
            caseNumber = "REC-101",
            subject = "Invoice rounding issue",
            status = "OPEN",
            claimedAmount = 5050.0,
            systemAmount = 5000.0,
            varianceAmount = 50.0,
            currency = "BDT",
            createdBy = "vendor_rep",
            createdAt = 1756291200000L,
            updatedAt = 1756291200000L
        )

        assertEquals(50.0, caseDto.varianceAmount, 0.001)
        assertEquals("OPEN", caseDto.status)
    }
}
