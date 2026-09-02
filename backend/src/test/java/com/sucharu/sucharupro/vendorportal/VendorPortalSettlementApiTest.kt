package com.sucharu.sucharupro.vendorportal

import com.sucharu.sucharupro.data.api.model.*
import com.sucharu.sucharupro.data.api.server.BackendUseCases
import com.sucharu.sucharupro.data.datasource.*
import com.sucharu.sucharupro.data.persistence.postgres.PostgresRepositoryFactory
import com.sucharu.sucharupro.data.persistence.postgres.TenantContext
import com.sucharu.sucharupro.data.persistence.postgres.TransactionContext
import com.sucharu.sucharupro.data.persistence.postgres.TransactionManager
import com.sucharu.sucharupro.data.repository.*
import com.sucharu.sucharupro.domain.model.common.Money
import com.sucharu.sucharupro.domain.model.vendor.*
import com.sucharu.sucharupro.domain.repository.VendorPortalSettlementRepository
import com.sucharu.sucharupro.domain.service.vendor.*
import com.sucharu.sucharupro.domain.service.vendorportal.VendorPortalSettlementService
import com.sucharu.sucharupro.domain.service.vendorportal.VendorPortalSettlementServiceImpl
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

class VendorPortalSettlementApiTest {

    private lateinit var useCases: BackendUseCases

    private val tenantId = "PRJ-001"
    private val projectId = "PRJ-001"
    private val vendorId = "VND-API-01"

    private val vendorPrincipal = AuthenticatedPrincipal(
        userId = "vendor_rep_01",
        projectId = projectId,
        username = "vendor_rep",
        role = UserRole.VENDOR,
        vendorId = vendorId
    )

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

        runBlocking {
            vendorRepo.createVendor(
                Vendor(
                    vendorId = vendorId,
                    projectId = projectId,
                    vendorCode = "VND-API",
                    vendorName = "API Test Vendor",
                    vendorCategory = VendorCategory.PRINTING,
                    status = VendorStatus.ACTIVE
                )
            )

            canonicalSettlementService.createSettlement(
                vendorId = vendorId,
                settlementNumber = "SETTL-API-101",
                totalAmount = Money(BigDecimal("55000.00")),
                settlementMethod = SettlementMethod.BANK_TRANSFER,
                allocations = listOf(
                    VendorSettlementAllocation(
                        allocationId = "ALLOC-API-01",
                        settlementId = "SETTL-API-101",
                        payableId = "PAY-01",
                        invoiceId = "INV-01",
                        allocatedAmount = Money(BigDecimal("55000.00")),
                        currency = "BDT"
                    )
                ),
                tenantId = tenantId,
                projectId = projectId,
                actorId = "system"
            )
        }

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
    fun testGetSettlementsUseCase() = runBlocking {
        val settlements = useCases.listVendorPortalSettlements(vendorPrincipal)
        assertEquals(1, settlements.size)
        assertEquals("SETTL-API-101", settlements.first().settlementNumber)
    }

    @Test
    fun testCreateReconciliationQueryUseCase() = runBlocking {
        val request = VendorPortalReconciliationQueryRequest(
            subject = "Missing discount adjustment",
            claimedAmount = 5000.0,
            systemAmount = 4000.0,
            currency = "BDT",
            notes = "Discrepancy on invoice line 2"
        )
        val result = useCases.createVendorPortalReconciliationQuery(vendorPrincipal, request)
        assertEquals("Missing discount adjustment", result.subject)
        assertEquals(1000.0, result.varianceAmount, 0.001)
    }

    @Test
    fun testCreateFinancialDisputeUseCase() = runBlocking {
        val request = VendorPortalFinancialDisputeCreateRequest(
            category = "TAX_WITHHOLDING",
            priority = "HIGH",
            disputedAmount = 2500.0,
            reason = "Certificate applied incorrectly"
        )
        val result = useCases.createVendorPortalFinancialDispute(vendorPrincipal, request)
        assertEquals("TAX_WITHHOLDING", result.category)
        assertEquals(2500.0, result.disputedAmount, 0.001)
    }

    @Test
    fun testGetFinancialWorkspaceUseCase() = runBlocking {
        val workspace = useCases.getVendorPortalFinancialWorkspace(vendorPrincipal)
        assertNotNull(workspace.analytics)
        assertEquals(1, workspace.settlementOverview.size)
    }
}
