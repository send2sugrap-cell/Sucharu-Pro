package com.sucharu.sucharupro.vendorportal

import com.sucharu.sucharupro.data.datasource.*
import com.sucharu.sucharupro.data.repository.*
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.common.Money
import com.sucharu.sucharupro.domain.model.vendor.*
import com.sucharu.sucharupro.domain.model.vendorportal.*
import com.sucharu.sucharupro.domain.service.vendor.*
import com.sucharu.sucharupro.domain.service.vendorportal.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

class VendorPortalQualityServiceTest {

    private val tenantId = "TENANT-001"
    private val projectId = "PRJ-001"
    private val vendorId = "VND-001"

    private lateinit var qualityService: VendorPortalQualityService
    private lateinit var vendorRepo: VendorRepositoryImpl
    private lateinit var canonicalQualityService: VendorQualityService
    private lateinit var portalQualityRepo: VendorPortalQualityRepositoryImpl

    @Before
    fun setup() {
        runBlocking {
            val vendorDs = FakeVendorDataSource()
            val capDs = FakeVendorCapabilityDataSource()
            val rateDs = FakeVendorServiceRateDataSource()
            val poDs = FakeVendorPurchaseOrderDataSource()
            val receiptDs = FakeVendorDeliveryReceiptDataSource()
            val qualityDs = FakeVendorQualityDataSource()
            val portalQualityDs = FakeVendorPortalQualityDataSource()

            vendorRepo = VendorRepositoryImpl(vendorDs)
            val capRepo = VendorCapabilityRepositoryImpl(capDs)
            val rateRepo = VendorServiceRateRepositoryImpl(rateDs)
            val poRepo = VendorPurchaseOrderRepositoryImpl(poDs)
            val receiptRepo = VendorDeliveryReceiptRepositoryImpl(receiptDs)
            val qualityRepo = VendorQualityRepositoryImpl(qualityDs)
            portalQualityRepo = VendorPortalQualityRepositoryImpl(portalQualityDs)

            val rateService = VendorServiceRateServiceImpl(vendorRepo, capRepo, rateRepo)
            val poService = VendorPurchaseOrderServiceImpl(vendorRepo, capRepo, rateService, poRepo)
            val receiptService = VendorDeliveryReceiptServiceImpl(vendorRepo, poRepo, receiptRepo)

            canonicalQualityService = VendorQualityServiceImpl(
                vendorRepository = vendorRepo,
                purchaseOrderRepository = poRepo,
                receiptRepository = receiptRepo,
                qualityRepository = qualityRepo
            )

            qualityService = VendorPortalQualityServiceImpl(
                qualityRepository = portalQualityRepo,
                canonicalQualityService = canonicalQualityService,
                vendorPurchaseOrderService = poService,
                vendorDeliveryReceiptService = receiptService,
                vendorRepository = vendorRepo
            )

            // Seed active vendor
            vendorRepo.createVendor(
                Vendor(
                    vendorId = vendorId,
                    projectId = projectId,
                    vendorCode = "VND-APEX-01",
                    vendorName = "Apex Metal Fabrication Ltd.",
                    vendorCategory = VendorCategory.RAW_MATERIALS,
                    status = VendorStatus.ACTIVE
                )
            )
        }
    }

    @Test
    fun testQualityCaseLifecycle() {
        runBlocking {
            // Seed a quality case
            val case = VendorPortalQualityCase(
                caseId = "CASE-100",
                tenantId = tenantId,
                projectId = projectId,
                vendorId = vendorId,
                caseNumber = "QC-2026-100",
                title = "Weld porosity issue",
                description = "Porosity detected in seam weld line 3",
                status = VendorPortalQualityCaseStatus.OPEN
            )
            portalQualityRepo.saveQualityCase(case)

            // 1. List cases
            val listRes = qualityService.listQualityCases(tenantId, projectId, vendorId)
            assertTrue(listRes is DomainResult.Success)
            assertEquals(1, (listRes as DomainResult.Success).data.size)

            // 2. Acknowledge case
            val ackRes = qualityService.acknowledgeQualityCase(tenantId, projectId, vendorId, "CASE-100", "VENDOR_USER_01")
            assertTrue(ackRes is DomainResult.Success)
            val ackCase = (ackRes as DomainResult.Success).data
            assertEquals(VendorPortalQualityCaseStatus.ACKNOWLEDGED, ackCase.status)
            assertNotNull(ackCase.acknowledgedAt)

            // 3. Respond to case
            val respRes = qualityService.respondToQualityCase(
                tenantId = tenantId,
                projectId = projectId,
                vendorId = vendorId,
                caseId = "CASE-100",
                comment = "We replaced the shielding gas regulator and purged the lines.",
                correctiveActionPlan = "Shielding gas purged and welder retrained.",
                promisedReplacementDate = System.currentTimeMillis() + 86400000L * 3,
                actorId = "VENDOR_USER_01"
            )
            assertTrue(respRes is DomainResult.Success)
            val respCase = (respRes as DomainResult.Success).data
            assertEquals(VendorPortalQualityCaseStatus.RESPONSE_SUBMITTED, respCase.status)
        }
    }

    @Test
    fun testCapaPlanCreationSubmissionAndCompletion() {
        runBlocking {
            val input = VendorPortalCapaPlanInput(
                caseId = "CASE-100",
                title = "Welding Gas Contamination CAPA",
                rootCause = "Impurity in argon shielding gas cylinder",
                correctiveAction = "Replaced batch of argon gas cylinders",
                preventiveAction = "Implemented gas purity certificate check upon receipt",
                responsiblePerson = "Quality Lead Mark",
                targetCompletionDate = System.currentTimeMillis() + 86400000L * 10
            )

            // 1. Create CAPA draft
            val createRes = qualityService.createCapaPlan(tenantId, projectId, vendorId, input, "VENDOR_USER_01")
            assertTrue(createRes is DomainResult.Success)
            val capa = (createRes as DomainResult.Success).data
            assertEquals(VendorPortalCapaStatus.DRAFT, capa.status)

            // 2. Add action item
            val actionInput = VendorPortalCapaActionInput(
                actionType = VendorPortalCapaActionType.PREVENTIVE,
                description = "Establish argon gas QA incoming SOP",
                owner = "Quality Tech",
                targetDate = System.currentTimeMillis() + 86400000L * 5
            )
            val actionRes = qualityService.addCapaAction(tenantId, projectId, vendorId, capa.capaId, actionInput, "VENDOR_USER_01")
            assertTrue(actionRes is DomainResult.Success)

            // 3. Submit CAPA
            val submitRes = qualityService.submitCapaPlan(tenantId, projectId, vendorId, capa.capaId, "VENDOR_USER_01")
            assertTrue(submitRes is DomainResult.Success)
            assertEquals(VendorPortalCapaStatus.SUBMITTED, (submitRes as DomainResult.Success).data.status)
        }
    }

    @Test
    fun testDisputeCreationAndResolutionProposalWorkflow() {
        runBlocking {
            val input = VendorPortalDisputeInput(
                sourceType = "REJECTION",
                sourceId = "REJ-200",
                disputeType = VendorDisputeType.QUALITY,
                subject = "Rejection of batch B-101",
                description = "Certificates show coating thickness meets ISO 12944 standard C3",
                requestedResolution = VendorPortalResolutionType.CREDIT,
                disputedQuantity = BigDecimal("100"),
                disputedAmount = Money(BigDecimal("4500.00"))
            )

            // 1. Create dispute
            val dispRes = qualityService.createDispute(tenantId, projectId, vendorId, input, "VENDOR_USER_01")
            assertTrue(dispRes is DomainResult.Success)
            val dispute = (dispRes as DomainResult.Success).data
            assertEquals(VendorPortalDisputeStatus.OPEN, dispute.status)

            // 2. Respond to dispute
            val respRes = qualityService.respondToDispute(tenantId, projectId, vendorId, dispute.disputeId, "Additional metallurgical photos uploaded", "VENDOR_USER_01")
            assertTrue(respRes is DomainResult.Success)
            assertEquals(VendorPortalDisputeStatus.VENDOR_RESPONDED, (respRes as DomainResult.Success).data.status)

            // 3. Respond to resolution proposal
            val propRes = qualityService.respondToResolutionProposal(
                tenantId = tenantId,
                projectId = projectId,
                vendorId = vendorId,
                disputeId = dispute.disputeId,
                action = VendorPortalProposalAction.ACCEPT,
                rationale = "We agree to the 50% credit note proposal.",
                actorId = "VENDOR_USER_01"
            )
            assertTrue(propRes is DomainResult.Success)
            assertEquals(VendorPortalProposalAction.ACCEPT, (propRes as DomainResult.Success).data.proposalAction)
        }
    }

    @Test
    fun testQualityKpiSummaryCalculation() {
        runBlocking {
            val kpiRes = qualityService.getQualityKpiSummary(tenantId, projectId, vendorId)
            assertTrue(kpiRes is DomainResult.Success)
            val kpi = (kpiRes as DomainResult.Success).data
            assertEquals(vendorId, kpi.vendorId)
            assertEquals(BigDecimal("100.00"), kpi.qualityPassRate)
        }
    }
}
