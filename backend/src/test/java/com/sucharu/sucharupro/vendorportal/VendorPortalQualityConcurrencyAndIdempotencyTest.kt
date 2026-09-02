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
import com.sucharu.sucharupro.domain.model.common.Money
import com.sucharu.sucharupro.domain.model.vendor.*
import com.sucharu.sucharupro.domain.model.vendorportal.*
import com.sucharu.sucharupro.domain.repository.VendorPortalQualityRepository
import com.sucharu.sucharupro.domain.service.vendor.*
import com.sucharu.sucharupro.domain.service.vendorportal.VendorPortalQualityService
import com.sucharu.sucharupro.domain.service.vendorportal.VendorPortalQualityServiceImpl
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

class VendorPortalQualityConcurrencyAndIdempotencyTest {

    private lateinit var useCases: BackendUseCases
    private lateinit var portalQualityRepo: VendorPortalQualityRepositoryImpl
    private lateinit var vendorRepo: VendorRepositoryImpl

    private val vendorPrincipal = AuthenticatedPrincipal(
        userId = "user-conc-1",
        projectId = "PRJ-001",
        username = "vendor_conc",
        role = UserRole.VENDOR,
        vendorId = "VND-CONC-01"
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

            val qualityService = VendorPortalQualityServiceImpl(
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

            vendorRepo.createVendor(
                Vendor(
                    vendorId = "VND-CONC-01",
                    projectId = "PRJ-001",
                    vendorCode = "VND-CONC-CODE",
                    vendorName = "Concurrent Vendor Inc.",
                    vendorCategory = VendorCategory.RAW_MATERIALS,
                    status = VendorStatus.ACTIVE
                )
            )
        }
    }

    @Test
    fun testConcurrentCapaPlanCreation() {
        runBlocking {
            val jobs = (1..10).map { idx ->
                async(Dispatchers.Default) {
                    useCases.createVendorPortalCapaPlan(
                        vendorPrincipal,
                        VendorPortalCapaPlanCreateRequest(
                            title = "Concurrent CAPA #$idx",
                            rootCause = "Root cause analysis #$idx",
                            correctiveAction = "Corrective action #$idx",
                            preventiveAction = "Preventive action #$idx",
                            responsiblePerson = "Owner #$idx",
                            targetCompletionDate = System.currentTimeMillis() + 86400000L * idx
                        )
                    )
                }
            }
            val results = jobs.awaitAll()
            assertEquals(10, results.size)
            assertEquals(10, results.map { it.capaId }.toSet().size)
        }
    }

    @Test
    fun testConcurrentActionItemAddition() {
        runBlocking {
            val capa = useCases.createVendorPortalCapaPlan(
                vendorPrincipal,
                VendorPortalCapaPlanCreateRequest(
                    title = "Multi-Action CAPA",
                    rootCause = "Multi-step root cause",
                    correctiveAction = "Multi-step corrective",
                    preventiveAction = "Multi-step preventive",
                    responsiblePerson = "QA Team",
                    targetCompletionDate = System.currentTimeMillis() + 86400000L * 10
                )
            )

            val jobs = (1..5).map { idx ->
                async(Dispatchers.Default) {
                    useCases.addVendorPortalCapaAction(
                        vendorPrincipal,
                        capa.capaId,
                        VendorPortalCapaActionCreateRequest(
                            description = "Action item #$idx",
                            owner = "Eng #$idx",
                            targetDate = System.currentTimeMillis() + 86400000L * idx
                        )
                    )
                }
            }
            val actions = jobs.awaitAll()
            assertEquals(5, actions.size)

            val fetched = useCases.getVendorPortalCapaPlanById(vendorPrincipal, capa.capaId)
            assertEquals(5, fetched.actions.size)
        }
    }

    @Test
    fun testQualityCaseAcknowledgmentIdempotency() {
        runBlocking {
            portalQualityRepo.saveQualityCase(
                VendorPortalQualityCase(
                    caseId = "CASE-IDEM",
                    tenantId = "PRJ-001",
                    projectId = "PRJ-001",
                    vendorId = "VND-CONC-01",
                    caseNumber = "QC-IDEM-01",
                    title = "Idempotent acknowledgment case",
                    description = "Description here",
                    status = VendorPortalQualityCaseStatus.OPEN
                )
            )

            val ack1 = useCases.acknowledgeVendorPortalQualityCase(vendorPrincipal, "CASE-IDEM")
            assertEquals("ACKNOWLEDGED", ack1.status)

            // Second acknowledgment does not fail and remains ACKNOWLEDGED
            val ack2 = useCases.acknowledgeVendorPortalQualityCase(vendorPrincipal, "CASE-IDEM")
            assertEquals("ACKNOWLEDGED", ack2.status)
        }
    }
}
