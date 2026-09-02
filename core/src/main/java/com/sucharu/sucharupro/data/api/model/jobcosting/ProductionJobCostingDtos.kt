package com.sucharu.sucharupro.data.api.model.jobcosting

import com.sucharu.sucharupro.domain.model.jobcosting.*
import com.sucharu.sucharupro.domain.model.production.ProductionStageType
import java.math.BigDecimal

data class ActualMaterialCostItemDto(
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
    val varianceClassification: String,
    val batchLotNumber: String? = null
)

data class ActualLaborCostItemDto(
    val stageType: String,
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
    val varianceClassification: String
)

data class ActualMachineCostItemDto(
    val machineId: String,
    val machineName: String,
    val stageType: String,
    val plannedMachineHours: BigDecimal,
    val actualMachineHours: BigDecimal,
    val recordedDowntimeHours: BigDecimal,
    val machineHourlyRate: BigDecimal,
    val plannedMachineCost: BigDecimal,
    val actualMachineCost: BigDecimal,
    val downtimeCostImpact: BigDecimal,
    val utilizationVariance: BigDecimal,
    val varianceClassification: String
)

data class ScrapReworkValuationItemDto(
    val defectRecordId: String,
    val stageType: String,
    val defectType: String,
    val scrappedQuantity: BigDecimal,
    val unitMaterialCost: BigDecimal,
    val scrapMaterialLoss: BigDecimal,
    val reworkWorkOrderId: String? = null,
    val reworkLaborCost: BigDecimal = BigDecimal.ZERO,
    val scrapSalvageRecoveryValue: BigDecimal = BigDecimal.ZERO,
    val netQualityCost: BigDecimal
)

data class ActualPackagingCostItemDto(
    val packagingRecordId: String,
    val packagingType: String,
    val cartonCount: Int,
    val unitsPerCarton: BigDecimal,
    val totalPackagedUnits: BigDecimal,
    val standardUnitPackagingCost: BigDecimal,
    val actualTotalPackagingCost: BigDecimal
)

data class CalculateActualJobCostRequestDto(
    val orderId: String,
    val manufacturedGoodQuantity: BigDecimal,
    val packagingUnitRate: BigDecimal = BigDecimal("25.0000"),
    val overheadAllocationRate: BigDecimal = BigDecimal("0.1000")
)

data class CalculateJobCostVarianceRequestDto(
    val quotedSellingPrice: BigDecimal,
    val estimatedTotalCost: BigDecimal,
    val estimatedMaterialCost: BigDecimal,
    val estimatedLaborCost: BigDecimal,
    val estimatedMachineCost: BigDecimal,
    val orderQuantity: BigDecimal
)

data class ProductionActualJobCostResponseDto(
    val costRecordId: String,
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
    val materialBreakdown: List<ActualMaterialCostItemDto>,
    val laborBreakdown: List<ActualLaborCostItemDto>,
    val machineBreakdown: List<ActualMachineCostItemDto>,
    val scrapReworkBreakdown: List<ScrapReworkValuationItemDto>,
    val packagingBreakdown: List<ActualPackagingCostItemDto>,
    val costStatus: String,
    val calculatedAt: Long,
    val calculatedBy: String
)

data class ProductionJobCostVarianceResponseDto(
    val executionJobId: String,
    val orderId: String,
    val orderQuantity: BigDecimal,
    val actualGoodOutputQuantity: BigDecimal,
    val quotedSellingPrice: BigDecimal,
    val estimatedTotalCost: BigDecimal,
    val actualTotalCost: BigDecimal,
    val totalCostVariance: BigDecimal,
    val totalCostVariancePercentage: BigDecimal,
    val overallCostClassification: String,
    val estimatedMaterialCost: BigDecimal,
    val actualMaterialCost: BigDecimal,
    val materialVariance: BigDecimal,
    val materialVariancePercentage: BigDecimal,
    val materialCostClassification: String,
    val estimatedLaborCost: BigDecimal,
    val actualLaborCost: BigDecimal,
    val laborVariance: BigDecimal,
    val laborVariancePercentage: BigDecimal,
    val laborCostClassification: String,
    val estimatedMachineCost: BigDecimal,
    val actualMachineCost: BigDecimal,
    val machineVariance: BigDecimal,
    val machineVariancePercentage: BigDecimal,
    val machineCostClassification: String,
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
    val generatedAt: Long
)

data class ProductionJobCostingReconciliationResponseDto(
    val executionJobId: String,
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
    val discrepancies: List<String>,
    val reconciledAt: Long
)

data class Module17Step09JobCostingVarianceHandoffContractDto(
    val contractVersion: String,
    val executionJobId: String,
    val orderId: String,
    val costStatus: String,
    val orderQuantity: BigDecimal,
    val manufacturedGoodQuantity: BigDecimal,
    val quotedSellingPrice: BigDecimal,
    val estimatedTotalCost: BigDecimal,
    val actualTotalCost: BigDecimal,
    val totalCostVariance: BigDecimal,
    val overallCostClassification: String,
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
    val exportedAt: Long
)

fun ProductionActualJobCostRecord.toDto() = ProductionActualJobCostResponseDto(
    costRecordId = costRecordId,
    executionJobId = executionJobId,
    orderId = orderId,
    manufacturedGoodQuantity = manufacturedGoodQuantity,
    totalMaterialCost = totalMaterialCost,
    totalLaborCost = totalLaborCost,
    totalMachineCost = totalMachineCost,
    totalQualityScrapCost = totalQualityScrapCost,
    totalReworkCost = totalReworkCost,
    totalPackagingCost = totalPackagingCost,
    totalOverheadAllocatedCost = totalOverheadAllocatedCost,
    grandTotalActualCost = grandTotalActualCost,
    actualUnitCost = actualUnitCost,
    materialBreakdown = materialBreakdown.map {
        ActualMaterialCostItemDto(
            materialCode = it.materialCode,
            materialName = it.materialName,
            unitOfMeasure = it.unitOfMeasure,
            plannedQuantity = it.plannedQuantity,
            actualQuantity = it.actualQuantity,
            quantityVariance = it.quantityVariance,
            standardUnitPrice = it.standardUnitPrice,
            actualUnitPrice = it.actualUnitPrice,
            priceVariance = it.priceVariance,
            plannedCost = it.plannedCost,
            actualCost = it.actualCost,
            totalVariance = it.totalVariance,
            varianceClassification = it.varianceClassification.name,
            batchLotNumber = it.batchLotNumber
        )
    },
    laborBreakdown = laborBreakdown.map {
        ActualLaborCostItemDto(
            stageType = it.stageType.name,
            stageName = it.stageName,
            plannedSetupHours = it.plannedSetupHours,
            actualSetupHours = it.actualSetupHours,
            plannedRunHours = it.plannedRunHours,
            actualRunHours = it.actualRunHours,
            standardHourlyRate = it.standardHourlyRate,
            actualHourlyRate = it.actualHourlyRate,
            plannedLaborCost = it.plannedLaborCost,
            actualLaborCost = it.actualLaborCost,
            efficiencyVariance = it.efficiencyVariance,
            rateVariance = it.rateVariance,
            totalVariance = it.totalVariance,
            varianceClassification = it.varianceClassification.name
        )
    },
    machineBreakdown = machineBreakdown.map {
        ActualMachineCostItemDto(
            machineId = it.machineId,
            machineName = it.machineName,
            stageType = it.stageType.name,
            plannedMachineHours = it.plannedMachineHours,
            actualMachineHours = it.actualMachineHours,
            recordedDowntimeHours = it.recordedDowntimeHours,
            machineHourlyRate = it.machineHourlyRate,
            plannedMachineCost = it.plannedMachineCost,
            actualMachineCost = it.actualMachineCost,
            downtimeCostImpact = it.downtimeCostImpact,
            utilizationVariance = it.utilizationVariance,
            varianceClassification = it.varianceClassification.name
        )
    },
    scrapReworkBreakdown = scrapReworkBreakdown.map {
        ScrapReworkValuationItemDto(
            defectRecordId = it.defectRecordId,
            stageType = it.stageType.name,
            defectType = it.defectType,
            scrappedQuantity = it.scrappedQuantity,
            unitMaterialCost = it.unitMaterialCost,
            scrapMaterialLoss = it.scrapMaterialLoss,
            reworkWorkOrderId = it.reworkWorkOrderId,
            reworkLaborCost = it.reworkLaborCost,
            scrapSalvageRecoveryValue = it.scrapSalvageRecoveryValue,
            netQualityCost = it.netQualityCost
        )
    },
    packagingBreakdown = packagingBreakdown.map {
        ActualPackagingCostItemDto(
            packagingRecordId = it.packagingRecordId,
            packagingType = it.packagingType,
            cartonCount = it.cartonCount,
            unitsPerCarton = it.unitsPerCarton,
            totalPackagedUnits = it.totalPackagedUnits,
            standardUnitPackagingCost = it.standardUnitPackagingCost,
            actualTotalPackagingCost = it.actualTotalPackagingCost
        )
    },
    costStatus = costStatus.name,
    calculatedAt = calculatedAt,
    calculatedBy = calculatedBy
)

fun ProductionJobCostVarianceSummary.toDto() = ProductionJobCostVarianceResponseDto(
    executionJobId = executionJobId,
    orderId = orderId,
    orderQuantity = orderQuantity,
    actualGoodOutputQuantity = actualGoodOutputQuantity,
    quotedSellingPrice = quotedSellingPrice,
    estimatedTotalCost = estimatedTotalCost,
    actualTotalCost = actualTotalCost,
    totalCostVariance = totalCostVariance,
    totalCostVariancePercentage = totalCostVariancePercentage,
    overallCostClassification = overallCostClassification.name,
    estimatedMaterialCost = estimatedMaterialCost,
    actualMaterialCost = actualMaterialCost,
    materialVariance = materialVariance,
    materialVariancePercentage = materialVariancePercentage,
    materialCostClassification = materialCostClassification.name,
    estimatedLaborCost = estimatedLaborCost,
    actualLaborCost = actualLaborCost,
    laborVariance = laborVariance,
    laborVariancePercentage = laborVariancePercentage,
    laborCostClassification = laborCostClassification.name,
    estimatedMachineCost = estimatedMachineCost,
    actualMachineCost = actualMachineCost,
    machineVariance = machineVariance,
    machineVariancePercentage = machineVariancePercentage,
    machineCostClassification = machineCostClassification.name,
    totalQualityScrapReworkCost = totalQualityScrapReworkCost,
    estimatedUnitCost = estimatedUnitCost,
    actualUnitCost = actualUnitCost,
    unitCostVariance = unitCostVariance,
    estimatedGrossProfit = estimatedGrossProfit,
    actualGrossProfit = actualGrossProfit,
    grossProfitVariance = grossProfitVariance,
    estimatedGrossMarginPercentage = estimatedGrossMarginPercentage,
    actualGrossMarginPercentage = actualGrossMarginPercentage,
    grossMarginPercentageDelta = grossMarginPercentageDelta,
    generatedAt = generatedAt
)

fun ProductionJobCostingReconciliationResult.toDto() = ProductionJobCostingReconciliationResponseDto(
    executionJobId = executionJobId,
    bomQuantitiesReconciled = bomQuantitiesReconciled,
    laborHoursReconciled = laborHoursReconciled,
    machineHoursReconciled = machineHoursReconciled,
    scrapReworkValuationConsistent = scrapReworkValuationConsistent,
    packagingCostBalanced = packagingCostBalanced,
    actualCostMathBalanced = actualCostMathBalanced,
    varianceIntegrityHashValid = varianceIntegrityHashValid,
    multiTenantIsolationVerified = multiTenantIsolationVerified,
    isFullyReconciled = isFullyReconciled,
    certificateHash = certificateHash,
    discrepancies = discrepancies,
    reconciledAt = reconciledAt
)

fun Module17Step09JobCostingVarianceHandoffContract.toDto() = Module17Step09JobCostingVarianceHandoffContractDto(
    contractVersion = contractVersion,
    executionJobId = executionJobId,
    orderId = orderId,
    costStatus = costStatus.name,
    orderQuantity = orderQuantity,
    manufacturedGoodQuantity = manufacturedGoodQuantity,
    quotedSellingPrice = quotedSellingPrice,
    estimatedTotalCost = estimatedTotalCost,
    actualTotalCost = actualTotalCost,
    totalCostVariance = totalCostVariance,
    overallCostClassification = overallCostClassification.name,
    estimatedUnitCost = estimatedUnitCost,
    actualUnitCost = actualUnitCost,
    estimatedGrossProfit = estimatedGrossProfit,
    actualGrossProfit = actualGrossProfit,
    grossMarginPercentageDelta = grossMarginPercentageDelta,
    isFullyReconciled = isFullyReconciled,
    costCertificateHash = costCertificateHash,
    materialCostSummary = materialCostSummary,
    laborCostSummary = laborCostSummary,
    machineCostSummary = machineCostSummary,
    scrapReworkSummary = scrapReworkSummary,
    exportedAt = exportedAt
)
