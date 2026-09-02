package com.sucharu.sucharupro.vendor

import com.sucharu.sucharupro.data.datasource.FakeVendorDataSource
import com.sucharu.sucharupro.data.datasource.FakeVendorDeliveryReceiptDataSource
import com.sucharu.sucharupro.data.datasource.FakeVendorPurchaseOrderDataSource
import com.sucharu.sucharupro.data.repository.VendorDeliveryReceiptRepositoryImpl
import com.sucharu.sucharupro.data.repository.VendorPurchaseOrderRepositoryImpl
import com.sucharu.sucharupro.data.repository.VendorRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.common.Money
import com.sucharu.sucharupro.domain.model.vendor.*
import com.sucharu.sucharupro.domain.service.vendor.VendorDeliveryReceiptService
import com.sucharu.sucharupro.domain.service.vendor.VendorDeliveryReceiptServiceImpl
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

class VendorDeliveryReceiptOverReceiptTest {

    private lateinit var vendorRepo: VendorRepositoryImpl
    private lateinit var poRepo: VendorPurchaseOrderRepositoryImpl
    private lateinit var receiptRepo: VendorDeliveryReceiptRepositoryImpl
    private lateinit var receiptService: VendorDeliveryReceiptService

    @Before
    fun setUp() {
        val vDs = FakeVendorDataSource()
        val poDs = FakeVendorPurchaseOrderDataSource()
        val rDs = FakeVendorDeliveryReceiptDataSource()

        vendorRepo = VendorRepositoryImpl(vDs)
        poRepo = VendorPurchaseOrderRepositoryImpl(poDs)
        receiptRepo = VendorDeliveryReceiptRepositoryImpl(rDs)
        receiptService = VendorDeliveryReceiptServiceImpl(vendorRepo, poRepo, receiptRepo)

        runBlocking {
            vendorRepo.createVendor(
                Vendor(
                    vendorId = "vendor_001",
                    projectId = "PRJ-01",
                    vendorCode = "VEND-001",
                    vendorName = "Prime Coating Mills",
                    vendorType = VendorType.SERVICE_PROVIDER,
                    vendorCategory = VendorCategory.PRINTING,
                    status = VendorStatus.ACTIVE
                )
            )

            poRepo.createOrder(
                VendorPurchaseOrder(
                    purchaseOrderId = "po_001",
                    projectId = "PRJ-01",
                    orderNumber = "PO-2026-0001",
                    vendorId = "vendor_001",
                    status = VendorPurchaseOrderStatus.APPROVED,
                    requestedBy = "user_001",
                    approvedBy = "manager_001",
                    subtotal = Money(BigDecimal("150.00")),
                    totalAmount = Money(BigDecimal("150.00")),
                    items = listOf(
                        VendorPurchaseOrderItem(
                            itemId = "poi_001",
                            purchaseOrderId = "po_001",
                            itemDescription = "Paper Stock 300 GSM",
                            quantity = BigDecimal("100.00"),
                            unitRate = Money(BigDecimal("1.50")),
                            lineTotal = Money(BigDecimal("150.00"))
                        )
                    )
                )
            )
        }
    }

    @Test
    fun testDirectOverReceivingIsBlocked() = runBlocking {
        val item = VendorDeliveryReceiptItem(
            receiptItemId = "",
            deliveryReceiptId = "",
            purchaseOrderId = "po_001",
            purchaseOrderItemId = "poi_001",
            itemDescription = "",
            orderedQuantity = BigDecimal.ZERO,
            receivedQuantity = BigDecimal("150.00") // Ordered was 100
        )

        val res = receiptService.createReceipt(
            projectId = "PRJ-01",
            purchaseOrderId = "po_001",
            items = listOf(item),
            actorId = "staff_01"
        )

        assertTrue(res is DomainResult.Error)
        val err = (res as DomainResult.Error).message
        assertTrue(err.contains("Over-receiving blocked"))
    }

    @Test
    fun testCumulativeOverReceivingIsBlocked() = runBlocking {
        // Receipt 1: receive 70 units
        val item1 = VendorDeliveryReceiptItem(
            receiptItemId = "",
            deliveryReceiptId = "",
            purchaseOrderId = "po_001",
            purchaseOrderItemId = "poi_001",
            itemDescription = "",
            orderedQuantity = BigDecimal.ZERO,
            receivedQuantity = BigDecimal("70.00")
        )
        val r1Res = receiptService.createReceipt("PRJ-01", "po_001", items = listOf(item1), actorId = "staff_01")
        val r1Id = (r1Res as DomainResult.Success).data.deliveryReceiptId

        receiptService.startReceiving("PRJ-01", r1Id, "staff_01")
        receiptService.recordReceived("PRJ-01", r1Id, "staff_01")
        receiptService.acceptReceipt("PRJ-01", r1Id, "manager_01")

        // Receipt 2: attempt to receive 40 units (70 + 40 = 110 > 100)
        val item2 = VendorDeliveryReceiptItem(
            receiptItemId = "",
            deliveryReceiptId = "",
            purchaseOrderId = "po_001",
            purchaseOrderItemId = "poi_001",
            itemDescription = "",
            orderedQuantity = BigDecimal.ZERO,
            receivedQuantity = BigDecimal("40.00")
        )
        val r2Res = receiptService.createReceipt("PRJ-01", "po_001", items = listOf(item2), actorId = "staff_01")

        assertTrue(r2Res is DomainResult.Error)
        val err = (r2Res as DomainResult.Error).message
        assertTrue(err.contains("Over-receiving blocked"))
    }
}
