package com.sucharu.sucharupro.domain.model.jobcosting

import com.sucharu.sucharupro.domain.model.production.ProductionStageType
import java.math.BigDecimal

enum class CostCategory {
    MATERIAL,
    DIRECT_LABOR,
    MACHINE_OPERATION,
    QUALITY_SCRAP,
    REWORK_CONVERSION,
    PACKAGING,
    OUTSOURCED_FINISHING,
    OVERHEAD_ALLOCATION
}

enum class VarianceClassification {
    FAVORABLE,
    UNFAVORABLE,
    NEUTRAL
}

enum class JobCostStatus {
    ESTIMATED_ONLY,
    IN_PROGRESS_TRACKED,
    ACTUAL_COSTED,
    VARIANCE_AUDITED,
    RECONCILED,
    COST_LOCKED
}

data class ActualMaterialCostItem(
    val materialCode: String,
    val materialName: String,
    val unitOfMeasure: String,
    val plannedQuantity: BigDecimal,
    val actualQuantity: BigDecimal,
    val quantityVariance: BigDecimal,
    val standardUnitPrice: BigDecimal,
    val actualUnitPrice: BigDecimal,
    val priceVariance: BigDecimal,
    val plannedCost: BigDecimal,
    val actualCost: BigDecimal,
    val totalVariance: BigDecimal,
    val varianceClassification: VarianceClassification,
    val batchLotNumber: String? = null
)

data class ActualLaborCostItem(
    val stageType: ProductionStageType,
    val stageName: String,
    val plannedSetupHours: BigDecimal,
    val actualSetupHours: BigDecimal,
    val plannedRunHours: BigDecimal,
    val actualRunHours: BigDecimal,
    val standardHourlyRate: BigDecimal,
    val actualHourlyRate: BigDecimal,
    val plannedLaborCost: BigDecimal,
    val actualLaborCost: BigDecimal,
    val efficiencyVariance: BigDecimal,
    val rateVariance: BigDecimal,
    val totalVariance: BigDecimal,
    val varianceClassification: VarianceClassification
)

data class ActualMachineCostItem(
    val machineId: String,
    val machineName: String,
    val stageType: ProductionStageType,
    val plannedMachineHours: BigDecimal,
    val actualMachineHours: BigDecimal,
    val recordedDowntimeHours: BigDecimal,
    val machineHourlyRate: BigDecimal,
    val plannedMachineCost: BigDecimal,
    val actualMachineCost: BigDecimal,
    val downtimeCostImpact: BigDecimal,
    val utilizationVariance: BigDecimal,
    val varianceClassification: VarianceClassification
)

data class ScrapReworkValuationItem(
    val defectRecordId: String,
    val stageType: ProductionStageType,
    val defectType: String,
    val scrappedQuantity: BigDecimal,
    val unitMaterialCost: BigDecimal,
    val scrapMaterialLoss: BigDecimal,
    val reworkWorkOrderId: String? = null,
    val reworkAdditionalHours: BigDecimal = BigDecimal.ZERO,
    val reworkHourlyRate: BigDecimal = BigDecimal.ZERO,
    val reworkLaborCost: BigDecimal = BigDecimal.ZERO,
    val scrapSalvageRecoveryValue: BigDecimal = BigDecimal.ZERO,
    val netQualityCost: BigDecimal
)

data class ActualPackagingCostItem(
    val packagingRecordId: String,
    val packagingType: String,
    val cartonCount: Int,
    val unitsPerCarton: BigDecimal,
    val totalPackagedUnits: BigDecimal,
    val standardUnitPackagingCost: BigDecimal,
    val actualTotalPackagingCost: BigDecimal
)

data class ProductionActualJobCostRecord(
    val costRecordId: String,
    val tenantId: String,
    val executionJobId: String,
    val orderId: String,
    val manufacturedGoodQuantity: BigDecimal,
    val totalMaterialCost: BigDecimal,
    val totalLaborCost: BigDecimal,
    val totalMachineCost: BigDecimal,
    val totalQualityScrapCost: BigDecimal,
    val totalReworkCost: BigDecimal,
    val totalPackagingCost: BigDecimal,
    val totalOverheadAllocatedCost: BigDecimal,
    val grandTotalActualCost: BigDecimal,
    val actualUnitCost: BigDecimal,
    val materialBreakdown: List<ActualMaterialCostItem> = emptyList(),
    val laborBreakdown: List<ActualLaborCostItem> = emptyList(),
    val machineBreakdown: List<ActualMachineCostItem> = emptyList(),
    val scrapReworkBreakdown: List<ScrapReworkValuationItem> = emptyList(),
    val packagingBreakdown: List<ActualPackagingCostItem> = emptyList(),
    val costStatus: JobCostStatus = JobCostStatus.ACTUAL_COSTED,
    val calculatedAt: Long = System.currentTimeMillis(),
    val calculatedBy: String = "cost-engine"
)

data class ProductionJobCostVarianceSummary(
    val executionJobId: String,
    val tenantId: String,
    val orderId: String,
    val orderQuantity: BigDecimal,
    val actualGoodOutputQuantity: BigDecimal,
    val quotedSellingPrice: BigDecimal,
    val estimatedTotalCost: BigDecimal,
    val actualTotalCost: BigDecimal,
    val totalCostVariance: BigDecimal,
    val totalCostVariancePercentage: BigDecimal,
    val overallCostClassification: VarianceClassification,
    val estimatedMaterialCost: BigDecimal,
    val actualMaterialCost: BigDecimal,
    val materialVariance: BigDecimal,
    val materialVariancePercentage: BigDecimal,
    val materialCostClassification: VarianceClassification,
    val estimatedLaborCost: BigDecimal,
    val actualLaborCost: BigDecimal,
    val laborVariance: BigDecimal,
    val laborVariancePercentage: BigDecimal,
    val laborCostClassification: VarianceClassification,
    val estimatedMachineCost: BigDecimal,
    val actualMachineCost: BigDecimal,
    val machineVariance: BigDecimal,
    val machineVariancePercentage: BigDecimal,
    val machineCostClassification: VarianceClassification,
    val totalQualityScrapReworkCost: BigDecimal,
    val estimatedUnitCost: BigDecimal,
    val actualUnitCost: BigDecimal,
    val unitCostVariance: BigDecimal,
    val estimatedGrossProfit: BigDecimal,
    val actualGrossProfit: BigDecimal,
    val grossProfitVariance: BigDecimal,
    val estimatedGrossMarginPercentage: BigDecimal,
    val actualGrossMarginPercentage: BigDecimal,
    val grossMarginPercentageDelta: BigDecimal,
    val generatedAt: Long = System.currentTimeMillis()
)

data class ProductionJobCostingReconciliationResult(
    val executionJobId: String,
    val tenantId: String,
    val bomQuantitiesReconciled: Boolean,
    val laborHoursReconciled: Boolean,
    val machineHoursReconciled: Boolean,
    val scrapReworkValuationConsistent: Boolean,
    val packagingCostBalanced: Boolean,
    val actualCostMathBalanced: Boolean,
    val varianceIntegrityHashValid: Boolean,
    val multiTenantIsolationVerified: Boolean,
    val isFullyReconciled: Boolean,
    val certificateHash: String,
    val discrepancies: List<String> = emptyList(),
    val reconciledAt: Long = System.currentTimeMillis()
)

data class ProductionJobCostingEvent(
    val eventId: String,
    val tenantId: String,
    val executionJobId: String,
    val eventType: String,
    val actor: String,
    val payload: String,
    val timestamp: Long = System.currentTimeMillis()
)

data class Module17Step09JobCostingVarianceHandoffContract(
    val contractVersion: String = "1.0.0",
    val tenantId: String,
    val executionJobId: String,
    val orderId: String,
    val costStatus: JobCostStatus,
    val orderQuantity: BigDecimal,
    val manufacturedGoodQuantity: BigDecimal,
    val quotedSellingPrice: BigDecimal,
    val estimatedTotalCost: BigDecimal,
    val actualTotalCost: BigDecimal,
    val totalCostVariance: BigDecimal,
    val overallCostClassification: VarianceClassification,
    val estimatedUnitCost: BigDecimal,
    val actualUnitCost: BigDecimal,
    val estimatedGrossProfit: BigDecimal,
    val actualGrossProfit: BigDecimal,
    val grossMarginPercentageDelta: BigDecimal,
    val isFullyReconciled: Boolean,
    val costCertificateHash: String,
    val materialCostSummary: String,
    val laborCostSummary: String,
    val machineCostSummary: String,
    val scrapReworkSummary: String,
    val exportedAt: Long = System.currentTimeMillis()
)
