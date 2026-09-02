package com.sucharu.sucharupro.vendorportal

import com.sucharu.sucharupro.data.api.model.AuthenticatedPrincipal
import com.sucharu.sucharupro.data.api.model.ForbiddenException
import com.sucharu.sucharupro.data.api.model.UserRole
import com.sucharu.sucharupro.data.api.server.BackendUseCases
import com.sucharu.sucharupro.data.datasource.*
import com.sucharu.sucharupro.data.persistence.postgres.PostgresRepositoryFactory
import com.sucharu.sucharupro.data.persistence.postgres.TenantContext
import com.sucharu.sucharupro.data.persistence.postgres.TransactionContext
import com.sucharu.sucharupro.data.persistence.postgres.TransactionManager
import com.sucharu.sucharupro.data.repository.*
import com.sucharu.sucharupro.domain.model.common.Money
import com.sucharu.sucharupro.domain.model.vendor.Vendor
import com.sucharu.sucharupro.domain.model.vendor.VendorCategory
import com.sucharu.sucharupro.domain.model.vendor.VendorPurchaseOrder
import com.sucharu.sucharupro.domain.model.vendor.VendorPurchaseOrderStatus
import com.sucharu.sucharupro.domain.model.vendor.VendorStatus
import com.sucharu.sucharupro.domain.service.vendor.VendorPurchaseOrderServiceImpl
import com.sucharu.sucharupro.domain.service.vendor.VendorServiceRateServiceImpl
import com.sucharu.sucharupro.domain.service.vendor.VendorWorkOrderServiceImpl
import com.sucharu.sucharupro.domain.service.vendorportal.VendorPortalCollaborationService
import com.sucharu.sucharupro.domain.service.vendorportal.VendorPortalCollaborationServiceImpl
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

class VendorPortalCollaborationIsolationTest {

    private lateinit var useCases: BackendUseCases
    private lateinit var poRepo: VendorPurchaseOrderRepositoryImpl
    private lateinit var woRepo: VendorWorkOrderRepositoryImpl
    private lateinit var vendorRepo: VendorRepositoryImpl
    private lateinit var capabilityRepo: VendorCapabilityRepositoryImpl
    private lateinit var rateRepo: VendorServiceRateRepositoryImpl
    private lateinit var collabRepo: VendorCollaborationRepositoryImpl

    private val vendorAPrincipal = AuthenticatedPrincipal(
        userId = "user-vendor-a",
        username = "vendorA",
        role = UserRole.VENDOR,
        projectId = "tenant-alpha",
        vendorId = "vendor-AAA"
    )

    private val vendorBPrincipal = AuthenticatedPrincipal(
        userId = "user-vendor-b",
        username = "vendorB",
        role = UserRole.VENDOR,
        projectId = "tenant-alpha",
        vendorId = "vendor-BBB"
    )

    @Before
    fun setup() {
        runBlocking {
            poRepo = VendorPurchaseOrderRepositoryImpl(FakeVendorPurchaseOrderDataSource())
            woRepo = VendorWorkOrderRepositoryImpl(FakeVendorWorkOrderDataSource())
            vendorRepo = VendorRepositoryImpl(FakeVendorDataSource())
            capabilityRepo = VendorCapabilityRepositoryImpl(FakeVendorCapabilityDataSource())
            rateRepo = VendorServiceRateRepositoryImpl(FakeVendorServiceRateDataSource())
            collabRepo = VendorCollaborationRepositoryImpl(FakeVendorCollaborationDataSource())

            vendorRepo.createVendor(
                Vendor(
                    vendorId = "vendor-AAA",
                    projectId = "tenant-alpha",
                    vendorCode = "VND-A",
                    vendorName = "Vendor Alpha",
                    vendorCategory = VendorCategory.PAPER_SUPPLIER,
                    status = VendorStatus.ACTIVE
                )
            )

            vendorRepo.createVendor(
                Vendor(
                    vendorId = "vendor-BBB",
                    projectId = "tenant-alpha",
                    vendorCode = "VND-B",
                    vendorName = "Vendor Beta",
                    vendorCategory = VendorCategory.PRINTING,
                    status = VendorStatus.ACTIVE
                )
            )

            val rateService = VendorServiceRateServiceImpl(vendorRepo, capabilityRepo, rateRepo)
            val poService = VendorPurchaseOrderServiceImpl(vendorRepo, capabilityRepo, rateService, poRepo)
            val woService = VendorWorkOrderServiceImpl(vendorRepo, capabilityRepo, rateService, woRepo)

            val fakeTxManager = object : TransactionManager {
                override suspend fun <T> inTransaction(tenantContext: TenantContext, block: suspend (TransactionContext) -> T): T {
                    throw UnsupportedOperationException("Not required for mock tests")
                }
                override suspend fun <T> inReadOnly(tenantContext: TenantContext, block: suspend (TransactionContext) -> T): T {
                    throw UnsupportedOperationException("Not required for mock tests")
                }
            }

            val customRepoFactory = object : PostgresRepositoryFactory(fakeTxManager) {
                override fun createVendorPurchaseOrderRepository(tenantId: String) = poRepo
                override fun createVendorRepository(tenantId: String) = vendorRepo
                override fun createVendorPortalCollaborationService(tenantId: String): VendorPortalCollaborationService {
                    return VendorPortalCollaborationServiceImpl(
                        collaborationRepository = collabRepo,
                        vendorPurchaseOrderService = poService,
                        vendorWorkOrderService = woService,
                        vendorRepository = vendorRepo
                    )
                }
            }

            useCases = BackendUseCases(fakeTxManager, customRepoFactory)
        }
    }

    @Test
    fun testVendorACannotAccessPoBelongingToVendorB() = runBlocking {
        // Pre-populate PO belonging to Vendor B
        poRepo.createOrder(
            VendorPurchaseOrder(
                purchaseOrderId = "po-for-vendor-b",
                orderNumber = "PO-B-001",
                vendorId = "vendor-BBB",
                projectId = "tenant-alpha",
                status = VendorPurchaseOrderStatus.ISSUED,
                currency = "BDT",
                subtotal = Money(BigDecimal("50000")),
                taxAmount = Money.ZERO,
                discountAmount = Money.ZERO,
                totalAmount = Money(BigDecimal("50000")),
                requestedBy = "admin-1"
            )
        )

        // Attempt access or acknowledge with Vendor A principal
        try {
            useCases.getVendorPortalPurchaseOrderDetails(vendorAPrincipal, "po-for-vendor-b")
            fail("Expected exception when Vendor A accesses Vendor B PO")
        } catch (e: Exception) {
            assertTrue(e is ForbiddenException || e is SecurityException || e is IllegalArgumentException)
        }
    }
}
