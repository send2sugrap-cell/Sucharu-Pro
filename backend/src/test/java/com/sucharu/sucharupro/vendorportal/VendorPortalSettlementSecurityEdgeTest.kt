package com.sucharu.sucharupro.vendorportal

import com.sucharu.sucharupro.data.api.model.*
import com.sucharu.sucharupro.data.api.server.BackendUseCases
import com.sucharu.sucharupro.data.datasource.*
import com.sucharu.sucharupro.data.persistence.postgres.PostgresRepositoryFactory
import com.sucharu.sucharupro.data.persistence.postgres.TenantContext
import com.sucharu.sucharupro.data.persistence.postgres.TransactionContext
import com.sucharu.sucharupro.data.persistence.postgres.TransactionManager
import com.sucharu.sucharupro.data.repository.*
import com.sucharu.sucharupro.domain.repository.VendorPortalSettlementRepository
import com.sucharu.sucharupro.domain.service.vendor.*
import com.sucharu.sucharupro.domain.service.vendorportal.VendorPortalSettlementService
import com.sucharu.sucharupro.domain.service.vendorportal.VendorPortalSettlementServiceImpl
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class VendorPortalSettlementSecurityEdgeTest {

    private lateinit var useCases: BackendUseCases

    @Before
    fun setup() {
        val portalDataSource = FakeVendorPortalSettlementDataSource()
        val portalRepository = VendorPortalSettlementRepositoryImpl(portalDataSource)

        val vendorDs = FakeVendorDataSource()
        val vendorRepo = VendorRepositoryImpl(vendorDs)
        val invoiceDs = FakeVendorInvoiceDataSource()
        val invoiceRepo = VendorInvoiceRepositoryImpl(invoiceDs)
        val poDs = FakeVendorPurchaseOrderDataSource()
        val poRepo = VendorPurchaseOrderRepositoryImpl(poDs)
        val receiptDs = FakeVendorDeliveryReceiptDataSource()
        val receiptRepo = VendorDeliveryReceiptRepositoryImpl(receiptDs)
        val qualityDs = FakeVendorQualityDataSource()
        val qualityRepo = VendorQualityRepositoryImpl(qualityDs)
        val perfDs = FakeVendorPerformanceDataSource()
        val perfRepo = VendorPerformanceRepositoryImpl(perfDs)
        val settlementDs = FakeVendorSettlementDataSource()
        val settlementRepo = VendorSettlementRepositoryImpl(settlementDs)

        val canonicalInvoiceService = VendorInvoiceServiceImpl(vendorRepo, poRepo, receiptRepo, invoiceRepo)
        val analyticsRepo = VendorAnalyticsRepositoryImpl(vendorRepo, poRepo, receiptRepo, invoiceRepo, qualityRepo, perfRepo, settlementRepo)
        val canonicalSettlementService = VendorSettlementServiceImpl(settlementRepo, analyticsRepo, vendorRepo, invoiceRepo)

        val settlementService = VendorPortalSettlementServiceImpl(
            portalRepository = portalRepository,
            canonicalSettlementService = canonicalSettlementService,
            canonicalInvoiceService = canonicalInvoiceService,
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
            override fun createVendorPortalSettlementService(tenantId: String): VendorPortalSettlementService = settlementService
            override fun createVendorPortalSettlementRepository(tenantId: String): VendorPortalSettlementRepository = portalRepository
        }

        useCases = BackendUseCases(fakeTxManager, customFactory)
    }

    @Test
    fun testPrincipalWithoutVendorIdThrowsException() = runBlocking {
        val principalWithoutVendor = AuthenticatedPrincipal(
            userId = "user_no_vendor",
            projectId = "PRJ-001",
            username = "user1",
            role = UserRole.VENDOR,
            vendorId = null
        )

        try {
            useCases.listVendorPortalSettlements(principalWithoutVendor)
            fail("Expected IllegalArgumentException when principal has no associated vendor identity")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message?.contains("no associated vendor identity") == true)
        }
    }

    @Test
    fun testUnauthorizedRoleBlocked() = runBlocking {
        val customerPrincipal = AuthenticatedPrincipal(
            userId = "cust_01",
            projectId = "PRJ-001",
            username = "cust1",
            role = UserRole.CUSTOMER,
            vendorId = "VND-001"
        )

        try {
            useCases.listVendorPortalSettlements(customerPrincipal)
            fail("Expected SecurityException for CUSTOMER role trying to access vendor settlement workspace")
        } catch (e: Exception) {
            assertTrue(e is SecurityException || e.message?.contains("not authorized") == true || e.message?.contains("Access denied") == true || e.message?.contains("Forbidden") == true)
        }
    }
}
