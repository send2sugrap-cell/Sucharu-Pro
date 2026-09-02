package com.sucharu.sucharupro.vendorportal

import com.sucharu.sucharupro.data.api.model.*
import com.sucharu.sucharupro.domain.model.common.Money
import com.sucharu.sucharupro.domain.model.vendor.*
import com.sucharu.sucharupro.domain.model.vendorportal.*
import org.junit.Assert.*
import org.junit.Test
import java.math.BigDecimal

class VendorPortalQualityUiTest {

    @Test
    fun testMappingQualityCaseModelToUiDto() {
        val case = VendorPortalQualityCase(
            caseId = "QC-UI-01",
            tenantId = "TENANT-001",
            projectId = "PRJ-001",
            vendorId = "VND-001",
            caseNumber = "QC-2026-001",
            status = VendorPortalQualityCaseStatus.RESPONSE_REQUIRED,
            title = "Dimensional variation",
            description = "Diameter off by 0.5mm",
            severity = VendorDefectSeverity.HIGH,
            acknowledgedAt = 1700000000000L,
            acknowledgedBy = "VENDOR_USER"
        )

        val dto = case.toDto()
        assertEquals("QC-UI-01", dto.caseId)
        assertEquals("RESPONSE_REQUIRED", dto.status)
        assertEquals("HIGH", dto.severity)
        assertEquals("QC-2026-001", dto.caseNumber)
        assertEquals(1700000000000L, dto.acknowledgedAt)
    }

    @Test
    fun testMappingCapaPlanAndActionsToUiDto() {
        val action = VendorPortalCapaAction(
            actionId = "ACT-01",
            capaId = "CAPA-01",
            tenantId = "TENANT-001",
            projectId = "PRJ-001",
            actionNumber = 1,
            actionType = VendorPortalCapaActionType.CORRECTIVE,
            description = "Tooling overhaul",
            owner = "Lead Machinist",
            targetDate = 1700000000000L,
            status = VendorPortalCapaActionStatus.COMPLETED
        )

        val capa = VendorPortalCapaPlan(
            capaId = "CAPA-01",
            tenantId = "TENANT-001",
            projectId = "PRJ-001",
            vendorId = "VND-001",
            capaNumber = "CAPA-2026-001",
            status = VendorPortalCapaStatus.APPROVED,
            priority = VendorPortalQualityPriority.HIGH,
            title = "Tooling CAPA",
            rootCause = "Tool fatigue",
            correctiveAction = "Replaced tool",
            preventiveAction = "Preventive tool change cycle",
            responsiblePerson = "Machinist",
            targetCompletionDate = 1700000000000L,
            actions = listOf(action)
        )

        val dto = capa.toDto()
        assertEquals("CAPA-01", dto.capaId)
        assertEquals("APPROVED", dto.status)
        assertEquals("HIGH", dto.priority)
        assertEquals(1, dto.actions.size)
        assertEquals("COMPLETED", dto.actions[0].status)
    }

    @Test
    fun testMappingDisputeModelToUiDto() {
        val dispute = VendorPortalDisputeSummary(
            disputeId = "DISP-01",
            tenantId = "TENANT-001",
            projectId = "PRJ-001",
            vendorId = "VND-001",
            disputeReference = "DISP-2026-001",
            sourceType = "REJECTION",
            sourceId = "REJ-01",
            disputeType = VendorDisputeType.SPECIFICATION,
            priority = VendorPortalQualityPriority.URGENT,
            status = VendorPortalDisputeStatus.RESOLUTION_PROPOSED,
            subject = "Specification mismatch",
            description = "Spec revised after order placement",
            requestedResolution = VendorPortalResolutionType.PRICE_ADJUSTMENT,
            disputedQuantity = BigDecimal("100"),
            disputedAmount = Money(BigDecimal("5000.00")),
            raisedBy = "VENDOR_USER"
        )

        val dto = dispute.toDto()
        assertEquals("DISP-01", dto.disputeId)
        assertEquals("SPECIFICATION", dto.disputeType)
        assertEquals("URGENT", dto.priority)
        assertEquals("RESOLUTION_PROPOSED", dto.status)
        assertEquals(5000.0, dto.disputedAmount, 0.001)
    }

    @Test
    fun testMappingWorkspaceModelToUiDto() {
        val kpi = VendorPortalQualityKpiSummary(
            vendorId = "VND-001",
            openQualityCases = 2,
            pendingVendorResponses = 1,
            activeCapaCount = 1,
            overdueCapaCount = 0,
            openDisputesCount = 1,
            totalInspectionsCount = 10,
            totalRejectionsCount = 1,
            totalRejectedQuantity = BigDecimal("5"),
            totalAcceptedQuantity = BigDecimal("95"),
            qualityPassRate = BigDecimal("95.00")
        )

        val ws = VendorPortalQualityWorkspace(
            kpiSummary = kpi,
            recentCases = emptyList(),
            recentInspections = emptyList(),
            recentRejections = emptyList(),
            activeCapas = emptyList(),
            activeDisputes = emptyList()
        )

        val dto = ws.toDto()
        assertEquals(2, dto.kpiSummary.openQualityCases)
        assertEquals(95.0, dto.kpiSummary.qualityPassRate, 0.001)
        assertEquals(0, dto.recentCases.size)
    }
}
