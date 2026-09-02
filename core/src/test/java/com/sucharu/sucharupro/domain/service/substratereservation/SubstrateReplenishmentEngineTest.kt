package com.sucharu.sucharupro.domain.service.substratereservation

import com.sucharu.sucharupro.domain.model.printingcalculator.MeasurementUnit
import com.sucharu.sucharupro.domain.model.printingcalculator.PaperStockType
import com.sucharu.sucharupro.domain.model.printingcalculator.PrintingDimension
import com.sucharu.sucharupro.domain.model.substratereservation.*
import com.sucharu.sucharupro.domain.model.vendor.Vendor
import com.sucharu.sucharupro.domain.model.vendor.VendorStatus
import org.junit.Assert.*
import org.junit.Test
import java.math.BigDecimal

class SubstrateReplenishmentEngineTest {

    private val tenantId = "TENANT-TEST"
    private val policy = SubstrateReplenishmentPolicy(
        policyId = "POL-001",
        tenantId = tenantId,
        sku = "ART-300-25X36",
        policyType = ReplenishmentPolicyType.DEMAND_AWARE,
        minimumStockSheets = 2000L,
        safetyStockSheets = 4000L,
        reorderPointSheets = 10000L,
        targetStockSheets = 30000L,
        minimumOrderQuantitySheets = 5000L,
        standardPackReamSize = 500,
        leadTimeDays = 5,
        policyVersion = "1.0.0"
    )

    private val dimension = PrintingDimension(
        width = BigDecimal("635.0000"),
        height = BigDecimal("914.4000"),
        unit = MeasurementUnit.MILLIMETERS
    )

    private val vendors = listOf(
        Vendor(
            vendorId = "V-02",
            projectId = tenantId,
            vendorCode = "VND-B",
            vendorName = "Beta Paper Corp",
            status = VendorStatus.ACTIVE
        ),
        Vendor(
            vendorId = "V-01",
            projectId = tenantId,
            vendorCode = "VND-A",
            vendorName = "Alpha Paper Ltd",
            status = VendorStatus.ACTIVE
        ),
        Vendor(
            vendorId = "V-03",
            projectId = tenantId,
            vendorCode = "VND-C",
            vendorName = "Inactive Mills",
            status = VendorStatus.SUSPENDED
        )
    )

    @Test
    fun `test healthy stock results in NORMAL trigger state`() {
        val input = SubstrateReplenishmentEngine.EvaluationInput(
            tenantId = tenantId,
            productId = "P-1",
            sku = "ART-300-25X36",
            materialName = "Art Card 300",
            stockType = PaperStockType.ART_CARD,
            gsm = BigDecimal("300.0000"),
            sheetDimension = dimension,
            warehouseId = "WH-1",
            warehouseName = "Main WH",
            onHandPhysicalSheets = 25000L,
            activeReservedSheets = 5000L, // Available = 20000 > 10000 + 1500
            policy = policy,
            candidateVendors = vendors
        )

        val result = SubstrateReplenishmentEngine.evaluate(input)
        assertEquals(ReplenishmentTriggerState.NORMAL, result.triggerState)
        assertFalse(result.isReorderRequired)
        assertEquals(0L, result.recommendedReorderSheets)
    }

    @Test
    fun `test stock approaching boundary triggers WATCH state`() {
        val input = SubstrateReplenishmentEngine.EvaluationInput(
            tenantId = tenantId,
            productId = "P-1",
            sku = "ART-300-25X36",
            materialName = "Art Card 300",
            stockType = PaperStockType.ART_CARD,
            gsm = BigDecimal("300.0000"),
            sheetDimension = dimension,
            warehouseId = "WH-1",
            warehouseName = "Main WH",
            onHandPhysicalSheets = 15000L,
            activeReservedSheets = 4000L, // Available = 11000 <= 11500 (10000 + 15%)
            policy = policy,
            candidateVendors = vendors
        )

        val result = SubstrateReplenishmentEngine.evaluate(input)
        assertEquals(ReplenishmentTriggerState.WATCH, result.triggerState)
        assertFalse(result.isReorderRequired)
        assertEquals(0L, result.recommendedReorderSheets)
    }

    @Test
    fun `test stock below reorder point triggers REORDER_TRIGGERED with recommended quantity`() {
        val input = SubstrateReplenishmentEngine.EvaluationInput(
            tenantId = tenantId,
            productId = "P-1",
            sku = "ART-300-25X36",
            materialName = "Art Card 300",
            stockType = PaperStockType.ART_CARD,
            gsm = BigDecimal("300.0000"),
            sheetDimension = dimension,
            warehouseId = "WH-1",
            warehouseName = "Main WH",
            onHandPhysicalSheets = 12000L,
            activeReservedSheets = 4000L, // Available = 8000 <= 10000
            policy = policy,
            candidateVendors = vendors
        )

        val result = SubstrateReplenishmentEngine.evaluate(input)
        assertEquals(ReplenishmentTriggerState.REORDER_TRIGGERED, result.triggerState)
        assertTrue(result.isReorderRequired)
        assertEquals(ReplenishmentPriority.NORMAL, result.priority)
        assertEquals(ReplenishmentReason.REORDER_POINT_REACHED, result.primaryReason)
        // Deficit = Target(30000) - Available(8000) = 22000 sheets
        assertEquals(22000L, result.projectedShortfallSheets)
        assertEquals(22000L, result.recommendedReorderSheets)
        assertEquals(BigDecimal("44.0000"), result.recommendedReorderReams)
    }

    @Test
    fun `test safety stock breach elevates priority to HIGH`() {
        val input = SubstrateReplenishmentEngine.EvaluationInput(
            tenantId = tenantId,
            productId = "P-1",
            sku = "ART-300-25X36",
            materialName = "Art Card 300",
            stockType = PaperStockType.ART_CARD,
            gsm = BigDecimal("300.0000"),
            sheetDimension = dimension,
            warehouseId = "WH-1",
            warehouseName = "Main WH",
            onHandPhysicalSheets = 5000L,
            activeReservedSheets = 2000L, // Available = 3000 < SafetyStock(4000)
            policy = policy,
            candidateVendors = vendors
        )

        val result = SubstrateReplenishmentEngine.evaluate(input)
        assertEquals(ReplenishmentTriggerState.REORDER_TRIGGERED, result.triggerState)
        assertEquals(ReplenishmentPriority.HIGH, result.priority)
        assertEquals(ReplenishmentReason.SAFETY_STOCK_BREACH, result.primaryReason)
    }

    @Test
    fun `test minimum stock floor violation escalates priority to CRITICAL`() {
        val input = SubstrateReplenishmentEngine.EvaluationInput(
            tenantId = tenantId,
            productId = "P-1",
            sku = "ART-300-25X36",
            materialName = "Art Card 300",
            stockType = PaperStockType.ART_CARD,
            gsm = BigDecimal("300.0000"),
            sheetDimension = dimension,
            warehouseId = "WH-1",
            warehouseName = "Main WH",
            onHandPhysicalSheets = 1500L, // Physical On-Hand < MinimumStock(2000)
            activeReservedSheets = 0L,
            policy = policy,
            candidateVendors = vendors
        )

        val result = SubstrateReplenishmentEngine.evaluate(input)
        assertEquals(ReplenishmentTriggerState.REORDER_TRIGGERED, result.triggerState)
        assertEquals(ReplenishmentPriority.CRITICAL, result.priority)
        assertEquals(ReplenishmentReason.MIN_STOCK_VIOLATION, result.primaryReason)
    }

    @Test
    fun `test reorder quantity rounds up to standard ream pack size and respects MOQ`() {
        val smallShortfallPolicy = policy.copy(
            targetStockSheets = 10100L,
            minimumOrderQuantitySheets = 5000L,
            standardPackReamSize = 500
        )

        val input = SubstrateReplenishmentEngine.EvaluationInput(
            tenantId = tenantId,
            productId = "P-1",
            sku = "ART-300-25X36",
            materialName = "Art Card 300",
            stockType = PaperStockType.ART_CARD,
            gsm = BigDecimal("300.0000"),
            sheetDimension = dimension,
            warehouseId = "WH-1",
            warehouseName = "Main WH",
            onHandPhysicalSheets = 9000L,
            activeReservedSheets = 0L, // Available = 9000. Deficit = 10100 - 9000 = 1100. But MOQ = 5000.
            policy = smallShortfallPolicy,
            candidateVendors = vendors
        )

        val result = SubstrateReplenishmentEngine.evaluate(input)
        assertEquals(5000L, result.recommendedReorderSheets)
        assertEquals(BigDecimal("10.0000"), result.recommendedReorderReams)
    }

    @Test
    fun `test deterministic supplier ranking orders active vendors by name and filters suspended`() {
        val ranked = SubstrateReplenishmentEngine.rankSuppliers(vendors, policy)
        assertEquals(2, ranked.size)
        // Alpha Paper Ltd before Beta Paper Corp
        assertEquals("Alpha Paper Ltd", ranked[0].vendorName)
        assertEquals(1, ranked[0].rank)
        assertEquals("Beta Paper Corp", ranked[1].vendorName)
        assertEquals(2, ranked[1].rank)
    }

    @Test
    fun `test SHA-256 fingerprint remains identical for identical inventory state`() {
        val fp1 = SubstrateReplenishmentEngine.computeFingerprint(
            tenantId = tenantId,
            sku = "ART-300",
            warehouseId = "WH-1",
            policyVersion = "1.0.0",
            onHand = 10000L,
            reserved = 2000L,
            inbound = 500L,
            demand = 1000L,
            triggerState = ReplenishmentTriggerState.REORDER_TRIGGERED,
            recommendedSheets = 15000L
        )

        val fp2 = SubstrateReplenishmentEngine.computeFingerprint(
            tenantId = tenantId,
            sku = "ART-300",
            warehouseId = "WH-1",
            policyVersion = "1.0.0",
            onHand = 10000L,
            reserved = 2000L,
            inbound = 500L,
            demand = 1000L,
            triggerState = ReplenishmentTriggerState.REORDER_TRIGGERED,
            recommendedSheets = 15000L
        )

        assertEquals(fp1, fp2)
        assertEquals(64, fp1.length) // Valid SHA-256 hex string
    }
}
