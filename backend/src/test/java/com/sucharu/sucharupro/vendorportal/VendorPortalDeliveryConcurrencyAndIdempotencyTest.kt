package com.sucharu.sucharupro.vendorportal

import com.sucharu.sucharupro.data.datasource.*
import com.sucharu.sucharupro.data.repository.*
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.common.Money
import com.sucharu.sucharupro.domain.model.vendor.*
import com.sucharu.sucharupro.domain.model.vendorportal.*
import com.sucharu.sucharupro.domain.service.vendor.*
import com.sucharu.sucharupro.domain.service.vendorportal.*
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

class VendorPortalDeliveryConcurrencyAndIdempotencyTest {

    private lateinit var service: VendorPortalDeliveryService
    private val tenantId = "tenant-conc"
    private val projectId = "proj-conc"
    private val vendorId = "vendor-conc"

    @Before
    fun setup() = runBlocking {
        val vendorRepo = VendorRepositoryImpl(FakeVendorDataSource())
        val capRepo = VendorCapabilityRepositoryImpl(FakeVendorCapabilityDataSource())
        val rateRepo = VendorServiceRateRepositoryImpl(FakeVendorServiceRateDataSource())
        val poRepo = VendorPurchaseOrderRepositoryImpl(FakeVendorPurchaseOrderDataSource())
        val receiptRepo = VendorDeliveryReceiptRepositoryImpl(FakeVendorDeliveryReceiptDataSource())
        val qualityRepo = VendorQualityRepositoryImpl(FakeVendorQualityDataSource())

        val rateService = VendorServiceRateServiceImpl(vendorRepo, capRepo, rateRepo)
        val poService = VendorPurchaseOrderServiceImpl(vendorRepo, capRepo, rateService, poRepo)
        val receiptService = VendorDeliveryReceiptServiceImpl(vendorRepo, poRepo, receiptRepo)
        val qualityService = VendorQualityServiceImpl(vendorRepo, poRepo, receiptRepo, qualityRepo)

        val deliveryRepo = VendorPortalDeliveryRepositoryImpl(FakeVendorPortalDeliveryDataSource())
        service = VendorPortalDeliveryServiceImpl(deliveryRepo, poService, receiptService, qualityService, vendorRepo)

        vendorRepo.createVendor(
            Vendor(
                vendorId = vendorId,
                projectId = projectId,
                vendorCode = "VND-CONC",
                vendorName = "Concurrent Vendor Corp",
                vendorCategory = VendorCategory.PRINTING,
                status = VendorStatus.ACTIVE
            )
        )

        poRepo.createOrder(
            VendorPurchaseOrder(
                purchaseOrderId = "po-conc-1",
                projectId = projectId,
                orderNumber = "PO-CONC-1",
                vendorId = vendorId,
                requestedBy = "buyer",
                status = VendorPurchaseOrderStatus.ISSUED,
                currency = "USD",
                subtotal = Money(BigDecimal("1000")),
                taxAmount = Money.ZERO,
                discountAmount = Money.ZERO,
                totalAmount = Money(BigDecimal("1000")),
                items = listOf(
                    VendorPurchaseOrderItem(
                        itemId = "poi-conc-1",
                        purchaseOrderId = "po-conc-1",
                        itemCode = "ITM-1",
                        itemDescription = "Widget",
                        quantity = BigDecimal("1000"),
                        unitOfMeasure = UnitOfMeasure.PIECE,
                        unitRate = Money(BigDecimal("1")),
                        lineTotal = Money(BigDecimal("1000"))
                    )
                ),
                createdAt = 1700000000000L,
                createdBy = "buyer"
            )
        )
        Unit
    }

    @Test
    fun testConcurrentDeliveryNoticeSubmissions() = runBlocking {
        val jobs = (1..10).map { i ->
            async(Dispatchers.Default) {
                service.createDeliveryNotice(
                    tenantId = tenantId,
                    projectId = projectId,
                    vendorId = vendorId,
                    purchaseOrderId = "po-conc-1",
                    plannedDeliveryDate = 1700100000000L + (i * 1000L),
                    carrierName = "Carrier-$i",
                    trackingNumber = "TRK-$i",
                    vehicleNumber = null,
                    driverName = null,
                    driverPhone = null,
                    vendorNotes = null,
                    items = listOf(
                        VendorPortalDeliveryNoticeItemInput(
                            purchaseOrderItemId = "poi-conc-1",
                            deliveryQuantity = BigDecimal("10")
                        )
                    ),
                    actorId = "worker-$i"
                )
            }
        }

        val results = jobs.awaitAll()
        val successful = results.filterIsInstance<DomainResult.Success<VendorPortalDeliveryNotice>>()
        assertEquals(10, successful.size)

        val listRes = service.listDeliveryNotices(tenantId, projectId, vendorId)
        assertTrue(listRes is DomainResult.Success)
        assertEquals(10, (listRes as DomainResult.Success).data.size)
    }
}
