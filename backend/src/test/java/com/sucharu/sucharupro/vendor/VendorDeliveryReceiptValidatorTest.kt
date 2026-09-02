package com.sucharu.sucharupro.vendor

import com.sucharu.sucharupro.domain.model.vendor.VendorDeliveryReceipt
import com.sucharu.sucharupro.domain.model.vendor.VendorDeliveryReceiptItem
import com.sucharu.sucharupro.domain.model.vendor.VendorDeliveryReceiptStatus
import com.sucharu.sucharupro.domain.validation.vendor.VendorDeliveryReceiptValidator
import org.junit.Assert.*
import org.junit.Test
import java.math.BigDecimal

class VendorDeliveryReceiptValidatorTest {

    private fun sampleReceipt(
        receiptId: String = "vdr_001",
        projectId: String = "PRJ-01",
        receiptNumber: String = "VDR-2026-0001",
        purchaseOrderId: String = "po_001",
        vendorId: String = "vendor_001",
        receivedBy: String = "user_001",
        items: List<VendorDeliveryReceiptItem> = listOf(
            VendorDeliveryReceiptItem(
                receiptItemId = "vri_001",
                deliveryReceiptId = "vdr_001",
                purchaseOrderId = "po_001",
                purchaseOrderItemId = "poi_001",
                itemDescription = "Lamination Gloss",
                orderedQuantity = BigDecimal("100.00"),
                receivedQuantity = BigDecimal("50.00")
            )
        )
    ) = VendorDeliveryReceipt(
        deliveryReceiptId = receiptId,
        projectId = projectId,
        tenantId = "TENANT-001",
        receiptNumber = receiptNumber,
        purchaseOrderId = purchaseOrderId,
        vendorId = vendorId,
        receivedBy = receivedBy,
        items = items
    )

    @Test
    fun testValidReceiptPassesValidation() {
        val receipt = sampleReceipt()
        val result = VendorDeliveryReceiptValidator.validate(receipt)
        assertTrue(result.isValid)
    }

    @Test
    fun testBlankIdentifiersFailValidation() {
        val r1 = sampleReceipt(receiptId = "")
        assertFalse(VendorDeliveryReceiptValidator.validate(r1).isValid)

        val r2 = sampleReceipt(projectId = "")
        assertFalse(VendorDeliveryReceiptValidator.validate(r2).isValid)

        val r3 = sampleReceipt(receiptNumber = "")
        assertFalse(VendorDeliveryReceiptValidator.validate(r3).isValid)

        val r4 = sampleReceipt(purchaseOrderId = "")
        assertFalse(VendorDeliveryReceiptValidator.validate(r4).isValid)

        val r5 = sampleReceipt(vendorId = "")
        assertFalse(VendorDeliveryReceiptValidator.validate(r5).isValid)
    }

    @Test
    fun testReceiptWithoutItemsFailsValidation() {
        val receipt = sampleReceipt(items = emptyList())
        val result = VendorDeliveryReceiptValidator.validate(receipt)
        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.contains("at least one line item") })
    }

    @Test
    fun testNegativeQuantitiesFailValidation() {
        val item = VendorDeliveryReceiptItem(
            receiptItemId = "vri_001",
            deliveryReceiptId = "vdr_001",
            purchaseOrderId = "po_001",
            purchaseOrderItemId = "poi_001",
            itemDescription = "Gloss",
            orderedQuantity = BigDecimal("100.00"),
            receivedQuantity = BigDecimal("-10.00")
        )
        val receipt = sampleReceipt(items = listOf(item))
        val result = VendorDeliveryReceiptValidator.validate(receipt)
        assertFalse(result.isValid)
    }

    @Test
    fun testAccountedQuantityExceedingReceivedFailsValidation() {
        val item = VendorDeliveryReceiptItem(
            receiptItemId = "vri_001",
            deliveryReceiptId = "vdr_001",
            purchaseOrderId = "po_001",
            purchaseOrderItemId = "poi_001",
            itemDescription = "Gloss",
            orderedQuantity = BigDecimal("100.00"),
            receivedQuantity = BigDecimal("50.00"),
            acceptedQuantity = BigDecimal("40.00"),
            rejectedQuantity = BigDecimal("20.00"), // 40 + 20 = 60 > 50
            damagedQuantity = BigDecimal.ZERO
        )
        val receipt = sampleReceipt(items = listOf(item)).copy(status = VendorDeliveryReceiptStatus.INSPECTED)
        val result = VendorDeliveryReceiptValidator.validate(receipt)
        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.contains("exceeds received") })
    }
}
