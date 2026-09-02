package com.sucharu.sucharupro.vendor

import com.sucharu.sucharupro.domain.model.vendor.KpiDirection
import com.sucharu.sucharupro.domain.model.vendor.KpiType
import com.sucharu.sucharupro.domain.model.vendor.MeasurementConfidenceState
import com.sucharu.sucharupro.domain.model.vendor.PerformanceRating
import com.sucharu.sucharupro.domain.model.vendor.VendorPerformanceScorecardItem
import com.sucharu.sucharupro.domain.service.vendor.VendorPerformanceCalculator
import org.junit.Assert.*
import org.junit.Test
import java.time.Instant

class VendorPerformanceCalculationTest {

    @Test
    fun testOnTimeDeliveryRate() {
        val res = VendorPerformanceCalculator.calculateOnTimeDeliveryRate(8.0, 10.0)
        assertEquals(80.0, res.value, 0.001)

        val resZero = VendorPerformanceCalculator.calculateOnTimeDeliveryRate(0.0, 0.0)
        assertEquals(0.0, resZero.value, 0.001)
        assertEquals(MeasurementConfidenceState.NO_DATA, resZero.confidenceState)

        val resCapped = VendorPerformanceCalculator.calculateOnTimeDeliveryRate(15.0, 10.0)
        assertEquals(100.0, resCapped.value, 0.001)
    }

    @Test
    fun testQualityAcceptanceRate() {
        val res = VendorPerformanceCalculator.calculateQualityAcceptanceRate(950.0, 1000.0)
        assertEquals(95.0, res.value, 0.001)

        val resZero = VendorPerformanceCalculator.calculateQualityAcceptanceRate(0.0, 0.0)
        assertEquals(0.0, resZero.value, 0.001)
    }

    @Test
    fun testDefectRate() {
        val res = VendorPerformanceCalculator.calculateDefectRate(25.0, 1000.0)
        assertEquals(2.5, res.value, 0.001)
    }

    @Test
    fun testPriceVariance() {
        val res = VendorPerformanceCalculator.calculatePriceVarianceRate(5.0, 100.0)
        assertEquals(5.0, res.value, 0.001)
    }

    @Test
    fun testScoreNormalizationHigherIsBetter() {
        val norm100 = VendorPerformanceCalculator.normalizeScore(100.0, 90.0, KpiDirection.HIGHER_IS_BETTER, 80.0, 100.0)
        assertEquals(100.0, norm100, 0.001)

        val normTarget = VendorPerformanceCalculator.normalizeScore(90.0, 90.0, KpiDirection.HIGHER_IS_BETTER, 80.0, 100.0)
        assertEquals(100.0, normTarget, 0.001)

        val normBelowMin = VendorPerformanceCalculator.normalizeScore(70.0, 90.0, KpiDirection.HIGHER_IS_BETTER, 80.0, 100.0)
        assertEquals(0.0, normBelowMin, 0.001)
    }

    @Test
    fun testScoreNormalizationLowerIsBetter() {
        val normZeroDefect = VendorPerformanceCalculator.normalizeScore(0.0, 2.0, KpiDirection.LOWER_IS_BETTER, null, 5.0)
        assertEquals(100.0, normZeroDefect, 0.001)

        val normTarget = VendorPerformanceCalculator.normalizeScore(2.0, 2.0, KpiDirection.LOWER_IS_BETTER, null, 5.0)
        assertEquals(100.0, normTarget, 0.001)

        val normMax = VendorPerformanceCalculator.normalizeScore(5.0, 2.0, KpiDirection.LOWER_IS_BETTER, null, 5.0)
        assertEquals(0.0, normMax, 0.001)
    }

    @Test
    fun testWeightedScoreCalculation() {
        val items = listOf(
            VendorPerformanceScorecardItem(
                itemId = "I1",
                scorecardId = "SC1",
                kpiId = "K1",
                kpiCode = "OTD",
                kpiName = "On Time Delivery",
                kpiType = KpiType.DELIVERY,
                weight = 2.0,
                direction = KpiDirection.HIGHER_IS_BETTER,
                targetValue = 95.0,
                actualValue = 90.0,
                normalizedScore = 90.0,
                weightedScore = 180.0,
                numerator = 9.0,
                denominator = 10.0,
                sampleSize = 10
            ),
            VendorPerformanceScorecardItem(
                itemId = "I2",
                scorecardId = "SC1",
                kpiId = "K2",
                kpiCode = "QUAL",
                kpiName = "Quality Acceptance",
                kpiType = KpiType.QUALITY,
                weight = 1.0,
                direction = KpiDirection.HIGHER_IS_BETTER,
                targetValue = 98.0,
                actualValue = 80.0,
                normalizedScore = 80.0,
                weightedScore = 80.0,
                numerator = 80.0,
                denominator = 100.0,
                sampleSize = 100
            )
        )
        // (90 * 2 + 80 * 1) / 3 = 260 / 3 = 86.67
        assertEquals(86.67, VendorPerformanceCalculator.calculateOverallScore(items), 0.01)
    }

    @Test
    fun testMapScoreToRating() {
        assertEquals(PerformanceRating.EXCELLENT, VendorPerformanceCalculator.mapScoreToRating(95.0))
        assertEquals(PerformanceRating.GOOD, VendorPerformanceCalculator.mapScoreToRating(85.0))
        assertEquals(PerformanceRating.ACCEPTABLE, VendorPerformanceCalculator.mapScoreToRating(72.0))
        assertEquals(PerformanceRating.NEEDS_IMPROVEMENT, VendorPerformanceCalculator.mapScoreToRating(55.0))
        assertEquals(PerformanceRating.CRITICAL, VendorPerformanceCalculator.mapScoreToRating(35.0))
    }
}
