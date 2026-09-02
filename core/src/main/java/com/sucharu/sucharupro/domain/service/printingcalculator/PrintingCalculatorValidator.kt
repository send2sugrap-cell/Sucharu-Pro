package com.sucharu.sucharupro.domain.service.printingcalculator

import com.sucharu.sucharupro.domain.model.printingcalculator.CalculationDiagnostic
import com.sucharu.sucharupro.domain.model.printingcalculator.DiagnosticCode
import com.sucharu.sucharupro.domain.model.printingcalculator.DiagnosticSeverity
import com.sucharu.sucharupro.domain.model.printingcalculator.NormalizedPrintingSpecification
import com.sucharu.sucharupro.domain.model.printingcalculator.PrintingCalculationRequest
import com.sucharu.sucharupro.domain.model.printingcalculator.PrintingCalculatorMathUtils
import com.sucharu.sucharupro.domain.model.printingcalculator.PrintingProcessType
import java.math.BigDecimal

/**
 * Validation Engine for Printing Calculation Requests.
 * Module 17 Step 01.
 */
object PrintingCalculatorValidator {

    data class ValidationResult(
        val isValid: Boolean,
        val diagnostics: List<CalculationDiagnostic>
    ) {
        val hasErrors: Boolean get() = diagnostics.any { it.severity == DiagnosticSeverity.ERROR }
        val errorMessages: List<String> get() = diagnostics.filter { it.severity == DiagnosticSeverity.ERROR }.map { it.message }
    }

    fun validateRequest(request: PrintingCalculationRequest): ValidationResult {
        val diagnostics = mutableListOf<CalculationDiagnostic>()

        // 1. Context validation
        if (request.tenantId.isBlank()) {
            diagnostics.add(
                CalculationDiagnostic(
                    code = DiagnosticCode.INSUFFICIENT_INPUT,
                    severity = DiagnosticSeverity.ERROR,
                    message = "Tenant context is missing or blank.",
                    targetField = "tenantId"
                )
            )
        }
        if (request.projectId.isBlank()) {
            diagnostics.add(
                CalculationDiagnostic(
                    code = DiagnosticCode.INSUFFICIENT_INPUT,
                    severity = DiagnosticSeverity.ERROR,
                    message = "Project context is missing or blank.",
                    targetField = "projectId"
                )
            )
        }

        // 2. Quantity validation
        if (request.quantity <= 0L) {
            diagnostics.add(
                CalculationDiagnostic(
                    code = DiagnosticCode.INVALID_QUANTITY,
                    severity = DiagnosticSeverity.ERROR,
                    message = "Ordered quantity must be greater than zero. Received: ${request.quantity}",
                    targetField = "quantity",
                    suggestedRemediation = "Specify a positive integer quantity (e.g. 1000)."
                )
            )
        }

        // 3. Finished Dimensions
        if (request.finishedWidth <= BigDecimal.ZERO || request.finishedHeight <= BigDecimal.ZERO) {
            diagnostics.add(
                CalculationDiagnostic(
                    code = DiagnosticCode.INVALID_DIMENSION,
                    severity = DiagnosticSeverity.ERROR,
                    message = "Finished dimensions must be strictly positive. Width: ${request.finishedWidth}, Height: ${request.finishedHeight}",
                    targetField = "finishedDimensions"
                )
            )
        }

        // 4. Material Validation
        if (request.materialName.isBlank()) {
            diagnostics.add(
                CalculationDiagnostic(
                    code = DiagnosticCode.INSUFFICIENT_INPUT,
                    severity = DiagnosticSeverity.ERROR,
                    message = "Paper / Substrate material name must be specified.",
                    targetField = "materialName"
                )
            )
        }

        // 5. Sheet Dimensions vs Finished Dimensions
        if (request.sheetWidth != null && request.sheetHeight != null) {
            if (request.sheetWidth <= BigDecimal.ZERO || request.sheetHeight <= BigDecimal.ZERO) {
                diagnostics.add(
                    CalculationDiagnostic(
                        code = DiagnosticCode.INVALID_DIMENSION,
                        severity = DiagnosticSeverity.ERROR,
                        message = "Parent sheet dimensions must be strictly positive when specified.",
                        targetField = "sheetDimensions"
                    )
                )
            } else {
                val fWMm = PrintingCalculatorMathUtils.toMillimeters(request.finishedWidth, request.dimensionUnit)
                val fHMm = PrintingCalculatorMathUtils.toMillimeters(request.finishedHeight, request.dimensionUnit)
                val sWMm = PrintingCalculatorMathUtils.toMillimeters(request.sheetWidth, request.sheetDimensionUnit)
                val sHMm = PrintingCalculatorMathUtils.toMillimeters(request.sheetHeight, request.sheetDimensionUnit)

                val fitsStandard = sWMm >= fWMm && sHMm >= fHMm
                val fitsRotated = sWMm >= fHMm && sHMm >= fWMm

                if (!fitsStandard && !fitsRotated) {
                    diagnostics.add(
                        CalculationDiagnostic(
                            code = DiagnosticCode.SHEET_SIZE_SMALLER_THAN_ITEM,
                            severity = DiagnosticSeverity.ERROR,
                            message = "Finished item (${fWMm}mm x ${fHMm}mm) exceeds parent sheet size (${sWMm}mm x ${sHMm}mm).",
                            targetField = "sheetDimensions",
                            suggestedRemediation = "Choose a larger parent sheet dimension."
                        )
                    )
                }
            }
        }

        // 6. Waste Percentage Validation
        if (request.runningWastePercentage < BigDecimal.ZERO || request.runningWastePercentage > PrintingCalculatorMathUtils.ONE_HUNDRED) {
            diagnostics.add(
                CalculationDiagnostic(
                    code = DiagnosticCode.INVALID_WASTE_PERCENTAGE,
                    severity = DiagnosticSeverity.ERROR,
                    message = "Running waste percentage must be between 0% and 100%. Received: ${request.runningWastePercentage}%",
                    targetField = "runningWastePercentage"
                )
            )
        } else if (request.runningWastePercentage > BigDecimal("30.0000")) {
            diagnostics.add(
                CalculationDiagnostic(
                    code = DiagnosticCode.EXCESSIVE_WASTE_PERCENTAGE,
                    severity = DiagnosticSeverity.WARNING,
                    message = "Running waste percentage (${request.runningWastePercentage}%) is abnormally high.",
                    targetField = "runningWastePercentage",
                    suggestedRemediation = "Verify if excessive waste allowance is intentional."
                )
            )
        }

        // 7. Colors Validation
        if (request.frontColorsCount < 0 || request.backColorsCount < 0 || request.spotColorsCount < 0) {
            diagnostics.add(
                CalculationDiagnostic(
                    code = DiagnosticCode.INSUFFICIENT_INPUT,
                    severity = DiagnosticSeverity.ERROR,
                    message = "Color counts cannot be negative.",
                    targetField = "colors"
                )
            )
        }

        // 8. Material Price Diagnostic
        if (request.materialUnitPricePerSheet == null) {
            diagnostics.add(
                CalculationDiagnostic(
                    code = DiagnosticCode.MISSING_MATERIAL_PRICE,
                    severity = DiagnosticSeverity.INFO,
                    message = "Material unit price not provided. Calculation will produce physical sheet requirement without estimated paper cost.",
                    targetField = "materialUnitPricePerSheet"
                )
            )
        } else if (request.materialUnitPricePerSheet < BigDecimal.ZERO) {
            diagnostics.add(
                CalculationDiagnostic(
                    code = DiagnosticCode.INSUFFICIENT_INPUT,
                    severity = DiagnosticSeverity.ERROR,
                    message = "Material unit price cannot be negative.",
                    targetField = "materialUnitPricePerSheet"
                )
            )
        }

        // 9. Machine Limits (if provided)
        request.machine?.let { m ->
            if (m.hourlyRate != null && m.hourlyRate < BigDecimal.ZERO) {
                diagnostics.add(
                    CalculationDiagnostic(
                        code = DiagnosticCode.INSUFFICIENT_INPUT,
                        severity = DiagnosticSeverity.ERROR,
                        message = "Machine hourly rate cannot be negative.",
                        targetField = "machine.hourlyRate"
                    )
                )
            }
        }

        return ValidationResult(
            isValid = diagnostics.none { it.severity == DiagnosticSeverity.ERROR },
            diagnostics = diagnostics
        )
    }

    fun validateNormalized(spec: NormalizedPrintingSpecification): ValidationResult {
        val diagnostics = mutableListOf<CalculationDiagnostic>()

        if (spec.material.sheetDimension == null) {
            diagnostics.add(
                CalculationDiagnostic(
                    code = DiagnosticCode.INSUFFICIENT_INPUT,
                    severity = DiagnosticSeverity.WARNING,
                    message = "Parent sheet dimensions not specified. Standard sheet utilization cannot be computed.",
                    targetField = "material.sheetDimension"
                )
            )
        }

        return ValidationResult(
            isValid = diagnostics.none { it.severity == DiagnosticSeverity.ERROR },
            diagnostics = diagnostics
        )
    }
}
