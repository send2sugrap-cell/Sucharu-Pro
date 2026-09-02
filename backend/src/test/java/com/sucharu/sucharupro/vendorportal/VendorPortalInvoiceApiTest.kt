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
import com.sucharu.sucharupro.domain.repository.VendorPortalInvoiceRepository
import com.sucharu.sucharupro.domain.service.vendor.*
import com.sucharu.sucharupro.domain.service.vendorportal.VendorPortalInvoiceService
import com.sucharu.sucharupro.domain.service.vendorportal.VendorPortalInvoiceServiceImpl
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

class VendorPortalInvoiceApiTest {

    private lateinit var useCases: BackendUseCases
    private lateinit var poRepo: VendorPurchaseOrderRepositoryImpl
    private lateinit var invoiceRepo: VendorInvoiceRepositoryImpl
    private lateinit var portalInvoiceRepo: VendorPortalInvoiceRepositoryImpl
    private lateinit var vendorRepo: VendorRepositoryImpl

    private lateinit var invoiceService: VendorPortalInvoiceService

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
        role = UserRole.ADMIN
    )

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

            vendorRepo = VendorRepositoryImpl(vendorDs)
            val capRepo = VendorCapabilityRepositoryImpl(capDs)
            val rateRepo = VendorServiceRateRepositoryImpl(rateDs)
            poRepo = VendorPurchaseOrderRepositoryImpl(poDs)
            val receiptRepo = VendorDeliveryReceiptRepositoryImpl(receiptDs)
            invoiceRepo = VendorInvoiceRepositoryImpl(invoiceDs)
            portalInvoiceRepo = VendorPortalInvoiceRepositoryImpl(portalInvoiceDs)
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

            invoiceService = VendorPortalInvoiceServiceImpl(
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

            // Seed Vendor
            vendorRepo.createVendor(
                Vendor(
                    vendorId = "VND-API-01",
                    projectId = "PRJ-001",
                    vendorCode = "VND-01",
                    vendorName = "Apex Tools",
                    vendorCategory = VendorCategory.PRINTING,
                    status = VendorStatus.ACTIVE
                )
            )
        }
    }

    @Test
    fun testCreateInvoiceSubmissionViaUseCase() {
        runBlocking {
            poRepo.createOrder(
                VendorPurchaseOrder(
                    purchaseOrderId = "po-api-1",
                    projectId = "PRJ-001",
                    orderNumber = "PO-2026-999",
                    vendorId = "VND-API-01",
                    requestedBy = "admin-1",
                    status = VendorPurchaseOrderStatus.ISSUED,
                    currency = "BDT",
                    subtotal = Money(BigDecimal("1000")),
                    taxAmount = Money.ZERO,
                    discountAmount = Money.ZERO,
                    totalAmount = Money(BigDecimal("1000")),
                    items = listOf(
                        VendorPurchaseOrderItem(
                            itemId = "poi-api-1",
                            purchaseOrderId = "po-api-1",
                            itemDescription = "Industrial Drill",
                            quantity = BigDecimal("5"),
                            unitRate = Money(BigDecimal("200.00")),
                            lineTotal = Money(BigDecimal("1000.00"))
                        )
                    )
                )
            )

            val req = SubmitVendorInvoiceRequestDto(
                purchaseOrderId = "po-api-1",
                vendorInvoiceNumber = "APEX-INV-101",
                items = listOf(
                    SubmitVendorInvoiceItemRequestDto(
                        purchaseOrderItemId = "poi-api-1",
                        invoicedQuantity = 5.0,
                        unitPrice = 200.0
                    )
                )
            )

            val submission = useCases.createVendorPortalInvoiceSubmission(vendorPrincipal, req)
            assertEquals("APEX-INV-101", submission.vendorInvoiceNumber)
            assertEquals(1000.0, submission.totalAmount, 0.001)

            // List submissions
            val list = useCases.listVendorPortalInvoiceSubmissions(vendorPrincipal)
            assertEquals(1, list.size)
        }
    }

    @Test
    fun testUploadFinancialEvidenceAndRetrieveKpiSummary() {
        runBlocking {
            val evReq = UploadFinancialEvidenceRequestDto(
                entityType = "INVOICE",
                entityId = "INV-101",
                evidenceType = "TAX_DOCUMENT",
                filename = "mushak_6_3.pdf",
                fileReference = "storage://invoices/mushak_6_3.pdf",
                sizeBytes = 15000
            )

            val ev = useCases.uploadVendorPortalFinancialEvidence(vendorPrincipal, evReq)
            assertEquals("mushak_6_3.pdf", ev.filename)

            val evidenceList = useCases.listVendorPortalFinancialEvidence(vendorPrincipal, "INVOICE", "INV-101")
            assertEquals(1, evidenceList.size)

            val kpi = useCases.getVendorPortalInvoiceFinancialSummary(vendorPrincipal)
            assertEquals("VND-API-01", kpi.vendorId)
        }
    }
}
