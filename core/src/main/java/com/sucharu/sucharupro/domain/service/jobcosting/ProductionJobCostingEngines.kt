package com.sucharu.sucharupro.domain.service.jobcosting

import com.sucharu.sucharupro.domain.model.finalqc.*
import com.sucharu.sucharupro.domain.model.jobcosting.*
import com.sucharu.sucharupro.domain.model.production.ProductionStageType
import com.sucharu.sucharupro.domain.model.shopfloortracking.*
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.UUID

class ActualJobCostingEngine {

    fun calculateActualJobCost(
        tenantId: String,
        executionJobId: String,
        orderId: String,
        manufacturedGoodQuantity: BigDecimal,
        materialConsumptions: List<ProductionMaterialConsumptionRecord>,
        operatorTimeRecords: List<OperatorTimeTrackingRecord>,
        machineTelemetryLogs: List<MachineTelemetryLog>,
        defectRecords: List<ProductionDefectContainmentRecord>,
        packagingRecords: List<ProductionPackagingRecord>,
        standardMaterialRates: Map<String, BigDecimal> = emptyMap(),
        standardLaborHourlyRates: Map<ProductionStageType, BigDecimal> = emptyMap(),
        standardMachineHourlyRates: Map<String, BigDecimal> = emptyMap(),
        packagingUnitRate: BigDecimal = BigDecimal("25.0000"),
        overheadAllocationRate: BigDecimal = BigDecimal("0.1000"), // 10% overhead on prime cost
        calculatedBy: String = "cost-engine"
    ): ProductionActualJobCostRecord {

        // 1. Material Costs
        val materialItems = materialConsumptions.map { mat ->
            val stdPrice = standardMaterialRates[mat.materialCode] ?: BigDecimal("2.5000")
            val actualPrice = stdPrice // If price is fixed by standard BOM
            val plannedQty = mat.plannedQuantity
            val actualQty = mat.actualQuantityConsumed
            val qtyVar = ProductionJobCostingMathUtils.calculateVariance(actualQty, plannedQty)
            val plannedCost = ProductionJobCostingMathUtils.roundScale4(plannedQty.multiply(stdPrice))
            val actualCost = ProductionJobCostingMathUtils.roundScale4(actualQty.multiply(actualPrice))
            val totalVar = ProductionJobCostingMathUtils.calculateVariance(actualCost, plannedCost)
            val classification = ProductionJobCostingMathUtils.classifyCostVariance(actualCost, plannedCost)

            ActualMaterialCostItem(
                materialCode = mat.materialCode,
                materialName = mat.materialName,
                unitOfMeasure = mat.unitOfMeasure,
                plannedQuantity = plannedQty,
                actualQuantity = actualQty,
                quantityVariance = qtyVar,
                standardUnitPrice = stdPrice,
                actualUnitPrice = actualPrice,
                priceVariance = BigDecimal.ZERO,
                plannedCost = plannedCost,
                actualCost = actualCost,
                totalVariance = totalVar,
                varianceClassification = classification,
                batchLotNumber = mat.batchLotNumber
            )
        }
        val totalMatCost = materialItems.fold(BigDecimal.ZERO) { acc, item -> acc.add(item.actualCost) }

        // 2. Labor Costs
        val laborItems = operatorTimeRecords.map { op ->
            val hourlyRate = standardLaborHourlyRates[op.stageType] ?: BigDecimal("250.0000") // BDT/hour
            val setupHours = ProductionJobCostingMathUtils.roundScale4(BigDecimal(op.setupMinutes.toLong()).divide(BigDecimal("60.0000"), 4, RoundingMode.HALF_UP))
            val runHours = ProductionJobCostingMathUtils.roundScale4(BigDecimal(op.runMinutes.toLong()).divide(BigDecimal("60.0000"), 4, RoundingMode.HALF_UP))
            val totalActualHours = setupHours.add(runHours)
            val plannedHours = totalActualHours // Standard reference
            val actualLaborCost = ProductionJobCostingMathUtils.roundScale4(totalActualHours.multiply(hourlyRate))
            val plannedLaborCost = actualLaborCost

            ActualLaborCostItem(
                stageType = op.stageType,
                stageName = op.stageType.name,
                plannedSetupHours = setupHours,
                actualSetupHours = setupHours,
                plannedRunHours = runHours,
                actualRunHours = runHours,
                standardHourlyRate = hourlyRate,
                actualHourlyRate = hourlyRate,
                plannedLaborCost = plannedLaborCost,
                actualLaborCost = actualLaborCost,
                efficiencyVariance = BigDecimal.ZERO,
                rateVariance = BigDecimal.ZERO,
                totalVariance = BigDecimal.ZERO,
                varianceClassification = VarianceClassification.NEUTRAL
            )
        }
        val totalLaborCost = laborItems.fold(BigDecimal.ZERO) { acc, item -> acc.add(item.actualLaborCost) }

        // 3. Machine Costs
        val machineItems = machineTelemetryLogs.groupBy { it.machineId }.map { (mId, logs) ->
            val firstLog = logs.first()
            val mHourlyRate = standardMachineHourlyRates[mId] ?: BigDecimal("600.0000")
            val totalDowntimeMins = logs.sumOf { it.downtimeMinutes.toLong() }
            val downtimeHours = ProductionJobCostingMathUtils.roundScale4(BigDecimal(totalDowntimeMins).divide(BigDecimal("60.0000"), 4, RoundingMode.HALF_UP))
            val runningHours = ProductionJobCostingMathUtils.roundScale4(BigDecimal(logs.size.toLong()).multiply(BigDecimal("0.5000"))) // 30 min per telemetry point
            val machineCost = ProductionJobCostingMathUtils.roundScale4(runningHours.multiply(mHourlyRate))
            val dtImpact = ProductionJobCostingMathUtils.roundScale4(downtimeHours.multiply(mHourlyRate.multiply(BigDecimal("0.5000")))) // 50% idle overhead cost

            ActualMachineCostItem(
                machineId = mId,
                machineName = firstLog.machineName,
                stageType = ProductionStageType.PRINTING,
                plannedMachineHours = runningHours,
                actualMachineHours = runningHours,
                recordedDowntimeHours = downtimeHours,
                machineHourlyRate = mHourlyRate,
                plannedMachineCost = machineCost,
                actualMachineCost = machineCost.add(dtImpact),
                downtimeCostImpact = dtImpact,
                utilizationVariance = dtImpact,
                varianceClassification = if (dtImpact.compareTo(BigDecimal.ZERO) > 0) VarianceClassification.UNFAVORABLE else VarianceClassification.NEUTRAL
            )
        }

        val totalMachineCost = machineItems.fold(BigDecimal.ZERO) { acc, item -> acc.add(item.actualMachineCost) }

        // 4. Quality Scrap & Rework Costs
        val scrapItems = defectRecords.map { defect ->
            val unitCost = BigDecimal("15.0000") // Approximate material/conversion cost per unit
            val scrapLoss = ProductionJobCostingMathUtils.roundScale4(defect.defectQuantity.multiply(unitCost))
            val reworkLabor = if (defect.reworkWorkOrderId != null) BigDecimal("500.0000") else BigDecimal.ZERO

            ScrapReworkValuationItem(
                defectRecordId = defect.containmentId,
                stageType = defect.rootCauseStage,
                defectType = defect.defectType.name,
                scrappedQuantity = defect.defectQuantity,
                unitMaterialCost = unitCost,
                scrapMaterialLoss = scrapLoss,
                reworkWorkOrderId = defect.reworkWorkOrderId,
                reworkLaborCost = reworkLabor,
                scrapSalvageRecoveryValue = ProductionJobCostingMathUtils.roundScale4(scrapLoss.multiply(BigDecimal("0.1000"))), // 10% paper recycling salvage
                netQualityCost = ProductionJobCostingMathUtils.roundScale4(scrapLoss.add(reworkLabor).subtract(scrapLoss.multiply(BigDecimal("0.1000"))))
            )
        }
        val totalScrapCost = scrapItems.fold(BigDecimal.ZERO) { acc, item -> acc.add(item.scrapMaterialLoss) }
        val totalReworkCost = scrapItems.fold(BigDecimal.ZERO) { acc, item -> acc.add(item.reworkLaborCost) }

        // 5. Packaging Costs
        val packagingItems = packagingRecords.map { pkg ->
            val pkgCost = ProductionJobCostingMathUtils.roundScale4(BigDecimal(pkg.totalPackageCount).multiply(packagingUnitRate))
            ActualPackagingCostItem(
                packagingRecordId = pkg.packagingId,
                packagingType = pkg.packagingType.name,
                cartonCount = pkg.totalPackageCount,
                unitsPerCarton = pkg.unitsPerPackage,
                totalPackagedUnits = pkg.totalPackagedQuantity,
                standardUnitPackagingCost = packagingUnitRate,
                actualTotalPackagingCost = pkgCost
            )
        }
        val totalPkgCost = packagingItems.fold(BigDecimal.ZERO) { acc, item -> acc.add(item.actualTotalPackagingCost) }

        // 6. Overhead Allocation
        val primeCost = totalMatCost.add(totalLaborCost).add(totalMachineCost)
        val totalOverhead = ProductionJobCostingMathUtils.roundScale4(primeCost.multiply(overheadAllocationRate))

        val grandTotal = ProductionJobCostingMathUtils.roundScale4(
            totalMatCost.add(totalLaborCost).add(totalMachineCost).add(totalScrapCost).add(totalReworkCost).add(totalPkgCost).add(totalOverhead)
        )
        val goodQty = if (manufacturedGoodQuantity.compareTo(BigDecimal.ZERO) > 0) manufacturedGoodQuantity else BigDecimal("1.0000")
        val unitCost = ProductionJobCostingMathUtils.calculateUnitCost(grandTotal, goodQty)

        return ProductionActualJobCostRecord(
            costRecordId = "COST-" + UUID.randomUUID().toString().take(8),
            tenantId = tenantId,
            executionJobId = executionJobId,
            orderId = orderId,
            manufacturedGoodQuantity = manufacturedGoodQuantity,
            totalMaterialCost = totalMatCost,
            totalLaborCost = totalLaborCost,
            totalMachineCost = totalMachineCost,
            totalQualityScrapCost = totalScrapCost,
            totalReworkCost = totalReworkCost,
            totalPackagingCost = totalPkgCost,
            totalOverheadAllocatedCost = totalOverhead,
            grandTotalActualCost = grandTotal,
            actualUnitCost = unitCost,
            materialBreakdown = materialItems,
            laborBreakdown = laborItems,
            machineBreakdown = machineItems,
            scrapReworkBreakdown = scrapItems,
            packagingBreakdown = packagingItems,
            costStatus = JobCostStatus.ACTUAL_COSTED,
            calculatedAt = System.currentTimeMillis(),
            calculatedBy = calculatedBy
        )
    }
}

class ManufacturingVarianceEngine {

    fun generateVarianceSummary(
        actualCostRecord: ProductionActualJobCostRecord,
        quotedSellingPrice: BigDecimal,
        estimatedTotalCost: BigDecimal,
        estimatedMaterialCost: BigDecimal,
        estimatedLaborCost: BigDecimal,
        estimatedMachineCost: BigDecimal,
        orderQuantity: BigDecimal
    ): ProductionJobCostVarianceSummary {

        val actMatCost = actualCostRecord.totalMaterialCost
        val actLabCost = actualCostRecord.totalLaborCost
        val actMacCost = actualCostRecord.totalMachineCost
        val actTotalCost = actualCostRecord.grandTotalActualCost
        val actualGoodQty = actualCostRecord.manufacturedGoodQuantity

        val matVar = ProductionJobCostingMathUtils.calculateVariance(actMatCost, estimatedMaterialCost)
        val matVarPct = ProductionJobCostingMathUtils.calculateVariancePercentage(actMatCost, estimatedMaterialCost)
        val matClass = ProductionJobCostingMathUtils.classifyCostVariance(actMatCost, estimatedMaterialCost)

        val labVar = ProductionJobCostingMathUtils.calculateVariance(actLabCost, estimatedLaborCost)
        val labVarPct = ProductionJobCostingMathUtils.calculateVariancePercentage(actLabCost, estimatedLaborCost)
        val labClass = ProductionJobCostingMathUtils.classifyCostVariance(actLabCost, estimatedLaborCost)

        val macVar = ProductionJobCostingMathUtils.calculateVariance(actMacCost, estimatedMachineCost)
        val macVarPct = ProductionJobCostingMathUtils.calculateVariancePercentage(actMacCost, estimatedMachineCost)
        val macClass = ProductionJobCostingMathUtils.classifyCostVariance(actMacCost, estimatedMachineCost)

        val totalVar = ProductionJobCostingMathUtils.calculateVariance(actTotalCost, estimatedTotalCost)
        val totalVarPct = ProductionJobCostingMathUtils.calculateVariancePercentage(actTotalCost, estimatedTotalCost)
        val totalClass = ProductionJobCostingMathUtils.classifyCostVariance(actTotalCost, estimatedTotalCost)

        val estUnitCost = ProductionJobCostingMathUtils.calculateUnitCost(estimatedTotalCost, orderQuantity)
        val actUnitCost = actualCostRecord.actualUnitCost
        val unitCostVar = ProductionJobCostingMathUtils.calculateVariance(actUnitCost, estUnitCost)

        val estGrossProfit = ProductionJobCostingMathUtils.roundScale4(quotedSellingPrice.subtract(estimatedTotalCost))
        val actGrossProfit = ProductionJobCostingMathUtils.roundScale4(quotedSellingPrice.subtract(actTotalCost))
        val grossProfitVar = ProductionJobCostingMathUtils.calculateVariance(actGrossProfit, estGrossProfit)

        val estMarginPct = ProductionJobCostingMathUtils.calculateGrossMarginPercentage(quotedSellingPrice, estimatedTotalCost)
        val actMarginPct = ProductionJobCostingMathUtils.calculateGrossMarginPercentage(quotedSellingPrice, actTotalCost)
        val marginDelta = ProductionJobCostingMathUtils.calculateVariance(actMarginPct, estMarginPct)

        return ProductionJobCostVarianceSummary(
            executionJobId = actualCostRecord.executionJobId,
            tenantId = actualCostRecord.tenantId,
            orderId = actualCostRecord.orderId,
            orderQuantity = orderQuantity,
            actualGoodOutputQuantity = actualGoodQty,
            quotedSellingPrice = quotedSellingPrice,
            estimatedTotalCost = estimatedTotalCost,
            actualTotalCost = actTotalCost,
            totalCostVariance = totalVar,
            totalCostVariancePercentage = totalVarPct,
            overallCostClassification = totalClass,
            estimatedMaterialCost = estimatedMaterialCost,
            actualMaterialCost = actMatCost,
            materialVariance = matVar,
            materialVariancePercentage = matVarPct,
            materialCostClassification = matClass,
            estimatedLaborCost = estimatedLaborCost,
            actualLaborCost = actLabCost,
            laborVariance = labVar,
            laborVariancePercentage = labVarPct,
            laborCostClassification = labClass,
            estimatedMachineCost = estimatedMachineCost,
            actualMachineCost = actMacCost,
            machineVariance = macVar,
            machineVariancePercentage = macVarPct,
            machineCostClassification = macClass,
            totalQualityScrapReworkCost = actualCostRecord.totalQualityScrapCost.add(actualCostRecord.totalReworkCost),
            estimatedUnitCost = estUnitCost,
            actualUnitCost = actUnitCost,
            unitCostVariance = unitCostVar,
            estimatedGrossProfit = estGrossProfit,
            actualGrossProfit = actGrossProfit,
            grossProfitVariance = grossProfitVar,
            estimatedGrossMarginPercentage = estMarginPct,
            actualGrossMarginPercentage = actMarginPct,
            grossMarginPercentageDelta = marginDelta,
            generatedAt = System.currentTimeMillis()
        )
    }
}

class ScrapReworkValuationEngine {

    fun valueScrapAndRework(
        defects: List<ProductionDefectContainmentRecord>,
        substrateUnitPrice: BigDecimal = BigDecimal("5.0000"),
        hourlyReworkRate: BigDecimal = BigDecimal("300.0000"),
        salvageRecoveryRate: BigDecimal = BigDecimal("0.1000")
    ): List<ScrapReworkValuationItem> {
        return defects.map { defect ->
            val scrapLoss = ProductionJobCostingMathUtils.roundScale4(defect.defectQuantity.multiply(substrateUnitPrice))
            val reworkCost = if (defect.reworkWorkOrderId != null) hourlyReworkRate else BigDecimal.ZERO
            val salvage = ProductionJobCostingMathUtils.roundScale4(scrapLoss.multiply(salvageRecoveryRate))
            val netCost = ProductionJobCostingMathUtils.roundScale4(scrapLoss.add(reworkCost).subtract(salvage))

            ScrapReworkValuationItem(
                defectRecordId = defect.containmentId,
                stageType = defect.rootCauseStage,
                defectType = defect.defectType.name,
                scrappedQuantity = defect.defectQuantity,
                unitMaterialCost = substrateUnitPrice,
                scrapMaterialLoss = scrapLoss,
                reworkWorkOrderId = defect.reworkWorkOrderId,
                reworkLaborCost = reworkCost,
                scrapSalvageRecoveryValue = salvage,
                netQualityCost = netCost
            )
        }
    }
}

class ManufacturingCostReconciliationEngine {

    fun reconcile(
        costRecord: ProductionActualJobCostRecord,
        varianceSummary: ProductionJobCostVarianceSummary,
        reconciledBy: String = "cost-auditor"
    ): ProductionJobCostingReconciliationResult {
        val discrepancies = mutableListOf<String>()

        // 1. BOM Quantities check
        val bomReconciled = costRecord.materialBreakdown.isNotEmpty()
        if (!bomReconciled) discrepancies.add("No material consumption breakdown found")

        // 2. Labor Hours check
        val laborReconciled = costRecord.totalLaborCost.compareTo(BigDecimal.ZERO) >= 0
        if (!laborReconciled) discrepancies.add("Negative labor cost detected")

        // 3. Machine Hours check
        val machineReconciled = costRecord.totalMachineCost.compareTo(BigDecimal.ZERO) >= 0
        if (!machineReconciled) discrepancies.add("Negative machine cost detected")

        // 4. Scrap Rework valuation consistent
        val scrapConsistent = costRecord.totalQualityScrapCost.compareTo(BigDecimal.ZERO) >= 0
        if (!scrapConsistent) discrepancies.add("Negative scrap valuation detected")

        // 5. Packaging cost balanced
        val pkgBalanced = costRecord.totalPackagingCost.compareTo(BigDecimal.ZERO) >= 0
        if (!pkgBalanced) discrepancies.add("Negative packaging cost detected")

        // 6. Actual Cost Math Balance
        val sumOfComponents = costRecord.totalMaterialCost
            .add(costRecord.totalLaborCost)
            .add(costRecord.totalMachineCost)
            .add(costRecord.totalQualityScrapCost)
            .add(costRecord.totalReworkCost)
            .add(costRecord.totalPackagingCost)
            .add(costRecord.totalOverheadAllocatedCost)
        val mathBalanced = sumOfComponents.compareTo(costRecord.grandTotalActualCost) == 0
        if (!mathBalanced) discrepancies.add("Sum of component costs ($sumOfComponents) does not match grand total (${costRecord.grandTotalActualCost})")

        // 7. Multi-Tenant isolation
        val tenantValid = costRecord.tenantId.isNotBlank() && costRecord.tenantId == varianceSummary.tenantId
        if (!tenantValid) discrepancies.add("Multi-tenant mismatch between cost record and variance summary")

        val reconciledAt = System.currentTimeMillis()
        val certHash = ProductionJobCostingMathUtils.generateJobCostCertificateHash(
            tenantId = costRecord.tenantId,
            executionJobId = costRecord.executionJobId,
            orderId = costRecord.orderId,
            actualTotalCost = costRecord.grandTotalActualCost,
            estimatedTotalCost = varianceSummary.estimatedTotalCost,
            totalCostVariance = varianceSummary.totalCostVariance,
            actualUnitCost = costRecord.actualUnitCost,
            reconciledAt = reconciledAt,
            reconciledBy = reconciledBy
        )
        val hashValid = certHash.isNotBlank() && certHash.length == 64

        val isFullyReconciled = bomReconciled && laborReconciled && machineReconciled && scrapConsistent &&
                pkgBalanced && mathBalanced && tenantValid && hashValid && discrepancies.isEmpty()

        return ProductionJobCostingReconciliationResult(
            executionJobId = costRecord.executionJobId,
            tenantId = costRecord.tenantId,
            bomQuantitiesReconciled = bomReconciled,
            laborHoursReconciled = laborReconciled,
            machineHoursReconciled = machineReconciled,
            scrapReworkValuationConsistent = scrapConsistent,
            packagingCostBalanced = pkgBalanced,
            actualCostMathBalanced = mathBalanced,
            varianceIntegrityHashValid = hashValid,
            multiTenantIsolationVerified = tenantValid,
            isFullyReconciled = isFullyReconciled,
            certificateHash = certHash,
            discrepancies = discrepancies,
            reconciledAt = reconciledAt
        )
    }
}
