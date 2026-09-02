package com.sucharu.sucharupro.domain.service.substratereservation

import com.sucharu.sucharupro.domain.model.printingcalculator.MeasurementUnit
import com.sucharu.sucharupro.domain.model.printingcalculator.PrintingDimension
import com.sucharu.sucharupro.domain.model.substratereservation.*
import java.math.BigDecimal
import java.math.RoundingMode
import java.security.MessageDigest
import java.util.UUID

/**
 * Deterministic Batch/Lot Selection, Grain Direction & Sheet Dimension Matching Engine.
 * Module 19 Step 03.
 */
object BatchLotSelectionEngine {

    private val TOLERANCE_MM = BigDecimal("0.5000")
    private val GSM_PERCENT_TOLERANCE = BigDecimal("0.0300") // 3% GSM variation tolerance

    /**
     * Evaluates sheet dimension matching between required and candidate sheet dimensions.
     */
    fun evaluateDimensionMatch(
        required: PrintingDimension,
        candidate: PrintingDimension,
        allowRotation: Boolean
    ): Pair<DimensionMatch, Boolean> {
        val reqMm = SubstrateReservationMathUtils.toMillimeters(required)
        val candMm = SubstrateReservationMathUtils.toMillimeters(candidate)

        if (reqMm.width <= BigDecimal.ZERO || reqMm.height <= BigDecimal.ZERO ||
            candMm.width <= BigDecimal.ZERO || candMm.height <= BigDecimal.ZERO
        ) {
            return Pair(DimensionMatch.INVALID_DIMENSION, false)
        }

        val widthDiff = candMm.width.subtract(reqMm.width).abs()
        val heightDiff = candMm.height.subtract(reqMm.height).abs()

        // 1. Direct unrotated match
        if (widthDiff <= TOLERANCE_MM && heightDiff <= TOLERANCE_MM) {
            return Pair(DimensionMatch.EXACT_MATCH, false)
        }

        // 2. Rotated match (width becomes height, height becomes width)
        if (allowRotation) {
            val rotWidthDiff = candMm.width.subtract(reqMm.height).abs()
            val rotHeightDiff = candMm.height.subtract(reqMm.width).abs()
            if (rotWidthDiff <= TOLERANCE_MM && rotHeightDiff <= TOLERANCE_MM) {
                return Pair(DimensionMatch.ROTATED_MATCH, true)
            }
        }

        // 3. Oversized check (unrotated)
        val isOversizeUnrotated = candMm.width >= reqMm.width.subtract(TOLERANCE_MM) &&
                candMm.height >= reqMm.height.subtract(TOLERANCE_MM)
        if (isOversizeUnrotated) {
            return Pair(DimensionMatch.OVERSIZED_CUTTABLE, false)
        }

        // 4. Oversized check (rotated)
        if (allowRotation) {
            val isOversizeRotated = candMm.width >= reqMm.height.subtract(TOLERANCE_MM) &&
                    candMm.height >= reqMm.width.subtract(TOLERANCE_MM)
            if (isOversizeRotated) {
                return Pair(DimensionMatch.OVERSIZED_CUTTABLE, true)
            }
        }

        // 5. Undersized mismatch
        return Pair(DimensionMatch.UNDERSIZED_MISMATCH, false)
    }

    /**
     * Evaluates grain direction compatibility considering whether the candidate sheet is rotated.
     */
    fun evaluateGrainCompatibility(
        requiredGrain: PaperGrainDirection,
        candidateGrain: PaperGrainDirection,
        isRotated: Boolean
    ): GrainCompatibility {
        if (requiredGrain == PaperGrainDirection.NOT_APPLICABLE || candidateGrain == PaperGrainDirection.NOT_APPLICABLE) {
            return GrainCompatibility.EXACT_MATCH
        }

        if (requiredGrain == PaperGrainDirection.UNKNOWN || candidateGrain == PaperGrainDirection.UNKNOWN) {
            return GrainCompatibility.UNKNOWN_GRAIN
        }

        return if (!isRotated) {
            if (requiredGrain == candidateGrain) {
                GrainCompatibility.EXACT_MATCH
            } else {
                GrainCompatibility.INCOMPATIBLE
            }
        } else {
            // Rotating a sheet 90° inverts its effective grain relative to the press feed axis
            if (requiredGrain == PaperGrainDirection.LONG_GRAIN && candidateGrain == PaperGrainDirection.SHORT_GRAIN) {
                GrainCompatibility.ROTATED_COMPATIBLE
            } else if (requiredGrain == PaperGrainDirection.SHORT_GRAIN && candidateGrain == PaperGrainDirection.LONG_GRAIN) {
                GrainCompatibility.ROTATED_COMPATIBLE
            } else {
                GrainCompatibility.INCOMPATIBLE
            }
        }
    }

    /**
     * Evaluates a single candidate batch/lot against target requirements.
     */
    fun evaluateCandidate(
        spec: BatchLotSelectionSpecification,
        candidate: BatchLotInventoryCandidate
    ): EvaluatedBatchCandidate {
        val evaluationReasons = mutableListOf<String>()
        val rejectionReasons = mutableListOf<String>()

        // 1. Dimension Match Evaluation
        val (dimMatch, isRotated) = evaluateDimensionMatch(
            required = spec.requiredSheetDimension,
            candidate = candidate.sheetDimension,
            allowRotation = spec.allowSheetRotation
        )

        // 2. Grain Compatibility Evaluation
        val grainComp = evaluateGrainCompatibility(
            requiredGrain = spec.requiredGrainDirection,
            candidateGrain = candidate.grainDirection,
            isRotated = isRotated
        )

        // 3. GSM Match Evaluation
        val gsmDiff = candidate.gsm.subtract(spec.targetGsm).abs()
        val gsmMatchScore = if (gsmDiff <= BigDecimal("0.5000")) {
            evaluationReasons.add("Exact GSM match: ${candidate.gsm} gsm")
            100
        } else {
            val ratio = gsmDiff.divide(spec.targetGsm, 4, RoundingMode.HALF_UP)
            if (ratio <= GSM_PERCENT_TOLERANCE) {
                evaluationReasons.add("GSM within tolerance: ${candidate.gsm} gsm (Target: ${spec.targetGsm})")
                70
            } else {
                rejectionReasons.add("GSM mismatch: ${candidate.gsm} gsm vs required ${spec.targetGsm} gsm")
                0
            }
        }

        // Dimension diagnosis
        when (dimMatch) {
            DimensionMatch.EXACT_MATCH -> evaluationReasons.add("Exact sheet dimension: ${candidate.sheetDimension.width} x ${candidate.sheetDimension.height} mm")
            DimensionMatch.ROTATED_MATCH -> evaluationReasons.add("Rotated dimension match (90°): ${candidate.sheetDimension.width} x ${candidate.sheetDimension.height} mm")
            DimensionMatch.OVERSIZED_CUTTABLE -> evaluationReasons.add("Oversized sheet (pre-cut candidate): ${candidate.sheetDimension.width} x ${candidate.sheetDimension.height} mm")
            DimensionMatch.UNDERSIZED_MISMATCH -> rejectionReasons.add("Undersized sheet: ${candidate.sheetDimension.width} x ${candidate.sheetDimension.height} mm (Required: ${spec.requiredSheetDimension.width} x ${spec.requiredSheetDimension.height} mm)")
            DimensionMatch.INVALID_DIMENSION -> rejectionReasons.add("Invalid candidate sheet dimension")
        }

        // Grain diagnosis
        when (grainComp) {
            GrainCompatibility.EXACT_MATCH -> evaluationReasons.add("Exact grain alignment: ${candidate.grainDirection.name}")
            GrainCompatibility.ROTATED_COMPATIBLE -> evaluationReasons.add("Rotated grain alignment: ${candidate.grainDirection.name} becomes compatible when fed at 90°")
            GrainCompatibility.UNKNOWN_GRAIN -> evaluationReasons.add("Grain direction undefined on lot (Verify before printing)")
            GrainCompatibility.INCOMPATIBLE -> rejectionReasons.add("Incompatible grain direction: ${candidate.grainDirection.name} conflicts with required ${spec.requiredGrainDirection.name}")
        }

        // Usable stock check
        if (candidate.usableSheets <= 0L) {
            rejectionReasons.add("Zero usable stock in batch/lot (On-hand: ${candidate.onHandPhysicalSheets}, Reserved: ${candidate.reservedSheets}, HardAllocated: ${candidate.hardAllocatedSheets})")
        }

        val isEligible = dimMatch != DimensionMatch.UNDERSIZED_MISMATCH &&
                dimMatch != DimensionMatch.INVALID_DIMENSION &&
                grainComp != GrainCompatibility.INCOMPATIBLE &&
                gsmMatchScore > 0 &&
                candidate.usableSheets > 0L

        // Compute Weighted Composite Score (0–100)
        val dimWeight = BigDecimal("0.40")
        val grainWeight = BigDecimal("0.35")
        val gsmWeight = BigDecimal("0.15")
        val bonusWeight = BigDecimal("0.10")

        val dimScorePart = BigDecimal(dimMatch.scoreWeight).multiply(dimWeight)
        val grainScorePart = BigDecimal(grainComp.scoreWeight).multiply(grainWeight)
        val gsmScorePart = BigDecimal(gsmMatchScore).multiply(gsmWeight)

        // Preferred warehouse bonus
        val warehouseBonus = if (spec.preferredWarehouseId != null && candidate.warehouseId == spec.preferredWarehouseId) {
            BigDecimal("10.0000")
        } else {
            BigDecimal("5.0000")
        }
        val bonusPart = warehouseBonus.multiply(bonusWeight)

        val overallScore = if (isEligible) {
            dimScorePart.add(grainScorePart).add(gsmScorePart).add(bonusPart).setScale(4, RoundingMode.HALF_UP)
        } else {
            BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP)
        }

        return EvaluatedBatchCandidate(
            candidate = candidate,
            dimensionMatch = dimMatch,
            grainCompatibility = grainComp,
            isRotated = isRotated,
            gsmMatchScore = gsmMatchScore,
            overallScore = overallScore,
            isEligible = isEligible,
            evaluationReasons = evaluationReasons,
            rejectionReasons = rejectionReasons
        )
    }

    /**
     * Executes deterministic ranking and selection across all candidate batches.
     */
    fun selectBatches(
        spec: BatchLotSelectionSpecification,
        candidates: List<BatchLotInventoryCandidate>
    ): BatchLotSelectionResult {
        require(spec.tenantId.isNotBlank()) { "Tenant ID cannot be blank." }
        require(spec.orderId.isNotBlank()) { "Order ID cannot be blank." }
        require(spec.requiredSheets > 0L) { "Required sheets must be strictly positive." }

        // Evaluate all candidates
        val evaluated = candidates.map { evaluateCandidate(spec, it) }

        // Filter eligible
        val eligible = evaluated.filter { it.isEligible }

        if (eligible.isEmpty()) {
            val grainBlockers = evaluated.count { it.grainCompatibility == GrainCompatibility.INCOMPATIBLE }
            val dimBlockers = evaluated.count { it.dimensionMatch == DimensionMatch.UNDERSIZED_MISMATCH }

            val status = when {
                grainBlockers > 0 && dimBlockers == 0 -> BatchLotSelectionStatus.BLOCKED_BY_GRAIN
                dimBlockers > 0 && grainBlockers == 0 -> BatchLotSelectionStatus.BLOCKED_BY_DIMENSION
                candidates.isEmpty() || evaluated.all { it.candidate.usableSheets <= 0L } -> BatchLotSelectionStatus.INSUFFICIENT_STOCK
                else -> BatchLotSelectionStatus.NO_COMPATIBLE_BATCH
            }

            val explanation = "Selection failed: 0 eligible batches found among ${candidates.size} evaluated lots. Status: ${status.label}"
            val masterHash = computeMasterIntegrityHash(spec, status, emptyList(), evaluated, 0L)

            return BatchLotSelectionResult(
                selectionId = spec.selectionId,
                tenantId = spec.tenantId,
                specification = spec,
                status = status,
                requiredSheets = spec.requiredSheets,
                allocatedSheets = 0L,
                deficitSheets = spec.requiredSheets,
                allocatedReams = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP),
                allocatedWeightKg = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP),
                isFullySatisfied = false,
                isMultiBatchFulfillment = false,
                selectedBatches = emptyList(),
                evaluatedCandidates = evaluated,
                overallCompatibilityScore = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP),
                selectionExplanation = explanation,
                masterIntegrityHash = masterHash,
                selectedBy = spec.actor
            )
        }

        // Deterministic ranking comparator
        val ranked = eligible.sortedWith { a, b ->
            val scoreCmp = b.overallScore.compareTo(a.overallScore)
            if (scoreCmp != 0) return@sortedWith scoreCmp

            when (spec.selectionPolicy) {
                BatchSelectionPolicy.FIFO -> {
                    val timeCmp = a.candidate.receivedTimestamp.compareTo(b.candidate.receivedTimestamp)
                    if (timeCmp != 0) return@sortedWith timeCmp
                }
                BatchSelectionPolicy.FEFO -> {
                    val expA = a.candidate.expiryTimestamp ?: Long.MAX_VALUE
                    val expB = b.candidate.expiryTimestamp ?: Long.MAX_VALUE
                    val expCmp = expA.compareTo(expB)
                    if (expCmp != 0) return@sortedWith expCmp
                }
                BatchSelectionPolicy.MINIMAL_WASTE -> {
                    val remA = (a.candidate.usableSheets - spec.requiredSheets).let { if (it < 0) Long.MAX_VALUE else it }
                    val remB = (b.candidate.usableSheets - spec.requiredSheets).let { if (it < 0) Long.MAX_VALUE else it }
                    val remCmp = remA.compareTo(remB)
                    if (remCmp != 0) return@sortedWith remCmp
                }
                BatchSelectionPolicy.SINGLE_LOT_ONLY -> {
                    val singleFitA = if (a.candidate.usableSheets >= spec.requiredSheets) 0 else 1
                    val singleFitB = if (b.candidate.usableSheets >= spec.requiredSheets) 0 else 1
                    val fitCmp = singleFitA.compareTo(singleFitB)
                    if (fitCmp != 0) return@sortedWith fitCmp
                }
            }

            // Strict deterministic tie-breakers
            val batchCmp = a.candidate.batchNumber.compareTo(b.candidate.batchNumber)
            if (batchCmp != 0) return@sortedWith batchCmp

            val lotCmp = a.candidate.lotNumber.compareTo(b.candidate.lotNumber)
            if (lotCmp != 0) return@sortedWith lotCmp

            a.candidate.candidateId.compareTo(b.candidate.candidateId)
        }

        // Fulfill requirements
        val selectedAllocations = mutableListOf<SelectedBatchAllocation>()
        var remainingNeeded = spec.requiredSheets

        if (!spec.allowMultiBatchFulfillment || spec.selectionPolicy == BatchSelectionPolicy.SINGLE_LOT_ONLY) {
            // Find best single batch that satisfies full requirement
            val singleSatisfying = ranked.firstOrNull { it.candidate.usableSheets >= spec.requiredSheets }
            val chosen = singleSatisfying ?: ranked.first()

            val allocatedFromChosen = minOf(spec.requiredSheets, chosen.candidate.usableSheets)
            val reams = SubstrateReservationMathUtils.calculateReams(allocatedFromChosen)
            val weightKg = SubstrateReservationMathUtils.calculateTotalWeightKg(
                allocatedFromChosen,
                spec.targetGsm,
                chosen.candidate.sheetDimension
            )

            selectedAllocations.add(
                SelectedBatchAllocation(
                    allocationId = "SBA-${UUID.randomUUID().toString().take(8).uppercase()}",
                    selectionId = spec.selectionId,
                    tenantId = spec.tenantId,
                    warehouseId = chosen.candidate.warehouseId,
                    warehouseName = chosen.candidate.warehouseName,
                    locationId = chosen.candidate.locationId,
                    batchNumber = chosen.candidate.batchNumber,
                    lotNumber = chosen.candidate.lotNumber,
                    sku = chosen.candidate.sku,
                    allocatedSheets = allocatedFromChosen,
                    allocatedReams = reams,
                    allocatedWeightKg = weightKg,
                    sheetDimension = chosen.candidate.sheetDimension,
                    grainDirection = chosen.candidate.grainDirection,
                    isRotated = chosen.isRotated,
                    matchScore = chosen.overallScore
                )
            )
            remainingNeeded -= allocatedFromChosen
        } else {
            // Multi-batch fulfillment
            for (candidate in ranked) {
                if (remainingNeeded <= 0L) break
                val allocQty = minOf(remainingNeeded, candidate.candidate.usableSheets)
                if (allocQty > 0L) {
                    val reams = SubstrateReservationMathUtils.calculateReams(allocQty)
                    val weightKg = SubstrateReservationMathUtils.calculateTotalWeightKg(
                        allocQty,
                        spec.targetGsm,
                        candidate.candidate.sheetDimension
                    )

                    selectedAllocations.add(
                        SelectedBatchAllocation(
                            allocationId = "SBA-${UUID.randomUUID().toString().take(8).uppercase()}",
                            selectionId = spec.selectionId,
                            tenantId = spec.tenantId,
                            warehouseId = candidate.candidate.warehouseId,
                            warehouseName = candidate.candidate.warehouseName,
                            locationId = candidate.candidate.locationId,
                            batchNumber = candidate.candidate.batchNumber,
                            lotNumber = candidate.candidate.lotNumber,
                            sku = candidate.candidate.sku,
                            allocatedSheets = allocQty,
                            allocatedReams = reams,
                            allocatedWeightKg = weightKg,
                            sheetDimension = candidate.candidate.sheetDimension,
                            grainDirection = candidate.candidate.grainDirection,
                            isRotated = candidate.isRotated,
                            matchScore = candidate.overallScore
                        )
                    )
                    remainingNeeded -= allocQty
                }
            }
        }

        val totalAllocated = selectedAllocations.sumOf { it.allocatedSheets }
        val deficit = spec.requiredSheets - totalAllocated
        val isFullySatisfied = deficit <= 0L
        val isMultiBatch = selectedAllocations.size > 1

        val status = if (isFullySatisfied) {
            BatchLotSelectionStatus.FULLY_SATISFIED
        } else if (totalAllocated > 0L) {
            BatchLotSelectionStatus.PARTIALLY_SATISFIED
        } else {
            BatchLotSelectionStatus.INSUFFICIENT_STOCK
        }

        val totalReams = selectedAllocations.fold(BigDecimal.ZERO) { acc, a -> acc.add(a.allocatedReams) }.setScale(4, RoundingMode.HALF_UP)
        val totalWeight = selectedAllocations.fold(BigDecimal.ZERO) { acc, a -> acc.add(a.allocatedWeightKg) }.setScale(4, RoundingMode.HALF_UP)
        val avgScore = if (selectedAllocations.isNotEmpty()) {
            selectedAllocations.fold(BigDecimal.ZERO) { acc, a -> acc.add(a.matchScore) }
                .divide(BigDecimal(selectedAllocations.size), 4, RoundingMode.HALF_UP)
        } else {
            BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP)
        }

        val primaryBatch = selectedAllocations.firstOrNull()?.batchNumber
        val primaryLot = selectedAllocations.firstOrNull()?.lotNumber
        val primaryWh = selectedAllocations.firstOrNull()?.warehouseId

        val explanation = buildString {
            if (isFullySatisfied) {
                append("Successfully selected ${selectedAllocations.size} batch(es) fulfilling ${totalAllocated} sheets (100% satisfied). ")
                append("Primary lot: $primaryLot (Batch: $primaryBatch) at warehouse $primaryWh. ")
                append("Compatibility score: $avgScore/100. Policy: ${spec.selectionPolicy.label}.")
            } else {
                append("Selection partially fulfilled ${totalAllocated}/${spec.requiredSheets} sheets with ${deficit} sheets deficit. ")
                append("Status: ${status.label}.")
            }
        }

        val masterHash = computeMasterIntegrityHash(spec, status, selectedAllocations, evaluated, totalAllocated)

        return BatchLotSelectionResult(
            selectionId = spec.selectionId,
            tenantId = spec.tenantId,
            specification = spec,
            status = status,
            requiredSheets = spec.requiredSheets,
            allocatedSheets = totalAllocated,
            deficitSheets = deficit,
            allocatedReams = totalReams,
            allocatedWeightKg = totalWeight,
            isFullySatisfied = isFullySatisfied,
            isMultiBatchFulfillment = isMultiBatch,
            selectedBatches = selectedAllocations,
            evaluatedCandidates = evaluated,
            primarySelectedBatchNumber = primaryBatch,
            primarySelectedLotNumber = primaryLot,
            primaryWarehouseId = primaryWh,
            overallCompatibilityScore = avgScore,
            selectionExplanation = explanation,
            masterIntegrityHash = masterHash,
            selectedBy = spec.actor
        )
    }

    /**
     * Generates a cryptographic SHA-256 master integrity seal for the selection decision.
     */
    fun computeMasterIntegrityHash(
        spec: BatchLotSelectionSpecification,
        status: BatchLotSelectionStatus,
        allocations: List<SelectedBatchAllocation>,
        evaluated: List<EvaluatedBatchCandidate>,
        totalAllocated: Long
    ): String {
        val raw = buildString {
            append("TENANT:${spec.tenantId}|")
            append("ORDER:${spec.orderId}|")
            append("ITEM:${spec.orderItemId}|")
            append("REQ_SHEETS:${spec.requiredSheets}|")
            append("GSM:${spec.targetGsm}|")
            append("DIM:${spec.requiredSheetDimension.width}x${spec.requiredSheetDimension.height}|")
            append("GRAIN:${spec.requiredGrainDirection.name}|")
            append("STATUS:${status.name}|")
            append("ALLOCATED:$totalAllocated|")
            append("BATCHES:")
            allocations.sortedBy { it.batchNumber + it.lotNumber }.forEach {
                append("[${it.batchNumber}:${it.lotNumber}:${it.allocatedSheets}:${it.isRotated}]")
            }
            append("|EVAL_COUNT:${evaluated.size}")
        }
        val digest = MessageDigest.getInstance("SHA-256")
        return digest.digest(raw.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) }
    }
}
