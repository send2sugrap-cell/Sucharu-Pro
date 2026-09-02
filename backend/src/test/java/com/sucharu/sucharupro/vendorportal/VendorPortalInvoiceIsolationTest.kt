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

class VendorPortalInvoiceIsolationTest {

    private lateinit var useCases: BackendUseCases
    private lateinit var poRepo: VendorPurchaseOrderRepositoryImpl
    private lateinit var invoiceRepo: VendorInvoiceRepositoryImpl
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

            vendorRepo.createVendor(
                Vendor(
                    vendorId = "VND-ISO-01",
                    projectId = "PRJ-001",
                    vendorCode = "VND-01",
                    vendorName = "Vendor 1 Corp",
                    vendorCategory = VendorCategory.PRINTING,
                    status = VendorStatus.ACTIVE
                )
            )
            vendorRepo.createVendor(
                Vendor(
                    vendorId = "VND-ISO-02",
                    projectId = "PRJ-001",
                    vendorCode = "VND-02",
                    vendorName = "Vendor 2 Corp",
                    vendorCategory = VendorCategory.PACKAGING,
                    status = VendorStatus.ACTIVE
                )
            )
        }
    }

    @Test
    fun testVendor2CannotAccessVendor1InvoiceOrMatchResult() {
        runBlocking {
            poRepo.createOrder(
                VendorPurchaseOrder(
                    purchaseOrderId = "po-iso-1",
                    projectId = "PRJ-001",
                    orderNumber = "PO-2026-ISO-1",
                    vendorId = "VND-ISO-01",
                    requestedBy = "ADMIN-1",
                    status = VendorPurchaseOrderStatus.ISSUED,
                    currency = "BDT",
                    subtotal = Money(BigDecimal("500")),
                    taxAmount = Money.ZERO,
                    discountAmount = Money.ZERO,
                    totalAmount = Money(BigDecimal("500")),
                    items = listOf(
                        VendorPurchaseOrderItem(
                            itemId = "item-iso-1",
                            purchaseOrderId = "po-iso-1",
                            itemDescription = "Bricks",
                            quantity = BigDecimal("500"),
                            unitRate = Money(BigDecimal("1.00")),
                            lineTotal = Money(BigDecimal("500.00"))
                        )
                    )
                )
            )

            invoiceRepo.createInvoice(
                VendorInvoice(
                    invoiceId = "inv-iso-1",
                    projectId = "PRJ-001",
                    vendorId = "VND-ISO-01",
                    purchaseOrderId = "po-iso-1",
                    invoiceNumber = "INV-001",
                    vendorInvoiceNumber = "VINV-ISO-1",
                    subtotal = Money(BigDecimal("500.00")),
                    totalAmount = Money(BigDecimal("500.00")),
                    items = listOf(
                        VendorInvoiceItem(
                            itemId = "inv-item-1",
                            invoiceId = "inv-iso-1",
                            purchaseOrderItemId = "item-iso-1",
                            description = "Bricks",
                            quantity = BigDecimal("500"),
                            unitPrice = Money(BigDecimal("1.00")),
                            lineTotal = Money(BigDecimal("500.00"))
                        )
                    )
                )
            )

            // Vendor 1 can access
            val v1Invoice = useCases.getVendorPortalInvoiceById(vendor1Principal, "inv-iso-1")
            assertEquals("inv-iso-1", v1Invoice.invoiceId)

            // Vendor 2 gets access denied / NoSuchElementException
            try {
                useCases.getVendorPortalInvoiceById(vendor2Principal, "inv-iso-1")
                fail("Expected NoSuchElementException or SecurityException for cross-vendor access")
            } catch (e: Exception) {
                // expected
            }

            try {
                useCases.getVendorPortalInvoiceMatch(vendor2Principal, "inv-iso-1")
                fail("Expected NoSuchElementException or SecurityException for cross-vendor match access")
            } catch (e: Exception) {
                // expected
            }
        }
    }
}
