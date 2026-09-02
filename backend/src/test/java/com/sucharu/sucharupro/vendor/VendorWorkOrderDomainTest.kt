package com.sucharu.sucharupro.vendor

import com.sucharu.sucharupro.domain.model.common.Money
import com.sucharu.sucharupro.domain.model.vendor.*
import org.junit.Assert.*
import org.junit.Test
import java.math.BigDecimal

class VendorWorkOrderDomainTest {

    @Test
    fun `VendorWorkOrderStatus state machine transitions are valid`() {
        // DRAFT
        assertTrue(VendorWorkOrderStatus.DRAFT.canTransitionTo(VendorWorkOrderStatus.ASSIGNED))
        assertTrue(VendorWorkOrderStatus.DRAFT.canTransitionTo(VendorWorkOrderStatus.READY))
        assertTrue(VendorWorkOrderStatus.DRAFT.canTransitionTo(VendorWorkOrderStatus.CANCELLED))
        assertFalse(VendorWorkOrderStatus.DRAFT.canTransitionTo(VendorWorkOrderStatus.IN_PROGRESS))
        assertFalse(VendorWorkOrderStatus.DRAFT.canTransitionTo(VendorWorkOrderStatus.COMPLETED))

        // ASSIGNED
        assertTrue(VendorWorkOrderStatus.ASSIGNED.canTransitionTo(VendorWorkOrderStatus.READY))
        assertTrue(VendorWorkOrderStatus.ASSIGNED.canTransitionTo(VendorWorkOrderStatus.RELEASED))
        assertTrue(VendorWorkOrderStatus.ASSIGNED.canTransitionTo(VendorWorkOrderStatus.CANCELLED))

        // READY
        assertTrue(VendorWorkOrderStatus.READY.canTransitionTo(VendorWorkOrderStatus.RELEASED))
        assertTrue(VendorWorkOrderStatus.READY.canTransitionTo(VendorWorkOrderStatus.CANCELLED))

        // RELEASED
        assertTrue(VendorWorkOrderStatus.RELEASED.canTransitionTo(VendorWorkOrderStatus.IN_PROGRESS))
        assertTrue(VendorWorkOrderStatus.RELEASED.canTransitionTo(VendorWorkOrderStatus.CANCELLED))

        // IN_PROGRESS
        assertTrue(VendorWorkOrderStatus.IN_PROGRESS.canTransitionTo(VendorWorkOrderStatus.ON_HOLD))
        assertTrue(VendorWorkOrderStatus.IN_PROGRESS.canTransitionTo(VendorWorkOrderStatus.COMPLETED))
        assertTrue(VendorWorkOrderStatus.IN_PROGRESS.canTransitionTo(VendorWorkOrderStatus.CANCELLED))

        // ON_HOLD
        assertTrue(VendorWorkOrderStatus.ON_HOLD.canTransitionTo(VendorWorkOrderStatus.IN_PROGRESS))
        assertTrue(VendorWorkOrderStatus.ON_HOLD.canTransitionTo(VendorWorkOrderStatus.CANCELLED))

        // Terminal states
        assertFalse(VendorWorkOrderStatus.COMPLETED.canTransitionTo(VendorWorkOrderStatus.DRAFT))
        assertFalse(VendorWorkOrderStatus.COMPLETED.canTransitionTo(VendorWorkOrderStatus.IN_PROGRESS))
        assertFalse(VendorWorkOrderStatus.CANCELLED.canTransitionTo(VendorWorkOrderStatus.RELEASED))
    }

    @Test
    fun `VendorWorkOrder aggregate creation and property inspection`() {
        val snapshot = VendorWorkOrderRateSnapshot(
            sourceRateId = "rate_123",
            pricingMethod = PricingMethod.PER_UNIT,
            unitOfMeasure = UnitOfMeasure.PIECE,
            currency = "BDT",
            baseRate = Money(BigDecimal("5.50")),
            resolvedUnitRate = Money(BigDecimal("5.50")),
            quantityBasis = BigDecimal("100"),
            resolvedAt = 1000L
        )

        val order = VendorWorkOrder(
            workOrderId = "vwo_001",
            projectId = "p_main",
            workOrderNumber = "VWO-2026-0001",
            vendorId = "vnd_001",
            capabilityType = CapabilityType.CTP,
            serviceRateId = "rate_123",
            sourceReferenceId = "job_999",
            sourceReferenceType = "PRINT_JOB",
            title = "Plate Output for Catalog",
            description = "High resolution 2400 DPI plate output",
            quantity = BigDecimal("100"),
            unitOfMeasure = UnitOfMeasure.PLATE,
            pricingMethod = PricingMethod.PER_UNIT,
            rateSnapshot = snapshot,
            currency = "BDT",
            estimatedAmount = Money(BigDecimal("550.00")),
            scheduledStartAt = 1000L,
            scheduledDueAt = 2000L,
            priority = "HIGH",
            status = VendorWorkOrderStatus.ASSIGNED,
            notes = "Handle with care",
            createdAt = 1000L,
            createdBy = "user_1",
            updatedAt = 1000L,
            updatedBy = "user_1",
            version = 1L
        )

        assertEquals("vwo_001", order.workOrderId)
        assertEquals("p_main", order.projectId)
        assertEquals(CapabilityType.CTP, order.capabilityType)
        assertEquals(Money(BigDecimal("550.00")), order.estimatedAmount)
        assertEquals(VendorWorkOrderStatus.ASSIGNED, order.status)
        assertTrue(order.status.isActive)
        assertTrue(order.status.isEditable)
        assertFalse(order.status.isTerminal)
    }
}
