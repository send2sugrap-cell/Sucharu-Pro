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
import com.sucharu.sucharupro.domain.repository.VendorPortalInvoiceRepository
import com.sucharu.sucharupro.domain.service.vendor.*
import com.sucharu.sucharupro.domain.service.vendorportal.VendorPortalInvoiceService
import com.sucharu.sucharupro.domain.service.vendorportal.VendorPortalInvoiceServiceImpl
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class VendorPortalInvoiceSecurityEdgeTest {

    private lateinit var useCases: BackendUseCases

    @Before
    fun setup() {
        runBlocking {
            val vendorDs = FakeVendorDataSource()
            val capDs = FakeVendorCapabilityDataSource()
            val rateDs = FakeVendorServiceRateDataSource()
            val poDs = FakeVendorPurchaseOrderDataSource()
            val receiptDs = FakeVendorDeliveryReceiptDataSource()
            val invoiceDs = FakeVendorInvoiceDataSource()
            val portalInvoiceDs = FakeVendorPortalInvoiceDataSource()
            val qualityDs = FakeVendorQualityDataSource()
            val perfDs = FakeVendorPerformanceDataSource()
            val settlementDs = FakeVendorSettlementDataSource()

            val vendorRepo = VendorRepositoryImpl(vendorDs)
            val capRepo = VendorCapabilityRepositoryImpl(capDs)
            val rateRepo = VendorServiceRateRepositoryImpl(rateDs)
            val poRepo = VendorPurchaseOrderRepositoryImpl(poDs)
            val receiptRepo = VendorDeliveryReceiptRepositoryImpl(receiptDs)
            val invoiceRepo = VendorInvoiceRepositoryImpl(invoiceDs)
            val portalInvoiceRepo = VendorPortalInvoiceRepositoryImpl(portalInvoiceDs)
            val qualityRepo = VendorQualityRepositoryImpl(qualityDs)
            val perfRepo = VendorPerformanceRepositoryImpl(perfDs)
            val settlementRepo = VendorSettlementRepositoryImpl(settlementDs)

            val rateService = VendorServiceRateServiceImpl(vendorRepo, capRepo, rateRepo)
            val poService = VendorPurchaseOrderServiceImpl(vendorRepo, capRepo, rateService, poRepo)
            val receiptService = VendorDeliveryReceiptServiceImpl(vendorRepo, poRepo, receiptRepo)
            val canonicalInvoiceService = VendorInvoiceServiceImpl(vendorRepo, poRepo, receiptRepo, invoiceRepo)

            val analyticsRepo = VendorAnalyticsRepositoryImpl(
                vendorRepository = vendorRepo,
                poRepository = poRepo,
                deliveryRepository = receiptRepo,
                invoiceRepository = invoiceRepo,
                qualityRepository = qualityRepo,
                performanceRepository = perfRepo,
                settlementRepository = settlementRepo
            )

            val canonicalSettlementService = VendorSettlementServiceImpl(
                settlementRepository = settlementRepo,
                analyticsRepository = analyticsRepo,
                vendorRepository = vendorRepo,
                invoiceRepository = invoiceRepo
            )

            val invoiceService = VendorPortalInvoiceServiceImpl(
                invoiceRepository = portalInvoiceRepo,
                vendorInvoiceService = canonicalInvoiceService,
                vendorPurchaseOrderService = poService,
                vendorSettlementService = canonicalSettlementService,
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
                override fun createVendorPortalInvoiceService(tenantId: String): VendorPortalInvoiceService = invoiceService
                override fun createVendorPortalInvoiceRepository(tenantId: String): VendorPortalInvoiceRepository = portalInvoiceRepo
            }

            useCases = BackendUseCases(fakeTxManager, customFactory)
        }
    }

    @Test
    fun testVendorWithoutVendorIdInPrincipalIsRejectedWithDescriptiveError() {
        runBlocking {
            val orphanVendorPrincipal = AuthenticatedPrincipal(
                userId = "orphan-user",
                projectId = "PRJ-001",
                username = "orphan",
                role = UserRole.VENDOR,
                vendorId = null // Missing explicit vendor identity
            )

            try {
                useCases.listVendorPortalInvoices(orphanVendorPrincipal)
                fail("Expected Exception for non-existent orphan vendor")
            } catch (e: Exception) {
                assertTrue(e is NoSuchElementException || e is IllegalArgumentException || e is SecurityException)
            }

            try {
                useCases.getVendorPortalInvoiceFinancialSummary(orphanVendorPrincipal)
                fail("Expected Exception for non-existent orphan vendor")
            } catch (e: Exception) {
                assertTrue(e is NoSuchElementException || e is IllegalArgumentException || e is SecurityException)
            }
        }
    }

    @Test
    fun testInvalidRoleWithoutPortalPermissionsIsRejectedByAuthPolicy() {
        runBlocking {
            val guestPrincipal = AuthenticatedPrincipal(
                userId = "guest-user",
                projectId = "PRJ-001",
                username = "guest",
                role = UserRole.CUSTOMER, // Not allowed on vendor portal
                vendorId = "VND-GUEST"
            )

            try {
                useCases.listVendorPortalInvoices(guestPrincipal)
                fail("Expected ForbiddenException for non-vendor role")
            } catch (e: Exception) {
                assertTrue(e is ForbiddenException || e is SecurityException || e is IllegalArgumentException)
            }
        }
    }
}
