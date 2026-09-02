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

class VendorDeliveryReceiptWorkflowTest {

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
                    subtotal = Money(BigDecimal("5000.00")),
                    totalAmount = Money(BigDecimal("5000.00")),
                    items = listOf(
                        VendorPurchaseOrderItem(
                            itemId = "poi_001",
                            purchaseOrderId = "po_001",
                            itemDescription = "Gloss Lamination 50 Micron",
                            quantity = BigDecimal("500.00"),
                            unitRate = Money(BigDecimal("10.00")),
                            lineTotal = Money(BigDecimal("5000.00"))
                        )
                    )
                )
            )
        }
    }

    @Test
    fun testFullReceiptLifecycleFromDraftToAccepted() = runBlocking {
        val item = VendorDeliveryReceiptItem(
            receiptItemId = "",
            deliveryReceiptId = "",
            purchaseOrderId = "po_001",
            purchaseOrderItemId = "poi_001",
            itemDescription = "",
            orderedQuantity = BigDecimal.ZERO,
            receivedQuantity = BigDecimal("500.00")
        )

        // 1. Create Receipt (DRAFT)
        val createRes = receiptService.createReceipt(
            projectId = "PRJ-01",
            purchaseOrderId = "po_001",
            items = listOf(item),
            actorId = "staff_01"
        )
        assertTrue(createRes is DomainResult.Success)
        val receiptId = (createRes as DomainResult.Success).data.deliveryReceiptId

        // 2. Start Receiving (RECEIVING)
        val startRes = receiptService.startReceiving("PRJ-01", receiptId, "staff_01")
        assertTrue(startRes is DomainResult.Success)
        assertEquals(VendorDeliveryReceiptStatus.RECEIVING, (startRes as DomainResult.Success).data.status)

        // 3. Record Received (RECEIVED)
        val recRes = receiptService.recordReceived("PRJ-01", receiptId, "staff_01")
        assertTrue(recRes is DomainResult.Success)
        assertEquals(VendorDeliveryReceiptStatus.RECEIVED, (recRes as DomainResult.Success).data.status)

        // 4. Quality Inspection (INSPECTED)
        val inspectedItems = listOf(
            (recRes as DomainResult.Success).data.items[0].copy(
                acceptedQuantity = BigDecimal("490.00"),
                rejectedQuantity = BigDecimal("5.00"),
                damagedQuantity = BigDecimal("5.00")
            )
        )
        val inspectRes = receiptService.inspectReceipt("PRJ-01", receiptId, inspectedItems, "QC passed with 10 rejections/damages", "qc_inspector_01")
        assertTrue(inspectRes is DomainResult.Success)
        assertEquals(VendorDeliveryReceiptStatus.INSPECTED, (inspectRes as DomainResult.Success).data.status)

        // 5. Partial Acceptance (PARTIALLY_ACCEPTED)
        val acceptRes = receiptService.partialAcceptReceipt("PRJ-01", receiptId, "Partial accept 490 units", "manager_01")
        assertTrue(acceptRes is DomainResult.Success)
        assertEquals(VendorDeliveryReceiptStatus.PARTIALLY_ACCEPTED, (acceptRes as DomainResult.Success).data.status)

        // 6. Verify PO Status updated to PARTIALLY_FULFILLED
        val poRes = poRepo.findById("PRJ-01", "po_001")
        assertTrue(poRes is DomainResult.Success)
        assertEquals(VendorPurchaseOrderStatus.PARTIALLY_FULFILLED, (poRes as DomainResult.Success).data.status)
    }

    @Test
    fun testCancelReceiptLifecycle() = runBlocking {
        val item = VendorDeliveryReceiptItem(
            receiptItemId = "",
            deliveryReceiptId = "",
            purchaseOrderId = "po_001",
            purchaseOrderItemId = "poi_001",
            itemDescription = "",
            orderedQuantity = BigDecimal.ZERO,
            receivedQuantity = BigDecimal("100.00")
        )

        val createRes = receiptService.createReceipt(
            projectId = "PRJ-01",
            purchaseOrderId = "po_001",
            items = listOf(item),
            actorId = "staff_01"
        )
        val receiptId = (createRes as DomainResult.Success).data.deliveryReceiptId

        val cancelRes = receiptService.cancelReceipt("PRJ-01", receiptId, "Wrong challan delivered", "manager_01")
        assertTrue(cancelRes is DomainResult.Success)
        assertEquals(VendorDeliveryReceiptStatus.CANCELLED, (cancelRes as DomainResult.Success).data.status)
    }
}
