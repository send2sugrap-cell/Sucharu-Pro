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

class VendorDeliveryReceiptInventoryIntegrationTest {

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
                    subtotal = Money(BigDecimal("250.00")),
                    totalAmount = Money(BigDecimal("250.00")),
                    items = listOf(
                        VendorPurchaseOrderItem(
                            itemId = "poi_001",
                            purchaseOrderId = "po_001",
                            itemDescription = "Gloss Lamination 50 Micron",
                            quantity = BigDecimal("100.00"),
                            unitRate = Money(BigDecimal("2.50")),
                            lineTotal = Money(BigDecimal("250.00"))
                        )
                    )
                )
            )
        }
    }

    @Test
    fun testReceiptAcceptanceEmitsInventoryIntegrationAuditRecord() = runBlocking {
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
            warehouseId = "WH-RAW-01",
            items = listOf(item),
            actorId = "staff_001"
        )
        val receiptId = (createRes as DomainResult.Success).data.deliveryReceiptId

        receiptService.startReceiving("PRJ-01", receiptId, "staff_001")
        receiptService.recordReceived("PRJ-01", receiptId, "staff_001")
        val acceptRes = receiptService.acceptReceipt("PRJ-01", receiptId, "manager_001")
        assertTrue(acceptRes is DomainResult.Success)

        // Verify audits contain RECEIPT_ACCEPTED with reference to inventory integration
        val auditsRes = receiptRepo.listAudits("PRJ-01", receiptId)
        assertTrue(auditsRes is DomainResult.Success)
        val audits = (auditsRes as DomainResult.Success).data
        assertTrue(audits.any { it.eventType == "RECEIPT_ACCEPTED" && it.details!!.contains("Inventory integration triggered") })
    }
}
