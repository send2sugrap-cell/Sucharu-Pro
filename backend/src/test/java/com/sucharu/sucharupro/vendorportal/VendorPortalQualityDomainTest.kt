package com.sucharu.sucharupro.vendorportal

import com.sucharu.sucharupro.domain.model.common.Money
import com.sucharu.sucharupro.domain.model.vendor.*
import com.sucharu.sucharupro.domain.model.vendorportal.*
import org.junit.Assert.*
import org.junit.Test
import java.math.BigDecimal

class VendorPortalQualityDomainTest {

    @Test
    fun testQualityCaseModelDefaults() {
        val case = VendorPortalQualityCase(
            caseId = "QC-001",
            tenantId = "TENANT-001",
            projectId = "PRJ-001",
            vendorId = "VND-001",
            caseNumber = "QC-2026-001",
            title = "Surface defect on beam shipment",
            description = "Micro-cracks detected during receiving inspection",
            severity = VendorDefectSeverity.HIGH
        )

        assertEquals("QC-001", case.caseId)
        assertEquals(VendorPortalQualityCaseStatus.OPEN, case.status)
        assertEquals(VendorDefectSeverity.HIGH, case.severity)
        assertNull(case.acknowledgedAt)
        assertNull(case.closedAt)
    }

    @Test
    fun testCapaPlanModelAndActions() {
        val action = VendorPortalCapaAction(
            actionId = "ACT-01",
            capaId = "CAPA-001",
            tenantId = "TENANT-001",
            projectId = "PRJ-001",
            actionNumber = 1,
            actionType = VendorPortalCapaActionType.CORRECTIVE,
            description = "Recalibrate ultrasonic test sensor",
            owner = "John Quality",
            targetDate = System.currentTimeMillis() + 86400000L
        )

        val plan = VendorPortalCapaPlan(
            capaId = "CAPA-001",
            tenantId = "TENANT-001",
            projectId = "PRJ-001",
            vendorId = "VND-001",
            capaNumber = "CAPA-2026-001",
            title = "Sensor Calibration CAPA",
            rootCause = "Drift in optical temperature sensor",
            correctiveAction = "Replaced optical head",
            preventiveAction = "Weekly calibration schedule",
            responsiblePerson = "John Quality",
            targetCompletionDate = System.currentTimeMillis() + 86400000L * 14,
            actions = listOf(action)
        )

        assertEquals("CAPA-001", plan.capaId)
        assertEquals(VendorPortalCapaStatus.DRAFT, plan.status)
        assertEquals(1, plan.actions.size)
        assertEquals(VendorPortalCapaActionStatus.OPEN, plan.actions[0].status)
    }

    @Test
    fun testDisputeModelAndResolutionResponse() {
        val dispute = VendorPortalDisputeSummary(
            disputeId = "DISP-001",
            tenantId = "TENANT-001",
            projectId = "PRJ-001",
            vendorId = "VND-001",
            disputeReference = "DISP-2026-001",
            sourceType = "REJECTION",
            sourceId = "REJ-001",
            disputeType = VendorDisputeType.QUALITY,
            priority = VendorPortalQualityPriority.HIGH,
            status = VendorPortalDisputeStatus.OPEN,
            subject = "Dispute regarding dimensional tolerance rejection",
            description = "The parts were measured with calibrated CMM within drawing spec ISO 2768-m",
            requestedResolution = VendorPortalResolutionType.PARTIAL_ACCEPTANCE,
            disputedQuantity = BigDecimal("25"),
            disputedAmount = Money(BigDecimal("1250.00")),
            raisedBy = "VENDOR_USER_01"
        )

        assertEquals("DISP-001", dispute.disputeId)
        assertEquals(VendorPortalDisputeStatus.OPEN, dispute.status)
        assertEquals(BigDecimal("25"), dispute.disputedQuantity)
        assertEquals(Money(BigDecimal("1250.00")), dispute.disputedAmount)
    }

    @Test
    fun testQualityEvidenceModel() {
        val evidence = VendorPortalQualityEvidence(
            evidenceId = "EVD-001",
            tenantId = "TENANT-001",
            projectId = "PRJ-001",
            vendorId = "VND-001",
            entityType = "CAPA",
            entityId = "CAPA-001",
            evidenceType = VendorPortalQualityEvidenceType.REPORT,
            filename = "lab_analysis_report.pdf",
            fileReference = "gs://sucharu-evidence/lab_report.pdf",
            sizeBytes = 204800L,
            checksum = "SHA256:abc123xyz",
            description = "Independent metallurgical test report",
            uploadedBy = "VENDOR_USER_01"
        )

        assertEquals("EVD-001", evidence.evidenceId)
        assertEquals(VendorPortalQualityEvidenceType.REPORT, evidence.evidenceType)
        assertEquals(204800L, evidence.sizeBytes)
    }
}
