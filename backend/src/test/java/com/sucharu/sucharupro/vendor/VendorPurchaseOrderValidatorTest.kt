package com.sucharu.sucharupro.vendor

import com.sucharu.sucharupro.domain.model.common.Money
import com.sucharu.sucharupro.domain.model.vendor.VendorPurchaseOrder
import com.sucharu.sucharupro.domain.model.vendor.VendorPurchaseOrderItem
import com.sucharu.sucharupro.domain.model.vendor.VendorPurchaseOrderStatus
import com.sucharu.sucharupro.domain.validation.vendor.VendorPurchaseOrderValidator
import org.junit.Assert.*
import org.junit.Test
import java.math.BigDecimal

class VendorPurchaseOrderValidatorTest {

    private fun createValidOrder(): VendorPurchaseOrder {
        val item = VendorPurchaseOrderItem(
            itemId = "poi_1",
            purchaseOrderId = "vpo_1",
            itemDescription = "Offset Printing Plates",
            quantity = BigDecimal("20.00"),
            unitRate = Money(250.0),
            lineTotal = Money(5000.0)
        )
        return VendorPurchaseOrder(
            purchaseOrderId = "vpo_1",
            projectId = "proj_1",
            orderNumber = "PO-2026-0001",
            vendorId = "v_1",
            requestedBy = "usr_1",
            subtotal = Money(5000.0),
            totalAmount = Money(5000.0),
            items = listOf(item)
        )
    }

    @Test
    fun `test valid order passes validation`() {
        val order = createValidOrder()
        val res = VendorPurchaseOrderValidator.validate(order)
        assertTrue(res.isValid)
    }

    @Test
    fun `test order without items fails validation`() {
        val order = createValidOrder().copy(items = emptyList())
        val res = VendorPurchaseOrderValidator.validate(order)
        assertFalse(res.isValid)
        assertTrue(res.errors.any { it.contains("at least one line item") })
    }

    @Test
    fun `test zero or negative quantity fails validation`() {
        val item = VendorPurchaseOrderItem(
            itemId = "poi_1",
            purchaseOrderId = "vpo_1",
            itemDescription = "Plates",
            quantity = BigDecimal.ZERO,
            unitRate = Money(100.0),
            lineTotal = Money(0.0)
        )
        val order = createValidOrder().copy(items = listOf(item))
        val res = VendorPurchaseOrderValidator.validate(order)
        assertFalse(res.isValid)
        assertTrue(res.errors.any { it.contains("quantity must be strictly greater than zero") })
    }

    @Test
    fun `test blank identifiers fail validation`() {
        val order = createValidOrder().copy(
            purchaseOrderId = "",
            orderNumber = "",
            vendorId = ""
        )
        val res = VendorPurchaseOrderValidator.validate(order)
        assertFalse(res.isValid)
        assertTrue(res.errors.size >= 3)
    }

    @Test
    fun `test separation of duties enforcement on approval`() {
        val order = createValidOrder().copy(requestedBy = "usr_alice")
        
        // Alice trying to approve her own order without override
        val selfApprove = VendorPurchaseOrderValidator.validateApproval(order, "usr_alice", allowSelfApproval = false)
        assertFalse(selfApprove.isValid)
        assertTrue(selfApprove.errors.any { it.contains("Separation of duties violation") })

        // Bob approving Alice's order
        val validApprove = VendorPurchaseOrderValidator.validateApproval(order, "usr_bob", allowSelfApproval = false)
        assertTrue(validApprove.isValid)

        // Alice approving with explicit override (e.g. ADMIN)
        val adminOverride = VendorPurchaseOrderValidator.validateApproval(order, "usr_alice", allowSelfApproval = true)
        assertTrue(adminOverride.isValid)
    }

    @Test
    fun `test invalid status transition fails`() {
        val res = VendorPurchaseOrderValidator.validateStatusTransition(
            VendorPurchaseOrderStatus.DRAFT,
            VendorPurchaseOrderStatus.CLOSED
        )
        assertFalse(res.isValid)
    }
}
