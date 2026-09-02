package com.sucharu.sucharupro.vendor

import com.sucharu.sucharupro.domain.model.common.Money
import com.sucharu.sucharupro.domain.model.vendor.*
import com.sucharu.sucharupro.domain.validation.vendor.VendorWorkOrderValidator
import org.junit.Assert.*
import org.junit.Test
import java.math.BigDecimal

class VendorWorkOrderValidatorTest {

    @Test
    fun `valid work order passes validation`() {
        val order = VendorWorkOrder(
            workOrderId = "vwo_001",
            projectId = "p_main",
            workOrderNumber = "VWO-001",
            vendorId = "vnd_001",
            capabilityType = CapabilityType.LAMINATION,
            title = "Thermal Matt Lamination",
            quantity = BigDecimal("500.00"),
            estimatedAmount = Money(BigDecimal("2500.00")),
            scheduledStartAt = 1000L,
            scheduledDueAt = 2000L
        )
        val res = VendorWorkOrderValidator.validate(order)
        assertTrue(res.isValid)
        assertTrue(res.errors.isEmpty())
    }

    @Test
    fun `blank required fields fail validation`() {
        val order = VendorWorkOrder(
            workOrderId = "",
            projectId = "",
            workOrderNumber = "",
            vendorId = "",
            capabilityType = CapabilityType.LAMINATION,
            title = "",
            quantity = BigDecimal("500.00"),
            estimatedAmount = Money(BigDecimal("2500.00"))
        )
        val res = VendorWorkOrderValidator.validate(order)
        assertFalse(res.isValid)
        assertTrue(res.errors.any { it.contains("workOrderId") })
        assertTrue(res.errors.any { it.contains("projectId") })
        assertTrue(res.errors.any { it.contains("workOrderNumber") })
        assertTrue(res.errors.any { it.contains("vendorId") })
        assertTrue(res.errors.any { it.contains("title") })
    }

    @Test
    fun `quantity zero or negative fails validation`() {
        val orderZero = VendorWorkOrder(
            workOrderId = "vwo_001",
            projectId = "p_main",
            workOrderNumber = "VWO-001",
            vendorId = "vnd_001",
            capabilityType = CapabilityType.LAMINATION,
            title = "Valid Title",
            quantity = BigDecimal.ZERO,
            estimatedAmount = Money(BigDecimal("2500.00"))
        )
        val resZero = VendorWorkOrderValidator.validate(orderZero)
        assertFalse(resZero.isValid)
        assertTrue(resZero.errors.any { it.contains("quantity must be strictly greater than zero") })

        val orderNeg = orderZero.copy(quantity = BigDecimal("-10.00"))
        val resNeg = VendorWorkOrderValidator.validate(orderNeg)
        assertFalse(resNeg.isValid)
    }

    @Test
    fun `due date earlier than start date fails validation`() {
        val order = VendorWorkOrder(
            workOrderId = "vwo_001",
            projectId = "p_main",
            workOrderNumber = "VWO-001",
            vendorId = "vnd_001",
            capabilityType = CapabilityType.LAMINATION,
            title = "Valid Title",
            quantity = BigDecimal("100"),
            estimatedAmount = Money(BigDecimal("500.00")),
            scheduledStartAt = 5000L,
            scheduledDueAt = 4000L
        )
        val res = VendorWorkOrderValidator.validate(order)
        assertFalse(res.isValid)
        assertTrue(res.errors.any { it.contains("scheduledDueAt cannot be earlier than scheduledStartAt") })
    }
}
