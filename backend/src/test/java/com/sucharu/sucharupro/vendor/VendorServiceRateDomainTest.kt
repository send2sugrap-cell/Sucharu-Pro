package com.sucharu.sucharupro.vendor

import com.sucharu.sucharupro.domain.model.common.Money
import com.sucharu.sucharupro.domain.model.vendor.*
import org.junit.Assert.*
import org.junit.Test
import java.math.BigDecimal

class VendorServiceRateDomainTest {

    @Test
    fun `VendorServiceRate creation and snapshot conversion`() {
        val rate = VendorServiceRate(
            rateId = "rate_123",
            projectId = "tenant_1",
            vendorId = "vnd_1",
            capabilityType = CapabilityType.CTP,
            rateCode = "RATE-CTP-001",
            serviceName = "CTP Plate Output",
            pricingMethod = PricingMethod.PER_UNIT,
            unitOfMeasure = UnitOfMeasure.PLATE,
            rateAmount = Money(BigDecimal("800.00")),
            currency = "BDT",
            minimumQuantity = BigDecimal.ONE,
            effectiveFrom = 1700000000000L,
            status = RateStatus.ACTIVE
        )

        assertEquals("rate_123", rate.rateId)
        assertEquals(CapabilityType.CTP, rate.capabilityType)
        assertEquals(PricingMethod.PER_UNIT, rate.pricingMethod)
        assertEquals(UnitOfMeasure.PLATE, rate.unitOfMeasure)
        assertEquals(Money(BigDecimal("800.00")), rate.rateAmount)

        val snapshot = rate.toSnapshot(1700005000000L)
        assertEquals("rate_123", snapshot.rateId)
        assertEquals("vnd_1", snapshot.vendorId)
        assertEquals(CapabilityType.CTP, snapshot.capabilityType)
        assertEquals(Money(BigDecimal("800.00")), snapshot.rateAmount)
        assertEquals(1700005000000L, snapshot.effectiveDate)
    }

    @Test
    fun `PricingMethod and UnitOfMeasure enum values contain expected constants`() {
        val methods = PricingMethod.values().map { it.name }
        assertTrue(methods.containsAll(listOf("FIXED", "PER_UNIT", "PER_QUANTITY", "PER_AREA", "PER_WEIGHT", "PER_TIME", "TIERED")))

        val units = UnitOfMeasure.values().map { it.name }
        assertTrue(units.containsAll(listOf("JOB", "PIECE", "COPY", "PLATE", "SHEET", "SQ_FT", "SQ_M", "KG", "GRAM", "HOUR", "DAY", "OTHER")))
    }

    @Test
    fun `RateStatus transitions validate according to state machine`() {
        assertTrue(RateStatus.DRAFT.canTransitionTo(RateStatus.ACTIVE))
        assertTrue(RateStatus.DRAFT.canTransitionTo(RateStatus.ARCHIVED))
        assertFalse(RateStatus.DRAFT.canTransitionTo(RateStatus.SUSPENDED))

        assertTrue(RateStatus.ACTIVE.canTransitionTo(RateStatus.SUSPENDED))
        assertTrue(RateStatus.ACTIVE.canTransitionTo(RateStatus.EXPIRED))
        assertTrue(RateStatus.ACTIVE.canTransitionTo(RateStatus.ARCHIVED))

        assertTrue(RateStatus.SUSPENDED.canTransitionTo(RateStatus.ACTIVE))
        assertTrue(RateStatus.SUSPENDED.canTransitionTo(RateStatus.ARCHIVED))

        assertFalse(RateStatus.ARCHIVED.canTransitionTo(RateStatus.ACTIVE))
        assertFalse(RateStatus.ARCHIVED.canTransitionTo(RateStatus.DRAFT))
    }

    @Test
    fun `VendorServiceRateCalculator computes cost deterministically with Money`() {
        val perUnitRate = VendorServiceRate(
            rateId = "r1",
            projectId = "p1",
            vendorId = "v1",
            capabilityType = CapabilityType.DIE_CUTTING,
            rateCode = "RC1",
            serviceName = "Die Cutting",
            pricingMethod = PricingMethod.PER_UNIT,
            unitOfMeasure = UnitOfMeasure.PIECE,
            rateAmount = Money(BigDecimal("1.50")),
            effectiveFrom = 1000L
        )

        val costPerUnit = com.sucharu.sucharupro.domain.service.vendor.VendorServiceRateCalculator.calculateEstimatedCost(
            perUnitRate,
            BigDecimal("1000")
        )
        assertEquals(Money(BigDecimal("1500.00")), costPerUnit)

        val fixedRate = perUnitRate.copy(
            pricingMethod = PricingMethod.FIXED,
            rateAmount = Money(BigDecimal("500.00"))
        )
        val fixedCost = com.sucharu.sucharupro.domain.service.vendor.VendorServiceRateCalculator.calculateEstimatedCost(
            fixedRate,
            BigDecimal("5000")
        )
        assertEquals(Money(BigDecimal("500.00")), fixedCost)

        val areaRate = perUnitRate.copy(
            pricingMethod = PricingMethod.PER_AREA,
            unitOfMeasure = UnitOfMeasure.SQ_FT,
            rateAmount = Money(BigDecimal("12.00"))
        )
        val areaCost = com.sucharu.sucharupro.domain.service.vendor.VendorServiceRateCalculator.calculateEstimatedCost(
            areaRate,
            quantity = BigDecimal.ZERO,
            areaSqFt = BigDecimal("250.5")
        )
        assertEquals(Money(BigDecimal("3006.00")), areaCost)
    }
}
