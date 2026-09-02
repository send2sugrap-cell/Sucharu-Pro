package com.sucharu.sucharupro.vendorportal

import com.sucharu.sucharupro.domain.model.vendor.*
import com.sucharu.sucharupro.domain.model.vendorportal.*
import org.junit.Assert.*
import org.junit.Test

class VendorPortalPerformanceDomainTest {

    @Test
    fun testPerformanceOverviewCreation() {
        val overview = VendorPortalPerformanceOverview(
            vendorId = "VND-001",
            overallScore = 88.5,
            rating = PerformanceRating.GOOD,
            riskLevel = ComplianceRiskLevel.LOW,
            onTimeDeliveryRate = 95.0,
            poFulfillmentRate = 98.0,
            defectRate = 0.5,
            qualityRating = "EXCELLENT",
            totalScorecards = 5,
            activeEvaluations = 1,
            openCorrectiveActions = 0,
            topStrengths = listOf("High OTD", "Low Defect"),
            improvementAreas = emptyList()
        )

        assertEquals("VND-001", overview.vendorId)
        assertEquals(88.5, overview.overallScore, 0.001)
        assertEquals(PerformanceRating.GOOD, overview.rating)
        assertEquals(ComplianceRiskLevel.LOW, overview.riskLevel)
        assertEquals(2, overview.topStrengths.size)
        assertEquals(0, overview.openCorrectiveActions)
    }

    @Test
    fun testComplianceEvidenceCreation() {
        val evidence = VendorPortalComplianceEvidence(
            evidenceId = "VPCE-001",
            recordId = "REC-001",
            requirementId = "REQ-001",
            actionId = null,
            tenantId = "TENANT-001",
            projectId = "PRJ-001",
            vendorId = "VND-001",
            evidenceType = VendorPortalComplianceEvidenceType.CERTIFICATE,
            fileName = "iso9001.pdf",
            fileUrl = "https://storage.sucharu.com/iso9001.pdf",
            fileSizeBytes = 1024L,
            uploadedBy = "USER-001"
        )

        assertEquals("VPCE-001", evidence.evidenceId)
        assertEquals(VendorPortalComplianceEvidenceType.CERTIFICATE, evidence.evidenceType)
        assertEquals("iso9001.pdf", evidence.fileName)
        assertEquals(1024L, evidence.fileSizeBytes)
    }

    @Test
    fun testCorrectiveActionResponseCreation() {
        val response = VendorPortalCorrectiveActionResponse(
            responseId = "VPCAR-001",
            actionId = "CA-001",
            tenantId = "TENANT-001",
            projectId = "PRJ-001",
            vendorId = "VND-001",
            remediationNotes = "Machine recalibration completed",
            progressPercentage = 100.0,
            isCompletionRequest = true,
            status = VendorPortalRemediationStatus.COMPLETED_PENDING_VERIFICATION,
            submittedBy = "USER-001"
        )

        assertEquals("VPCAR-001", response.responseId)
        assertEquals(100.0, response.progressPercentage, 0.001)
        assertTrue(response.isCompletionRequest)
        assertEquals(VendorPortalRemediationStatus.COMPLETED_PENDING_VERIFICATION, response.status)
    }
}
