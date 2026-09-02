package com.sucharu.sucharupro.domain.service.jobcosting

import com.sucharu.sucharupro.domain.model.finalqc.*
import com.sucharu.sucharupro.domain.model.jobcosting.*
import com.sucharu.sucharupro.domain.model.production.ProductionStageType
import com.sucharu.sucharupro.domain.model.shopfloortracking.*
import java.math.BigDecimal

interface ProductionJobCostingService {

    suspend fun calculateActualJobCost(
        tenantId: String,
        executionJobId: String,
        orderId: String,
        manufacturedGoodQuantity: BigDecimal,
        materialConsumptions: List<ProductionMaterialConsumptionRecord> = emptyList(),
        operatorTimeRecords: List<OperatorTimeTrackingRecord> = emptyList(),
        machineTelemetryLogs: List<MachineTelemetryLog> = emptyList(),
        defectRecords: List<ProductionDefectContainmentRecord> = emptyList(),
        packagingRecords: List<ProductionPackagingRecord> = emptyList(),
        standardMaterialRates: Map<String, BigDecimal> = emptyMap(),
        standardLaborHourlyRates: Map<ProductionStageType, BigDecimal> = emptyMap(),
        standardMachineHourlyRates: Map<String, BigDecimal> = emptyMap(),
        packagingUnitRate: BigDecimal = BigDecimal("25.0000"),
        overheadAllocationRate: BigDecimal = BigDecimal("0.1000"),
        actor: String = "cost-accountant"
    ): ProductionActualJobCostRecord

    suspend fun getActualJobCostByJob(tenantId: String, executionJobId: String): ProductionActualJobCostRecord?

    suspend fun calculateJobCostVariance(
        tenantId: String,
        executionJobId: String,
        quotedSellingPrice: BigDecimal,
        estimatedTotalCost: BigDecimal,
        estimatedMaterialCost: BigDecimal,
        estimatedLaborCost: BigDecimal,
        estimatedMachineCost: BigDecimal,
        orderQuantity: BigDecimal,
        actor: String = "cost-accountant"
    ): ProductionJobCostVarianceSummary

    suspend fun getVarianceSummaryByJob(tenantId: String, executionJobId: String): ProductionJobCostVarianceSummary?

    suspend fun reconcileJobCosting(
        tenantId: String,
        executionJobId: String,
        actor: String = "cost-auditor"
    ): ProductionJobCostingReconciliationResult

    suspend fun getAiHandoffContract(
        tenantId: String,
        executionJobId: String
    ): Module17Step09JobCostingVarianceHandoffContract
}
