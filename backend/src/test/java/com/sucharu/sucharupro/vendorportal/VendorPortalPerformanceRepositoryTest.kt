package com.sucharu.sucharupro.vendorportal

import com.sucharu.sucharupro.data.datasource.FakeVendorPortalPerformanceComplianceDataSource
import com.sucharu.sucharupro.data.repository.VendorPortalPerformanceComplianceRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.vendorportal.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class VendorPortalPerformanceRepositoryTest {

    private lateinit var repo: VendorPortalPerformanceComplianceRepositoryImpl
    private val tenantId = "TENANT-001"
    private val projectId = "PRJ-001"
    private val vendorId = "VND-001"

    @Before
    fun setup() {
        val ds = FakeVendorPortalPerformanceComplianceDataSource()
        repo = VendorPortalPerformanceComplianceRepositoryImpl(ds)
    }

    @Test
    fun testEvaluationResponseCrud() = runBlocking {
        val response = VendorPortalEvaluationResponse(
            responseId = "VPER-01",
            evaluationId = "EV-01",
            tenantId = tenantId,
            projectId = projectId,
            vendorId = vendorId,
            responseType = VendorPortalEvaluationResponseType.FORMAL_RESPONSE,
            subject = "Evaluation response",
            remarks = "Clarification regarding shipment #104",
            status = VendorPortalEvaluationResponseStatus.SUBMITTED,
            submittedBy = "USER-01"
        )
        val saveRes = repo.saveEvaluationResponse(response)
        assertTrue(saveRes is DomainResult.Success)

        val listRes = repo.listEvaluationResponses(tenantId, projectId, vendorId, "EV-01")
        assertTrue(listRes is DomainResult.Success)
        val list = (listRes as DomainResult.Success).data
        assertEquals(1, list.size)
        assertEquals("VPER-01", list.first().responseId)
    }

    @Test
    fun testComplianceEvidenceCrud() = runBlocking {
        val evidence = VendorPortalComplianceEvidence(
            evidenceId = "VPCE-01",
            recordId = "REC-01",
            requirementId = "REQ-01",
            actionId = null,
            tenantId = tenantId,
            projectId = projectId,
            vendorId = vendorId,
            evidenceType = VendorPortalComplianceEvidenceType.AUDIT_REPORT,
            fileName = "audit.pdf",
            fileUrl = "https://files.com/audit.pdf",
            fileSizeBytes = 4096L,
            uploadedBy = "USER-01"
        )
        val saveRes = repo.saveComplianceEvidence(evidence)
        assertTrue(saveRes is DomainResult.Success)

        val listRes = repo.listComplianceEvidence(tenantId, projectId, vendorId, "REC-01", null)
        assertTrue(listRes is DomainResult.Success)
        val list = (listRes as DomainResult.Success).data
        assertEquals(1, list.size)
        assertEquals("audit.pdf", list.first().fileName)
    }

    @Test
    fun testCorrectiveActionResponseCrud() = runBlocking {
        val response = VendorPortalCorrectiveActionResponse(
            responseId = "VPCAR-01",
            actionId = "CA-01",
            tenantId = tenantId,
            projectId = projectId,
            vendorId = vendorId,
            remediationNotes = "Machine overhauled",
            progressPercentage = 100.0,
            isCompletionRequest = true,
            status = VendorPortalRemediationStatus.COMPLETED_PENDING_VERIFICATION,
            submittedBy = "USER-01"
        )
        val saveRes = repo.saveCorrectiveActionResponse(response)
        assertTrue(saveRes is DomainResult.Success)

        val listRes = repo.listCorrectiveActionResponses(tenantId, projectId, vendorId, "CA-01")
        assertTrue(listRes is DomainResult.Success)
        val list = (listRes as DomainResult.Success).data
        assertEquals(1, list.size)
        assertEquals(100.0, list.first().progressPercentage, 0.001)
    }

    @Test
    fun testAuditRecordAndQuery() = runBlocking {
        val activity = VendorPortalPerformanceComplianceActivity(
            activityId = "ACT-01",
            tenantId = tenantId,
            projectId = projectId,
            vendorId = vendorId,
            eventType = VendorPortalPerformanceComplianceAuditEventType.EVALUATION_ACKNOWLEDGED,
            entityType = "EVALUATION",
            entityId = "EV-01",
            actorId = "USER-01",
            description = "Vendor acknowledged evaluation score"
        )
        repo.recordAudit(activity)

        val listRes = repo.listAuditEvents(tenantId, projectId, vendorId, "EVALUATION", "EV-01")
        assertTrue(listRes is DomainResult.Success)
        assertEquals(1, (listRes as DomainResult.Success).data.size)
    }
}
