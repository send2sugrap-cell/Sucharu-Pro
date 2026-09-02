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

class VendorDeliveryReceiptServiceTest {

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
                    vendorName = "Standard Laminators Ltd",
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
                    subtotal = Money(BigDecimal("2500.00")),
                    totalAmount = Money(BigDecimal("2500.00")),
                    items = listOf(
                        VendorPurchaseOrderItem(
                            itemId = "poi_001",
                            purchaseOrderId = "po_001",
                            itemDescription = "Gloss Lamination 50 Micron",
                            quantity = BigDecimal("1000.00"),
                            unitRate = Money(BigDecimal("2.50")),
                            lineTotal = Money(BigDecimal("2500.00"))
                        )
                    )
                )
            )
        }
    }

    @Test
    fun testCreateDeliveryReceiptSuccess() = runBlocking {
        val item = VendorDeliveryReceiptItem(
            receiptItemId = "",
            deliveryReceiptId = "",
            purchaseOrderId = "po_001",
            purchaseOrderItemId = "poi_001",
            itemDescription = "",
            orderedQuantity = BigDecimal.ZERO,
            receivedQuantity = BigDecimal("400.00")
        )

        val createRes = receiptService.createReceipt(
            projectId = "PRJ-01",
            purchaseOrderId = "po_001",
            vendorDeliveryReference = "CHALLAN-999",
            remarks = "First partial batch",
            items = listOf(item),
            actorId = "staff_001"
        )

        assertTrue(createRes is DomainResult.Success)
        val receipt = (createRes as DomainResult.Success).data
        assertEquals(VendorDeliveryReceiptStatus.DRAFT, receipt.status)
        assertEquals("CHALLAN-999", receipt.vendorDeliveryReference)
        assertEquals(1, receipt.items.size)
        assertEquals(BigDecimal("400.00"), receipt.items[0].receivedQuantity)
        assertEquals(BigDecimal("1000.00"), receipt.items[0].orderedQuantity)
        assertEquals(Money(BigDecimal("1000.00")), receipt.items[0].lineTotal) // 400 * 2.50
    }

    @Test
    fun testReceivingAgainstInactiveVendorFails() = runBlocking {
        vendorRepo.createVendor(
            Vendor(
                vendorId = "vendor_suspended",
                projectId = "PRJ-01",
                vendorCode = "VEND-SUSP",
                vendorName = "Suspended Vendor",
                vendorType = VendorType.SERVICE_PROVIDER,
                vendorCategory = VendorCategory.PRINTING,
                status = VendorStatus.SUSPENDED
            )
        )

        poRepo.createOrder(
            VendorPurchaseOrder(
                purchaseOrderId = "po_susp",
                projectId = "PRJ-01",
                orderNumber = "PO-2026-0002",
                vendorId = "vendor_suspended",
                status = VendorPurchaseOrderStatus.APPROVED,
                requestedBy = "user_001",
                approvedBy = "manager_001",
                subtotal = Money(BigDecimal("500.00")),
                totalAmount = Money(BigDecimal("500.00")),
                items = listOf(
                    VendorPurchaseOrderItem(
                        itemId = "poi_susp",
                        purchaseOrderId = "po_susp",
                        itemDescription = "Material",
                        quantity = BigDecimal("100.00"),
                        unitRate = Money(BigDecimal("5.00")),
                        lineTotal = Money(BigDecimal("500.00"))
                    )
                )
            )
        )

        val item = VendorDeliveryReceiptItem(
            receiptItemId = "",
            deliveryReceiptId = "",
            purchaseOrderId = "po_susp",
            purchaseOrderItemId = "poi_susp",
            itemDescription = "Material",
            orderedQuantity = BigDecimal.ZERO,
            receivedQuantity = BigDecimal("50.00")
        )

        val res = receiptService.createReceipt(
            projectId = "PRJ-01",
            purchaseOrderId = "po_susp",
            items = listOf(item),
            actorId = "staff_001"
        )

        assertTrue(res is DomainResult.Error)
        assertTrue((res as DomainResult.Error).message.contains("SUSPENDED"))
    }

    @Test
    fun testReceivingSummaryCalculation() = runBlocking {
        val item = VendorDeliveryReceiptItem(
            receiptItemId = "",
            deliveryReceiptId = "",
            purchaseOrderId = "po_001",
            purchaseOrderItemId = "poi_001",
            itemDescription = "",
            orderedQuantity = BigDecimal.ZERO,
            receivedQuantity = BigDecimal("350.00")
        )

        val createRes = receiptService.createReceipt(
            projectId = "PRJ-01",
            purchaseOrderId = "po_001",
            items = listOf(item),
            actorId = "staff_001"
        )
        val receiptId = (createRes as DomainResult.Success).data.deliveryReceiptId

        receiptService.startReceiving("PRJ-01", receiptId, "staff_001")
        receiptService.recordReceived("PRJ-01", receiptId, "staff_001")
        receiptService.acceptReceipt("PRJ-01", receiptId, "manager_001")

        val summaryRes = receiptService.getReceivingSummary("PRJ-01", "po_001")
        assertTrue(summaryRes is DomainResult.Success)
        val summary = (summaryRes as DomainResult.Success).data

        assertEquals(BigDecimal("1000.00"), summary.totalOrderedQuantity)
        assertEquals(BigDecimal("350.00"), summary.totalReceivedQuantity)
        assertEquals(BigDecimal("350.00"), summary.totalAcceptedQuantity)
        assertEquals(BigDecimal("650.00"), summary.remainingReceivableQuantity)
        assertEquals(1, summary.receiptCount)
        assertFalse(summary.isFullyReceived)
    }
}
