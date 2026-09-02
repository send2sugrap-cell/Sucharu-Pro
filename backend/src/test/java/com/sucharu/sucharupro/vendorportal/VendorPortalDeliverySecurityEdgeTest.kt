package com.sucharu.sucharupro.vendorportal

import com.sucharu.sucharupro.data.api.model.*
import com.sucharu.sucharupro.data.api.server.BackendUseCases
import com.sucharu.sucharupro.data.datasource.*
import com.sucharu.sucharupro.data.persistence.postgres.*
import com.sucharu.sucharupro.data.repository.*
import com.sucharu.sucharupro.domain.repository.*
import com.sucharu.sucharupro.domain.service.vendor.*
import com.sucharu.sucharupro.domain.service.vendorportal.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class VendorPortalDeliverySecurityEdgeTest {

    private lateinit var useCases: BackendUseCases

    private val staffPrincipal = AuthenticatedPrincipal(
        userId = "staff-user-1",
        projectId = "proj-sec-1",
        username = "staff1",
        role = UserRole.STAFF
    )

    private val vendorPrincipalWithoutVendorId = AuthenticatedPrincipal(
        userId = "orphan-vendor",
        projectId = "proj-sec-1",
        username = "orphan",
        role = UserRole.VENDOR,
        vendorId = null // missing effective vendor id
    )

    @Before
    fun setup() = runBlocking {
        val vendorRepo = VendorRepositoryImpl(FakeVendorDataSource())
        val capRepo = VendorCapabilityRepositoryImpl(FakeVendorCapabilityDataSource())
        val rateRepo = VendorServiceRateRepositoryImpl(FakeVendorServiceRateDataSource())
        val poRepo = VendorPurchaseOrderRepositoryImpl(FakeVendorPurchaseOrderDataSource())
        val receiptRepo = VendorDeliveryReceiptRepositoryImpl(FakeVendorDeliveryReceiptDataSource())
        val qualityRepo = VendorQualityRepositoryImpl(FakeVendorQualityDataSource())
        val deliveryRepo = VendorPortalDeliveryRepositoryImpl(FakeVendorPortalDeliveryDataSource())

        val rateService = VendorServiceRateServiceImpl(vendorRepo, capRepo, rateRepo)
        val poService = VendorPurchaseOrderServiceImpl(vendorRepo, capRepo, rateService, poRepo)
        val receiptService = VendorDeliveryReceiptServiceImpl(vendorRepo, poRepo, receiptRepo)
        val qualityService = VendorQualityServiceImpl(vendorRepo, poRepo, receiptRepo, qualityRepo)
        val deliveryService = VendorPortalDeliveryServiceImpl(deliveryRepo, poService, receiptService, qualityService, vendorRepo)

        val fakeTxManager = object : TransactionManager {
            override suspend fun <T> inTransaction(tenantContext: TenantContext, block: suspend (TransactionContext) -> T): T {
                throw UnsupportedOperationException("Not required for mock tests")
            }
            override suspend fun <T> inReadOnly(tenantContext: TenantContext, block: suspend (TransactionContext) -> T): T {
                throw UnsupportedOperationException("Not required for mock tests")
            }
        }

        val customFactory = object : PostgresRepositoryFactory(fakeTxManager) {
            override fun createVendorPortalDeliveryService(tenantId: String): VendorPortalDeliveryService = deliveryService
            override fun createVendorPortalDeliveryRepository(tenantId: String): VendorPortalDeliveryRepository = deliveryRepo
        }

        useCases = BackendUseCases(fakeTxManager, customFactory)
    }

    @Test(expected = IllegalArgumentException::class)
    fun testOrphanVendorCannotCreateDeliveryNotice() = runBlocking {
        useCases.createVendorPortalDeliveryNotice(
            vendorPrincipalWithoutVendorId,
            CreateDeliveryNoticeRequestDto(
                purchaseOrderId = "po-1",
                plannedDeliveryDate = 1700000000000L,
                items = emptyList()
            )
        )
        Unit
    }

    @Test
    fun testStaffCannotResolveDeliveryException() = runBlocking {
        try {
            useCases.resolveVendorPortalDeliveryException(
                staffPrincipal,
                "exc-1",
                ResolveDeliveryExceptionRequestDto("Resolution notes")
            )
            fail("Expected ForbiddenException / SecurityException when STAFF attempts to resolve delivery exception")
        } catch (e: Exception) {
            assertTrue(e is ForbiddenException || e is SecurityException || e is IllegalArgumentException)
        }
    }
}
