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
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

class VendorPortalInvoiceConcurrencyAndIdempotencyTest {

    private lateinit var useCases: BackendUseCases
    private lateinit var poRepo: VendorPurchaseOrderRepositoryImpl
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

            vendorRepo.createVendor(
                Vendor(
                    vendorId = "VND-CONC-01",
                    projectId = "PRJ-001",
                    vendorCode = "VND-01",
                    vendorName = "Concurrent Vendor",
                    vendorCategory = VendorCategory.PRINTING,
                    status = VendorStatus.ACTIVE
                )
            )
        }
    }

    @Test
    fun testConcurrentInvoiceSubmissionCreatesDistinctImmutableRecordsSafely() {
        runBlocking {
            poRepo.createOrder(
                VendorPurchaseOrder(
                    purchaseOrderId = "po-conc-1",
                    projectId = "PRJ-001",
                    orderNumber = "PO-2026-CONC",
                    vendorId = "VND-CONC-01",
                    requestedBy = "ADMIN-1",
                    status = VendorPurchaseOrderStatus.ISSUED,
                    currency = "BDT",
                    subtotal = Money(BigDecimal("1500")),
                    taxAmount = Money.ZERO,
                    discountAmount = Money.ZERO,
                    totalAmount = Money(BigDecimal("1500")),
                    items = listOf(
                        VendorPurchaseOrderItem(
                            itemId = "item-conc-1",
                            purchaseOrderId = "po-conc-1",
                            itemDescription = "Tiles",
                            quantity = BigDecimal("100"),
                            unitRate = Money(BigDecimal("15.00")),
                            lineTotal = Money(BigDecimal("1500.00"))
                        )
                    )
                )
            )

            val concurrentCount = 10
            val deferredList = (1..concurrentCount).map { i ->
                async(Dispatchers.Default) {
                    useCases.createVendorPortalInvoiceSubmission(
                        vendorPrincipal,
                        SubmitVendorInvoiceRequestDto(
                            purchaseOrderId = "po-conc-1",
                            vendorInvoiceNumber = "VINV-CONC-$i",
                            items = listOf(
                                SubmitVendorInvoiceItemRequestDto(
                                    purchaseOrderItemId = "item-conc-1",
                                    invoicedQuantity = 10.0,
                                    unitPrice = 15.0
                                )
                            )
                        )
                    )
                }
            }

            val results = deferredList.awaitAll()
            assertEquals(concurrentCount, results.size)
            val distinctSubmissionIds = results.map { it.submissionId }.toSet()
            assertEquals(concurrentCount, distinctSubmissionIds.size)

            val list = useCases.listVendorPortalInvoiceSubmissions(vendorPrincipal)
            assertEquals(concurrentCount, list.size)
        }
    }
}
