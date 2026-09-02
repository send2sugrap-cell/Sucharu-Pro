package com.sucharu.sucharupro.domain.service.finalqc

import com.sucharu.sucharupro.domain.model.finalqc.*
import com.sucharu.sucharupro.domain.model.production.ProductionStageType
import java.math.BigDecimal

interface FinalQcPackagingService {

    suspend fun createFinalQcInspection(
        tenantId: String,
        executionJobId: String,
        orderId: String,
        samplePlanType: InspectionSamplePlanType,
        totalLotQuantity: BigDecimal,
        sampleSize: BigDecimal,
        checklist: List<QcChecklistItem>,
        inspectorId: String,
        inspectorName: String,
        notes: String? = null,
        actor: String = inspectorName
    ): ProductionFinalQcInspection

    suspend fun completeFinalQcInspection(
        tenantId: String,
        inspectionId: String,
        acceptedQuantity: BigDecimal,
        rejectedQuantity: BigDecimal,
        reworkQuantity: BigDecimal,
        notes: String? = null,
        actor: String = "inspector"
    ): ProductionFinalQcInspection

    suspend fun getInspection(tenantId: String, inspectionId: String): ProductionFinalQcInspection?

    suspend fun listInspectionsByJob(tenantId: String, executionJobId: String): List<ProductionFinalQcInspection>

    suspend fun recordDefectContainment(
        tenantId: String,
        executionJobId: String,
        inspectionId: String,
        rootCauseStage: ProductionStageType,
        defectType: DefectClassificationType,
        severity: DefectSeverity,
        defectQuantity: BigDecimal,
        disposition: ContainmentDisposition,
        quarantineLocation: String,
        reworkWorkOrderId: String? = null,
        rootCauseDetails: String,
        actor: String = "inspector"
    ): ProductionDefectContainmentRecord

    suspend fun listDefectsByJob(tenantId: String, executionJobId: String): List<ProductionDefectContainmentRecord>

    suspend fun createPackagingRecord(
        tenantId: String,
        executionJobId: String,
        inspectionId: String,
        packagingType: PackagingType,
        unitsPerPackage: BigDecimal,
        totalPackageCount: Int,
        palletIdentifier: String? = null,
        cartonNumbersRange: String? = null,
        grossWeightKg: BigDecimal? = null,
        packagedBy: String,
        notes: String? = null,
        actor: String = packagedBy
    ): ProductionPackagingRecord

    suspend fun listPackagingRecordsByJob(tenantId: String, executionJobId: String): List<ProductionPackagingRecord>

    suspend fun authorizeFinishedGoodsRelease(
        tenantId: String,
        executionJobId: String,
        orderId: String,
        inspectionId: String,
        packagingId: String,
        releasedQuantity: BigDecimal,
        destination: String,
        authorizedBy: String,
        notes: String? = null,
        actor: String = authorizedBy
    ): FinishedGoodsReleaseRecord

    suspend fun listReleaseRecordsByJob(tenantId: String, executionJobId: String): List<FinishedGoodsReleaseRecord>

    suspend fun getQualityVarianceSummary(
        tenantId: String,
        executionJobId: String,
        totalShopFloorGoodOutput: BigDecimal = BigDecimal.ZERO
    ): FinalQcPackagingVarianceSummary

    suspend fun reconcileFinalQcPackaging(
        tenantId: String,
        executionJobId: String,
        totalShopFloorGoodOutput: BigDecimal = BigDecimal.ZERO
    ): FinalQcPackagingReconciliationResult

    suspend fun getAiHandoffContract(
        tenantId: String,
        executionJobId: String,
        orderId: String = ""
    ): Module17Step08FinalQcPackagingHandoffContract
}
