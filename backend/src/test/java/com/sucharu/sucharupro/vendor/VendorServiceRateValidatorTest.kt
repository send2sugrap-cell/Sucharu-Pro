package com.sucharu.sucharupro.vendor

import com.sucharu.sucharupro.domain.model.common.Money
import com.sucharu.sucharupro.domain.model.vendor.*
import com.sucharu.sucharupro.domain.validation.vendor.VendorServiceRateValidator
import org.junit.Assert.*
import org.junit.Test
import java.math.BigDecimal

class VendorServiceRateValidatorTest {

    private fun validRate(): VendorServiceRate {
        return VendorServiceRate(
            rateId = "rate_001",
            projectId = "tenant_1",
            vendorId = "vnd_001",
            capabilityType = CapabilityType.LAMINATION,
            rateCode = "RATE-LAM-01",
            serviceName = "Thermal Matt Lamination",
            pricingMethod = PricingMethod.PER_AREA,
            unitOfMeasure = UnitOfMeasure.SQ_FT,
            rateAmount = Money(BigDecimal("12.50")),
            currency = "BDT",
            minimumQuantity = BigDecimal.ZERO,
            maximumQuantity = BigDecimal("10000"),
            effectiveFrom = 1700000000000L,
            effectiveTo = 1800000000000L,
            status = RateStatus.ACTIVE
        )
    }

    @Test
    fun `valid rate passes validation`() {
        val res = VendorServiceRateValidator.validate(validRate())
        assertTrue(res.isValid)
        assertNull(res.errorMessage)
    }

    @Test
    fun `blank required fields fail validation`() {
        val rate = validRate().copy(rateId = "", projectId = " ", vendorId = "", rateCode = "", serviceName = "")
        val res = VendorServiceRateValidator.validate(rate)
        assertFalse(res.isValid)
        assertTrue(res.errorMessage!!.contains("rateId"))
        assertTrue(res.errorMessage!!.contains("projectId"))
        assertTrue(res.errorMessage!!.contains("vendorId"))
        assertTrue(res.errorMessage!!.contains("rateCode"))
        assertTrue(res.errorMessage!!.contains("serviceName"))
    }

    @Test
    fun `negative rateAmount or invalid date range fails validation`() {
        val rate = validRate().copy(
            rateAmount = Money(BigDecimal("-10.00")),
            effectiveFrom = 2000L,
            effectiveTo = 1000L,
            maximumQuantity = BigDecimal("5"),
            minimumQuantity = BigDecimal("10")
        )
        val res = VendorServiceRateValidator.validate(rate)
        assertFalse(res.isValid)
        assertTrue(res.errorMessage!!.contains("negative"))
        assertTrue(res.errorMessage!!.contains("effectiveTo"))
        assertTrue(res.errorMessage!!.contains("maximumQuantity"))
    }

    @Test
    fun `tiered pricing requires tiers and non-overlapping ranges`() {
        val tieredRateNoTiers = validRate().copy(
            pricingMethod = PricingMethod.TIERED,
            tiers = emptyList()
        )
        val res1 = VendorServiceRateValidator.validate(tieredRateNoTiers)
        assertFalse(res1.isValid)
        assertTrue(res1.errorMessage!!.contains("Tiered pricing method requires at least one rate tier"))

        val overlappingTiers = listOf(
            VendorServiceRateTier("t1", "p1", "r1", BigDecimal("0"), BigDecimal("100"), Money(10.0)),
            VendorServiceRateTier("t2", "p1", "r1", BigDecimal("50"), BigDecimal("200"), Money(9.0)) // overlaps at 50 < 100
        )
        val tieredRateOverlap = validRate().copy(
            pricingMethod = PricingMethod.TIERED,
            tiers = overlappingTiers
        )
        val res2 = VendorServiceRateValidator.validate(tieredRateOverlap)
        assertFalse(res2.isValid)
        assertTrue(res2.errorMessage!!.contains("overlaps"))
    }
}
