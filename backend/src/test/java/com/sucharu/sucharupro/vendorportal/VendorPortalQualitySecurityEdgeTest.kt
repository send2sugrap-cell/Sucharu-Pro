package com.sucharu.sucharupro.vendorportal

import com.sucharu.sucharupro.data.api.model.*
import com.sucharu.sucharupro.data.api.server.BackendUseCases
import com.sucharu.sucharupro.data.datasource.*
import com.sucharu.sucharupro.data.persistence.postgres.PostgresRepositoryFactory
import com.sucharu.sucharupro.data.persistence.postgres.TenantContext
import com.sucharu.sucharupro.data.persistence.postgres.TransactionContext
import com.sucharu.sucharupro.data.persistence.postgres.TransactionManager
import com.sucharu.sucharupro.data.repository.*
import com.sucharu.sucharupro.domain.model.vendor.*
import com.sucharu.sucharupro.domain.repository.VendorPortalQualityRepository
import com.sucharu.sucharupro.domain.service.vendor.*
import com.sucharu.sucharupro.domain.service.vendorportal.VendorPortalQualityService
import com.sucharu.sucharupro.domain.service.vendorportal.VendorPortalQualityServiceImpl
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

class VendorPortalQualitySecurityEdgeTest {

    private lateinit var useCases: BackendUseCases
    private lateinit var vendorRepo: VendorRepositoryImpl

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
            val portalQualityRepo = VendorPortalQualityRepositoryImpl(portalQualityDs)

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

            // Seed suspended vendor
            vendorRepo.createVendor(
                Vendor(
                    vendorId = "VND-SUSPENDED",
                    projectId = "PRJ-001",
                    vendorCode = "VND-SUSP-CODE",
                    vendorName = "Suspended Vendor Ltd.",
                    vendorCategory = VendorCategory.RAW_MATERIALS,
                    status = VendorStatus.SUSPENDED
                )
            )
        }
    }

    @Test
    fun testSuspendedVendorCannotAccessQualityWorkspace() {
        val suspendedPrincipal = AuthenticatedPrincipal(
            userId = "user-susp",
            projectId = "PRJ-001",
            username = "vendor_susp",
            role = UserRole.VENDOR,
            vendorId = "VND-SUSPENDED"
        )

        assertThrows(IllegalArgumentException::class.java) {
            runBlocking {
                useCases.getVendorPortalQualityWorkspace(suspendedPrincipal)
            }
        }
    }

    @Test
    fun testPrincipalWithoutVendorIdCannotAccessVendorQuality() {
        val noVendorPrincipal = AuthenticatedPrincipal(
            userId = "user-orphan",
            projectId = "PRJ-001",
            username = "orphan",
            role = UserRole.VENDOR,
            vendorId = null
        )

        assertThrows(IllegalArgumentException::class.java) {
            runBlocking {
                useCases.listVendorPortalQualityCases(noVendorPrincipal)
            }
        }
    }

    @Test
    fun testSuspendedVendorCannotSubmitCapa() {
        val suspendedPrincipal = AuthenticatedPrincipal(
            userId = "user-susp",
            projectId = "PRJ-001",
            username = "vendor_susp",
            role = UserRole.VENDOR,
            vendorId = "VND-SUSPENDED"
        )

        assertThrows(IllegalArgumentException::class.java) {
            runBlocking {
                useCases.createVendorPortalCapaPlan(
                    suspendedPrincipal,
                    VendorPortalCapaPlanCreateRequest(
                        title = "Suspended CAPA",
                        rootCause = "Root cause description",
                        correctiveAction = "Corrective action description",
                        preventiveAction = "Preventive action description",
                        responsiblePerson = "Owner",
                        targetCompletionDate = System.currentTimeMillis() + 86400000L
                    )
                )
            }
        }
    }
}
