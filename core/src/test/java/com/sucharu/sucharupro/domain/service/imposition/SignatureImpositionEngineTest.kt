package com.sucharu.sucharupro.domain.service.imposition

import com.sucharu.sucharupro.domain.model.imposition.*
import com.sucharu.sucharupro.domain.model.printingcalculator.MeasurementUnit
import com.sucharu.sucharupro.domain.model.printingcalculator.PaperStockType
import com.sucharu.sucharupro.domain.model.printingcalculator.PrintingDimension
import org.junit.Assert.*
import org.junit.Test
import java.math.BigDecimal

/**
 * Unit Tests for Multi-Page Signature Imposition Engine.
 * Module 18 Step 04.
 */
class SignatureImpositionEngineTest {

    private val a4Page = PrintingDimension(BigDecimal("210.0000"), BigDecimal("297.0000"), MeasurementUnit.MILLIMETERS)
    private val a5Page = PrintingDimension(BigDecimal("148.5000"), BigDecimal("210.0000"), MeasurementUnit.MILLIMETERS)
    private val a6Page = PrintingDimension(BigDecimal("105.0000"), BigDecimal("148.5000"), MeasurementUnit.MILLIMETERS)
    private val pressSheet25x36 = PrintingDimension(BigDecimal("635.0000"), BigDecimal("914.4000"), MeasurementUnit.MILLIMETERS)

    @Test
    fun `optimizeSignatureImposition should generate 16pp signature layout with front and back forms`() {
        val spec = SignatureImpositionEngine.optimizeSignatureImposition(
            tenantId = "TENANT-001",
            name = "64pp Saddle-Stitched Book",
            jobId = "JOB-BOOK-64",
            orderId = "ORD-001",
            orderItemId = "ITEM-01",
            productName = "Product Catalog 2026",
            totalPages = 64,
            signaturePageCount = 16,
            bindingMethod = BindingMethod.SADDLE_STITCH,
            sheetTurningMethod = SheetTurningMethod.SHEETWISE,
            foldingScheme = FoldingScheme.RIGHT_ANGLE_16PP,
            pageDimension = a4Page,
            parentSheetDimension = pressSheet25x36,
            requiredQuantity = 1000L,
            paperStockType = PaperStockType.ART_PAPER,
            gsm = BigDecimal("150.0000")
        )

        assertNotNull(spec)
        assertEquals(64, spec.totalPages)
        assertEquals(64, spec.paddedTotalPages)
        assertEquals(16, spec.signaturePageCount)
        assertEquals(4, spec.totalSignaturesCount) // 64 / 16 = 4 signatures
        assertEquals(8, spec.signatureForms.size) // 4 signatures * 2 forms (Front & Back)

        // Verify forms have 8 pages per side (4 cols x 2 rows or 2 cols x 4 rows)
        spec.signatureForms.forEach { form ->
            assertEquals(8, form.pagesPerSide)
            assertEquals(8, form.pagePlacements.size)
            assertTrue(form.occupiedAreaMm2 > BigDecimal.ZERO)
        }

        // Verify required sheets
        assertEquals(1000L, spec.commonRequiredSheets) // 1000 sheets per signature
        assertEquals(4000L, spec.totalParentSheetsRequired) // 4 signatures * 1000 sheets

        // Verify SHA-256 integrity hash is present
        assertNotNull(spec.integrityHash)
        assertTrue(spec.integrityHash.length >= 64)
    }

    @Test
    fun `optimizeSignatureImposition with WORK_AND_TURN should create single plate form and halve press run`() {
        val spec = SignatureImpositionEngine.optimizeSignatureImposition(
            tenantId = "TENANT-001",
            name = "16pp Work-and-Turn Booklet",
            jobId = "JOB-WT-16",
            orderId = "ORD-002",
            orderItemId = "ITEM-02",
            productName = "Quick Guide Booklet",
            totalPages = 16,
            signaturePageCount = 16,
            bindingMethod = BindingMethod.SADDLE_STITCH,
            sheetTurningMethod = SheetTurningMethod.WORK_AND_TURN,
            foldingScheme = FoldingScheme.RIGHT_ANGLE_16PP,
            pageDimension = a4Page,
            parentSheetDimension = pressSheet25x36,
            requiredQuantity = 2000L,
            paperStockType = PaperStockType.ART_PAPER,
            gsm = BigDecimal("150.0000")
        )

        assertEquals(1, spec.totalSignaturesCount)
        assertEquals(1, spec.signatureForms.size) // 1 combined plate form
        assertEquals(SignatureFormSide.WORK_AND_TURN_COMBINED, spec.signatureForms.first().formSide)

        // Work and Turn requires half the sheets (2000 copies / 2 copies per sheet = 1000 sheets)
        assertEquals(1000L, spec.commonRequiredSheets)
        assertEquals(1000L, spec.totalParentSheetsRequired)
    }

    @Test
    fun `optimizeSignatureImposition with Saddle Stitch should calculate progressive creep compensation`() {
        val spec = SignatureImpositionEngine.optimizeSignatureImposition(
            tenantId = "TENANT-001",
            name = "96pp Heavy Saddle Stitch",
            jobId = "JOB-CREEP-96",
            orderId = "ORD-003",
            orderItemId = "ITEM-03",
            productName = "Heavy Magazine",
            totalPages = 96,
            signaturePageCount = 16,
            bindingMethod = BindingMethod.SADDLE_STITCH,
            sheetTurningMethod = SheetTurningMethod.SHEETWISE,
            foldingScheme = FoldingScheme.RIGHT_ANGLE_16PP,
            pageDimension = a4Page,
            parentSheetDimension = pressSheet25x36,
            requiredQuantity = 500L,
            paperStockType = PaperStockType.ART_PAPER,
            gsm = BigDecimal("200.0000"), // 200 GSM * 0.0012 = 0.24mm caliper
            enableCreepCompensation = true
        )

        assertTrue(spec.creepSummary.isEnabled)
        assertTrue(spec.creepSummary.paperCaliperMm > BigDecimal.ZERO)
        assertTrue(spec.creepSummary.totalCreepMm > BigDecimal.ZERO)
        assertTrue(spec.creepSummary.innermostPageShiftMm > BigDecimal.ZERO)

        // Verify that inner signature pages have non-zero creep shift
        val lastSigForms = spec.signatureForms.filter { it.signatureNumber == spec.totalSignaturesCount }
        val hasCreepShift = lastSigForms.flatMap { it.pagePlacements }.any { it.creepShiftXMm > BigDecimal.ZERO }
        assertTrue("Innermost signature pages must have creep shift compensation", hasCreepShift)
    }

    @Test
    fun `optimizeSignatureImposition should pad non-multiple page counts with blank pages`() {
        val spec = SignatureImpositionEngine.optimizeSignatureImposition(
            tenantId = "TENANT-001",
            name = "14pp Booklet Padded to 16pp",
            jobId = "JOB-PAD-14",
            orderId = "ORD-004",
            orderItemId = "ITEM-04",
            productName = "Unpadded Booklet",
            totalPages = 14,
            signaturePageCount = 16,
            bindingMethod = BindingMethod.SADDLE_STITCH,
            sheetTurningMethod = SheetTurningMethod.SHEETWISE,
            foldingScheme = FoldingScheme.RIGHT_ANGLE_16PP,
            pageDimension = a4Page,
            parentSheetDimension = pressSheet25x36,
            requiredQuantity = 500L,
            paperStockType = PaperStockType.ART_PAPER,
            gsm = BigDecimal("150.0000")
        )

        assertEquals(14, spec.totalPages)
        assertEquals(16, spec.paddedTotalPages)
        val blankPages = spec.signatureForms.flatMap { it.pagePlacements }.filter { it.isBlankPage }
        assertEquals(2, blankPages.size) // 16 - 14 = 2 blank pages
    }

    @Test
    fun `optimizeSignatureImposition should support 4pp, 8pp, and 32pp signatures`() {
        // 4pp
        val spec4 = SignatureImpositionEngine.optimizeSignatureImposition(
            tenantId = "TENANT-001", name = "4pp Leaflet", jobId = "J4", orderId = "O4", orderItemId = "I4",
            productName = "4pp Folded Card", totalPages = 4, signaturePageCount = 4,
            bindingMethod = BindingMethod.FOLDED_LEAFLET, sheetTurningMethod = SheetTurningMethod.SHEETWISE,
            foldingScheme = FoldingScheme.HALF_FOLD, pageDimension = a4Page, parentSheetDimension = pressSheet25x36,
            requiredQuantity = 1000L, paperStockType = PaperStockType.ART_CARD, gsm = BigDecimal("250.0000")
        )
        assertEquals(1, spec4.totalSignaturesCount)
        assertEquals(2, spec4.signatureForms.size)

        // 8pp
        val spec8 = SignatureImpositionEngine.optimizeSignatureImposition(
            tenantId = "TENANT-001", name = "8pp Brochure", jobId = "J8", orderId = "O8", orderItemId = "I8",
            productName = "8pp Brochure", totalPages = 8, signaturePageCount = 8,
            bindingMethod = BindingMethod.SADDLE_STITCH, sheetTurningMethod = SheetTurningMethod.SHEETWISE,
            foldingScheme = FoldingScheme.RIGHT_ANGLE_8PP, pageDimension = a4Page, parentSheetDimension = pressSheet25x36,
            requiredQuantity = 1000L, paperStockType = PaperStockType.ART_PAPER, gsm = BigDecimal("150.0000")
        )
        assertEquals(1, spec8.totalSignaturesCount)
        assertEquals(2, spec8.signatureForms.size)

        // 32pp
        val spec32 = SignatureImpositionEngine.optimizeSignatureImposition(
            tenantId = "TENANT-001", name = "32pp Section", jobId = "J32", orderId = "O32", orderItemId = "I32",
            productName = "32pp Section", totalPages = 32, signaturePageCount = 32,
            bindingMethod = BindingMethod.PERFECT_BOUND, sheetTurningMethod = SheetTurningMethod.SHEETWISE,
            foldingScheme = FoldingScheme.DOUBLE_RIGHT_ANGLE_32PP, pageDimension = a6Page, parentSheetDimension = pressSheet25x36,
            requiredQuantity = 1000L, paperStockType = PaperStockType.ART_PAPER, gsm = BigDecimal("100.0000")
        )
        assertEquals(1, spec32.totalSignaturesCount)
        assertEquals(2, spec32.signatureForms.size)
    }

    @Test
    fun `optimizeSignatureImposition should be deterministic and produce identical SHA-256 seal`() {
        val spec1 = SignatureImpositionEngine.optimizeSignatureImposition(
            tenantId = "TENANT-001", name = "Deterministic Test", jobId = "J-DET", orderId = "O-DET", orderItemId = "I-DET",
            productName = "Report", totalPages = 32, signaturePageCount = 16,
            bindingMethod = BindingMethod.SADDLE_STITCH, sheetTurningMethod = SheetTurningMethod.SHEETWISE,
            foldingScheme = FoldingScheme.RIGHT_ANGLE_16PP, pageDimension = a4Page, parentSheetDimension = pressSheet25x36,
            requiredQuantity = 1000L, paperStockType = PaperStockType.ART_PAPER, gsm = BigDecimal("150.0000")
        )

        val spec2 = SignatureImpositionEngine.optimizeSignatureImposition(
            tenantId = "TENANT-001", name = "Deterministic Test", jobId = "J-DET", orderId = "O-DET", orderItemId = "I-DET",
            productName = "Report", totalPages = 32, signaturePageCount = 16,
            bindingMethod = BindingMethod.SADDLE_STITCH, sheetTurningMethod = SheetTurningMethod.SHEETWISE,
            foldingScheme = FoldingScheme.RIGHT_ANGLE_16PP, pageDimension = a4Page, parentSheetDimension = pressSheet25x36,
            requiredQuantity = 1000L, paperStockType = PaperStockType.ART_PAPER, gsm = BigDecimal("150.0000")
        )

        assertEquals(spec1.integrityHash, spec2.integrityHash)
    }
}
