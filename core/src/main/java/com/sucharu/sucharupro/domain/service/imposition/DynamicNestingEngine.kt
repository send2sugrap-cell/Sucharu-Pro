package com.sucharu.sucharupro.domain.service.imposition

import com.sucharu.sucharupro.domain.model.imposition.*
import com.sucharu.sucharupro.domain.model.printingcalculator.MeasurementUnit
import com.sucharu.sucharupro.domain.model.printingcalculator.PrintingDimension
import java.math.BigDecimal
import java.math.RoundingMode
import java.security.MessageDigest
import java.util.UUID

/**
 * High-performance, deterministic Dynamic 2D Nesting & Wastage Optimization Engine.
 * Module 18 Step 03.
 */
object DynamicNestingEngine {

    private const val SCALE = 4
    private val ROUNDING = RoundingMode.HALF_UP
    private val HUNDRED = BigDecimal("100.0000")

    private data class FreeRect(
        val x: BigDecimal,
        val y: BigDecimal,
        val width: BigDecimal,
        val height: BigDecimal
    ) {
        val area: BigDecimal get() = width.multiply(height).setScale(SCALE, ROUNDING)
    }

    /**
     * Optimizes rectangular nesting for a pool of candidate print jobs.
     */
    fun optimizeNesting(
        tenantId: String,
        name: String,
        candidateItems: List<NestingCandidateItem>,
        parentSheetDimension: PrintingDimension,
        marginSpec: ImpositionMarginSpec = ImpositionMarginSpec(),
        spacingSpec: ImpositionSpacingSpec = ImpositionSpacingSpec(),
        orientationPolicy: NestingOrientationPolicy = NestingOrientationPolicy.ALLOW_ROTATION,
        placementStrategy: NestingPlacementStrategy = NestingPlacementStrategy.BOTTOM_LEFT_FILL,
        minOffcutDimensionMm: BigDecimal = BigDecimal("100.0000"),
        actor: String
    ): DynamicNestingSpecification {
        require(tenantId.isNotBlank()) { "Tenant ID must not be blank." }
        require(candidateItems.isNotEmpty()) { "Candidate items pool cannot be empty." }
        require(actor.isNotBlank()) { "Actor must not be blank." }

        // 1. Normalize dimensions to MM
        val parentDimMm = ImpositionMathUtils.toMillimeters(parentSheetDimension)
        val parentWidthMm = parentDimMm.width
        val parentHeightMm = parentDimMm.height

        require(parentWidthMm > BigDecimal.ZERO && parentHeightMm > BigDecimal.ZERO) {
            "Parent sheet dimensions must be strictly positive: ${parentWidthMm}mm x ${parentHeightMm}mm"
        }

        val usableWidthMm = parentWidthMm.subtract(marginSpec.totalHorizontalMarginMm).setScale(SCALE, ROUNDING)
        val usableHeightMm = parentHeightMm.subtract(marginSpec.totalVerticalMarginMm).setScale(SCALE, ROUNDING)

        require(usableWidthMm > BigDecimal.ZERO && usableHeightMm > BigDecimal.ZERO) {
            "Usable sheet area is non-positive after subtracting margins: ${usableWidthMm}mm x ${usableHeightMm}mm"
        }

        // 2. Validate substrate homogeneity across candidates
        val referenceStock = candidateItems.first().paperStockType
        val referenceGsm = candidateItems.first().gsm
        val referenceColor = candidateItems.first().colorMode
        val referenceSide = candidateItems.first().printingSideOption

        candidateItems.forEach { item ->
            require(item.paperStockType == referenceStock) {
                "Incompatible paper stock in nesting pool: expected $referenceStock, found ${item.paperStockType} on job ${item.jobId}"
            }
            val gsmDiff = item.gsm.subtract(referenceGsm).abs()
            require(gsmDiff <= BigDecimal("15.0000")) {
                "GSM variance exceeds tolerance (15 GSM): reference $referenceGsm, item ${item.gsm} on job ${item.jobId}"
            }
        }

        // 3. Formulate candidate item placement instances
        // Deterministic sorting: priority descending, area descending, max dimension descending, then jobId ascending
        val sortedCandidates = candidateItems.sortedWith(
            compareByDescending<NestingCandidateItem> { it.priorityScore }
                .thenByDescending {
                    val norm = ImpositionMathUtils.toMillimeters(it.finishedDimension)
                    norm.width.multiply(norm.height)
                }
                .thenByDescending {
                    val norm = ImpositionMathUtils.toMillimeters(it.finishedDimension)
                    maxOf(norm.width, norm.height)
                }
                .thenBy { it.jobId }
        )

        // 4. Execute 2D Bin Packing with Free Rectangle Splitting
        val freeRectangles = mutableListOf(
            FreeRect(
                x = BigDecimal.ZERO.setScale(SCALE, ROUNDING),
                y = BigDecimal.ZERO.setScale(SCALE, ROUNDING),
                width = usableWidthMm,
                height = usableHeightMm
            )
        )

        val placedItems = mutableListOf<NestingItemPlacement>()
        val jobCopiesMap = mutableMapOf<String, Int>()
        var slotCounter = 0

        // Attempt packing in waves: initial 1-up per candidate, followed by proportional fills based on required quantity
        val placementQueue = mutableListOf<NestingCandidateItem>()
        // Pass 1: Ensure every candidate gets at least 1 slot if it fits
        placementQueue.addAll(sortedCandidates)

        // Pass 2: Fill remaining sheet capacity proportionally up to 32 items
        val totalQuantitySum = sortedCandidates.sumOf { it.requiredQuantity }
        if (totalQuantitySum > 0L) {
            sortedCandidates.forEach { candidate ->
                val targetCopies = ((candidate.requiredQuantity.toDouble() / totalQuantitySum.toDouble()) * 16.0).toInt().coerceAtLeast(1)
                for (i in 2..targetCopies) {
                    placementQueue.add(candidate)
                }
            }
        }

        val bleed2x = spacingSpec.bleedMm.multiply(BigDecimal("2.0000")).setScale(SCALE, ROUNDING)

        for (candidate in placementQueue) {
            val itemDim = ImpositionMathUtils.toMillimeters(candidate.finishedDimension)
            val itemWidthMm = itemDim.width
            val itemHeightMm = itemDim.height

            val effStandardW = itemWidthMm.add(bleed2x).setScale(SCALE, ROUNDING)
            val effStandardH = itemHeightMm.add(bleed2x).setScale(SCALE, ROUNDING)

            val effRotatedW = itemHeightMm.add(bleed2x).setScale(SCALE, ROUNDING)
            val effRotatedH = itemWidthMm.add(bleed2x).setScale(SCALE, ROUNDING)

            // Determine candidate orientations
            val candidateOrientations: List<Pair<ImpositionLayoutOrientation, Pair<BigDecimal, BigDecimal>>> = when (orientationPolicy) {
                NestingOrientationPolicy.ALLOW_ROTATION -> {
                    if (candidate.allowRotation) {
                        listOf(
                            Pair(ImpositionLayoutOrientation.STANDARD, Pair(effStandardW, effStandardH)),
                            Pair(ImpositionLayoutOrientation.ROTATED, Pair(effRotatedW, effRotatedH))
                        )
                    } else {
                        listOf(Pair(ImpositionLayoutOrientation.STANDARD, Pair(effStandardW, effStandardH)))
                    }
                }
                NestingOrientationPolicy.FORCE_STANDARD_0_DEG -> {
                    listOf(Pair(ImpositionLayoutOrientation.STANDARD, Pair(effStandardW, effStandardH)))
                }
                NestingOrientationPolicy.FORCE_ROTATED_90_DEG -> {
                    listOf(Pair(ImpositionLayoutOrientation.ROTATED, Pair(effRotatedW, effRotatedH)))
                }
                NestingOrientationPolicy.PROHIBIT_ROTATION -> {
                    listOf(Pair(ImpositionLayoutOrientation.STANDARD, Pair(effStandardW, effStandardH)))
                }
            }

            // Find best free rectangle
            var bestRectIndex = -1
            var bestOrientation: ImpositionLayoutOrientation = ImpositionLayoutOrientation.STANDARD
            var bestPlacedW = BigDecimal.ZERO
            var bestPlacedH = BigDecimal.ZERO
            var bestScore = BigDecimal("999999999.0000")

            for ((rectIdx, rect) in freeRectangles.withIndex()) {
                for (cand in candidateOrientations) {
                    val orient = cand.first
                    val reqW = cand.second.first
                    val reqH = cand.second.second

                    if (rect.width >= reqW && rect.height >= reqH) {
                        val score = when (placementStrategy) {
                            NestingPlacementStrategy.BOTTOM_LEFT_FILL -> {
                                // Prefer lowest Y, then lowest X
                                rect.y.multiply(BigDecimal("10000.0000")).add(rect.x)
                            }
                            NestingPlacementStrategy.BEST_AREA_FIT -> {
                                // Prefer minimum leftover area
                                rect.area.subtract(reqW.multiply(reqH))
                            }
                            NestingPlacementStrategy.GUILLOTINE_CUT_FIRST -> {
                                rect.y.multiply(BigDecimal("1000.0000")).add(rect.width.subtract(reqW))
                            }
                        }

                        if (score < bestScore) {
                            bestScore = score
                            bestRectIndex = rectIdx
                            bestOrientation = orient
                            bestPlacedW = reqW
                            bestPlacedH = reqH
                        }
                    }
                }
            }

            if (bestRectIndex >= 0) {
                val targetRect = freeRectangles.removeAt(bestRectIndex)
                val posX = targetRect.x
                val posY = targetRect.y

                val actualItemW = if (bestOrientation == ImpositionLayoutOrientation.STANDARD) itemWidthMm else itemHeightMm
                val actualItemH = if (bestOrientation == ImpositionLayoutOrientation.STANDARD) itemHeightMm else itemWidthMm

                val placementId = "PLACE-${UUID.randomUUID().toString().take(8).uppercase()}"
                val canvasXMm = marginSpec.leftMm.add(posX).add(spacingSpec.bleedMm).setScale(SCALE, ROUNDING)
                val canvasYMm = marginSpec.topMm.add(posY).add(spacingSpec.bleedMm).setScale(SCALE, ROUNDING)

                placedItems.add(
                    NestingItemPlacement(
                        placementId = placementId,
                        slotIndex = slotCounter++,
                        jobId = candidate.jobId,
                        orderId = candidate.orderId,
                        orderItemId = candidate.orderItemId,
                        productName = candidate.productName,
                        xMm = canvasXMm,
                        yMm = canvasYMm,
                        placedWidthMm = actualItemW,
                        placedHeightMm = actualItemH,
                        orientation = bestOrientation,
                        occupiedAreaMm2 = actualItemW.multiply(actualItemH).setScale(SCALE, ROUNDING)
                    )
                )

                jobCopiesMap[candidate.jobId] = (jobCopiesMap[candidate.jobId] ?: 0) + 1

                // Split the free rectangle into right and top remainders including cutting gutters
                val rightWidth = targetRect.width.subtract(bestPlacedW).subtract(spacingSpec.horizontalGutterMm).setScale(SCALE, ROUNDING)
                val topHeight = targetRect.height.subtract(bestPlacedH).subtract(spacingSpec.verticalGutterMm).setScale(SCALE, ROUNDING)

                if (rightWidth > BigDecimal.ZERO) {
                    freeRectangles.add(
                        FreeRect(
                            x = posX.add(bestPlacedW).add(spacingSpec.horizontalGutterMm).setScale(SCALE, ROUNDING),
                            y = posY,
                            width = rightWidth,
                            height = bestPlacedH
                        )
                    )
                }

                if (topHeight > BigDecimal.ZERO) {
                    freeRectangles.add(
                        FreeRect(
                            x = posX,
                            y = posY.add(bestPlacedH).add(spacingSpec.verticalGutterMm).setScale(SCALE, ROUNDING),
                            width = targetRect.width,
                            height = topHeight
                        )
                    )
                }
            }
        }

        require(placedItems.isNotEmpty()) {
            "No candidate items could be nested on the parent sheet ($parentWidthMm x $parentHeightMm mm)."
        }

        // 5. Analyze Offcut Remnants
        val offcutRemnants = freeRectangles
            .filter { it.width > BigDecimal.ZERO && it.height > BigDecimal.ZERO }
            .sortedWith(compareByDescending<FreeRect> { it.area }.thenBy { it.x }.thenBy { it.y })
            .mapIndexed { idx, rect ->
                val isRecoverable = rect.width >= minOffcutDimensionMm && rect.height >= minOffcutDimensionMm
                val offcutId = "OFFCUT-${idx + 1}"
                NestingOffcutRemnant(
                    offcutId = offcutId,
                    xMm = marginSpec.leftMm.add(rect.x).setScale(SCALE, ROUNDING),
                    yMm = marginSpec.topMm.add(rect.y).setScale(SCALE, ROUNDING),
                    widthMm = rect.width,
                    heightMm = rect.height,
                    areaMm2 = rect.area,
                    isRecoverable = isRecoverable
                )
            }

        // 6. Compute Job Allocation Summaries & Required Press Sheets
        val distinctCandidates = sortedCandidates.associateBy { it.jobId }
        val jobSummaries = distinctCandidates.values.mapNotNull { candidate ->
            val assignedSlots = jobCopiesMap[candidate.jobId] ?: 0
            if (assignedSlots == 0) return@mapNotNull null

            val itemDim = ImpositionMathUtils.toMillimeters(candidate.finishedDimension)
            val itemW = itemDim.width
            val itemH = itemDim.height
            val singleItemArea = itemW.multiply(itemH).setScale(SCALE, ROUNDING)
            val totalJobOccupiedArea = singleItemArea.multiply(BigDecimal(assignedSlots)).setScale(SCALE, ROUNDING)

            NestingJobAllocationSummary(
                jobId = candidate.jobId,
                orderId = candidate.orderId,
                orderItemId = candidate.orderItemId,
                productName = candidate.productName,
                assignedCopiesOnSheet = assignedSlots,
                requiredQuantity = candidate.requiredQuantity,
                producedQuantity = 0L,
                overageQuantity = 0L,
                totalOccupiedAreaMm2 = totalJobOccupiedArea,
                relativeYieldPercentage = BigDecimal.ZERO
            )
        }

        require(jobSummaries.isNotEmpty()) { "At least one job must be allocated on the nested sheet." }

        val commonRequiredSheets = jobSummaries.maxOf { summary ->
            Math.ceil(summary.requiredQuantity.toDouble() / summary.assignedCopiesOnSheet.toDouble()).toLong()
        }.coerceAtLeast(1L)

        // 7. Area & Utilization Calculations
        val totalSheetAreaMm2 = parentWidthMm.multiply(parentHeightMm).setScale(SCALE, ROUNDING)
        val usableAreaMm2 = usableWidthMm.multiply(usableHeightMm).setScale(SCALE, ROUNDING)
        val totalOccupiedAreaMm2 = placedItems.fold(BigDecimal.ZERO) { acc, p -> acc.add(p.occupiedAreaMm2) }.setScale(SCALE, ROUNDING)
        val wasteAreaMm2 = totalSheetAreaMm2.subtract(totalOccupiedAreaMm2).setScale(SCALE, ROUNDING)
        val recoverableOffcutAreaMm2 = offcutRemnants.filter { it.isRecoverable }
            .fold(BigDecimal.ZERO) { acc, o -> acc.add(o.areaMm2) }.setScale(SCALE, ROUNDING)

        val sheetUtilizationPercentage = if (totalSheetAreaMm2 > BigDecimal.ZERO) {
            totalOccupiedAreaMm2.multiply(HUNDRED).divide(totalSheetAreaMm2, SCALE, ROUNDING)
        } else BigDecimal.ZERO

        val usableYieldPercentage = if (usableAreaMm2 > BigDecimal.ZERO) {
            totalOccupiedAreaMm2.multiply(HUNDRED).divide(usableAreaMm2, SCALE, ROUNDING)
        } else BigDecimal.ZERO

        val offcutRecoveryPercentage = if (totalSheetAreaMm2 > BigDecimal.ZERO) {
            recoverableOffcutAreaMm2.multiply(HUNDRED).divide(totalSheetAreaMm2, SCALE, ROUNDING)
        } else BigDecimal.ZERO

        // Update finalized job summaries with produced/overage quantities and yield
        val finalizedJobSummaries = jobSummaries.map { summary ->
            val produced = commonRequiredSheets * summary.assignedCopiesOnSheet
            val overage = produced - summary.requiredQuantity
            val relYield = if (totalOccupiedAreaMm2 > BigDecimal.ZERO) {
                summary.totalOccupiedAreaMm2.multiply(HUNDRED).divide(totalOccupiedAreaMm2, SCALE, ROUNDING)
            } else BigDecimal.ZERO

            summary.copy(
                producedQuantity = produced,
                overageQuantity = overage,
                relativeYieldPercentage = relYield
            )
        }

        val totalProducedItems = finalizedJobSummaries.sumOf { it.producedQuantity }
        val totalOverageItems = finalizedJobSummaries.sumOf { it.overageQuantity }

        val nestingId = "NEST-${UUID.randomUUID().toString().take(8).uppercase()}"

        // 8. Generate SHA-256 Tamper-Evident Cryptographic Hash
        val rawIntegrityString = buildString {
            append(nestingId).append("|")
            append(tenantId).append("|")
            append(parentWidthMm.toPlainString()).append("x").append(parentHeightMm.toPlainString()).append("|")
            append(usableYieldPercentage.toPlainString()).append("|")
            append(placedItems.size).append("|")
            placedItems.forEach { p ->
                append(p.jobId).append(":")
                append(p.xMm.toPlainString()).append(",")
                append(p.yMm.toPlainString()).append(":")
                append(p.placedWidthMm.toPlainString()).append("x")
                append(p.placedHeightMm.toPlainString()).append(":")
                append(p.orientation.name).append(";")
            }
            append(commonRequiredSheets)
        }
        val integrityHash = computeSha256(rawIntegrityString)

        return DynamicNestingSpecification(
            nestingId = nestingId,
            tenantId = tenantId,
            name = name.ifBlank { "Dynamic Nesting $nestingId" },
            paperStockType = referenceStock,
            gsm = referenceGsm,
            colorMode = referenceColor,
            printingSideOption = referenceSide,
            parentSheetDimension = PrintingDimension(parentWidthMm, parentHeightMm, MeasurementUnit.MILLIMETERS),
            marginSpec = marginSpec,
            spacingSpec = spacingSpec,
            orientationPolicy = orientationPolicy,
            placementStrategy = placementStrategy,
            usableWidthMm = usableWidthMm,
            usableHeightMm = usableHeightMm,
            placements = placedItems,
            offcutRemnants = offcutRemnants,
            jobSummaries = finalizedJobSummaries,
            totalItemsPlaced = placedItems.size,
            commonRequiredSheets = commonRequiredSheets,
            totalProducedItems = totalProducedItems,
            totalOverageItems = totalOverageItems,
            totalSheetAreaMm2 = totalSheetAreaMm2,
            usableAreaMm2 = usableAreaMm2,
            occupiedAreaMm2 = totalOccupiedAreaMm2,
            wasteAreaMm2 = wasteAreaMm2,
            recoverableOffcutAreaMm2 = recoverableOffcutAreaMm2,
            sheetUtilizationPercentage = sheetUtilizationPercentage,
            usableYieldPercentage = usableYieldPercentage,
            offcutRecoveryPercentage = offcutRecoveryPercentage,
            version = 1,
            status = NestingStatus.OPTIMIZED,
            integrityHash = integrityHash,
            notes = "Dynamic nesting generated with ${placedItems.size} items across ${finalizedJobSummaries.size} jobs. Offcuts: ${offcutRemnants.count { it.isRecoverable }} recoverable.",
            createdAt = System.currentTimeMillis(),
            createdBy = actor
        )
    }

    private fun computeSha256(input: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(input.toByteArray(Charsets.UTF_8))
        return hash.joinToString("") { "%02x".format(it) }
    }
}
