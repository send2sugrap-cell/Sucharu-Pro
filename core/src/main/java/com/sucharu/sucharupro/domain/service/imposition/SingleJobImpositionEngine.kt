package com.sucharu.sucharupro.domain.service.imposition

import com.sucharu.sucharupro.domain.model.imposition.*
import com.sucharu.sucharupro.domain.model.printingcalculator.PrintingDimension
import java.math.BigDecimal
import java.util.UUID

/**
 * Validates inputs for imposition calculations.
 * Module 18 Step 01.
 */
class ImpositionValidator {

    fun validateInput(
        tenantId: String,
        orderId: String,
        orderItemId: String,
        finishedItemDimension: PrintingDimension,
        parentSheetDimension: PrintingDimension,
        margins: ImpositionMarginSpec,
        spacing: ImpositionSpacingSpec,
        requiredQuantity: Long
    ) {
        require(tenantId.isNotBlank()) { "Tenant ID must not be blank." }
        require(orderId.isNotBlank()) { "Order ID must not be blank." }
        require(orderItemId.isNotBlank()) { "Order Item ID must not be blank." }
        require(requiredQuantity > 0L) { "Required quantity must be strictly positive: $requiredQuantity" }

        val itemW = finishedItemDimension.width
        val itemH = finishedItemDimension.height
        val sheetW = parentSheetDimension.width
        val sheetH = parentSheetDimension.height

        require(itemW > BigDecimal.ZERO) { "Finished item width must be positive: $itemW" }
        require(itemH > BigDecimal.ZERO) { "Finished item height must be positive: $itemH" }
        require(sheetW > BigDecimal.ZERO) { "Parent sheet width must be positive: $sheetW" }
        require(sheetH > BigDecimal.ZERO) { "Parent sheet height must be positive: $sheetH" }

        require(margins.topMm >= BigDecimal.ZERO) { "Top margin must be non-negative: ${margins.topMm}" }
        require(margins.bottomMm >= BigDecimal.ZERO) { "Bottom margin must be non-negative: ${margins.bottomMm}" }
        require(margins.leftMm >= BigDecimal.ZERO) { "Left margin must be non-negative: ${margins.leftMm}" }
        require(margins.rightMm >= BigDecimal.ZERO) { "Right margin must be non-negative: ${margins.rightMm}" }

        require(spacing.bleedMm >= BigDecimal.ZERO) { "Bleed must be non-negative: ${spacing.bleedMm}" }
        require(spacing.horizontalGutterMm >= BigDecimal.ZERO) { "Horizontal gutter must be non-negative: ${spacing.horizontalGutterMm}" }
        require(spacing.verticalGutterMm >= BigDecimal.ZERO) { "Vertical gutter must be non-negative: ${spacing.verticalGutterMm}" }

        val (usableW, usableH) = ImpositionMathUtils.calculateUsableSheetDimension(sheetW, sheetH, margins)
        require(usableW > BigDecimal.ZERO && usableH > BigDecimal.ZERO) {
            "Usable sheet dimension is invalid or zero after deducting margins: ${usableW}mm x ${usableH}mm"
        }
    }
}

/**
 * Deterministic Optimization Engine for Single-Job Orthogonal Sheet Layout.
 * Module 18 Step 01.
 */
class SingleJobImpositionEngine(
    private val validator: ImpositionValidator = ImpositionValidator()
) {

    /**
     * Calculates the optimal single-job imposition layout given specifications.
     */
    fun calculateOptimalLayout(
        tenantId: String,
        jobId: String?,
        orderId: String,
        orderItemId: String,
        calculationId: String? = null,
        productName: String,
        finishedItemDimension: PrintingDimension,
        parentSheetDimension: PrintingDimension,
        margins: ImpositionMarginSpec = ImpositionMarginSpec(),
        spacing: ImpositionSpacingSpec = ImpositionSpacingSpec(),
        orientationPolicy: ImpositionOrientationPolicy = ImpositionOrientationPolicy.AUTO_OPTIMAL,
        requiredQuantity: Long,
        notes: String? = null,
        actor: String = "prepress_operator"
    ): ImpositionSpecification {
        // 1. Normalize dimensions to millimeters
        val normItemDim = ImpositionMathUtils.toMillimeters(finishedItemDimension)
        val normSheetDim = ImpositionMathUtils.toMillimeters(parentSheetDimension)

        // 2. Validate
        validator.validateInput(
            tenantId = tenantId,
            orderId = orderId,
            orderItemId = orderItemId,
            finishedItemDimension = normItemDim,
            parentSheetDimension = normSheetDim,
            margins = margins,
            spacing = spacing,
            requiredQuantity = requiredQuantity
        )

        // 3. Compute usable area
        val (usableW, usableH) = ImpositionMathUtils.calculateUsableSheetDimension(
            normSheetDim.width,
            normSheetDim.height,
            margins
        )

        // 4. Evaluate candidates based on orientation policy
        val standardCandidate = ImpositionMathUtils.evaluateCandidate(
            orientation = ImpositionLayoutOrientation.STANDARD,
            usableWidthMm = usableW,
            usableHeightMm = usableH,
            itemWidthMm = normItemDim.width,
            itemHeightMm = normItemDim.height,
            spacing = spacing
        )

        val rotatedCandidate = ImpositionMathUtils.evaluateCandidate(
            orientation = ImpositionLayoutOrientation.ROTATED,
            usableWidthMm = usableW,
            usableHeightMm = usableH,
            itemWidthMm = normItemDim.width,
            itemHeightMm = normItemDim.height,
            spacing = spacing
        )

        val evaluatedCandidates = listOf(standardCandidate, rotatedCandidate)

        // 5. Select optimal candidate deterministically
        val selectedCandidate = when (orientationPolicy) {
            ImpositionOrientationPolicy.FORCE_STANDARD_0_DEG,
            ImpositionOrientationPolicy.PROHIBIT_ROTATION -> standardCandidate

            ImpositionOrientationPolicy.FORCE_ROTATED_90_DEG -> rotatedCandidate

            ImpositionOrientationPolicy.AUTO_OPTIMAL -> {
                // Ranking:
                // 1. Highest copies per sheet
                // 2. Lowest waste area
                // 3. Highest yield percentage
                // 4. Tie-breaker: prefer STANDARD (0°)
                if (rotatedCandidate.copiesPerSheet > standardCandidate.copiesPerSheet) {
                    rotatedCandidate
                } else if (standardCandidate.copiesPerSheet > rotatedCandidate.copiesPerSheet) {
                    standardCandidate
                } else if (rotatedCandidate.wasteAreaMm2 < standardCandidate.wasteAreaMm2) {
                    rotatedCandidate
                } else {
                    standardCandidate
                }
            }
        }

        require(selectedCandidate.isFeasible && selectedCandidate.copiesPerSheet > 0) {
            "No feasible imposition layout found. Item dimensions (${normItemDim.width}mm x ${normItemDim.height}mm) exceed usable sheet area (${usableW}mm x ${usableH}mm)."
        }

        // 6. Calculate required sheets, production capacity, and overage
        val requiredSheets = ImpositionMathUtils.calculateRequiredSheets(requiredQuantity, selectedCandidate.copiesPerSheet)
        val (totalCapacity, overage) = ImpositionMathUtils.calculateOverage(requiredQuantity, requiredSheets, selectedCandidate.copiesPerSheet)

        // 7. Generate deterministic integrity hash
        val integrityHash = ImpositionMathUtils.generateImpositionIntegrityHash(
            tenantId = tenantId,
            orderId = orderId,
            orderItemId = orderItemId,
            sheetW = normSheetDim.width,
            sheetH = normSheetDim.height,
            itemW = normItemDim.width,
            itemH = normItemDim.height,
            orientation = selectedCandidate.orientation.name,
            copiesPerSheet = selectedCandidate.copiesPerSheet,
            requiredSheets = requiredSheets
        )

        val impositionId = "IMP-${UUID.randomUUID().toString().take(12)}"

        return ImpositionSpecification(
            impositionId = impositionId,
            tenantId = tenantId,
            jobId = jobId,
            orderId = orderId,
            orderItemId = orderItemId,
            calculationId = calculationId,
            productName = productName,
            finishedItemDimension = normItemDim,
            parentSheetDimension = normSheetDim,
            marginSpec = margins,
            spacingSpec = spacing,
            orientationPolicy = orientationPolicy,
            selectedOrientation = selectedCandidate.orientation,
            columns = selectedCandidate.columns,
            rows = selectedCandidate.rows,
            copiesPerSheet = selectedCandidate.copiesPerSheet,
            requiredQuantity = requiredQuantity,
            requiredSheets = requiredSheets,
            totalProducedCapacity = totalCapacity,
            overageQuantity = overage,
            usableWidthMm = usableW,
            usableHeightMm = usableH,
            occupiedAreaMm2 = selectedCandidate.occupiedAreaMm2,
            usableAreaMm2 = selectedCandidate.usableAreaMm2,
            wasteAreaMm2 = selectedCandidate.wasteAreaMm2,
            yieldPercentage = selectedCandidate.yieldPercentage,
            version = 1,
            status = ImpositionStatus.OPTIMIZED,
            integrityHash = integrityHash,
            notes = notes,
            candidateBreakdown = evaluatedCandidates,
            createdAt = System.currentTimeMillis(),
            createdBy = actor
        )
    }
}
