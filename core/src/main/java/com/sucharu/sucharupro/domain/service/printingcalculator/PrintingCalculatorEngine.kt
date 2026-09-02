package com.sucharu.sucharupro.domain.service.printingcalculator

import com.sucharu.sucharupro.domain.model.printingcalculator.CalculationBreakdownItem
import com.sucharu.sucharupro.domain.model.printingcalculator.CalculationDiagnostic
import com.sucharu.sucharupro.domain.model.printingcalculator.CalculationStatus
import com.sucharu.sucharupro.domain.model.printingcalculator.DiagnosticCode
import com.sucharu.sucharupro.domain.model.printingcalculator.DiagnosticSeverity
import com.sucharu.sucharupro.domain.model.printingcalculator.EstimateActualClassification
import com.sucharu.sucharupro.domain.model.printingcalculator.FinishingRequirementResult
import com.sucharu.sucharupro.domain.model.printingcalculator.MaterialRequirementResult
import com.sucharu.sucharupro.domain.model.printingcalculator.NormalizedPrintingSpecification
import com.sucharu.sucharupro.domain.model.printingcalculator.PrintingCalculationRequest
import com.sucharu.sucharupro.domain.model.printingcalculator.PrintingCalculationResult
import com.sucharu.sucharupro.domain.model.printingcalculator.PrintingCalculatorMathUtils
import com.sucharu.sucharupro.domain.model.printingcalculator.PrintingProcessType
import com.sucharu.sucharupro.domain.model.printingcalculator.PrintingRequirementResult
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Core Deterministic Calculation Engine for the Smart Printing Calculator.
 * Module 17 Step 01.
 */
object PrintingCalculatorEngine {

    fun calculate(
        request: PrintingCalculationRequest,
        spec: NormalizedPrintingSpecification,
        initialDiagnostics: List<CalculationDiagnostic>
    ): PrintingCalculationResult {
        val now = System.currentTimeMillis()
        val calculationId = request.calculationId ?: "calc-${request.tenantId}-${now}".take(64)
        val accumulatedDiagnostics = initialDiagnostics.toMutableList()
        val breakdownItems = mutableListOf<CalculationBreakdownItem>()

        val quantity = spec.quantity.normalizedQuantity
        val finishedWMm = spec.normalizedDimensionMm.width
        val finishedHMm = spec.normalizedDimensionMm.height

        // 1. Material Requirement Engine
        val sheetDim = spec.material.sheetDimension
        val materialResult: MaterialRequirementResult = if (sheetDim != null) {
            val cutInfo = PrintingCalculatorMathUtils.calculateItemsPerSheet(
                sheetWidthMm = sheetDim.width,
                sheetHeightMm = sheetDim.height,
                itemWidthMm = finishedWMm,
                itemHeightMm = finishedHMm
            )

            if (cutInfo.itemsPerSheet <= 0) {
                accumulatedDiagnostics.add(
                    CalculationDiagnostic(
                        code = DiagnosticCode.SHEET_SIZE_SMALLER_THAN_ITEM,
                        severity = DiagnosticSeverity.ERROR,
                        message = "No items can be cut from the specified sheet size.",
                        targetField = "sheetDimension"
                    )
                )
                MaterialRequirementResult(
                    finishedItemsPerSheet = 0,
                    cutDirection = cutInfo.cutDirection,
                    productiveSheetsRequired = 0L,
                    wasteSheetsRequired = 0L,
                    totalSheetsRequired = 0L,
                    totalReamsRequired = PrintingCalculatorMathUtils.ZERO,
                    totalWeightKg = null,
                    estimatedMaterialCost = null,
                    costStatus = CalculationStatus.INVALID_REQUEST,
                    missingPriceReason = "Sheet size cannot accommodate item dimensions."
                )
            } else {
                val productiveSheets = PrintingCalculatorMathUtils.calculateProductiveSheets(quantity, cutInfo.itemsPerSheet)
                val wasteSheets = PrintingCalculatorMathUtils.calculateTotalWasteSheets(
                    productiveSheets = productiveSheets,
                    setupSheets = spec.waste.setupSheets,
                    runningWastePercentage = spec.waste.runningWastePercentage,
                    finishingWastePercentage = spec.waste.finishingWastePercentage
                )
                val totalSheets = productiveSheets + wasteSheets
                val reams = PrintingCalculatorMathUtils.calculateReams(totalSheets, spec.material.sheetsPerReam)
                val weightKg = PrintingCalculatorMathUtils.calculatePaperWeightKg(
                    sheetWidthMm = sheetDim.width,
                    sheetHeightMm = sheetDim.height,
                    gsm = spec.material.gsm,
                    totalSheets = totalSheets
                )

                val unitPrice = spec.material.unitPricePerSheet
                val (cost, costStatus, reason) = if (unitPrice != null && unitPrice > BigDecimal.ZERO) {
                    val matCost = PrintingCalculatorMathUtils.scale4(BigDecimal(totalSheets).multiply(unitPrice))
                    Triple(matCost, CalculationStatus.SUCCESSFUL, null)
                } else {
                    Triple(null, CalculationStatus.PARTIAL_CALCULATION, "Material unit price per sheet not supplied.")
                }

                MaterialRequirementResult(
                    finishedItemsPerSheet = cutInfo.itemsPerSheet,
                    cutDirection = cutInfo.cutDirection,
                    productiveSheetsRequired = productiveSheets,
                    wasteSheetsRequired = wasteSheets,
                    totalSheetsRequired = totalSheets,
                    totalReamsRequired = reams,
                    totalWeightKg = weightKg,
                    estimatedMaterialCost = cost,
                    costStatus = costStatus,
                    missingPriceReason = reason
                )
            }
        } else {
            // Direct piece-to-sheet calculation
            val productiveSheets = quantity
            val wasteSheets = PrintingCalculatorMathUtils.calculateTotalWasteSheets(
                productiveSheets = productiveSheets,
                setupSheets = spec.waste.setupSheets,
                runningWastePercentage = spec.waste.runningWastePercentage,
                finishingWastePercentage = spec.waste.finishingWastePercentage
            )
            val totalSheets = productiveSheets + wasteSheets
            val unitPrice = spec.material.unitPricePerSheet
            val (cost, costStatus, reason) = if (unitPrice != null && unitPrice > BigDecimal.ZERO) {
                val matCost = PrintingCalculatorMathUtils.scale4(BigDecimal(totalSheets).multiply(unitPrice))
                Triple(matCost, CalculationStatus.SUCCESSFUL, null)
            } else {
                Triple(null, CalculationStatus.PARTIAL_CALCULATION, "Parent sheet dimensions and unit price not supplied.")
            }

            MaterialRequirementResult(
                finishedItemsPerSheet = 1,
                cutDirection = "DIRECT_1_TO_1",
                productiveSheetsRequired = productiveSheets,
                wasteSheetsRequired = wasteSheets,
                totalSheetsRequired = totalSheets,
                totalReamsRequired = PrintingCalculatorMathUtils.calculateReams(totalSheets, spec.material.sheetsPerReam),
                totalWeightKg = null,
                estimatedMaterialCost = cost,
                costStatus = costStatus,
                missingPriceReason = reason
            )
        }

        // Add Material to Breakdown
        if (materialResult.totalSheetsRequired > 0L) {
            breakdownItems.add(
                CalculationBreakdownItem(
                    componentCode = "MAT_PAPER",
                    description = "Paper / Substrate: ${spec.material.materialName} (${spec.material.stockType.displayName}${spec.material.gsm?.let { " $it GSM" } ?: ""})",
                    quantity = BigDecimal(materialResult.totalSheetsRequired).setScale(4, RoundingMode.HALF_UP),
                    unit = "SHEETS",
                    unitRate = spec.material.unitPricePerSheet,
                    calculatedAmount = materialResult.estimatedMaterialCost,
                    classification = EstimateActualClassification.ESTIMATED,
                    formulaReference = "totalSheetsRequired * unitPricePerSheet",
                    diagnosticCode = if (materialResult.estimatedMaterialCost == null) DiagnosticCode.MISSING_MATERIAL_PRICE else null
                )
            )
        }

        // 2. Printing Requirement Engine
        val plateCount = PrintingCalculatorMathUtils.calculatePlateCount(spec.processType, spec.color, spec.sides)
        val impressions = PrintingCalculatorMathUtils.calculateImpressions(materialResult.totalSheetsRequired, spec.sides)
        val passes = if (spec.sides.isDoubleSided) 2 else 1

        val machine = spec.machine
        var estPlateCost: BigDecimal? = null
        var estPrintCost: BigDecimal? = null
        var printCostStatus = CalculationStatus.PARTIAL_CALCULATION
        var missingRateReason: String? = null

        if (spec.processType == PrintingProcessType.OFFSET && plateCount > 0) {
            if (machine?.plateCostPerUnit != null) {
                estPlateCost = PrintingCalculatorMathUtils.scale4(BigDecimal(plateCount).multiply(machine.plateCostPerUnit))
                breakdownItems.add(
                    CalculationBreakdownItem(
                        componentCode = "PRINT_PLATES_CTP",
                        description = "CTP Offset Plates ($plateCount Plate(s) - Front: ${spec.color.frontColorsCount}, Back: ${spec.color.backColorsCount})",
                        quantity = BigDecimal(plateCount).setScale(4, RoundingMode.HALF_UP),
                        unit = "PLATES",
                        unitRate = machine.plateCostPerUnit,
                        calculatedAmount = estPlateCost,
                        classification = EstimateActualClassification.ESTIMATED,
                        formulaReference = "plateCount * plateCostPerUnit"
                    )
                )
            } else {
                accumulatedDiagnostics.add(
                    CalculationDiagnostic(
                        code = DiagnosticCode.MISSING_PLATE_RATE,
                        severity = DiagnosticSeverity.INFO,
                        message = "Plate cost rate not supplied for offset process ($plateCount plate(s) required).",
                        targetField = "machine.plateCostPerUnit"
                    )
                )
            }
        }

        if (machine?.hourlyRate != null && machine.impressionsPerHour != null && machine.impressionsPerHour > 0) {
            val hours = BigDecimal(impressions).divide(BigDecimal(machine.impressionsPerHour), 4, RoundingMode.HALF_UP)
            estPrintCost = PrintingCalculatorMathUtils.scale4(hours.multiply(machine.hourlyRate))
            breakdownItems.add(
                CalculationBreakdownItem(
                    componentCode = "PRINT_RUN_MACHINE",
                    description = "Press Run: ${machine.machineName ?: spec.processType.displayName} ($impressions Impressions)",
                    quantity = BigDecimal(impressions).setScale(4, RoundingMode.HALF_UP),
                    unit = "IMPRESSIONS",
                    unitRate = machine.hourlyRate.divide(BigDecimal(machine.impressionsPerHour), 4, RoundingMode.HALF_UP),
                    calculatedAmount = estPrintCost,
                    classification = EstimateActualClassification.ESTIMATED,
                    formulaReference = "(impressions / impressionsPerHour) * hourlyRate"
                )
            )
            printCostStatus = CalculationStatus.SUCCESSFUL
        } else {
            missingRateReason = "Machine hourly rate or impression speed not supplied."
            accumulatedDiagnostics.add(
                CalculationDiagnostic(
                    code = DiagnosticCode.MISSING_MACHINE_RATE,
                    severity = DiagnosticSeverity.INFO,
                    message = "Machine printing rates not supplied. Physical impression requirements computed ($impressions impressions).",
                    targetField = "machine.hourlyRate"
                )
            )
        }

        val printingResult = PrintingRequirementResult(
            totalImpressions = impressions,
            totalPasses = passes,
            plateCount = plateCount,
            estimatedPrintingCost = estPrintCost,
            estimatedPlateCost = estPlateCost,
            costStatus = printCostStatus,
            missingRateReason = missingRateReason
        )

        // 3. Finishing Requirement Engine
        val finishingBreakdown = mutableListOf<CalculationBreakdownItem>()
        var totalFinishingCost: BigDecimal? = PrintingCalculatorMathUtils.ZERO
        var finishingStatus = CalculationStatus.SUCCESSFUL

        for (op in spec.finishingOperations) {
            val (amount, diag) = if (op.unitRate != null) {
                val variable = BigDecimal(quantity).multiply(op.unitRate)
                val fixed = op.setupRate ?: PrintingCalculatorMathUtils.ZERO
                val totalOp = PrintingCalculatorMathUtils.scale4(variable.add(fixed))
                totalFinishingCost = totalFinishingCost?.add(totalOp)
                Pair(totalOp, null)
            } else {
                totalFinishingCost = null
                finishingStatus = CalculationStatus.PARTIAL_CALCULATION
                Pair(null, DiagnosticCode.INSUFFICIENT_INPUT)
            }

            val item = CalculationBreakdownItem(
                componentCode = "FIN_${op.operationType.name}",
                description = op.description,
                quantity = BigDecimal(quantity).setScale(4, RoundingMode.HALF_UP),
                unit = "PIECES",
                unitRate = op.unitRate,
                calculatedAmount = amount,
                classification = EstimateActualClassification.ESTIMATED,
                formulaReference = "(quantity * unitRate) + setupRate",
                diagnosticCode = diag
            )
            finishingBreakdown.add(item)
            breakdownItems.add(item)
        }

        val finishingResult = FinishingRequirementResult(
            operations = finishingBreakdown,
            totalEstimatedFinishingCost = totalFinishingCost,
            costStatus = finishingStatus
        )

        // 4. Waste Breakdown Item
        if (materialResult.wasteSheetsRequired > 0L) {
            val wasteCost = if (spec.material.unitPricePerSheet != null) {
                PrintingCalculatorMathUtils.scale4(BigDecimal(materialResult.wasteSheetsRequired).multiply(spec.material.unitPricePerSheet))
            } else {
                null
            }
            breakdownItems.add(
                CalculationBreakdownItem(
                    componentCode = "WASTE_ALLOWANCE",
                    description = "Waste & Spoilage Allowance (${materialResult.wasteSheetsRequired} Sheets: Setup=${spec.waste.setupSheets}, Run=${spec.waste.runningWastePercentage}%, Finish=${spec.waste.finishingWastePercentage}%)",
                    quantity = BigDecimal(materialResult.wasteSheetsRequired).setScale(4, RoundingMode.HALF_UP),
                    unit = "SHEETS",
                    unitRate = spec.material.unitPricePerSheet,
                    calculatedAmount = wasteCost,
                    classification = EstimateActualClassification.ESTIMATED,
                    formulaReference = "wasteSheetsRequired * unitPricePerSheet",
                    diagnosticCode = if (wasteCost == null) DiagnosticCode.MISSING_MATERIAL_PRICE else null
                )
            )
        }

        // 5. Total Cost Calculation
        val allCosts = listOfNotNull(
            materialResult.estimatedMaterialCost,
            printingResult.estimatedPrintingCost,
            printingResult.estimatedPlateCost,
            finishingResult.totalEstimatedFinishingCost
        )

        val totalEstimatedCost = if (allCosts.isNotEmpty() && materialResult.estimatedMaterialCost != null) {
            allCosts.fold(PrintingCalculatorMathUtils.ZERO) { acc, c -> acc.add(c) }
        } else {
            null
        }

        val unitEstimatedCost = PrintingCalculatorMathUtils.calculateUnitCost(totalEstimatedCost, quantity)

        val overallStatus = when {
            accumulatedDiagnostics.any { it.severity == DiagnosticSeverity.ERROR } -> CalculationStatus.INVALID_REQUEST
            totalEstimatedCost != null -> CalculationStatus.SUCCESSFUL
            else -> CalculationStatus.PARTIAL_CALCULATION
        }

        // 6. Cryptographic Fingerprint and Integrity Hash
        val fingerprint = PrintingCalculatorMathUtils.generateRequestFingerprint(
            tenantId = request.tenantId,
            projectId = request.projectId,
            quantity = quantity,
            finishedWMm = finishedWMm,
            finishedHMm = finishedHMm,
            sheetWMm = spec.material.sheetDimension?.width,
            sheetHMm = spec.material.sheetDimension?.height,
            stockType = spec.material.stockType,
            gsm = spec.material.gsm,
            processType = spec.processType,
            sides = spec.sides,
            colorMode = spec.color.colorMode,
            frontColors = spec.color.frontColorsCount,
            backColors = spec.color.backColorsCount,
            spotColors = spec.color.spotColorsCount,
            wastePct = spec.waste.runningWastePercentage,
            finishingOperations = spec.finishingOperations
        )

        val integrityHash = PrintingCalculatorMathUtils.generateResultIntegrityHash(
            calculationId = calculationId,
            tenantId = request.tenantId,
            projectId = request.projectId,
            fingerprint = fingerprint,
            status = overallStatus,
            totalSheets = materialResult.totalSheetsRequired,
            impressions = printingResult.totalImpressions,
            totalCost = totalEstimatedCost,
            unitCost = unitEstimatedCost,
            calculatedAt = now
        )

        return PrintingCalculationResult(
            calculationId = calculationId,
            tenantId = request.tenantId,
            projectId = request.projectId,
            requestFingerprint = fingerprint,
            requestedAt = request.requestedAt,
            calculatedAt = now,
            status = overallStatus,
            classification = EstimateActualClassification.ESTIMATED,
            normalizedSpecification = spec,
            materialRequirement = materialResult,
            printingRequirement = printingResult,
            finishingRequirement = finishingResult,
            breakdownItems = breakdownItems,
            totalEstimatedCost = totalEstimatedCost,
            estimatedUnitCost = unitEstimatedCost,
            currency = spec.currency,
            diagnostics = accumulatedDiagnostics,
            integrityHash = integrityHash,
            calculationVersion = "1.0.0"
        )
    }
}
