package com.sucharu.sucharupro.vendorportal

import com.sucharu.sucharupro.data.api.model.*
import org.junit.Assert.*
import org.junit.Test

class VendorPortalWorkflowUiTest {

    @Test
    fun testWorkflowDtoAndSummaryMappingIntegrity() {
        val wfDto = VendorWorkflowDto(
            workflowId = "WF-UI-01",
            tenantId = "TENANT-001",
            projectId = "PRJ-001",
            vendorId = "VND-001",
            correlationId = "PO-101",
            workflowTitle = "Commercial Order #PO-101",
            currentStage = "PRODUCTION_IN_PROGRESS",
            status = "ACTIVE",
            slaStatus = "ON_TRACK",
            startedAt = 1756291200000L,
            createdAt = 1756291200000L,
            updatedAt = 1756291200000L
        )

        assertEquals("WF-UI-01", wfDto.workflowId)
        assertEquals("PRODUCTION_IN_PROGRESS", wfDto.currentStage)
        assertEquals("ACTIVE", wfDto.status)

        val summary = VendorWorkflowHubSummaryDto(
            vendorId = "VND-001",
            tenantId = "TENANT-001",
            projectId = "PRJ-001",
            totalActiveWorkflows = 5,
            completedWorkflows = 12,
            blockedWorkflows = 1,
            overdueWorkflows = 0,
            averageCycleTimeDays = 6.5,
            stageBreakdown = mapOf("PRODUCTION_IN_PROGRESS" to 3, "AWARDED" to 2),
            recentWorkflows = listOf(wfDto),
            urgentActions = listOf(
                VendorWorkflowNextActionDto(
                    actionId = "ACT-01",
                    workflowId = "WF-UI-01",
                    tenantId = "TENANT-001",
                    projectId = "PRJ-001",
                    vendorId = "VND-001",
                    actionType = "SUBMIT_ASN",
                    title = "Submit Advance Shipping Notice",
                    description = "Order PO-101 ready for dispatch",
                    requiredRole = "VENDOR_LOGISTICS",
                    priority = "HIGH",
                    createdAt = 1756291200000L,
                    updatedAt = 1756291200000L
                )
            )
        )

        assertEquals(5, summary.totalActiveWorkflows)
        assertEquals(1, summary.urgentActions.size)
        assertEquals("SUBMIT_ASN", summary.urgentActions[0].actionType)
    }
}
