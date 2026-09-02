package com.sucharu.sucharupro.domain.service.substratereservation

import com.sucharu.sucharupro.domain.model.inventory.InventoryProduct
import com.sucharu.sucharupro.domain.model.printingcalculator.MeasurementUnit
import com.sucharu.sucharupro.domain.model.printingcalculator.PaperMaterialSpecification
import com.sucharu.sucharupro.domain.model.printingcalculator.PaperStockType
import com.sucharu.sucharupro.domain.model.printingcalculator.PrintingDimension
import com.sucharu.sucharupro.domain.model.substratereservation.SubstrateSkuMatchConfidence
import org.junit.Assert.*
import org.junit.Test
import java.math.BigDecimal

class SubstrateReservationDomainTest {

    private val resolver = SubstrateRequirementResolver()
    private val matcher = SubstrateSkuMatcher()

    @Test
    fun `test requirement resolver accurately computes gross demand, reams, and physical tonnage`() {
        val spec = PaperMaterialSpecification(
            materialCode = "ART-300-25X36",
            materialName = "Art Card 300 GSM",
            stockType = PaperStockType.ART_CARD,
            gsm = BigDecimal("300.0000"),
            sheetDimension = PrintingDimension(BigDecimal("635.0000"), BigDecimal("914.4000"), MeasurementUnit.MILLIMETERS)
        )

        val req = resolver.resolveRequirement(
            tenantId = "TENANT-001",
            orderId = "ORD-101",
            orderItemId = "ITEM-01",
            calculationId = "CALC-001",
            materialSpec = spec,
            productiveSheetsRequired = 4500L,
            wasteSheetsRequired = 500L
        )

        assertEquals(5000L, req.totalSheetsRequired)
        assertEquals(BigDecimal("10.0000"), req.totalReamsRequired)
        assertTrue(req.totalWeightKg > BigDecimal.ZERO)
        assertEquals("ART-300-25X36", req.requestedMaterialCode)
    }

    @Test
    fun `test SKU matcher matches exact normalized SKU and evaluates availability`() {
        val spec = PaperMaterialSpecification(
            materialCode = "ART-300-25X36",
            materialName = "Art Card 300 GSM",
            stockType = PaperStockType.ART_CARD,
            gsm = BigDecimal("300.0000"),
            sheetDimension = PrintingDimension(BigDecimal("635.0000"), BigDecimal("914.4000"), MeasurementUnit.MILLIMETERS)
        )

        val req = resolver.resolveRequirement(
            tenantId = "TENANT-001",
            orderId = "ORD-101",
            orderItemId = "ITEM-01",
            materialSpec = spec,
            productiveSheetsRequired = 2000L,
            wasteSheetsRequired = 200L
        )

        val inventoryProducts = listOf(
            InventoryProduct(
                id = "PROD-01",
                sku = "ART-300-25X36",
                name = "Art Card 300 GSM (25x36)",
                createdAt = "2026-09-01T00:00:00Z",
                updatedAt = "2026-09-01T00:00:00Z",
                createdBy = "system"
            )
        )

        val res = matcher.matchSku(
            requirement = req,
            inventoryProducts = inventoryProducts,
            onHandPhysicalSheets = 10000L,
            currentlyReservedSheets = 2000L
        )

        assertEquals(SubstrateSkuMatchConfidence.EXACT_SKU_MATCH, res.confidence)
        assertEquals("PROD-01", res.matchedProductId)
        assertEquals(8000L, res.availableReservableSheets)
        assertTrue(res.isSufficientStockAvailable)
        assertEquals(0L, res.missingDeficitSheets)
    }

    @Test
    fun `test SKU matcher detects stock deficit and computes exact missing sheets`() {
        val spec = PaperMaterialSpecification(
            materialCode = "ART-300-25X36",
            materialName = "Art Card 300 GSM",
            stockType = PaperStockType.ART_CARD,
            gsm = BigDecimal("300.0000"),
            sheetDimension = PrintingDimension(BigDecimal("635.0000"), BigDecimal("914.4000"), MeasurementUnit.MILLIMETERS)
        )

        val req = resolver.resolveRequirement(
            tenantId = "TENANT-001",
            orderId = "ORD-101",
            orderItemId = "ITEM-01",
            materialSpec = spec,
            productiveSheetsRequired = 8000L,
            wasteSheetsRequired = 1000L
        )

        val inventoryProducts = listOf(
            InventoryProduct(
                id = "PROD-01",
                sku = "ART-300-25X36",
                name = "Art Card 300 GSM (25x36)",
                createdAt = "2026-09-01T00:00:00Z",
                updatedAt = "2026-09-01T00:00:00Z",
                createdBy = "system"
            )
        )

        val res = matcher.matchSku(
            requirement = req,
            inventoryProducts = inventoryProducts,
            onHandPhysicalSheets = 5000L,
            currentlyReservedSheets = 1000L
        )

        assertEquals(4000L, res.availableReservableSheets)
        assertFalse(res.isSufficientStockAvailable)
        assertEquals(5000L, res.missingDeficitSheets) // 9000 - 4000 = 5000 deficit
        assertNotNull(res.diagnosticReason)
    }
}
