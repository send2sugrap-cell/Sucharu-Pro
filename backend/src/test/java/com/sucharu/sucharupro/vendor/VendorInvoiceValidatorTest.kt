package com.sucharu.sucharupro.vendor

import com.sucharu.sucharupro.domain.model.common.Money
import com.sucharu.sucharupro.domain.model.vendor.*
import com.sucharu.sucharupro.domain.validation.vendor.VendorInvoiceValidator
import org.junit.Assert.*
import org.junit.Test
import java.math.BigDecimal

class VendorInvoiceValidatorTest {

    private fun createValidInvoice(): VendorInvoice {
        val item = VendorInvoiceItem(
            itemId = "vii_001",
            invoiceId = "vinv_001",
            purchaseOrderItemId = "poi_001",
            description = "Test Item",
            quantity = BigDecimal("10"),
            unitPrice = Money(100.0),
            lineTotal = Money(1000.0)
        )
        return VendorInvoice(
            invoiceId = "vinv_001",
            projectId = "PRJ-01",
            vendorId = "VND-001",
            purchaseOrderId = "PO-001",
            invoiceNumber = "INV-2026-000001",
            vendorInvoiceNumber = "VINV-9988",
            subtotal = Money(1000.0),
            totalAmount = Money(1000.0),
            items = listOf(item)
        )
    }

    @Test
    fun testValidInvoicePasses() {
        val invoice = createValidInvoice()
        val res = VendorInvoiceValidator.validate(invoice)
        assertTrue(res.isValid)
    }

    @Test
    fun testBlankIdentifiersFail() {
        val invoice = createValidInvoice().copy(invoiceId = "", vendorId = "", purchaseOrderId = "", invoiceNumber = "")
        val res = VendorInvoiceValidator.validate(invoice)
        assertFalse(res.isValid)
        assertTrue(res.errors.any { it.contains("invoiceId") })
        assertTrue(res.errors.any { it.contains("vendorId") })
        assertTrue(res.errors.any { it.contains("purchaseOrderId") })
        assertTrue(res.errors.any { it.contains("invoiceNumber") })
    }

    @Test
    fun testEmptyItemsFail() {
        val invoice = createValidInvoice().copy(items = emptyList())
        val res = VendorInvoiceValidator.validate(invoice)
        assertFalse(res.isValid)
        assertTrue(res.errors.any { it.contains("at least one line item") })
    }

    @Test
    fun testZeroOrNegativeQuantityFails() {
        val item = VendorInvoiceItem(
            itemId = "vii_001",
            invoiceId = "vinv_001",
            purchaseOrderItemId = "poi_001",
            description = "Test Item",
            quantity = BigDecimal("0"),
            unitPrice = Money(100.0),
            lineTotal = Money(0.0)
        )
        val invoice = createValidInvoice().copy(items = listOf(item))
        val res = VendorInvoiceValidator.validate(invoice)
        assertFalse(res.isValid)
        assertTrue(res.errors.any { it.contains("quantity must be greater than zero") })
    }

    @Test
    fun testIllegalStatusTransitionFails() {
        val res = VendorInvoiceValidator.validateStatusTransition(VendorInvoiceStatus.DRAFT, VendorInvoiceStatus.POSTED)
        assertFalse(res.isValid)
        assertTrue(res.errors.any { it.contains("Illegal vendor invoice status transition") })
    }
}
