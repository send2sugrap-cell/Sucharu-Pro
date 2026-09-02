package com.sucharu.sucharupro.domain.service.jobclosure

import com.sucharu.sucharupro.domain.model.finalqc.FinishedGoodsReleaseRecord
import com.sucharu.sucharupro.domain.model.finalqc.ProductionFinalQcInspection
import com.sucharu.sucharupro.domain.model.jobclosure.*
import com.sucharu.sucharupro.domain.model.jobcosting.ProductionActualJobCostRecord
import com.sucharu.sucharupro.domain.model.jobcosting.ProductionJobCostVarianceSummary
import com.sucharu.sucharupro.domain.model.jobcosting.ProductionJobCostingReconciliationResult
import com.sucharu.sucharupro.domain.model.productionexecution.ProductionJobExecution
import com.sucharu.sucharupro.domain.model.productionexecution.ProductionWorkOrder
import com.sucharu.sucharupro.domain.model.productionscheduling.ProductionSchedule
import com.sucharu.sucharupro.domain.model.shopfloortracking.OperatorTimeTrackingRecord
import com.sucharu.sucharupro.domain.model.shopfloortracking.ProductionMaterialConsumptionRecord
import java.math.BigDecimal

interface ProductionJobClosureService {

    suspend fun auditJobClosureReadiness(
        tenantId: String,
        executionJobId: String,
        orderId: String,
        jobExecution: ProductionJobExecution?,
        workOrders: List<ProductionWorkOrder> = emptyList(),
        schedule: ProductionSchedule? = null,
        materialConsumptions: List<ProductionMaterialConsumptionRecord> = emptyList(),
        timeRecords: List<OperatorTimeTrackingRecord> = emptyList(),
        finalQcInspection: ProductionFinalQcInspection? = null,
        goodsRelease: FinishedGoodsReleaseRecord? = null,
        costRecord: ProductionActualJobCostRecord? = null,
        reconciliationResult: ProductionJobCostingReconciliationResult? = null,
        actor: String = "closure-auditor"
    ): JobClosureReadinessAudit

    suspend fun closeAndSealJob(
        tenantId: String,
        executionJobId: String,
        orderId: String,
        orderQuantity: BigDecimal,
        goodUnitsReleased: BigDecimal,
        estimatedTotalCost: BigDecimal,
        actualTotalCost: BigDecimal,
        totalCostVariance: BigDecimal,
        reworkOrScrapUnits: BigDecimal = BigDecimal.ZERO,
        machineEfficiency: BigDecimal = BigDecimal("85.0000"),
        onTime: Boolean = true,
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
        primaryDowntimeDrivers: List<String> = emptyList(),
        scrapAndDefectTakeaways: List<String> = emptyList(),
        costVarianceTakeaways: List<String> = emptyList(),
        operationalRecommendations: List<String> = emptyList(),
        actor: String = "plant-manager"
    ): ProductionJobClosureRecord

    suspend fun getClosureRecordByJob(tenantId: String, executionJobId: String): ProductionJobClosureRecord?

    suspend fun getScorecardByJob(tenantId: String, executionJobId: String): ManufacturingPerformanceScorecard?

    suspend fun getAiHandoffContract(tenantId: String, executionJobId: String): Module17Step10JobClosureGovernanceHandoffContract
}
