package com.sucharu.sucharupro.vendorportal

import com.sucharu.sucharupro.data.datasource.*
import com.sucharu.sucharupro.data.repository.*
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.common.Money
import com.sucharu.sucharupro.domain.model.vendor.*
import com.sucharu.sucharupro.domain.model.vendorportal.*
import com.sucharu.sucharupro.domain.service.vendor.*
import com.sucharu.sucharupro.domain.service.vendorportal.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

class VendorPortalDeliveryIsolationTest {

    private lateinit var service: VendorPortalDeliveryService
    private lateinit var deliveryRepo: VendorPortalDeliveryRepositoryImpl
    private lateinit var vendorRepo: VendorRepositoryImpl
    private lateinit var poRepo: VendorPurchaseOrderRepositoryImpl
    private lateinit var receiptRepo: VendorDeliveryReceiptRepositoryImpl
    private lateinit var qualityService: VendorQualityService

    private val tenantA = "tenant-A"
    private val tenantB = "tenant-B"
    private val vendorA = "vendor-A"
    private val vendorB = "vendor-B"

    @Before
    fun setup() = runBlocking {
        vendorRepo = VendorRepositoryImpl(FakeVendorDataSource())
        val capRepo = VendorCapabilityRepositoryImpl(FakeVendorCapabilityDataSource())
        val rateRepo = VendorServiceRateRepositoryImpl(FakeVendorServiceRateDataSource())
        poRepo = VendorPurchaseOrderRepositoryImpl(FakeVendorPurchaseOrderDataSource())
        receiptRepo = VendorDeliveryReceiptRepositoryImpl(FakeVendorDeliveryReceiptDataSource())
        val qualityRepo = VendorQualityRepositoryImpl(FakeVendorQualityDataSource())

        val rateService = VendorServiceRateServiceImpl(vendorRepo, capRepo, rateRepo)
        val poService = VendorPurchaseOrderServiceImpl(vendorRepo, capRepo, rateService, poRepo)
        val receiptService = VendorDeliveryReceiptServiceImpl(vendorRepo, poRepo, receiptRepo)
        qualityService = VendorQualityServiceImpl(vendorRepo, poRepo, receiptRepo, qualityRepo)

        deliveryRepo = VendorPortalDeliveryRepositoryImpl(FakeVendorPortalDeliveryDataSource())
        service = VendorPortalDeliveryServiceImpl(deliveryRepo, poService, receiptService, qualityService, vendorRepo)

        // Seed Vendor A in Tenant A
        vendorRepo.createVendor(
            Vendor(
                vendorId = vendorA,
                projectId = tenantA,
                vendorCode = "VND-A",
                vendorName = "Vendor A Corp",
                vendorCategory = VendorCategory.PRINTING,
                status = VendorStatus.ACTIVE
            )
        )
        // Seed Vendor B in Tenant A
        vendorRepo.createVendor(
            Vendor(
                vendorId = vendorB,
                projectId = tenantA,
                vendorCode = "VND-B",
                vendorName = "Vendor B Corp",
                vendorCategory = VendorCategory.PRINTING,
                status = VendorStatus.ACTIVE
            )
        )

        // Seed PO for Vendor A
        poRepo.createOrder(
            VendorPurchaseOrder(
                purchaseOrderId = "po-A",
                projectId = tenantA,
                orderNumber = "PO-A",
                vendorId = vendorA,
                requestedBy = "buyer",
                status = VendorPurchaseOrderStatus.ISSUED,
                currency = "USD",
                subtotal = Money(BigDecimal("100")),
                taxAmount = Money.ZERO,
                discountAmount = Money.ZERO,
                totalAmount = Money(BigDecimal("100")),
                items = listOf(
                    VendorPurchaseOrderItem(
                        itemId = "poi-A",
                        purchaseOrderId = "po-A",
                        itemCode = "ITM-A",
                        itemDescription = "Item A",
                        quantity = BigDecimal("100"),
                        unitOfMeasure = UnitOfMeasure.PIECE,
                        unitRate = Money(BigDecimal("1")),
                        lineTotal = Money(BigDecimal("100"))
                    )
                ),
                createdAt = 1700000000000L,
                createdBy = "buyer"
            )
        )
        Unit
    }

    @Test
    fun testVendorBCannotCreateDeliveryNoticeForVendorAPO() = runBlocking {
        val res = service.createDeliveryNotice(
            tenantId = tenantA,
            projectId = tenantA,
            vendorId = vendorB, // Attacker vendor B attempting to touch Vendor A's PO
            purchaseOrderId = "po-A",
            plannedDeliveryDate = 1700100000000L,
            carrierName = "Malicious Carrier",
            trackingNumber = null,
            vehicleNumber = null,
            driverName = null,
            driverPhone = null,
            vendorNotes = null,
            items = listOf(
                VendorPortalDeliveryNoticeItemInput(
                    purchaseOrderItemId = "poi-A",
                    deliveryQuantity = BigDecimal("10")
                )
            ),
            actorId = "attacker"
        )
        assertTrue(res is DomainResult.Error)
        assertTrue((res as DomainResult.Error).exception is SecurityException)
    }

    @Test
    fun testTenantBCannotReadTenantADeliveryNotices() = runBlocking {
        // Create notice for Vendor A in Tenant A
        val notice = (service.createDeliveryNotice(
            tenantId = tenantA,
            projectId = tenantA,
            vendorId = vendorA,
            purchaseOrderId = "po-A",
            plannedDeliveryDate = 1700100000000L,
            carrierName = "Carrier A",
            trackingNumber = null,
            vehicleNumber = null,
            driverName = null,
            driverPhone = null,
            vendorNotes = null,
            items = listOf(
                VendorPortalDeliveryNoticeItemInput(
                    purchaseOrderItemId = "poi-A",
                    deliveryQuantity = BigDecimal("10")
                )
            ),
            actorId = "userA"
        ) as DomainResult.Success).data

        // Querying in Tenant B should return empty list or not found
        val listRes = service.listDeliveryNotices(tenantB, tenantB, vendorA)
        assertTrue(listRes is DomainResult.Success)
        val list = (listRes as DomainResult.Success).data
        assertTrue(list.isEmpty())

        val getRes = service.getDeliveryNotice(tenantB, tenantB, vendorA, notice.noticeId)
        assertTrue(getRes is DomainResult.Error)
    }
}
