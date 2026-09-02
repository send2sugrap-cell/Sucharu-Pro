package com.sucharu.sucharupro.domain.service.jobcosting

import com.sucharu.sucharupro.domain.model.finalqc.*
import com.sucharu.sucharupro.domain.model.jobcosting.*
import com.sucharu.sucharupro.domain.model.production.ProductionStageType
import com.sucharu.sucharupro.domain.model.shopfloortracking.*
import com.sucharu.sucharupro.domain.repository.jobcosting.ProductionJobCostingRepository
import java.math.BigDecimal
import java.util.UUID

class ProductionJobCostingServiceImpl(
    private val repository: ProductionJobCostingRepository,
    private val actualCostEngine: ActualJobCostingEngine = ActualJobCostingEngine(),
    private val varianceEngine: ManufacturingVarianceEngine = ManufacturingVarianceEngine(),
    private val reconciliationEngine: ManufacturingCostReconciliationEngine = ManufacturingCostReconciliationEngine()
) : ProductionJobCostingService {

    override suspend fun calculateActualJobCost(
        tenantId: String,
        executionJobId: String,
        orderId: String,
        manufacturedGoodQuantity: BigDecimal,
        materialConsumptions: List<ProductionMaterialConsumptionRecord>,
        operatorTimeRecords: List<OperatorTimeTrackingRecord>,
        machineTelemetryLogs: List<MachineTelemetryLog>,
        defectRecords: List<ProductionDefectContainmentRecord>,
        packagingRecords: List<ProductionPackagingRecord>,
        standardMaterialRates: Map<String, BigDecimal>,
        standardLaborHourlyRates: Map<ProductionStageType, BigDecimal>,
        standardMachineHourlyRates: Map<String, BigDecimal>,
        packagingUnitRate: BigDecimal,
        overheadAllocationRate: BigDecimal,
        actor: String
    ): ProductionActualJobCostRecord {

        val record = actualCostEngine.calculateActualJobCost(
            tenantId = tenantId,
            executionJobId = executionJobId,
            orderId = orderId,
            manufacturedGoodQuantity = manufacturedGoodQuantity,
            materialConsumptions = materialConsumptions,
            operatorTimeRecords = operatorTimeRecords,
            machineTelemetryLogs = machineTelemetryLogs,
            defectRecords = defectRecords,
            packagingRecords = packagingRecords,
            standardMaterialRates = standardMaterialRates,
            standardLaborHourlyRates = standardLaborHourlyRates,
            standardMachineHourlyRates = standardMachineHourlyRates,
            packagingUnitRate = packagingUnitRate,
            overheadAllocationRate = overheadAllocationRate,
            calculatedBy = actor
        )

        repository.saveActualJobCost(tenantId, record)
        repository.saveEvent(
            tenantId,
            ProductionJobCostingEvent(
                eventId = "EVT-" + UUID.randomUUID().toString().take(8),
                tenantId = tenantId,
                executionJobId = executionJobId,
                eventType = "ACTUAL_JOB_COST_CALCULATED",
                actor = actor,
                payload = "Calculated actual job cost: ${record.grandTotalActualCost} (Unit Cost: ${record.actualUnitCost})"
            )
        )
        return record
    }

    override suspend fun getActualJobCostByJob(tenantId: String, executionJobId: String): ProductionActualJobCostRecord? {
        return repository.getActualJobCostByJob(tenantId, executionJobId)
    }

    override suspend fun calculateJobCostVariance(
        tenantId: String,
        executionJobId: String,
        quotedSellingPrice: BigDecimal,
        estimatedTotalCost: BigDecimal,
        estimatedMaterialCost: BigDecimal,
        estimatedLaborCost: BigDecimal,
        estimatedMachineCost: BigDecimal,
        orderQuantity: BigDecimal,
        actor: String
    ): ProductionJobCostVarianceSummary {

        val actualRecord = repository.getActualJobCostByJob(tenantId, executionJobId)
            ?: throw IllegalStateException("Actual cost record not found for job $executionJobId. Must calculate actual cost first.")

        val variance = varianceEngine.generateVarianceSummary(
            actualCostRecord = actualRecord,
            quotedSellingPrice = quotedSellingPrice,
            estimatedTotalCost = estimatedTotalCost,
            estimatedMaterialCost = estimatedMaterialCost,
            estimatedLaborCost = estimatedLaborCost,
            estimatedMachineCost = estimatedMachineCost,
            orderQuantity = orderQuantity
        )

        repository.saveVarianceSummary(tenantId, variance)
        repository.saveEvent(
            tenantId,
            ProductionJobCostingEvent(
                eventId = "EVT-" + UUID.randomUUID().toString().take(8),
                tenantId = tenantId,
                executionJobId = executionJobId,
                eventType = "JOB_COST_VARIANCE_ANALYZED",
                actor = actor,
                payload = "Variance analyzed: Total Variance = ${variance.totalCostVariance} (${variance.overallCostClassification})"
            )
        )
        return variance
    }

    override suspend fun getVarianceSummaryByJob(tenantId: String, executionJobId: String): ProductionJobCostVarianceSummary? {
        return repository.getVarianceSummaryByJob(tenantId, executionJobId)
    }

    override suspend fun reconcileJobCosting(
        tenantId: String,
        executionJobId: String,
        actor: String
    ): ProductionJobCostingReconciliationResult {

        val actualRecord = repository.getActualJobCostByJob(tenantId, executionJobId)
            ?: throw IllegalStateException("Actual cost record not found for job $executionJobId")
        val variance = repository.getVarianceSummaryByJob(tenantId, executionJobId)
            ?: throw IllegalStateException("Variance summary not found for job $executionJobId")

        val result = reconciliationEngine.reconcile(
            costRecord = actualRecord,
            varianceSummary = variance,
            reconciledBy = actor
        )

        repository.saveReconciliationResult(tenantId, result)
        repository.saveEvent(
            tenantId,
            ProductionJobCostingEvent(
                eventId = "EVT-" + UUID.randomUUID().toString().take(8),
                tenantId = tenantId,
                executionJobId = executionJobId,
                eventType = "JOB_COSTING_RECONCILED",
                actor = actor,
                payload = "Reconciliation completed: FullyReconciled=${result.isFullyReconciled}, CertificateHash=${result.certificateHash}"
            )
        )
        return result
    }

    override suspend fun getAiHandoffContract(
        tenantId: String,
        executionJobId: String
    ): Module17Step09JobCostingVarianceHandoffContract {

        val actualRecord = repository.getActualJobCostByJob(tenantId, executionJobId)
        val variance = repository.getVarianceSummaryByJob(tenantId, executionJobId)
        val recon = repository.getReconciliationResultByJob(tenantId, executionJobId)

        val costStatus = actualRecord?.costStatus ?: JobCostStatus.ESTIMATED_ONLY
        val orderQty = variance?.orderQuantity ?: BigDecimal.ZERO
        val goodQty = actualRecord?.manufacturedGoodQuantity ?: BigDecimal.ZERO
        val sellPrice = variance?.quotedSellingPrice ?: BigDecimal.ZERO
        val estCost = variance?.estimatedTotalCost ?: BigDecimal.ZERO
        val actCost = actualRecord?.grandTotalActualCost ?: BigDecimal.ZERO
        val totVar = variance?.totalCostVariance ?: BigDecimal.ZERO
        val overallClass = variance?.overallCostClassification ?: VarianceClassification.NEUTRAL
        val estUnit = variance?.estimatedUnitCost ?: BigDecimal.ZERO
        val actUnit = actualRecord?.actualUnitCost ?: BigDecimal.ZERO
        val estProfit = variance?.estimatedGrossProfit ?: BigDecimal.ZERO
        val actProfit = variance?.actualGrossProfit ?: BigDecimal.ZERO
        val marginDelta = variance?.grossMarginPercentageDelta ?: BigDecimal.ZERO
        val fullyRecon = recon?.isFullyReconciled ?: false
        val certHash = recon?.certificateHash ?: "UNRECONCILED"

        return Module17Step09JobCostingVarianceHandoffContract(
            contractVersion = "1.0.0",
            tenantId = tenantId,
            executionJobId = executionJobId,
            orderId = actualRecord?.orderId ?: "",
            costStatus = costStatus,
            orderQuantity = orderQty,
            manufacturedGoodQuantity = goodQty,
            quotedSellingPrice = sellPrice,
            estimatedTotalCost = estCost,
            actualTotalCost = actCost,
            totalCostVariance = totVar,
            overallCostClassification = overallClass,
            estimatedUnitCost = estUnit,
            actualUnitCost = actUnit,
            estimatedGrossProfit = estProfit,
            actualGrossProfit = actProfit,
            grossMarginPercentageDelta = marginDelta,
            isFullyReconciled = fullyRecon,
            costCertificateHash = certHash,
            materialCostSummary = "Actual: ${actualRecord?.totalMaterialCost ?: BigDecimal.ZERO}, Variance: ${variance?.materialVariance ?: BigDecimal.ZERO} (${variance?.materialCostClassification ?: "N/A"})",
            laborCostSummary = "Actual: ${actualRecord?.totalLaborCost ?: BigDecimal.ZERO}, Variance: ${variance?.laborVariance ?: BigDecimal.ZERO} (${variance?.laborCostClassification ?: "N/A"})",
            machineCostSummary = "Actual: ${actualRecord?.totalMachineCost ?: BigDecimal.ZERO}, Variance: ${variance?.machineVariance ?: BigDecimal.ZERO} (${variance?.machineCostClassification ?: "N/A"})",
            scrapReworkSummary = "Scrap: ${actualRecord?.totalQualityScrapCost ?: BigDecimal.ZERO}, Rework: ${actualRecord?.totalReworkCost ?: BigDecimal.ZERO}"
        )
    }
}
