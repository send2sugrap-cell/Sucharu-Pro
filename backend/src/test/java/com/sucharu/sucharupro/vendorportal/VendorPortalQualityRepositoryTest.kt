package com.sucharu.sucharupro.vendorportal

import com.sucharu.sucharupro.data.datasource.FakeVendorPortalQualityDataSource
import com.sucharu.sucharupro.data.repository.VendorPortalQualityRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.common.Money
import com.sucharu.sucharupro.domain.model.vendor.VendorDisputeType
import com.sucharu.sucharupro.domain.model.vendorportal.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

class VendorPortalQualityRepositoryTest {

    private lateinit var repository: VendorPortalQualityRepositoryImpl

    @Before
    fun setUp() {
        val ds = FakeVendorPortalQualityDataSource()
        repository = VendorPortalQualityRepositoryImpl(ds)
    }

    @Test
    fun testQualityCaseCrud() {
        runBlocking {
            val case = VendorPortalQualityCase(
                caseId = "CASE-01",
                tenantId = "TENANT-1",
                projectId = "PRJ-1",
                vendorId = "VND-1",
                caseNumber = "QC-001",
                title = "Paint peeling",
                description = "Paint thickness below minimum spec"
            )

            val saved = repository.saveQualityCase(case)
            assertTrue(saved is DomainResult.Success)

            val fetched = repository.findQualityCaseById("TENANT-1", "PRJ-1", "VND-1", "CASE-01")
            assertTrue(fetched is DomainResult.Success)
            assertEquals("Paint peeling", (fetched as DomainResult.Success).data?.title)

            val list = repository.listQualityCases("TENANT-1", "PRJ-1", "VND-1")
            assertTrue(list is DomainResult.Success)
            assertEquals(1, (list as DomainResult.Success).data.size)
        }
    }

    @Test
    fun testCapaPlanAndActionsCrud() {
        runBlocking {
            val plan = VendorPortalCapaPlan(
                capaId = "CAPA-01",
                tenantId = "TENANT-1",
                projectId = "PRJ-1",
                vendorId = "VND-1",
                capaNumber = "CAPA-001",
                title = "Paint curing CAPA",
                rootCause = "Oven temperature fluctuation",
                correctiveAction = "Replaced burner thermocouple",
                preventiveAction = "Installed digital chart recorder",
                responsiblePerson = "Maintenance Lead",
                targetCompletionDate = System.currentTimeMillis() + 86400000L
            )

            repository.saveCapaPlan(plan)

            val action = VendorPortalCapaAction(
                actionId = "ACT-01",
                capaId = "CAPA-01",
                tenantId = "TENANT-1",
                projectId = "PRJ-1",
                description = "Thermocouple replacement",
                owner = "Tech 1",
                targetDate = System.currentTimeMillis() + 86400000L
            )

            repository.saveCapaAction(action)

            val fetched = repository.findCapaPlanById("TENANT-1", "PRJ-1", "VND-1", "CAPA-01")
            assertTrue(fetched is DomainResult.Success)
            val data = (fetched as DomainResult.Success).data
            assertNotNull(data)
            assertEquals(1, data?.actions?.size)
            assertEquals("ACT-01", data?.actions?.get(0)?.actionId)
        }
    }

    @Test
    fun testDisputeAndResolutionResponseCrud() {
        runBlocking {
            val dispute = VendorPortalDisputeSummary(
                disputeId = "DISP-01",
                tenantId = "TENANT-1",
                projectId = "PRJ-1",
                vendorId = "VND-1",
                disputeReference = "DISP-001",
                sourceType = "INSPECTION",
                sourceId = "INSP-01",
                disputeType = VendorDisputeType.QUALITY,
                priority = VendorPortalQualityPriority.MEDIUM,
                status = VendorPortalDisputeStatus.OPEN,
                subject = "Hardness measurement dispute",
                description = "Vendor measured 62 HRC using Rockwell C",
                requestedResolution = VendorPortalResolutionType.REWORK,
                disputedQuantity = BigDecimal("50"),
                disputedAmount = Money(BigDecimal("2500")),
                raisedBy = "VENDOR_USER"
            )

            repository.saveDisputeSubmission(dispute)

            val response = VendorPortalResolutionResponse(
                responseId = "RESP-01",
                tenantId = "TENANT-1",
                projectId = "PRJ-1",
                vendorId = "VND-1",
                disputeId = "DISP-01",
                proposalAction = VendorPortalProposalAction.ACCEPT,
                rationale = "Agreed to rework batch by heat treating again",
                respondedBy = "VENDOR_USER"
            )

            repository.saveResolutionResponse(response)

            val respList = repository.listResolutionResponses("TENANT-1", "PRJ-1", "VND-1", "DISP-01")
            assertTrue(respList is DomainResult.Success)
            assertEquals(1, (respList as DomainResult.Success).data.size)
            assertEquals(VendorPortalProposalAction.ACCEPT, (respList as DomainResult.Success).data[0].proposalAction)
        }
    }

    @Test
    fun testEvidenceAndAuditCrud() {
        runBlocking {
            val ev = VendorPortalQualityEvidence(
                evidenceId = "EVD-01",
                tenantId = "TENANT-1",
                projectId = "PRJ-1",
                vendorId = "VND-1",
                entityType = "QUALITY_CASE",
                entityId = "CASE-01",
                evidenceType = VendorPortalQualityEvidenceType.PHOTO,
                filename = "crack.jpg",
                fileReference = "s3://bucket/crack.jpg",
                uploadedBy = "VENDOR_USER"
            )

            repository.saveEvidence(ev)

            val evList = repository.listEvidence("TENANT-1", "PRJ-1", "VND-1", "QUALITY_CASE", "CASE-01")
            assertTrue(evList is DomainResult.Success)
            assertEquals(1, (evList as DomainResult.Success).data.size)

            val audit = VendorPortalQualityActivity(
                activityId = "AUD-01",
                tenantId = "TENANT-1",
                projectId = "PRJ-1",
                vendorId = "VND-1",
                entityType = "QUALITY_CASE",
                entityId = "CASE-01",
                action = "CASE_ACKNOWLEDGED",
                actorId = "VENDOR_USER"
            )

            repository.recordAudit(audit)

            val audList = repository.listAuditEvents("TENANT-1", "PRJ-1", "VND-1", "QUALITY_CASE", "CASE-01")
            assertTrue(audList is DomainResult.Success)
            assertEquals(1, (audList as DomainResult.Success).data.size)
        }
    }
}
