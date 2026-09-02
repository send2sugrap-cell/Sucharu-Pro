package com.sucharu.sucharupro.domain.service.printingcalculator

import com.sucharu.sucharupro.domain.model.printingcalculator.ColorSpecification
import com.sucharu.sucharupro.domain.model.printingcalculator.MeasurementUnit
import com.sucharu.sucharupro.domain.model.printingcalculator.NormalizedPrintingSpecification
import com.sucharu.sucharupro.domain.model.printingcalculator.PaperMaterialSpecification
import com.sucharu.sucharupro.domain.model.printingcalculator.PrintingCalculationRequest
import com.sucharu.sucharupro.domain.model.printingcalculator.PrintingCalculatorMathUtils
import com.sucharu.sucharupro.domain.model.printingcalculator.PrintingDimension
import com.sucharu.sucharupro.domain.model.printingcalculator.QuantitySpecification
import com.sucharu.sucharupro.domain.model.printingcalculator.WasteAllowanceSpecification
import java.math.BigDecimal

/**
 * Normalizes raw Printing Calculation Requests into unified canonical specifications.
 * All dimensions normalized to Millimeters (scale = 4).
 * Module 17 Step 01.
 */
object PrintingSpecificationNormalizer {

    fun normalize(request: PrintingCalculationRequest): NormalizedPrintingSpecification {
        val finishedWidthMm = PrintingCalculatorMathUtils.toMillimeters(request.finishedWidth, request.dimensionUnit)
        val finishedHeightMm = PrintingCalculatorMathUtils.toMillimeters(request.finishedHeight, request.dimensionUnit)

        val finishedDimension = PrintingDimension(
            width = PrintingCalculatorMathUtils.scale4(request.finishedWidth),
            height = PrintingCalculatorMathUtils.scale4(request.finishedHeight),
            unit = request.dimensionUnit
        )

        val normalizedDimensionMm = PrintingDimension(
            width = finishedWidthMm,
            height = finishedHeightMm,
            unit = MeasurementUnit.MILLIMETERS
        )

        val sheetDimension = if (request.sheetWidth != null && request.sheetHeight != null) {
            val sWMm = PrintingCalculatorMathUtils.toMillimeters(request.sheetWidth, request.sheetDimensionUnit)
            val sHMm = PrintingCalculatorMathUtils.toMillimeters(request.sheetHeight, request.sheetDimensionUnit)
            PrintingDimension(
                width = sWMm,
                height = sHMm,
                unit = MeasurementUnit.MILLIMETERS
            )
        } else {
            null
        }

        val materialSpec = PaperMaterialSpecification(
            materialName = request.materialName.trim(),
            stockType = request.stockType,
            gsm = request.gsm?.let { PrintingCalculatorMathUtils.scale4(it) },
            sheetDimension = sheetDimension,
            unitPricePerSheet = request.materialUnitPricePerSheet?.let { PrintingCalculatorMathUtils.scale4(it) },
            currency = request.currency
        )

        val quantitySpec = QuantitySpecification(
            orderedQuantity = request.quantity,
            unit = request.quantityUnit,
            normalizedQuantity = request.quantity,
            spoilageAllowanceQuantity = 0L
        )

        val colorSpec = ColorSpecification(
            colorMode = request.colorMode,
            frontColorsCount = request.frontColorsCount,
            backColorsCount = if (request.sides.isDoubleSided) request.backColorsCount else 0,
            spotColorsCount = request.spotColorsCount
        )

        val wasteSpec = WasteAllowanceSpecification(
            setupSheets = request.setupSheets,
            runningWastePercentage = PrintingCalculatorMathUtils.scale4(request.runningWastePercentage),
            finishingWastePercentage = PrintingCalculatorMathUtils.scale4(request.finishingWastePercentage)
        )

        return NormalizedPrintingSpecification(
            jobTitle = request.jobTitle.trim(),
            productType = request.productType,
            finishedDimension = finishedDimension,
            normalizedDimensionMm = normalizedDimensionMm,
            quantity = quantitySpec,
            material = materialSpec,
            processType = request.processType,
            sides = request.sides,
            color = colorSpec,
            finishingOperations = request.finishingOperations,
            waste = wasteSpec,
            machine = request.machine,
            currency = request.currency
        )
    }
}
