package com.sucharu.sucharupro.domain.service.imposition

import com.sucharu.sucharupro.domain.model.imposition.*
import com.sucharu.sucharupro.domain.model.printingcalculator.MeasurementUnit
import com.sucharu.sucharupro.domain.model.printingcalculator.PaperStockType
import com.sucharu.sucharupro.domain.model.printingcalculator.PrintingDimension
import org.junit.Assert.*
import org.junit.Test
import java.math.BigDecimal

/**
 * Unit Tests for [CtpOutputGenerationEngine].
 * Module 18 Step 05.
 */
class CtpOutputGenerationEngineTest {

    private val tenantId = "tenant_test_ctp"

    private fun createSample16ppSignatureSpec(): SignatureImpositionSpecification {
        val pageDim = PrintingDimension(BigDecimal("210.0000"), BigDecimal("297.0000"), MeasurementUnit.MILLIMETERS)
        val sheetDim = PrintingDimension(BigDecimal("635.0000"), BigDecimal("914.4000"), MeasurementUnit.MILLIMETERS)

        return SignatureImpositionEngine.optimizeSignatureImposition(
            tenantId = tenantId,
            name = "Catalog 16pp Signature Imposition",
            jobId = "JOB-PUB-101",
            orderId = "ORD-991",
            orderItemId = "ITEM-01",
            productName = "Product Catalog 16pp",
            totalPages = 16,
            signaturePageCount = 16,
            bindingMethod = BindingMethod.SADDLE_STITCH,
            sheetTurningMethod = SheetTurningMethod.SHEETWISE,
            foldingScheme = FoldingScheme.RIGHT_ANGLE_16PP,
            pageDimension = pageDim,
            parentSheetDimension = sheetDim,
            requiredQuantity = 1000L,
            paperStockType = PaperStockType.ART_PAPER,
            gsm = BigDecimal("150.0000")
        )
    }

    @Test
    fun testGenerateFromSignature_producesCorrectPlatesAndSeparations() {
        val sigSpec = createSample16ppSignatureSpec()
        val ctpSpec = CtpOutputGenerationEngine.generateFromSignatureImposition(
            signatureSpec = sigSpec,
            colorSeparations = listOf(
                PlateColorSeparation.CYAN,
                PlateColorSeparation.MAGENTA,
                PlateColorSeparation.YELLOW,
                PlateColorSeparation.BLACK
            )
        )

        assertNotNull(ctpSpec)
        assertEquals(tenantId, ctpSpec.tenantId)
        assertEquals("JOB-PUB-101", ctpSpec.jobId)
        assertEquals(CtpOutputStatus.GENERATED, ctpSpec.status)

        // 16pp Sheetwise has 2 forms (Front and Back). With 4 color separations, total plates = 8 plates.
        val pkg = ctpSpec.outputPackage
        assertEquals(8, pkg.totalPlatesCount)
        assertEquals(4, pkg.frontPlatesCount)
        assertEquals(4, pkg.backPlatesCount)
        assertEquals(0, pkg.spotColorsCount)

        // Verify plate dimensions
        assertTrue(pkg.plateWidthMm > pkg.pressSheetWidthMm)
        assertTrue(pkg.plateHeightMm > pkg.pressSheetHeightMm)
    }

    @Test
    fun testGenerateFromSignature_withSpotColors_includesSpotPlates() {
        val sigSpec = createSample16ppSignatureSpec()
        val ctpSpec = CtpOutputGenerationEngine.generateFromSignatureImposition(
            signatureSpec = sigSpec,
            colorSeparations = listOf(
                PlateColorSeparation.CYAN,
                PlateColorSeparation.BLACK
            ),
            spotColorNames = listOf("Pantone 185 C", "Gold Foil Varnish")
        )

        val pkg = ctpSpec.outputPackage
        // 2 forms * (2 process colors + 2 spot colors) = 8 plates
        assertEquals(8, pkg.totalPlatesCount)
        assertEquals(4, pkg.spotColorsCount)

        val spotPlates = pkg.plates.filter { it.colorSeparation == PlateColorSeparation.SPOT_PANTONE }
        assertEquals(4, spotPlates.size)
        assertTrue(spotPlates.any { it.spotColorName == "Pantone 185 C" })
        assertTrue(spotPlates.any { it.spotColorName == "Gold Foil Varnish" })
    }

    @Test
    fun testGeneratePrepressMarks_containsRegistrationCropAndColorBars() {
        val sigSpec = createSample16ppSignatureSpec()
        val ctpSpec = CtpOutputGenerationEngine.generateFromSignatureImposition(
            signatureSpec = sigSpec,
            markPolicy = PrepressMarkPolicy(
                includeRegistrationMarks = true,
                includeCropMarks = true,
                includeBleedMarks = true,
                includeColorBars = true,
                includePlateSlugs = true
            )
        )

        val marks = ctpSpec.outputPackage.globalMarks
        assertTrue(marks.isNotEmpty())

        val regMarks = marks.filter { it.markType == PrepressMarkType.REGISTRATION_TARGET }
        assertTrue("Registration marks should be generated", regMarks.size >= 4)

        val cropMarks = marks.filter { it.markType == PrepressMarkType.CROP_CORNER_MARK }
        assertTrue("Crop marks should be generated", cropMarks.size >= 4)

        val bleedMarks = marks.filter { it.markType == PrepressMarkType.BLEED_LINE_MARK }
        assertTrue("Bleed mark should be generated", bleedMarks.isNotEmpty())

        val colorBars = marks.filter { it.markType == PrepressMarkType.COLOR_CALIBRATION_BAR }
        assertTrue("Color calibration bar should be generated", colorBars.isNotEmpty())

        val slugs = marks.filter { it.markType == PrepressMarkType.PLATE_IDENTIFIER_SLUG }
        assertTrue("Plate identifier slug should be generated", slugs.isNotEmpty())
    }

    @Test
    fun testDeterministicSha256IntegrityHash() {
        val sigSpec = createSample16ppSignatureSpec()
        val ctpSpec1 = CtpOutputGenerationEngine.generateFromSignatureImposition(signatureSpec = sigSpec)
        val ctpSpec2 = CtpOutputGenerationEngine.generateFromSignatureImposition(signatureSpec = sigSpec)

        assertEquals("Same input must generate identical integrity hash", ctpSpec1.integrityHash, ctpSpec2.integrityHash)
        assertEquals(64, ctpSpec1.integrityHash.length) // 64 hex characters for SHA-256
    }

    @Test(expected = IllegalArgumentException::class)
    fun testPlateGeometryValidation_rejectsTooSmallPlate() {
        val sigSpec = createSample16ppSignatureSpec()
        // Parent sheet is 635 x 914.4. A plate smaller than sheet + margins must be rejected.
        val tooSmallPlate = PlateDimensionSpec(
            plateWidthMm = BigDecimal("500.0000"), // smaller than sheet 635
            plateHeightMm = BigDecimal("600.0000")
        )

        CtpOutputGenerationEngine.generateFromSignatureImposition(
            signatureSpec = sigSpec,
            plateDimensionSpec = tooSmallPlate
        )
    }

    @Test
    fun testGenerateFromSingleJobImposition_producesValidFrontPlates() {
        val sheetDim = PrintingDimension(BigDecimal("635.0000"), BigDecimal("914.4000"), MeasurementUnit.MILLIMETERS)
        val itemDim = PrintingDimension(BigDecimal("210.0000"), BigDecimal("297.0000"), MeasurementUnit.MILLIMETERS)

        val singleJobSpec = SingleJobImpositionEngine().calculateOptimalLayout(
            tenantId = tenantId,
            jobId = "JOB-SINGLE-01",
            orderId = "ORD-01",
            orderItemId = "ITEM-01",
            productName = "A4 Poster",
            parentSheetDimension = sheetDim,
            finishedItemDimension = itemDim,
            requiredQuantity = 500L
        )

        val ctpSpec = CtpOutputGenerationEngine.generateFromSingleJobImposition(
            impositionSpec = singleJobSpec
        )

        assertNotNull(ctpSpec)
        assertEquals("SINGLE_JOB", ctpSpec.sourceImpositionType)
        assertEquals(4, ctpSpec.outputPackage.totalPlatesCount)
        assertEquals(4, ctpSpec.outputPackage.frontPlatesCount)
        assertEquals(0, ctpSpec.outputPackage.backPlatesCount)
    }
}
