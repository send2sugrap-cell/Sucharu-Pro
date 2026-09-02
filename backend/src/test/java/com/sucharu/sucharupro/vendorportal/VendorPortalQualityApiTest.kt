package com.sucharu.sucharupro.vendorportal

import com.sucharu.sucharupro.data.api.model.*
import com.sucharu.sucharupro.data.api.server.BackendUseCases
import com.sucharu.sucharupro.data.datasource.*
import com.sucharu.sucharupro.data.persistence.postgres.PostgresRepositoryFactory
import com.sucharu.sucharupro.data.persistence.postgres.TenantContext
import com.sucharu.sucharupro.data.persistence.postgres.TransactionContext
import com.sucharu.sucharupro.data.persistence.postgres.TransactionManager
import com.sucharu.sucharupro.data.repository.*
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.vendor.*
import com.sucharu.sucharupro.domain.model.vendorportal.*
import com.sucharu.sucharupro.domain.repository.VendorPortalQualityRepository
import com.sucharu.sucharupro.domain.service.vendor.*
import com.sucharu.sucharupro.domain.service.vendorportal.VendorPortalQualityService
import com.sucharu.sucharupro.domain.service.vendorportal.VendorPortalQualityServiceImpl
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

class VendorPortalQualityApiTest {

    private lateinit var useCases: BackendUseCases
    private lateinit var portalQualityRepo: VendorPortalQualityRepositoryImpl
    private lateinit var vendorRepo: VendorRepositoryImpl
    private lateinit var qualityService: VendorPortalQualityService

    private val vendorPrincipal = AuthenticatedPrincipal(
        userId = "vendor-user-1",
        projectId = "PRJ-001",
        username = "vendor1",
        role = UserRole.VENDOR,
        vendorId = "VND-API-01"
    )

    private val adminPrincipal = AuthenticatedPrincipal(
        userId = "admin-user-1",
        projectId = "PRJ-001",
        username = "admin1",
        role = UserRole.ADMIN,
        vendorId = "VND-API-01"
    )

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

            val canonicalQualityService = VendorQualityServiceImpl(
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

            val fakeTxManager = object : TransactionManager {
                override suspend fun <T> inTransaction(tenantContext: TenantContext, block: suspend (TransactionContext) -> T): T {
                    throw UnsupportedOperationException("Not required for mock tests")
                }
                override suspend fun <T> inReadOnly(tenantContext: TenantContext, block: suspend (TransactionContext) -> T): T {
                    throw UnsupportedOperationException("Not required for mock tests")
                }
            }

            val customFactory = object : PostgresRepositoryFactory(fakeTxManager) {
                override fun createVendorPortalQualityService(tenantId: String): VendorPortalQualityService {
                    return qualityService
                }
                override fun createVendorPortalQualityRepository(tenantId: String): VendorPortalQualityRepository {
                    return portalQualityRepo
                }
            }

            useCases = BackendUseCases(fakeTxManager, customFactory)

            // Seed active vendor
            vendorRepo.createVendor(
                Vendor(
                    vendorId = "VND-API-01",
                    projectId = "PRJ-001",
                    vendorCode = "VND-CODE-01",
                    vendorName = "Precision Machining Inc.",
                    vendorCategory = VendorCategory.RAW_MATERIALS,
                    status = VendorStatus.ACTIVE
                )
            )
        }
    }

    @Test
    fun testQualityCaseApiEndpoints() {
        runBlocking {
            // Seed a case
            portalQualityRepo.saveQualityCase(
                VendorPortalQualityCase(
                    caseId = "CASE-01",
                    tenantId = "PRJ-001",
                    projectId = "PRJ-001",
                    vendorId = "VND-API-01",
                    caseNumber = "QC-001",
                    title = "Thread tolerance error",
                    description = "Thread pitch M12x1.5 was out of tolerance",
                    status = VendorPortalQualityCaseStatus.OPEN
                )
            )

            // List
            val list = useCases.listVendorPortalQualityCases(vendorPrincipal)
            assertEquals(1, list.size)
            assertEquals("CASE-01", list[0].caseId)

            // Get By ID
            val fetched = useCases.getVendorPortalQualityCaseById(vendorPrincipal, "CASE-01")
            assertEquals("QC-001", fetched.caseNumber)

            // Acknowledge
            val ack = useCases.acknowledgeVendorPortalQualityCase(vendorPrincipal, "CASE-01")
            assertEquals("ACKNOWLEDGED", ack.status)

            // Respond
            val resp = useCases.respondVendorPortalQualityCase(
                vendorPrincipal,
                "CASE-01",
                VendorPortalQualityCaseResponseRequest(
                    comment = "Die re-threaded and verified with thread gauge GO/NO-GO.",
                    correctiveActionPlan = "Tooling replaced",
                    promisedReplacementDate = System.currentTimeMillis() + 86400000L * 2
                )
            )
            assertEquals("RESPONSE_SUBMITTED", resp.status)
        }
    }

    @Test
    fun testCapaPlanApiEndpoints() {
        runBlocking {
            val req = VendorPortalCapaPlanCreateRequest(
                title = "Threading Tooling Wear CAPA",
                rootCause = "Threading tap wear beyond 500 operations",
                correctiveAction = "Replaced threading tap",
                preventiveAction = "Added tool wear counter at 400 cycles",
                responsiblePerson = "Machining Lead",
                targetCompletionDate = System.currentTimeMillis() + 86400000L * 7
            )

            val created = useCases.createVendorPortalCapaPlan(vendorPrincipal, req)
            assertEquals("DRAFT", created.status)
            assertEquals("Threading Tooling Wear CAPA", created.title)

            // Add action
            val action = useCases.addVendorPortalCapaAction(
                vendorPrincipal,
                created.capaId,
                VendorPortalCapaActionCreateRequest(
                    actionType = "CORRECTIVE",
                    description = "Change tap tooling",
                    owner = "Tooling Tech",
                    targetDate = System.currentTimeMillis() + 86400000L * 2
                )
            )
            assertEquals("OPEN", action.status)

            // Submit
            val submitted = useCases.submitVendorPortalCapaPlan(vendorPrincipal, created.capaId)
            assertEquals("SUBMITTED", submitted.status)
        }
    }

    @Test
    fun testDisputeApiEndpoints() {
        runBlocking {
            val req = VendorPortalDisputeCreateRequest(
                sourceType = "REJECTION",
                sourceId = "REJ-01",
                subject = "Dimensional Dispute",
                description = "Measurements taken per drawing note 4",
                disputedQuantity = 10.0,
                disputedAmount = 500.0
            )

            val disp = useCases.createVendorPortalDispute(vendorPrincipal, req)
            assertEquals("OPEN", disp.status)
            assertEquals("Dimensional Dispute", disp.subject)

            // Respond
            val resp = useCases.respondVendorPortalDispute(
                vendorPrincipal,
                disp.disputeId,
                VendorPortalDisputeResponseRequest(response = "Provided CMM calibration report")
            )
            assertEquals("VENDOR_RESPONDED", resp.status)

            // Proposal response
            val propResp = useCases.respondVendorPortalResolutionProposal(
                vendorPrincipal,
                disp.disputeId,
                VendorPortalResolutionProposalResponseRequest(
                    proposalAction = "ACCEPT",
                    rationale = "Agreed to credit resolution"
                )
            )
            assertEquals("ACCEPT", propResp.proposalAction)
        }
    }

    @Test
    fun testQualityEvidenceAndSummaryApi() {
        runBlocking {
            val evReq = VendorPortalQualityEvidenceUploadRequest(
                entityType = "QUALITY_CASE",
                entityId = "CASE-01",
                filename = "cmm_report.pdf",
                fileReference = "gs://evidence/cmm_report.pdf"
            )

            val ev = useCases.uploadVendorPortalQualityEvidence(vendorPrincipal, evReq)
            assertEquals("cmm_report.pdf", ev.filename)

            val evList = useCases.listVendorPortalQualityEvidence(vendorPrincipal, "QUALITY_CASE", "CASE-01")
            assertEquals(1, evList.size)

            val summary = useCases.getVendorPortalQualityKpiSummary(vendorPrincipal)
            assertEquals("VND-API-01", summary.vendorId)

            val ws = useCases.getVendorPortalQualityWorkspace(vendorPrincipal)
            assertNotNull(ws.kpiSummary)
        }
    }
}
