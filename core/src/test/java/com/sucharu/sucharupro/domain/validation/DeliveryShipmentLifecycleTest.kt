package com.sucharu.sucharupro.domain.validation

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.delivery.shipment.DeliveryShipmentStatus
import org.junit.Assert.assertTrue
import org.junit.Test

class DeliveryShipmentLifecycleTest {

    @Test
    fun `valid happy-path progression from draft to delivered succeeds`() {
        // DRAFT -> READY
        assertTrue(DeliveryShipmentLifecycleValidator.validateTransition(DeliveryShipmentStatus.DRAFT, DeliveryShipmentStatus.READY) is DomainResult.Success)
        // READY -> DISPATCHED
        assertTrue(DeliveryShipmentLifecycleValidator.validateTransition(DeliveryShipmentStatus.READY, DeliveryShipmentStatus.DISPATCHED) is DomainResult.Success)
        // DISPATCHED -> IN_TRANSIT
        assertTrue(DeliveryShipmentLifecycleValidator.validateTransition(DeliveryShipmentStatus.DISPATCHED, DeliveryShipmentStatus.IN_TRANSIT) is DomainResult.Success)
        // IN_TRANSIT -> OUT_FOR_DELIVERY
        assertTrue(DeliveryShipmentLifecycleValidator.validateTransition(DeliveryShipmentStatus.IN_TRANSIT, DeliveryShipmentStatus.OUT_FOR_DELIVERY) is DomainResult.Success)
        // OUT_FOR_DELIVERY -> DELIVERED
        assertTrue(DeliveryShipmentLifecycleValidator.validateTransition(DeliveryShipmentStatus.OUT_FOR_DELIVERY, DeliveryShipmentStatus.DELIVERED) is DomainResult.Success)
    }

    @Test
    fun `operational delay and hold transitions succeed`() {
        // DISPATCHED -> DELAYED
        assertTrue(DeliveryShipmentLifecycleValidator.validateTransition(DeliveryShipmentStatus.DISPATCHED, DeliveryShipmentStatus.DELAYED) is DomainResult.Success)
        // DELAYED -> IN_TRANSIT
        assertTrue(DeliveryShipmentLifecycleValidator.validateTransition(DeliveryShipmentStatus.DELAYED, DeliveryShipmentStatus.IN_TRANSIT) is DomainResult.Success)
        // IN_TRANSIT -> ON_HOLD
        assertTrue(DeliveryShipmentLifecycleValidator.validateTransition(DeliveryShipmentStatus.IN_TRANSIT, DeliveryShipmentStatus.ON_HOLD) is DomainResult.Success)
        // ON_HOLD -> OUT_FOR_DELIVERY
        assertTrue(DeliveryShipmentLifecycleValidator.validateTransition(DeliveryShipmentStatus.ON_HOLD, DeliveryShipmentStatus.OUT_FOR_DELIVERY) is DomainResult.Success)
        // OUT_FOR_DELIVERY -> DELIVERY_ATTEMPTED
        assertTrue(DeliveryShipmentLifecycleValidator.validateTransition(DeliveryShipmentStatus.OUT_FOR_DELIVERY, DeliveryShipmentStatus.DELIVERY_ATTEMPTED) is DomainResult.Success)
        // DELIVERY_ATTEMPTED -> DELIVERED
        assertTrue(DeliveryShipmentLifecycleValidator.validateTransition(DeliveryShipmentStatus.DELIVERY_ATTEMPTED, DeliveryShipmentStatus.DELIVERED) is DomainResult.Success)
    }

    @Test
    fun `illegal direct state jumps fail`() {
        // DRAFT -> DELIVERED
        assertTrue(DeliveryShipmentLifecycleValidator.validateTransition(DeliveryShipmentStatus.DRAFT, DeliveryShipmentStatus.DELIVERED) is DomainResult.Error)
        // READY -> IN_TRANSIT
        assertTrue(DeliveryShipmentLifecycleValidator.validateTransition(DeliveryShipmentStatus.READY, DeliveryShipmentStatus.IN_TRANSIT) is DomainResult.Error)
        // DRAFT -> OUT_FOR_DELIVERY
        assertTrue(DeliveryShipmentLifecycleValidator.validateTransition(DeliveryShipmentStatus.DRAFT, DeliveryShipmentStatus.OUT_FOR_DELIVERY) is DomainResult.Error)
    }

    @Test
    fun `terminal states cannot transition further`() {
        assertTrue(DeliveryShipmentLifecycleValidator.validateTransition(DeliveryShipmentStatus.DELIVERED, DeliveryShipmentStatus.IN_TRANSIT) is DomainResult.Error)
        assertTrue(DeliveryShipmentLifecycleValidator.validateTransition(DeliveryShipmentStatus.CANCELLED, DeliveryShipmentStatus.READY) is DomainResult.Error)
    }
}
