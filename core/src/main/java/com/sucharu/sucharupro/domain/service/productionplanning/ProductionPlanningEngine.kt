package com.sucharu.sucharupro.domain.service.productionplanning

import com.sucharu.sucharupro.domain.model.commercialcommitment.CommercialCommitment
import com.sucharu.sucharupro.domain.model.order.Order
import com.sucharu.sucharupro.domain.model.order.OrderItem
import com.sucharu.sucharupro.domain.model.printingcalculator.PrintingCalculationResult
import com.sucharu.sucharupro.domain.model.printingquote.PrintingQuote
import com.sucharu.sucharupro.domain.model.printingquote.PrintingQuoteVersion
import com.sucharu.sucharupro.domain.model.production.ProductionStageType
import com.sucharu.sucharupro.domain.model.productionplanning.*
import com.sucharu.sucharupro.domain.validation.productionplanning.ProductionPlanningValidator
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.UUID

object ProductionPlanningEngine {

    /**
     * Normalizes an order item and its optional calculation/quotation provenance into a canonical [ProductionJobSpecification].
     */
    fun normalizeSpecification(
        order: Order,
        item: OrderItem,
        quote: PrintingQuote?,
        version: PrintingQuoteVersion?,
        calcResult: PrintingCalculationResult?
    ): ProductionJobSpecification {
        val orderedQty = item.quantity.toLong()
        val impositionUps = version?.quantityBreakdown?.impositionUps
            ?: calcResult?.materialRequirement?.finishedItemsPerSheet
            ?: 1

        val wastePct = version?.quantityBreakdown?.wastagePercentage
            ?: calcResult?.normalizedSpecification?.waste?.runningWastePercentage
            ?: BigDecimal("5.0000")

        val makeReadySheets = calcResult?.normalizedSpecification?.waste?.setupSheets ?: 50L
        val plannedQty = ProductionPlanningMathUtils.calculatePlannedQuantity(orderedQty, makeReadySheets, wastePct)

        val finishedWidth = calcResult?.normalizedSpecification?.normalizedDimensionMm?.width ?: BigDecimal("210.0000")
        val finishedHeight = calcResult?.normalizedSpecification?.normalizedDimensionMm?.height ?: BigDecimal("297.0000")
        val parentWidth = calcResult?.normalizedSpecification?.material?.sheetDimension?.width ?: BigDecimal("640.0000")
        val parentHeight = calcResult?.normalizedSpecification?.material?.sheetDimension?.height ?: BigDecimal("900.0000")
        val pressWidth = parentWidth
        val pressHeight = parentHeight.divide(BigDecimal("2.0000"), 4, RoundingMode.HALF_UP)

        val substrateType = calcResult?.normalizedSpecification?.material?.stockType?.name ?: "ART_PAPER"
        val substrateGsm = calcResult?.normalizedSpecification?.material?.gsm?.toInt() ?: 150
        val printMethod = calcResult?.normalizedSpecification?.processType?.name ?: "OFFSET"
        val colorsFront = calcResult?.normalizedSpecification?.color?.frontColorsCount ?: 4
        val colorsBack = calcResult?.normalizedSpecification?.color?.backColorsCount ?: 4
        val lamination = "NONE"
        val bindingMethod = "NONE"
        val foldingType = "NONE"

        val specFingerprint = ProductionPlanningMathUtils.sha256(
            "${item.itemId}|$substrateType|$substrateGsm|$finishedWidth|$finishedHeight|$printMethod|$colorsFront|$colorsBack|$lamination|$bindingMethod|$foldingType"
        )

        return ProductionJobSpecification(
            specId = "SPEC-${order.orderId}-${item.itemId}",
            jobTitle = quote?.jobTitle ?: item.description,
            productType = "PRINT_COMMERCIAL",
            orderedQuantity = orderedQty,
            plannedQuantity = plannedQty,
            finishedWidthMm = finishedWidth,
            finishedHeightMm = finishedHeight,
            substrateType = substrateType,
            substrateGsm = substrateGsm,
            substrateBrand = null,
            parentSheetWidthMm = parentWidth,
            parentSheetHeightMm = parentHeight,
            pressSheetWidthMm = pressWidth,
            pressSheetHeightMm = pressHeight,
            printingMethod = printMethod,
            colorsFront = colorsFront,
            colorsBack = colorsBack,
            coatingFront = "NONE",
            coatingBack = "NONE",
            impositionUps = impositionUps,
            sheetsPerItem = 1,
            itemsPerSheet = impositionUps,
            lamination = lamination,
            bindingMethod = bindingMethod,
            foldingType = foldingType,
            cuttingRequired = true,
            dieCuttingRequired = false,
            packagingMethod = "CARTON_BOX",
            artworkUrl = null,
            specialInstructions = item.notes ?: quote?.jobTitle,
            specFingerprint = specFingerprint
        )
    }

    /**
     * Derives material & consumable requirements (non-deducting planning estimate).
     */
    fun deriveRequirements(
        planningId: String,
        spec: ProductionJobSpecification
    ): List<ProductionPlanningRequirement> {
        val reqs = mutableListOf<ProductionPlanningRequirement>()

        // 1. Substrate
        val totalPressSheets = BigDecimal.valueOf(spec.plannedQuantity)
            .divide(BigDecimal.valueOf(spec.impositionUps.toLong().coerceAtLeast(1L)), 0, RoundingMode.CEILING)

        reqs.add(
            ProductionPlanningRequirement(
                requirementId = "REQ-${UUID.randomUUID()}",
                planningId = planningId,
                category = "SUBSTRATE",
                itemCode = "SUB-${spec.substrateType}-${spec.substrateGsm}GSM",
                description = "${spec.substrateType} ${spec.substrateGsm} GSM (${spec.pressSheetWidthMm}x${spec.pressSheetHeightMm}mm)",
                requiredQuantity = BigDecimal.valueOf(spec.orderedQuantity),
                makeReadyQuantity = BigDecimal("50.0000"),
                wasteQuantity = totalPressSheets.multiply(BigDecimal("0.0500")).setScale(0, RoundingMode.CEILING),
                totalPlannedQuantity = totalPressSheets,
                unitOfMeasure = "SHEETS",
                estimatedAvailable = true
            )
        )

        // 2. CTP Plates (if Offset)
        if (spec.printingMethod.equals("OFFSET", ignoreCase = true)) {
            val totalPlates = (spec.colorsFront + spec.colorsBack).coerceAtLeast(1)
            reqs.add(
                ProductionPlanningRequirement(
                    requirementId = "REQ-${UUID.randomUUID()}",
                    planningId = planningId,
                    category = "PLATE",
                    itemCode = "PLATE-CTP-STD",
                    description = "Thermal CTP Printing Plates (${spec.pressSheetWidthMm}x${spec.pressSheetHeightMm}mm)",
                    requiredQuantity = BigDecimal.valueOf(totalPlates.toLong()),
                    makeReadyQuantity = BigDecimal.ZERO,
                    wasteQuantity = BigDecimal.ZERO,
                    totalPlannedQuantity = BigDecimal.valueOf(totalPlates.toLong()),
                    unitOfMeasure = "PLATES",
                    estimatedAvailable = true
                )
            )
        }

        // 3. Inks
        val totalColors = (spec.colorsFront + spec.colorsBack).coerceAtLeast(1)
        reqs.add(
            ProductionPlanningRequirement(
                requirementId = "REQ-${UUID.randomUUID()}",
                planningId = planningId,
                category = "INK",
                itemCode = "INK-CMYK-PROCESS",
                description = "Standard Process CMYK Inks ($totalColors color passes)",
                requiredQuantity = BigDecimal("1.5000"),
                makeReadyQuantity = BigDecimal("0.2000"),
                wasteQuantity = BigDecimal("0.1000"),
                totalPlannedQuantity = BigDecimal("1.8000"),
                unitOfMeasure = "KG",
                estimatedAvailable = true
            )
        )

        // 4. Lamination Film (if needed)
        if (!spec.lamination.equals("NONE", ignoreCase = true)) {
            reqs.add(
                ProductionPlanningRequirement(
                    requirementId = "REQ-${UUID.randomUUID()}",
                    planningId = planningId,
                    category = "COATING",
                    itemCode = "LAM-ROLL-${spec.lamination}",
                    description = "${spec.lamination} Thermal Lamination Film",
                    requiredQuantity = totalPressSheets,
                    makeReadyQuantity = BigDecimal("20.0000"),
                    wasteQuantity = BigDecimal("10.0000"),
                    totalPlannedQuantity = totalPressSheets.add(BigDecimal("30.0000")),
                    unitOfMeasure = "SHEETS",
                    estimatedAvailable = true
                )
            )
        }

        return reqs
    }

    /**
     * Evaluates machine / work center compatibility.
     */
    fun evaluateMachineCompatibility(
        spec: ProductionJobSpecification
    ): List<MachineCompatibilityResult> {
        val results = mutableListOf<MachineCompatibilityResult>()

        if (spec.printingMethod.equals("OFFSET", ignoreCase = true)) {
            results.add(
                MachineCompatibilityResult(
                    machineId = "PRESS-OFFSET-4C-01",
                    machineName = "Heidelberg Speedmaster 4-Color Press",
                    status = if (spec.colorsFront <= 4 && spec.colorsBack <= 4) MachineCompatibilityStatus.COMPATIBLE else MachineCompatibilityStatus.CONDITIONALLY_COMPATIBLE,
                    formatMatch = true,
                    substrateMatch = spec.substrateGsm in 60..400,
                    colorMatch = spec.colorsFront <= 4 && spec.colorsBack <= 4,
                    notes = "Standard offset machine matching format and substrate specs."
                )
            )
        } else {
            results.add(
                MachineCompatibilityResult(
                    machineId = "PRESS-DIGITAL-01",
                    machineName = "Konica Minolta AccurioPress Digital",
                    status = MachineCompatibilityStatus.COMPATIBLE,
                    formatMatch = true,
                    substrateMatch = spec.substrateGsm in 70..350,
                    colorMatch = true,
                    notes = "High-speed digital press compatible with short-run commercial items."
                )
            )
        }

        return results
    }

    /**
     * Derives proposed sequential routing across the canonical 13-stage [ProductionStageType] workflow.
     */
    fun deriveRouting(
        planningId: String,
        spec: ProductionJobSpecification
    ): List<ProductionPlanningOperation> {
        val ops = mutableListOf<ProductionPlanningOperation>()
        var seq = 1

        // Stage 1: DESIGN / PREPRESS
        ops.add(
            ProductionPlanningOperation(
                operationId = "OP-${UUID.randomUUID()}",
                planningId = planningId,
                sequenceNumber = seq++,
                stageType = ProductionStageType.DESIGN,
                operationCode = "PREPRESS_IMPOSITION",
                operationName = "Artwork Verification & Imposition Setup",
                targetWorkCenter = "PREPRESS_DESK",
                estimatedSetupMinutes = 15,
                estimatedRunMinutes = 10,
                isMandatory = true
            )
        )

        // Stage 3: QC (Prepress Quality Checkpoint)
        ops.add(
            ProductionPlanningOperation(
                operationId = "OP-${UUID.randomUUID()}",
                planningId = planningId,
                sequenceNumber = seq++,
                stageType = ProductionStageType.QC,
                operationCode = "PREPRESS_PROOF_QC",
                operationName = "Imposition & Color Separation QC",
                targetWorkCenter = "QC_STATION",
                estimatedSetupMinutes = 5,
                estimatedRunMinutes = 10,
                isMandatory = true,
                isQcCheckpoint = true
            )
        )

        // Stage 5: CTP (Offset only)
        if (spec.printingMethod.equals("OFFSET", ignoreCase = true)) {
            ops.add(
                ProductionPlanningOperation(
                    operationId = "OP-${UUID.randomUUID()}",
                    planningId = planningId,
                    sequenceNumber = seq++,
                    stageType = ProductionStageType.CTP,
                    operationCode = "CTP_EXPOSURE",
                    operationName = "CTP Plate Exposure & Processing",
                    targetWorkCenter = "CTP_ROOM",
                    estimatedSetupMinutes = 10,
                    estimatedRunMinutes = (spec.colorsFront + spec.colorsBack) * 3,
                    isMandatory = true
                )
            )
        }

        // Stage 6: PRINTING
        ops.add(
            ProductionPlanningOperation(
                operationId = "OP-${UUID.randomUUID()}",
                planningId = planningId,
                sequenceNumber = seq++,
                stageType = ProductionStageType.PRINTING,
                operationCode = "MAIN_PRESS_RUN",
                operationName = "Press Run & Color Registration",
                targetWorkCenter = if (spec.printingMethod.equals("OFFSET", ignoreCase = true)) "OFFSET_PRESS_BAY" else "DIGITAL_PRESS_BAY",
                estimatedSetupMinutes = 30,
                estimatedRunMinutes = (spec.plannedQuantity / 100).coerceAtLeast(15L).toInt(),
                isMandatory = true
            )
        )

        // Stage 7: LAMINATION (if needed)
        if (!spec.lamination.equals("NONE", ignoreCase = true)) {
            ops.add(
                ProductionPlanningOperation(
                    operationId = "OP-${UUID.randomUUID()}",
                    planningId = planningId,
                    sequenceNumber = seq++,
                    stageType = ProductionStageType.LAMINATION,
                    operationCode = "THERMAL_LAMINATION",
                    operationName = "${spec.lamination} Thermal Lamination",
                    targetWorkCenter = "LAMINATION_UNIT",
                    estimatedSetupMinutes = 20,
                    estimatedRunMinutes = (spec.plannedQuantity / 150).coerceAtLeast(10L).toInt(),
                    isMandatory = true
                )
            )
        }

        // Stage 8: FOLDING (if needed)
        if (!spec.foldingType.equals("NONE", ignoreCase = true)) {
            ops.add(
                ProductionPlanningOperation(
                    operationId = "OP-${UUID.randomUUID()}",
                    planningId = planningId,
                    sequenceNumber = seq++,
                    stageType = ProductionStageType.FOLDING,
                    operationCode = "AUTOMATIC_FOLDING",
                    operationName = "Machine ${spec.foldingType} Folding",
                    targetWorkCenter = "FOLDING_SECTION",
                    estimatedSetupMinutes = 15,
                    estimatedRunMinutes = (spec.plannedQuantity / 200).coerceAtLeast(10L).toInt(),
                    isMandatory = true
                )
            )
        }

        // Stage 9: BINDING (if needed)
        if (!spec.bindingMethod.equals("NONE", ignoreCase = true)) {
            ops.add(
                ProductionPlanningOperation(
                    operationId = "OP-${UUID.randomUUID()}",
                    planningId = planningId,
                    sequenceNumber = seq++,
                    stageType = ProductionStageType.BINDING,
                    operationCode = "BINDING_GATHER_STITCH",
                    operationName = "Booklet Binding (${spec.bindingMethod})",
                    targetWorkCenter = "BINDERY_SECTION",
                    estimatedSetupMinutes = 25,
                    estimatedRunMinutes = (spec.plannedQuantity / 100).coerceAtLeast(15L).toInt(),
                    isMandatory = true
                )
            )
        }

        // Stage 10: FINAL QC
        ops.add(
            ProductionPlanningOperation(
                operationId = "OP-${UUID.randomUUID()}",
                planningId = planningId,
                sequenceNumber = seq++,
                stageType = ProductionStageType.FINAL_QC,
                operationCode = "FINAL_INSPECTION",
                operationName = "Final Quality & Count Inspection",
                targetWorkCenter = "QC_FINISHING_STATION",
                estimatedSetupMinutes = 5,
                estimatedRunMinutes = 15,
                isMandatory = true,
                isQcCheckpoint = true
            )
        )

        // Stage 11: PACKAGING
        ops.add(
            ProductionPlanningOperation(
                operationId = "OP-${UUID.randomUUID()}",
                planningId = planningId,
                sequenceNumber = seq,
                stageType = ProductionStageType.PACKAGING,
                operationCode = "CARTON_PACK_LABEL",
                operationName = "Carton Packaging & Shipping Labeling",
                targetWorkCenter = "PACKAGING_BAY",
                estimatedSetupMinutes = 10,
                estimatedRunMinutes = 20,
                isMandatory = true
            )
        )

        return ops
    }

    /**
     * Evaluates due-date feasibility deterministically.
     */
    fun evaluateDueDateFeasibility(
        orderRequestedDate: Long?,
        totalRunMinutes: Int
    ): FeasibilityStatus {
        if (orderRequestedDate == null || orderRequestedDate <= 0) {
            return FeasibilityStatus.UNKNOWN
        }

        val estimatedBufferMs = (totalRunMinutes + 120) * 60 * 1000L // 2hr buffer
        val estimatedCompletion = System.currentTimeMillis() + estimatedBufferMs

        return when {
            estimatedCompletion <= orderRequestedDate -> FeasibilityStatus.FEASIBLE
            estimatedCompletion <= orderRequestedDate + (24 * 3600 * 1000L) -> FeasibilityStatus.AT_RISK
            else -> FeasibilityStatus.NOT_FEASIBLE
        }
    }

    /**
     * Computes quantitative Manufacturing Readiness Score (0.0000 to 100.0000) and diagnostics.
     */
    fun evaluateManufacturingReadiness(
        order: Order,
        item: OrderItem,
        commitment: CommercialCommitment?,
        spec: ProductionJobSpecification,
        machines: List<MachineCompatibilityResult>,
        operations: List<ProductionPlanningOperation>,
        feasibility: FeasibilityStatus
    ): ManufacturingReadinessEvaluation {
        val orderDiagnostics = ProductionPlanningValidator.validateOrderAndItem(order.customerId, order, item, commitment)
        val specDiagnostics = ProductionPlanningValidator.validateJobSpecification(spec)
        val allDiagnostics = mutableListOf<PlanningDiagnostic>()
        allDiagnostics.addAll(orderDiagnostics)
        allDiagnostics.addAll(specDiagnostics)

        val hasCriticalBlockers = allDiagnostics.any { it.isBlocking }

        // Sub-scores
        val commercialScore = if (orderDiagnostics.none { it.category == "COMMERCIAL" && it.isBlocking }) {
            if (commitment != null) BigDecimal("100.0000") else BigDecimal("80.0000")
        } else {
            BigDecimal("20.0000")
        }

        val specScore = if (specDiagnostics.none { it.isBlocking }) {
            if (spec.artworkUrl != null) BigDecimal("100.0000") else BigDecimal("85.0000")
        } else {
            BigDecimal("30.0000")
        }

        val materialScore = if (spec.substrateGsm > 0 && spec.substrateType.isNotBlank()) BigDecimal("100.0000") else BigDecimal("0.0000")

        val machineScore = if (machines.any { it.status == MachineCompatibilityStatus.COMPATIBLE }) {
            BigDecimal("100.0000")
        } else if (machines.any { it.status == MachineCompatibilityStatus.CONDITIONALLY_COMPATIBLE }) {
            BigDecimal("70.0000")
        } else {
            BigDecimal("20.0000")
        }

        val scheduleScore = when (feasibility) {
            FeasibilityStatus.FEASIBLE -> BigDecimal("100.0000")
            FeasibilityStatus.AT_RISK -> BigDecimal("60.0000")
            FeasibilityStatus.NOT_FEASIBLE -> BigDecimal("20.0000")
            FeasibilityStatus.UNKNOWN -> BigDecimal("75.0000")
        }

        val overallScore = ProductionPlanningMathUtils.calculateReadinessScore(
            commercialScore = commercialScore,
            specificationScore = specScore,
            materialScore = materialScore,
            machineScore = machineScore,
            scheduleScore = scheduleScore,
            hasCriticalBlockers = hasCriticalBlockers
        )

        val isReady = !hasCriticalBlockers && overallScore >= BigDecimal("80.0000")

        return ManufacturingReadinessEvaluation(
            overallScore = overallScore,
            isManufacturingReady = isReady,
            feasibilityStatus = feasibility,
            commercialReadinessScore = commercialScore,
            specificationReadinessScore = specScore,
            materialReadinessScore = materialScore,
            machineReadinessScore = machineScore,
            scheduleReadinessScore = scheduleScore,
            blockingIssuesCount = allDiagnostics.count { it.isBlocking },
            warningsCount = allDiagnostics.count { !it.isBlocking },
            diagnostics = allDiagnostics
        )
    }
}
