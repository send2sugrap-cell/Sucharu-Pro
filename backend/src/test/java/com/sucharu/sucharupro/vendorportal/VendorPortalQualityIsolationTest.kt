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
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

class VendorPortalQualityIsolationTest {

    private lateinit var useCases: BackendUseCases
    private lateinit var portalQualityRepo: VendorPortalQualityRepositoryImpl
    private lateinit var vendorRepo: VendorRepositoryImpl

    private val vendor1Principal = AuthenticatedPrincipal(
        userId = "user-v1",
        projectId = "PRJ-001",
        username = "vendor1",
        role = UserRole.VENDOR,
        vendorId = "VND-ISO-01"
    )

    private val vendor2Principal = AuthenticatedPrincipal(
        userId = "user-v2",
        projectId = "PRJ-001",
        username = "vendor2",
        role = UserRole.VENDOR,
        vendorId = "VND-ISO-02"
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

            // Seed 2 active vendors
            vendorRepo.createVendor(
                Vendor(
                    vendorId = "VND-ISO-01",
                    projectId = "PRJ-001",
                    vendorCode = "VND-ISO-01",
                    vendorName = "Vendor 1 Corp",
                    vendorCategory = VendorCategory.RAW_MATERIALS,
                    status = VendorStatus.ACTIVE
                )
            )
            vendorRepo.createVendor(
                Vendor(
                    vendorId = "VND-ISO-02",
                    projectId = "PRJ-001",
                    vendorCode = "VND-ISO-02",
                    vendorName = "Vendor 2 Corp",
                    vendorCategory = VendorCategory.RAW_MATERIALS,
                    status = VendorStatus.ACTIVE
                )
            )
        }
    }

    @Test
    fun testQualityCaseVendorIsolation() {
        runBlocking {
            // Create case for Vendor 1
            portalQualityRepo.saveQualityCase(
                VendorPortalQualityCase(
                    caseId = "CASE-V1",
                    tenantId = "PRJ-001",
                    projectId = "PRJ-001",
                    vendorId = "VND-ISO-01",
                    caseNumber = "QC-V1-01",
                    title = "Vendor 1 Secret Issue",
                    description = "Description for V1 only"
                )
            )

            // Vendor 1 sees it
            val v1Cases = useCases.listVendorPortalQualityCases(vendor1Principal)
            assertEquals(1, v1Cases.size)
            assertEquals("CASE-V1", v1Cases[0].caseId)

            // Vendor 2 cannot see it in list
            val v2Cases = useCases.listVendorPortalQualityCases(vendor2Principal)
            assertEquals(0, v2Cases.size)

            // Vendor 2 cannot fetch it directly by ID
            assertThrows(NoSuchElementException::class.java) {
                runBlocking {
                    useCases.getVendorPortalQualityCaseById(vendor2Principal, "CASE-V1")
                }
            }

            // Vendor 2 cannot acknowledge Vendor 1's case
            assertThrows(IllegalArgumentException::class.java) {
                runBlocking {
                    useCases.acknowledgeVendorPortalQualityCase(vendor2Principal, "CASE-V1")
                }
            }
        }
    }

    @Test
    fun testCapaPlanVendorIsolation() {
        runBlocking {
            // Vendor 1 creates CAPA
            val capa = useCases.createVendorPortalCapaPlan(
                vendor1Principal,
                VendorPortalCapaPlanCreateRequest(
                    title = "Vendor 1 Process CAPA",
                    rootCause = "Root cause V1",
                    correctiveAction = "Corrective action V1",
                    preventiveAction = "Preventive action V1",
                    responsiblePerson = "Lead V1",
                    targetCompletionDate = System.currentTimeMillis() + 86400000L * 5
                )
            )

            // Vendor 1 lists CAPAs
            val v1Capas = useCases.listVendorPortalCapaPlans(vendor1Principal)
            assertEquals(1, v1Capas.size)

            // Vendor 2 lists CAPAs
            val v2Capas = useCases.listVendorPortalCapaPlans(vendor2Principal)
            assertEquals(0, v2Capas.size)

            // Vendor 2 cannot fetch V1 CAPA
            assertThrows(NoSuchElementException::class.java) {
                runBlocking {
                    useCases.getVendorPortalCapaPlanById(vendor2Principal, capa.capaId)
                }
            }

            // Vendor 2 cannot submit V1 CAPA
            assertThrows(IllegalArgumentException::class.java) {
                runBlocking {
                    useCases.submitVendorPortalCapaPlan(vendor2Principal, capa.capaId)
                }
            }
        }
    }

    @Test
    fun testDisputeSubmissionVendorIsolation() {
        runBlocking {
            // Vendor 1 creates dispute
            val disp = useCases.createVendorPortalDispute(
                vendor1Principal,
                VendorPortalDisputeCreateRequest(
                    sourceType = "REJECTION",
                    sourceId = "REJ-V1",
                    subject = "V1 Dispute Subject",
                    description = "V1 Dispute Description Detailed",
                    disputedQuantity = 10.0,
                    disputedAmount = 100.0
                )
            )

            // Vendor 1 lists disputes
            val v1Disps = useCases.listVendorPortalDisputes(vendor1Principal)
            assertEquals(1, v1Disps.size)

            // Vendor 2 lists disputes
            val v2Disps = useCases.listVendorPortalDisputes(vendor2Principal)
            assertEquals(0, v2Disps.size)

            // Vendor 2 cannot access V1 dispute
            assertThrows(NoSuchElementException::class.java) {
                runBlocking {
                    useCases.getVendorPortalDisputeById(vendor2Principal, disp.disputeId)
                }
            }
        }
    }
}
