package com.sucharu.sucharupro.domain.repository.finalqc

import com.sucharu.sucharupro.domain.model.finalqc.*

interface FinalQcPackagingRepository {
    suspend fun saveInspection(tenantId: String, inspection: ProductionFinalQcInspection)
    suspend fun getInspection(tenantId: String, inspectionId: String): ProductionFinalQcInspection?
    suspend fun listInspectionsByJob(tenantId: String, executionJobId: String): List<ProductionFinalQcInspection>

    suspend fun saveDefectContainment(tenantId: String, defect: ProductionDefectContainmentRecord)
    suspend fun listDefectsByJob(tenantId: String, executionJobId: String): List<ProductionDefectContainmentRecord>

    suspend fun savePackagingRecord(tenantId: String, packaging: ProductionPackagingRecord)
    suspend fun getPackagingRecord(tenantId: String, packagingId: String): ProductionPackagingRecord?
    suspend fun listPackagingRecordsByJob(tenantId: String, executionJobId: String): List<ProductionPackagingRecord>

    suspend fun saveReleaseRecord(tenantId: String, release: FinishedGoodsReleaseRecord)
    suspend fun getReleaseRecord(tenantId: String, releaseId: String): FinishedGoodsReleaseRecord?
    suspend fun listReleaseRecordsByJob(tenantId: String, executionJobId: String): List<FinishedGoodsReleaseRecord>

    suspend fun saveEvent(tenantId: String, event: FinalQcPackagingEvent)
    suspend fun listEventsByJob(tenantId: String, executionJobId: String): List<FinalQcPackagingEvent>
}
