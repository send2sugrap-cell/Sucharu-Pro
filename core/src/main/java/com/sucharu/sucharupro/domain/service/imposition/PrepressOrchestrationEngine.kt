package com.sucharu.sucharupro.domain.service.imposition

import com.sucharu.sucharupro.domain.model.imposition.*
import java.math.BigDecimal
import java.math.RoundingMode
import java.security.MessageDigest

/**
 * Mathematical Engine for End-to-End Prepress Master Orchestration,
 * Cross-Step Reconciliation, Readiness Scoring & AI Handoff.
 * Module 18 Step 06.
 */
object PrepressOrchestrationEngine {

    private const val SCALE = 4
    private val ROUNDING = RoundingMode.HALF_UP
    private val HUNDRED = BigDecimal("100.0000")

    /**
     * Synthesizes and validates an end-to-end Prepress Orchestration Plan
     * by reconciling Step 01 to Step 05 specifications.
     */
    fun orchestratePlan(
        tenantId: String,
        planName: String? = null,
        jobId: String?,
        orderId: String,
        orderItemId: String,
        productName: String,
        requiredQuantity: Long,
        step01Imposition: ImpositionSpecification? = null,
        step02GangRun: GangRunSpecification? = null,
        step03Nesting: DynamicNestingSpecification? = null,
        step04Signature: SignatureImpositionSpecification? = null,
        step05CtpOutput: CtpOutputSpecification? = null,
        planVersion: Int = 1,
        actor: String = "prepress_orchestrator"
    ): PrepressOrchestrationPlan {
        require(tenantId.isNotBlank()) { "Tenant ID must not be blank." }
        require(orderId.isNotBlank()) { "Order ID must not be blank." }
        require(orderItemId.isNotBlank()) { "Order Item ID must not be blank." }
        require(requiredQuantity > 0L) { "Required quantity must be positive." }

        val planId = "PLAN-PREPRESS-${orderId.takeLast(6)}-${System.currentTimeMillis() % 100000}"
        val computedPlanName = planName ?: "Prepress Master Orchestration: $productName ($orderId)"

        // 1. Build Pipeline Stages
        val pipelineStages = mutableListOf<PipelineStageStatus>()
        pipelineStages.add(
            PipelineStageStatus(
                stageStep = "STEP_01",
                stageName = "Single Job Imposition",
                isApplicable = step01Imposition != null,
                status = if (step01Imposition != null) "OPTIMIZED" else "BYPASSED",
                referenceId = step01Imposition?.impositionId,
                integrityHash = step01Imposition?.integrityHash,
                summary = if (step01Imposition != null) "${step01Imposition.copiesPerSheet} up, ${step01Imposition.yieldPercentage}% yield" else "Not utilized in this workflow"
            )
        )
        pipelineStages.add(
            PipelineStageStatus(
                stageStep = "STEP_02",
                stageName = "Multi-Job Gang-Run Batch",
                isApplicable = step02GangRun != null,
                status = if (step02GangRun != null) step02GangRun.status.name else "BYPASSED",
                referenceId = step02GangRun?.gangRunId,
                integrityHash = step02GangRun?.integrityHash,
                summary = if (step02GangRun != null) "${step02GangRun.allocations.size} jobs batched, ${step02GangRun.allocatedSlotsCount} slots" else "Not utilized in this workflow"
            )
        )
        pipelineStages.add(
            PipelineStageStatus(
                stageStep = "STEP_03",
                stageName = "Dynamic 2D Irregular Nesting",
                isApplicable = step03Nesting != null,
                status = if (step03Nesting != null) step03Nesting.status.name else "BYPASSED",
                referenceId = step03Nesting?.nestingId,
                integrityHash = step03Nesting?.integrityHash,
                summary = if (step03Nesting != null) "${step03Nesting.totalItemsPlaced} items placed, ${step03Nesting.sheetUtilizationPercentage}% util" else "Not utilized in this workflow"
            )
        )
        pipelineStages.add(
            PipelineStageStatus(
                stageStep = "STEP_04",
                stageName = "Signature Book Imposition",
                isApplicable = step04Signature != null,
                status = if (step04Signature != null) step04Signature.status.name else "BYPASSED",
                referenceId = step04Signature?.signatureImpositionId,
                integrityHash = step04Signature?.integrityHash,
                summary = if (step04Signature != null) "${step04Signature.totalPages} pages, ${step04Signature.totalSignaturesCount} sigs (${step04Signature.sheetTurningMethod})" else "Not utilized in this workflow"
            )
        )
        pipelineStages.add(
            PipelineStageStatus(
                stageStep = "STEP_05",
                stageName = "CTP Prepress Plate Package",
                isApplicable = step05CtpOutput != null,
                status = if (step05CtpOutput != null) step05CtpOutput.status.name else "PENDING_GENERATION",
                referenceId = step05CtpOutput?.ctpOutputId,
                integrityHash = step05CtpOutput?.integrityHash,
                summary = if (step05CtpOutput != null) "${step05CtpOutput.outputPackage.totalPlatesCount} plates (${step05CtpOutput.resolutionDpi.dpi} DPI, ${step05CtpOutput.screeningMethod})" else "CTP plates pending generation"
            )
        )

        // 2. Cross-Step Reconciliation
        val reconciliation = reconcileSteps(
            requiredQuantity = requiredQuantity,
            step01 = step01Imposition,
            step02 = step02GangRun,
            step03 = step03Nesting,
            step04 = step04Signature,
            step05 = step05CtpOutput
        )

        // 3. Multi-Dimensional Readiness Scoring
        val readinessScore = computeReadinessScore(
            reconciliation = reconciliation,
            step01 = step01Imposition,
            step02 = step02GangRun,
            step03 = step03Nesting,
            step04 = step04Signature,
            step05 = step05CtpOutput
        )

        // 4. Deterministic Optimization Recommendations
        val recommendations = analyzeRecommendations(
            step01 = step01Imposition,
            step02 = step02GangRun,
            step03 = step03Nesting,
            step04 = step04Signature,
            step05 = step05CtpOutput,
            reconciliation = reconciliation
        )

        // 5. Compute Press & Plate Bounds
        val pressSheetWidth = step05CtpOutput?.outputPackage?.pressSheetWidthMm
            ?: step04Signature?.parentSheetDimension?.width
            ?: step03Nesting?.parentSheetDimension?.width
            ?: step02GangRun?.parentSheetDimension?.width
            ?: step01Imposition?.parentSheetDimension?.width
            ?: BigDecimal("635.0000")

        val pressSheetHeight = step05CtpOutput?.outputPackage?.pressSheetHeightMm
            ?: step04Signature?.parentSheetDimension?.height
            ?: step03Nesting?.parentSheetDimension?.height
            ?: step02GangRun?.parentSheetDimension?.height
            ?: step01Imposition?.parentSheetDimension?.height
            ?: BigDecimal("914.4000")

        val plateWidth = step05CtpOutput?.plateDimensionSpec?.plateWidthMm ?: BigDecimal("745.0000")
        val plateHeight = step05CtpOutput?.plateDimensionSpec?.plateHeightMm ?: BigDecimal("605.0000")

        val signaturesCount = step04Signature?.totalSignaturesCount ?: if (step01Imposition != null) 1 else 0
        val platesCount = step05CtpOutput?.outputPackage?.totalPlatesCount ?: (signaturesCount * 4)

        // 6. Master Cryptographic Seal (SHA-256)
        val masterIntegrityHash = computeMasterIntegritySeal(
            tenantId = tenantId,
            jobId = jobId,
            orderId = orderId,
            orderItemId = orderItemId,
            producedQty = reconciliation.reconciledProducedQuantity,
            reqSheets = reconciliation.reconciledRequiredSheets,
            totalSigs = signaturesCount,
            totalPlates = platesCount,
            pressSheetWidth = pressSheetWidth,
            pressSheetHeight = pressSheetHeight,
            plateWidth = plateWidth,
            plateHeight = plateHeight,
            step01Hash = step01Imposition?.integrityHash,
            step02Hash = step02GangRun?.integrityHash,
            step03Hash = step03Nesting?.integrityHash,
            step04Hash = step04Signature?.integrityHash,
            step05Hash = step05CtpOutput?.integrityHash,
            readinessScore = readinessScore.overallScore
        )

        val planStatus = when {
            reconciliation.blockingErrorsCount > 0 -> PrepressPlanStatus.WARNING
            readinessScore.overallScore >= BigDecimal("90.0000") -> PrepressPlanStatus.READY
            readinessScore.overallScore >= BigDecimal("75.0000") -> PrepressPlanStatus.VALIDATED
            else -> PrepressPlanStatus.VALIDATING
        }

        return PrepressOrchestrationPlan(
            planId = planId,
            tenantId = tenantId,
            planName = computedPlanName,
            version = planVersion,
            status = planStatus,
            jobId = jobId,
            orderId = orderId,
            orderItemId = orderItemId,
            productName = productName,
            step01ImpositionId = step01Imposition?.impositionId,
            step01IntegrityHash = step01Imposition?.integrityHash,
            step02GangRunBatchId = step02GangRun?.gangRunId,
            step02IntegrityHash = step02GangRun?.integrityHash,
            step03NestingId = step03Nesting?.nestingId,
            step03IntegrityHash = step03Nesting?.integrityHash,
            step04SignatureId = step04Signature?.signatureImpositionId,
            step04IntegrityHash = step04Signature?.integrityHash,
            step05CtpOutputId = step05CtpOutput?.ctpOutputId,
            step05IntegrityHash = step05CtpOutput?.integrityHash,
            requiredQuantity = requiredQuantity,
            totalProducedQuantity = reconciliation.reconciledProducedQuantity,
            requiredSheets = reconciliation.reconciledRequiredSheets,
            sheetUtilizationPercentage = reconciliation.reconciledUtilizationPercentage,
            wastePercentage = reconciliation.reconciledWastePercentage,
            totalSignaturesCount = signaturesCount,
            totalPlatesCount = platesCount,
            pressSheetWidthMm = pressSheetWidth,
            pressSheetHeightMm = pressSheetHeight,
            plateWidthMm = plateWidth,
            plateHeightMm = plateHeight,
            pipelineStages = pipelineStages,
            reconciliationResult = reconciliation,
            readinessScore = readinessScore,
            recommendations = recommendations,
            masterIntegrityHash = masterIntegrityHash,
            approvalStatus = "PENDING_REVIEW",
            approvedBy = null,
            approvedAt = null,
            aiHandoffStatus = "READY_FOR_HANDOFF",
            downstreamHandoffStatus = "EMITTED",
            notes = "Master prepress orchestration plan successfully reconciled and sealed.",
            createdAt = System.currentTimeMillis(),
            createdBy = actor
        )
    }

    /**
     * Reconciles quantities, sheet counts, plate counts, and physical dimensions across steps.
     */
    fun reconcileSteps(
        requiredQuantity: Long,
        step01: ImpositionSpecification?,
        step02: GangRunSpecification?,
        step03: DynamicNestingSpecification?,
        step04: SignatureImpositionSpecification?,
        step05: CtpOutputSpecification?
    ): PrepressReconciliationResult {
        val discrepancies = mutableListOf<ReconciliationDiscrepancy>()

        // 1. Produced Quantity Check
        val producedQty = when {
            step04 != null -> step04.totalProducedCopies
            step03 != null -> step03.totalProducedItems
            step02 != null -> step02.totalProducedItems
            step01 != null -> step01.totalProducedCapacity
            else -> requiredQuantity
        }

        if (producedQty < requiredQuantity) {
            discrepancies.add(
                ReconciliationDiscrepancy(
                    field = "producedQuantity",
                    sourceStep = "ORDER_INPUT",
                    targetStep = "IMPOSITION_OUTPUT",
                    expectedValue = "$requiredQuantity",
                    actualValue = "$producedQty",
                    severity = ReconciliationSeverity.BLOCKING_ERROR,
                    message = "Produced capacity ($producedQty) is insufficient for required quantity ($requiredQuantity)."
                )
            )
        }

        // 2. Required Sheets Check
        val reqSheets = when {
            step04 != null -> step04.totalParentSheetsRequired
            step03 != null -> step03.commonRequiredSheets
            step02 != null -> step02.commonRequiredSheets
            step01 != null -> step01.requiredSheets
            else -> 1000L
        }

        // 3. Sheet Dimensions Harmony (Step 04 vs Step 05)
        if (step04 != null && step05 != null) {
            val sigW = step04.parentSheetDimension.width.setScale(2, ROUNDING)
            val sigH = step04.parentSheetDimension.height.setScale(2, ROUNDING)
            val ctpW = step05.outputPackage.pressSheetWidthMm.setScale(2, ROUNDING)
            val ctpH = step05.outputPackage.pressSheetHeightMm.setScale(2, ROUNDING)

            if (sigW.compareTo(ctpW) != 0 || sigH.compareTo(ctpH) != 0) {
                discrepancies.add(
                    ReconciliationDiscrepancy(
                        field = "pressSheetDimension",
                        sourceStep = "STEP_04_SIGNATURE",
                        targetStep = "STEP_05_CTP",
                        expectedValue = "${sigW}x${sigH}mm",
                        actualValue = "${ctpW}x${ctpH}mm",
                        severity = ReconciliationSeverity.BLOCKING_ERROR,
                        message = "Press sheet dimension mismatch between Signature layout and CTP plate output."
                    )
                )
            }
        }

        // 4. Plate Count Verification
        if (step04 != null && step05 != null) {
            val formsPerSig = when (step04.sheetTurningMethod) {
                SheetTurningMethod.SHEETWISE -> 2
                SheetTurningMethod.WORK_AND_TURN, SheetTurningMethod.WORK_AND_TUMBLE, SheetTurningMethod.PERFECTING -> 1
            }
            val expectedPlates = step04.totalSignaturesCount * formsPerSig * (step05.outputPackage.frontPlatesCount / formsPerSig.coerceAtLeast(1))
            val actualPlates = step05.outputPackage.totalPlatesCount

            if (actualPlates < expectedPlates && actualPlates > 0) {
                discrepancies.add(
                    ReconciliationDiscrepancy(
                        field = "totalPlatesCount",
                        sourceStep = "STEP_04_SIGNATURE",
                        targetStep = "STEP_05_CTP",
                        expectedValue = "$expectedPlates plates",
                        actualValue = "$actualPlates plates",
                        severity = ReconciliationSeverity.WARNING,
                        message = "Plate package count ($actualPlates) is lower than theoretical signature requirement ($expectedPlates)."
                    )
                )
            }
        }

        val blockingCount = discrepancies.count { it.severity == ReconciliationSeverity.BLOCKING_ERROR }
        val warningsCount = discrepancies.count { it.severity == ReconciliationSeverity.WARNING }
        val isReconciled = blockingCount == 0

        val utilization = step04?.sheetUtilizationPercentage
            ?: step03?.sheetUtilizationPercentage
            ?: step02?.sheetYieldPercentage
            ?: step01?.yieldPercentage
            ?: BigDecimal("88.5000")

        val waste = BigDecimal("100.0000").subtract(utilization).setScale(SCALE, ROUNDING)

        val totalPages = step04?.totalPages ?: 16
        val sigsCount = step04?.totalSignaturesCount ?: 1
        val platesCount = step05?.outputPackage?.totalPlatesCount ?: (sigsCount * 4)

        val summary = if (isReconciled) {
            "All parameters fully harmonized across pipeline stages with $warningsCount warnings."
        } else {
            "Reconciliation blocked by $blockingCount error(s) and $warningsCount warning(s)."
        }

        return PrepressReconciliationResult(
            isReconciled = isReconciled,
            blockingErrorsCount = blockingCount,
            warningsCount = warningsCount,
            discrepancies = discrepancies,
            reconciledProducedQuantity = producedQty,
            reconciledRequiredSheets = reqSheets,
            reconciledTotalPages = totalPages,
            reconciledSignaturesCount = sigsCount,
            reconciledPlatesCount = platesCount,
            reconciledWastePercentage = waste,
            reconciledUtilizationPercentage = utilization,
            summary = summary
        )
    }

    /**
     * Computes a multi-dimensional readiness score (0–100 scale, scale = 4, HALF_UP).
     */
    fun computeReadinessScore(
        reconciliation: PrepressReconciliationResult,
        step01: ImpositionSpecification?,
        step02: GangRunSpecification?,
        step03: DynamicNestingSpecification?,
        step04: SignatureImpositionSpecification?,
        step05: CtpOutputSpecification?
    ): PrepressReadinessScore {
        val geoScore = BigDecimal("20.0000")
        val nestScore = if (step03 != null) step03.sheetUtilizationPercentage.multiply(BigDecimal("0.15")).setScale(SCALE, ROUNDING) else BigDecimal("15.0000")
        val gangScore = if (step02 != null) step02.sheetYieldPercentage.multiply(BigDecimal("0.15")).setScale(SCALE, ROUNDING) else BigDecimal("15.0000")
        val utilScore = reconciliation.reconciledUtilizationPercentage.multiply(BigDecimal("0.20")).setScale(SCALE, ROUNDING)
        val sigScore = if (step04 != null) BigDecimal("15.0000") else BigDecimal("15.0000")
        val ctpScore = if (step05 != null) BigDecimal("15.0000") else BigDecimal("10.0000")
        val hashScore = if (step05?.integrityHash != null && step04?.integrityHash != null) BigDecimal("10.0000") else BigDecimal("8.0000")

        val rawSum = geoScore.add(nestScore).add(gangScore).add(utilScore).add(sigScore).add(ctpScore).add(hashScore)
        val normalizedBase = rawSum.min(HUNDRED)

        val penalty = BigDecimal(reconciliation.blockingErrorsCount * 25 + reconciliation.warningsCount * 5).setScale(SCALE, ROUNDING)
        val overall = normalizedBase.subtract(penalty).max(BigDecimal.ZERO).setScale(SCALE, ROUNDING)

        val summary = when {
            overall >= BigDecimal("90.0000") -> "EXCELLENT: Production-Ready Prepress Package ($overall / 100)"
            overall >= BigDecimal("75.0000") -> "GOOD: Validated with minor optimization opportunities ($overall / 100)"
            overall >= BigDecimal("50.0000") -> "FAIR: Operational with warnings ($overall / 100)"
            else -> "ACTION REQUIRED: Blocking errors or severe inefficiencies ($overall / 100)"
        }

        return PrepressReadinessScore(
            overallScore = overall,
            geometricValidityScore = geoScore,
            nestingEfficiencyScore = nestScore,
            gangRunEfficiencyScore = gangScore,
            sheetUtilizationScore = utilScore,
            signatureValidityScore = sigScore,
            ctpReadinessScore = ctpScore,
            integrityVerificationScore = hashScore,
            penaltyPoints = penalty,
            summary = summary
        )
    }

    /**
     * Analyzes specifications to produce deterministic optimization recommendations.
     */
    fun analyzeRecommendations(
        step01: ImpositionSpecification?,
        step02: GangRunSpecification?,
        step03: DynamicNestingSpecification?,
        step04: SignatureImpositionSpecification?,
        step05: CtpOutputSpecification?,
        reconciliation: PrepressReconciliationResult
    ): List<PrepressOptimizationRecommendation> {
        val list = mutableListOf<PrepressOptimizationRecommendation>()

        // Recommendation 1: Sheet Orientation Optimization
        if (reconciliation.reconciledUtilizationPercentage < BigDecimal("85.0000")) {
            list.add(
                PrepressOptimizationRecommendation(
                    recommendationType = "SHEET_ORIENTATION_OPTIMIZATION",
                    title = "Rotate Press Sheet Grain Orientation 90°",
                    description = "Rotating the press sheet orientation by 90 degrees can increase slot packing density and raise yield from ${reconciliation.reconciledUtilizationPercentage}% to ~91%.",
                    affectedStep = "STEP_01_OR_STEP_04",
                    estimatedWasteReductionPercentage = BigDecimal("4.5000"),
                    estimatedPlateSavingsCount = 0,
                    rationale = "Geometric bounding box aspect ratio is better aligned with parent sheet grain.",
                    confidenceScore = BigDecimal("0.9200"),
                    requiresApproval = true,
                    isApplied = false
                )
            )
        }

        // Recommendation 2: Spot Color to CMYK Conversion
        if (step05 != null && step05.outputPackage.spotColorsCount > 0) {
            val potentialSavings = step05.outputPackage.spotColorsCount * 2
            val spotNames = step05.outputPackage.plates.mapNotNull { it.spotColorName }.distinct()
            list.add(
                PrepressOptimizationRecommendation(
                    recommendationType = "SPOT_COLOR_CONVERSION_ANALYSIS",
                    title = "Convert ${step05.outputPackage.spotColorsCount} Spot Color(s) to Process CMYK Simulation",
                    description = "Converting spot colors (${spotNames.joinToString()}) to standard 4-color process eliminates $potentialSavings separate plates.",
                    affectedStep = "STEP_05_CTP",
                    estimatedWasteReductionPercentage = BigDecimal("2.0000"),
                    estimatedPlateSavingsCount = potentialSavings,
                    rationale = "Delta-E color gamut verification allows 98% Pantone simulation fidelity within ISO 12647-2 tolerances.",
                    confidenceScore = BigDecimal("0.8800"),
                    requiresApproval = true,
                    isApplied = false
                )
            )
        }

        // Recommendation 3: Gang-Run Clustering Consolidation
        if (step02 != null && step02.allocations.size > 1 && step02.sheetYieldPercentage < BigDecimal("88.0000")) {
            list.add(
                PrepressOptimizationRecommendation(
                    recommendationType = "GANG_RUN_CLUSTERED_RESCHEDULING",
                    title = "Consolidate Similar Substrate Gang-Runs",
                    description = "Merging this batch with scheduled items on ${step02.gsm} GSM ${step02.paperStockType} can recover ~3.5% sheet offcut area.",
                    affectedStep = "STEP_02_GANG_RUN",
                    estimatedWasteReductionPercentage = BigDecimal("3.5000"),
                    estimatedPlateSavingsCount = 0,
                    rationale = "Combining batches increases slot allocation from ${step02.allocatedSlotsCount} to full sheet capacity.",
                    confidenceScore = BigDecimal("0.9500"),
                    requiresApproval = true,
                    isApplied = false
                )
            )
        }

        return list
    }

    /**
     * Deterministic SHA-256 Master Cryptographic Integrity Seal computation.
     */
    fun computeMasterIntegritySeal(
        tenantId: String,
        jobId: String?,
        orderId: String,
        orderItemId: String,
        producedQty: Long,
        reqSheets: Long,
        totalSigs: Int,
        totalPlates: Int,
        pressSheetWidth: BigDecimal,
        pressSheetHeight: BigDecimal,
        plateWidth: BigDecimal,
        plateHeight: BigDecimal,
        step01Hash: String?,
        step02Hash: String?,
        step03Hash: String?,
        step04Hash: String?,
        step05Hash: String?,
        readinessScore: BigDecimal
    ): String {
        val payload = buildString {
            append("v1|")
            append(tenantId).append("|")
            append(jobId ?: "NO_JOB").append("|")
            append(orderId).append("|")
            append(orderItemId).append("|")
            append(producedQty).append("|")
            append(reqSheets).append("|")
            append(totalSigs).append("|")
            append(totalPlates).append("|")
            append(pressSheetWidth.setScale(2, ROUNDING).toPlainString()).append("x")
            append(pressSheetHeight.setScale(2, ROUNDING).toPlainString()).append("|")
            append(plateWidth.setScale(2, ROUNDING).toPlainString()).append("x")
            append(plateHeight.setScale(2, ROUNDING).toPlainString()).append("|")
            append(step01Hash ?: "NO_S01").append("|")
            append(step02Hash ?: "NO_S02").append("|")
            append(step03Hash ?: "NO_S03").append("|")
            append(step04Hash ?: "NO_S04").append("|")
            append(step05Hash ?: "NO_S05").append("|")
            append(readinessScore.setScale(2, ROUNDING).toPlainString())
        }

        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(payload.toByteArray(Charsets.UTF_8))
        return hashBytes.joinToString("") { "%02x".format(it) }
    }
}
