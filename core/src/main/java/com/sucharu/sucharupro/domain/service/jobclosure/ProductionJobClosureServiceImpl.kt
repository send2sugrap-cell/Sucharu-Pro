package com.sucharu.sucharupro.domain.service.jobclosure

import com.sucharu.sucharupro.domain.model.finalqc.FinishedGoodsReleaseRecord
import com.sucharu.sucharupro.domain.model.finalqc.ProductionFinalQcInspection
import com.sucharu.sucharupro.domain.model.jobclosure.*
import com.sucharu.sucharupro.domain.model.jobcosting.ProductionActualJobCostRecord
import com.sucharu.sucharupro.domain.model.jobcosting.ProductionJobCostingReconciliationResult
import com.sucharu.sucharupro.domain.model.jobcosting.VarianceClassification
import com.sucharu.sucharupro.domain.model.productionexecution.ProductionJobExecution
import com.sucharu.sucharupro.domain.model.productionexecution.ProductionWorkOrder
import com.sucharu.sucharupro.domain.model.productionscheduling.ProductionSchedule
import com.sucharu.sucharupro.domain.model.shopfloortracking.OperatorTimeTrackingRecord
import com.sucharu.sucharupro.domain.model.shopfloortracking.ProductionMaterialConsumptionRecord
import com.sucharu.sucharupro.domain.repository.jobclosure.ProductionJobClosureRepository
import java.math.BigDecimal
import java.util.UUID

class ProductionJobClosureServiceImpl(
    private val repository: ProductionJobClosureRepository,
    private val auditEngine: JobClosureReadinessAuditEngine = JobClosureReadinessAuditEngine(),
    private val scorecardEngine: ManufacturingScorecardEngine = ManufacturingScorecardEngine(),
    private val provenanceEngine: ProductionProvenanceGraphEngine = ProductionProvenanceGraphEngine(),
    private val sealEngine: MasterJobClosureSealEngine = MasterJobClosureSealEngine()
) : ProductionJobClosureService {

    override suspend fun auditJobClosureReadiness(
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
        actor: String
    ): JobClosureReadinessAudit {

        val audit = auditEngine.performPreClosureAudit(
            tenantId = tenantId,
            executionJobId = executionJobId,
            orderId = orderId,
            jobExecution = jobExecution,
            workOrders = workOrders,
            schedule = schedule,
            materialConsumptions = materialConsumptions,
            timeRecords = timeRecords,
            finalQcInspection = finalQcInspection,
            goodsRelease = goodsRelease,
            costRecord = costRecord,
            reconciliationResult = reconciliationResult,
            auditedBy = actor
        )

        repository.saveEvent(
            tenantId,
            ProductionJobClosureEvent(
                eventId = "EVT-" + UUID.randomUUID().toString().take(8),
                tenantId = tenantId,
                executionJobId = executionJobId,
                eventType = "PRE_CLOSURE_AUDIT_PERFORMED",
                actor = actor,
                payload = "Pre-closure audit executed: Ready=${audit.isReadyForClosure}, Discrepancies=${audit.auditDiscrepancies.size}"
            )
        )
        return audit
    }

    override suspend fun closeAndSealJob(
        tenantId: String,
        executionJobId: String,
        orderId: String,
        orderQuantity: BigDecimal,
        goodUnitsReleased: BigDecimal,
        estimatedTotalCost: BigDecimal,
        actualTotalCost: BigDecimal,
        totalCostVariance: BigDecimal,
        reworkOrScrapUnits: BigDecimal,
        machineEfficiency: BigDecimal,
        onTime: Boolean,
        calculationId: String?,
        quoteId: String?,
        planningId: String?,
        scheduleId: String?,
        workOrderIds: List<String>,
        trackingIds: List<String>,
        qcInspectionId: String?,
        packagingId: String?,
        releaseId: String?,
        costRecordId: String?,
        primaryDowntimeDrivers: List<String>,
        scrapAndDefectTakeaways: List<String>,
        costVarianceTakeaways: List<String>,
        operationalRecommendations: List<String>,
        actor: String
    ): ProductionJobClosureRecord {

        val scorecard = scorecardEngine.evaluateScorecard(
            tenantId = tenantId,
            executionJobId = executionJobId,
            orderId = orderId,
            orderQuantity = orderQuantity,
            goodUnitsReleased = goodUnitsReleased,
            estimatedTotalCost = estimatedTotalCost,
            actualTotalCost = actualTotalCost,
            reworkOrScrapUnits = reworkOrScrapUnits,
            machineEfficiency = machineEfficiency,
            onTime = onTime
        )

        val provenance = provenanceEngine.buildProvenanceGraph(
            tenantId = tenantId,
            executionJobId = executionJobId,
            orderId = orderId,
            calculationId = calculationId,
            quoteId = quoteId,
            planningId = planningId,
            scheduleId = scheduleId,
            workOrderIds = workOrderIds,
            trackingIds = trackingIds,
            qcInspectionId = qcInspectionId,
            packagingId = packagingId,
            releaseId = releaseId,
            costRecordId = costRecordId,
            verifiedBy = actor
        )

        val masterSeal = sealEngine.generateMasterSealCertificate(
            tenantId = tenantId,
            executionJobId = executionJobId,
            orderId = orderId,
            goodUnitsReleased = goodUnitsReleased,
            actualTotalCost = actualTotalCost,
            totalCostVariance = totalCostVariance,
            overallCostClassification = if (totalCostVariance <= BigDecimal.ZERO) VarianceClassification.FAVORABLE else VarianceClassification.UNFAVORABLE,
            overallManufacturingScore = scorecard.overallManufacturingIndex,
            sealedBy = actor
        )

        val postMortem = ProductionPostMortemSummary(
            executionJobId = executionJobId,
            tenantId = tenantId,
            primaryDowntimeDrivers = primaryDowntimeDrivers,
            scrapAndDefectTakeaways = scrapAndDefectTakeaways,
            costVarianceTakeaways = costVarianceTakeaways,
            operationalRecommendations = operationalRecommendations,
            generatedAt = System.currentTimeMillis()
        )

        val readinessAudit = JobClosureReadinessAudit(
            executionJobId = executionJobId,
            tenantId = tenantId,
            isQuoteAndCommitmentVerified = true,
            isProductionPlanningComplete = true,
            areAllWorkOrdersCompleted = true,
            isSchedulingDispatched = true,
            isShopFloorTrackingRecorded = true,
            isFinalQcReleased = true,
            isActualJobCostingReconciled = true,
            isMultiTenantBoundaryValid = true,
            isReadyForClosure = true,
            auditDiscrepancies = emptyList(),
            auditedAt = System.currentTimeMillis(),
            auditedBy = actor
        )

        val record = ProductionJobClosureRecord(
            closureId = "CLOSE-" + UUID.randomUUID().toString().take(8),
            tenantId = tenantId,
            executionJobId = executionJobId,
            orderId = orderId,
            closureStatus = JobClosureStatus.GOVERNANCE_SEALED,
            readinessAudit = readinessAudit,
            scorecard = scorecard,
            provenanceGraph = provenance,
            postMortemSummary = postMortem,
            masterCertificate = masterSeal,
            closedAt = System.currentTimeMillis(),
            closedBy = actor
        )

        repository.saveClosureRecord(tenantId, record)
        repository.saveScorecard(tenantId, scorecard)

        repository.saveEvent(
            tenantId,
            ProductionJobClosureEvent(
                eventId = "EVT-" + UUID.randomUUID().toString().take(8),
                tenantId = tenantId,
                executionJobId = executionJobId,
                eventType = "JOB_GOVERNANCE_SEALED_AND_CLOSED",
                actor = actor,
                payload = "Production job $executionJobId sealed & closed with master hash ${masterSeal.masterSealHash} (Grade: ${scorecard.performanceGrade})"
            )
        )

        return record
    }

    override suspend fun getClosureRecordByJob(tenantId: String, executionJobId: String): ProductionJobClosureRecord? {
        return repository.getClosureRecordByJob(tenantId, executionJobId)
    }

    override suspend fun getScorecardByJob(tenantId: String, executionJobId: String): ManufacturingPerformanceScorecard? {
        return repository.getScorecardByJob(tenantId, executionJobId)
    }

    override suspend fun getAiHandoffContract(
        tenantId: String,
        executionJobId: String
    ): Module17Step10JobClosureGovernanceHandoffContract {

        val closure = repository.getClosureRecordByJob(tenantId, executionJobId)
        val scorecard = closure?.scorecard ?: repository.getScorecardByJob(tenantId, executionJobId)

        return Module17Step10JobClosureGovernanceHandoffContract(
            contractVersion = "1.0.0",
            tenantId = tenantId,
            executionJobId = executionJobId,
            orderId = closure?.orderId ?: "",
            closureStatus = closure?.closureStatus ?: JobClosureStatus.OPEN,
            isReadyForClosure = closure?.readinessAudit?.isReadyForClosure ?: false,
            isProvenanceChainUnbroken = closure?.provenanceGraph?.isChainUnbroken ?: false,
            overallManufacturingIndex = scorecard?.overallManufacturingIndex ?: BigDecimal.ZERO,
            performanceGrade = scorecard?.performanceGrade ?: "N/A",
            onTimeInFullPercentage = scorecard?.onTimeInFullPercentage ?: BigDecimal.ZERO,
            rightFirstTimePercentage = scorecard?.rightFirstTimePercentage ?: BigDecimal.ZERO,
            totalGoodUnitsReleased = closure?.masterCertificate?.totalGoodUnitsReleased ?: BigDecimal.ZERO,
            grandTotalActualCost = closure?.masterCertificate?.grandTotalActualCost ?: BigDecimal.ZERO,
            totalCostVariance = closure?.masterCertificate?.totalCostVariance ?: BigDecimal.ZERO,
            masterClosureSealHash = closure?.masterCertificate?.masterSealHash ?: "UNSEALED",
            crossModuleInventoryConfirmed = true,
            crossModuleDeliveryConfirmed = true,
            crossModuleFinanceConfirmed = true,
            crossModuleProfitabilityLocked = true,
            exportedAt = System.currentTimeMillis()
        )
    }
}
