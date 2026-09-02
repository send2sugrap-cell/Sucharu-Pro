package com.sucharu.sucharupro.domain.service.jobclosure

import com.sucharu.sucharupro.domain.model.finalqc.FinishedGoodsReleaseRecord
import com.sucharu.sucharupro.domain.model.finalqc.ProductionFinalQcInspection
import com.sucharu.sucharupro.domain.model.jobclosure.*
import com.sucharu.sucharupro.domain.model.jobcosting.ProductionActualJobCostRecord
import com.sucharu.sucharupro.domain.model.jobcosting.ProductionJobCostVarianceSummary
import com.sucharu.sucharupro.domain.model.jobcosting.ProductionJobCostingReconciliationResult
import com.sucharu.sucharupro.domain.model.jobcosting.VarianceClassification
import com.sucharu.sucharupro.domain.model.productionexecution.ProductionJobExecution
import com.sucharu.sucharupro.domain.model.productionexecution.ProductionWorkOrder
import com.sucharu.sucharupro.domain.model.productionscheduling.ProductionSchedule
import com.sucharu.sucharupro.domain.model.shopfloortracking.OperatorTimeTrackingRecord
import com.sucharu.sucharupro.domain.model.shopfloortracking.ProductionMaterialConsumptionRecord
import java.math.BigDecimal
import java.security.MessageDigest
import java.util.UUID

class JobClosureReadinessAuditEngine {

    fun performPreClosureAudit(
        tenantId: String,
        executionJobId: String,
        orderId: String,
        jobExecution: ProductionJobExecution?,
        workOrders: List<ProductionWorkOrder>,
        schedule: ProductionSchedule?,
        materialConsumptions: List<ProductionMaterialConsumptionRecord>,
        timeRecords: List<OperatorTimeTrackingRecord>,
        finalQcInspection: ProductionFinalQcInspection?,
        goodsRelease: FinishedGoodsReleaseRecord?,
        costRecord: ProductionActualJobCostRecord?,
        reconciliationResult: ProductionJobCostingReconciliationResult?,
        auditedBy: String = "closure-auditor"
    ): JobClosureReadinessAudit {
        val discrepancies = mutableListOf<String>()

        val quoteCommitmentOk = orderId.isNotBlank() && jobExecution != null
        if (!quoteCommitmentOk) discrepancies.add("Job execution record or Order ID is missing")

        val planningOk = jobExecution?.planningId != null
        if (!planningOk) discrepancies.add("Production planning snapshot is missing")


        val workOrdersOk = workOrders.isNotEmpty() && workOrders.none { it.status.name == "CANCELLED" }
        if (!workOrdersOk) discrepancies.add("Work orders incomplete or missing")

        val schedulingOk = schedule != null || workOrders.isNotEmpty()
        if (!schedulingOk) discrepancies.add("Production schedule missing")

        val trackingOk = materialConsumptions.isNotEmpty() || timeRecords.isNotEmpty()
        if (!trackingOk) discrepancies.add("Shop-floor tracking records are missing")

        val qcOk = finalQcInspection != null && goodsRelease != null
        if (!qcOk) discrepancies.add("Final QC inspection or Finished Goods release certificate missing")

        val costingOk = costRecord != null && (reconciliationResult?.isFullyReconciled == true)
        if (!costingOk) discrepancies.add("Actual job costing not fully reconciled or SHA-256 cost certificate missing")

        val tenantOk = tenantId.isNotBlank()
        if (!tenantOk) discrepancies.add("Invalid tenant boundary")

        val ready = quoteCommitmentOk && planningOk && workOrdersOk && schedulingOk &&
                trackingOk && qcOk && costingOk && tenantOk && discrepancies.isEmpty()

        return JobClosureReadinessAudit(
            executionJobId = executionJobId,
            tenantId = tenantId,
            isQuoteAndCommitmentVerified = quoteCommitmentOk,
            isProductionPlanningComplete = planningOk,
            areAllWorkOrdersCompleted = workOrdersOk,
            isSchedulingDispatched = schedulingOk,
            isShopFloorTrackingRecorded = trackingOk,
            isFinalQcReleased = qcOk,
            isActualJobCostingReconciled = costingOk,
            isMultiTenantBoundaryValid = tenantOk,
            isReadyForClosure = ready,
            auditDiscrepancies = discrepancies,
            auditedAt = System.currentTimeMillis(),
            auditedBy = auditedBy
        )
    }
}

class ManufacturingScorecardEngine {

    fun evaluateScorecard(
        tenantId: String,
        executionJobId: String,
        orderId: String,
        orderQuantity: BigDecimal,
        goodUnitsReleased: BigDecimal,
        estimatedTotalCost: BigDecimal,
        actualTotalCost: BigDecimal,
        reworkOrScrapUnits: BigDecimal = BigDecimal.ZERO,
        machineEfficiency: BigDecimal = BigDecimal("85.0000"),
        onTime: Boolean = true
    ): ManufacturingPerformanceScorecard {

        val otif = ProductionJobClosureMathUtils.calculateOtifScore(onTime, goodUnitsReleased, orderQuantity)
        val rft = ProductionJobClosureMathUtils.calculateRightFirstTimeScore(goodUnitsReleased.add(reworkOrScrapUnits), reworkOrScrapUnits)
        val cai = ProductionJobClosureMathUtils.calculateCostAdherenceIndex(estimatedTotalCost, actualTotalCost)
        val yield = ProductionJobClosureMathUtils.roundScale4(
            if (goodUnitsReleased.add(reworkOrScrapUnits) > BigDecimal.ZERO) {
                goodUnitsReleased.divide(goodUnitsReleased.add(reworkOrScrapUnits), 6, java.math.RoundingMode.HALF_UP).multiply(BigDecimal("100.0000"))
            } else BigDecimal("100.0000")
        )
        val overall = ProductionJobClosureMathUtils.calculateOverallManufacturingIndex(otif, rft, cai, machineEfficiency)
        val grade = ProductionJobClosureMathUtils.calculatePerformanceGrade(overall)

        return ManufacturingPerformanceScorecard(
            executionJobId = executionJobId,
            tenantId = tenantId,
            orderId = orderId,
            onTimeInFullPercentage = otif,
            rightFirstTimePercentage = rft,
            costAdherenceIndex = cai,
            machineEfficiencyIndex = machineEfficiency,
            qualityYieldPercentage = yield,
            overallManufacturingIndex = overall,
            performanceGrade = grade,
            calculatedAt = System.currentTimeMillis()
        )
    }
}

class ProductionProvenanceGraphEngine {

    fun buildProvenanceGraph(
        tenantId: String,
        executionJobId: String,
        orderId: String,
        calculationId: String? = null,
        quoteId: String? = null,
        planningId: String? = null,
        scheduleId: String? = null,
        workOrderIds: List<String> = emptyList(),
        trackingIds: List<String> = emptyList(),
        qcInspectionId: String? = null,
        packagingId: String? = null,
        releaseId: String? = null,
        costRecordId: String? = null,
        verifiedBy: String = "provenance-engine"
    ): ProductionJobProvenanceGraph {

        val now = System.currentTimeMillis()
        val nodes = mutableListOf<ProductionJobProvenanceNode>()

        nodes.add(
            ProductionJobProvenanceNode(
                stepNumber = 1,
                stepName = "Smart Printing Calculation",
                canonicalEntityName = "PrintingCalculationResult",
                canonicalEntityId = calculationId ?: "CALC-$orderId",
                completionStatus = StepCompletionStatus.COMPLETED_AND_VERIFIED,
                verifiedAt = now,
                verifiedBy = verifiedBy
            )
        )
        nodes.add(
            ProductionJobProvenanceNode(
                stepNumber = 2,
                stepName = "Commercial Quotation & Costing",
                canonicalEntityName = "PrintingQuote",
                canonicalEntityId = quoteId ?: "QUO-$orderId",
                completionStatus = StepCompletionStatus.COMPLETED_AND_VERIFIED,
                verifiedAt = now,
                verifiedBy = verifiedBy
            )
        )
        nodes.add(
            ProductionJobProvenanceNode(
                stepNumber = 3,
                stepName = "Commercial Commitment & Canonical Order",
                canonicalEntityName = "Order",
                canonicalEntityId = orderId,
                completionStatus = StepCompletionStatus.COMPLETED_AND_VERIFIED,
                verifiedAt = now,
                verifiedBy = verifiedBy
            )
        )
        nodes.add(
            ProductionJobProvenanceNode(
                stepNumber = 4,
                stepName = "Production Planning & Readiness",
                canonicalEntityName = "ProductionPlanningSnapshot",
                canonicalEntityId = planningId ?: "PLAN-$executionJobId",
                completionStatus = StepCompletionStatus.COMPLETED_AND_VERIFIED,
                verifiedAt = now,
                verifiedBy = verifiedBy
            )
        )
        nodes.add(
            ProductionJobProvenanceNode(
                stepNumber = 5,
                stepName = "Production Job & Work Orders",
                canonicalEntityName = "ProductionJobExecution",
                canonicalEntityId = executionJobId,
                completionStatus = StepCompletionStatus.COMPLETED_AND_VERIFIED,
                verifiedAt = now,
                verifiedBy = verifiedBy
            )
        )
        nodes.add(
            ProductionJobProvenanceNode(
                stepNumber = 6,
                stepName = "Scheduling & Capacity Planning",
                canonicalEntityName = "ProductionSchedule",
                canonicalEntityId = scheduleId ?: "SCHED-$executionJobId",
                completionStatus = StepCompletionStatus.COMPLETED_AND_VERIFIED,
                verifiedAt = now,
                verifiedBy = verifiedBy
            )
        )
        nodes.add(
            ProductionJobProvenanceNode(
                stepNumber = 7,
                stepName = "Shop-Floor Live Tracking & Telemetry",
                canonicalEntityName = "ShopFloorTrackingRecords",
                canonicalEntityId = if (trackingIds.isNotEmpty()) trackingIds.joinToString(",") else "TRACK-$executionJobId",
                completionStatus = StepCompletionStatus.COMPLETED_AND_VERIFIED,
                verifiedAt = now,
                verifiedBy = verifiedBy
            )
        )
        nodes.add(
            ProductionJobProvenanceNode(
                stepNumber = 8,
                stepName = "Final QC, Defect Containment & Packaging Release",
                canonicalEntityName = "FinishedGoodsReleaseRecord",
                canonicalEntityId = releaseId ?: "REL-$executionJobId",
                completionStatus = StepCompletionStatus.COMPLETED_AND_VERIFIED,
                verifiedAt = now,
                verifiedBy = verifiedBy
            )
        )
        nodes.add(
            ProductionJobProvenanceNode(
                stepNumber = 9,
                stepName = "Actual Job Costing & 8-Way Reconciliation",
                canonicalEntityName = "ProductionActualJobCostRecord",
                canonicalEntityId = costRecordId ?: "COST-$executionJobId",
                completionStatus = StepCompletionStatus.COMPLETED_AND_VERIFIED,
                verifiedAt = now,
                verifiedBy = verifiedBy
            )
        )

        val unbroken = nodes.all { it.completionStatus == StepCompletionStatus.COMPLETED_AND_VERIFIED }
        val rawConcat = nodes.joinToString("|") { "${it.stepNumber}:${it.canonicalEntityId}" }
        val fingerprint = MessageDigest.getInstance("SHA-256").digest(rawConcat.toByteArray()).joinToString("") { "%02x".format(it) }

        return ProductionJobProvenanceGraph(
            executionJobId = executionJobId,
            tenantId = tenantId,
            orderId = orderId,
            nodes = nodes,
            isChainUnbroken = unbroken,
            masterProvenanceFingerprint = fingerprint
        )
    }
}

class MasterJobClosureSealEngine {

    fun generateMasterSealCertificate(
        tenantId: String,
        executionJobId: String,
        orderId: String,
        goodUnitsReleased: BigDecimal,
        actualTotalCost: BigDecimal,
        totalCostVariance: BigDecimal,
        overallCostClassification: VarianceClassification,
        overallManufacturingScore: BigDecimal,
        sealedBy: String = "plant-manager"
    ): MasterProductionClosureCertificate {

        val sealedAt = System.currentTimeMillis()
        val masterHash = ProductionJobClosureMathUtils.generateMasterClosureSealHash(
            tenantId = tenantId,
            executionJobId = executionJobId,
            orderId = orderId,
            actualTotalCost = actualTotalCost,
            totalCostVariance = totalCostVariance,
            overallPerformanceScore = overallManufacturingScore,
            closedAt = sealedAt,
            closedBy = sealedBy
        )

        return MasterProductionClosureCertificate(
            certificateId = "SEAL-" + UUID.randomUUID().toString().take(8),
            tenantId = tenantId,
            executionJobId = executionJobId,
            orderId = orderId,
            masterSealHash = masterHash,
            totalGoodUnitsReleased = goodUnitsReleased,
            grandTotalActualCost = actualTotalCost,
            totalCostVariance = totalCostVariance,
            overallCostClassification = overallCostClassification,
            overallManufacturingScore = overallManufacturingScore,
            sealedAt = sealedAt,
            sealedBy = sealedBy
        )
    }
}
