package com.sucharu.sucharupro.data.repository.finalqc

import com.sucharu.sucharupro.data.datasource.finalqc.FinalQcPackagingDataSource
import com.sucharu.sucharupro.domain.model.finalqc.*
import com.sucharu.sucharupro.domain.repository.finalqc.FinalQcPackagingRepository

class FinalQcPackagingRepositoryImpl(
    private val dataSource: FinalQcPackagingDataSource
) : FinalQcPackagingRepository {

    override suspend fun saveInspection(tenantId: String, inspection: ProductionFinalQcInspection) {
        dataSource.saveInspection(tenantId, inspection)
    }

    override suspend fun getInspection(tenantId: String, inspectionId: String): ProductionFinalQcInspection? {
        return dataSource.getInspection(tenantId, inspectionId)
    }

    override suspend fun listInspectionsByJob(tenantId: String, executionJobId: String): List<ProductionFinalQcInspection> {
        return dataSource.listInspectionsByJob(tenantId, executionJobId)
    }

    override suspend fun saveDefectContainment(tenantId: String, defect: ProductionDefectContainmentRecord) {
        dataSource.saveDefectContainment(tenantId, defect)
    }

    override suspend fun listDefectsByJob(tenantId: String, executionJobId: String): List<ProductionDefectContainmentRecord> {
        return dataSource.listDefectsByJob(tenantId, executionJobId)
    }

    override suspend fun savePackagingRecord(tenantId: String, packaging: ProductionPackagingRecord) {
        dataSource.savePackagingRecord(tenantId, packaging)
    }

    override suspend fun getPackagingRecord(tenantId: String, packagingId: String): ProductionPackagingRecord? {
        return dataSource.getPackagingRecord(tenantId, packagingId)
    }

    override suspend fun listPackagingRecordsByJob(tenantId: String, executionJobId: String): List<ProductionPackagingRecord> {
        return dataSource.listPackagingRecordsByJob(tenantId, executionJobId)
    }

    override suspend fun saveReleaseRecord(tenantId: String, release: FinishedGoodsReleaseRecord) {
        dataSource.saveReleaseRecord(tenantId, release)
    }

    override suspend fun getReleaseRecord(tenantId: String, releaseId: String): FinishedGoodsReleaseRecord? {
        return dataSource.getReleaseRecord(tenantId, releaseId)
    }

    override suspend fun listReleaseRecordsByJob(tenantId: String, executionJobId: String): List<FinishedGoodsReleaseRecord> {
        return dataSource.listReleaseRecordsByJob(tenantId, executionJobId)
    }

    override suspend fun saveEvent(tenantId: String, event: FinalQcPackagingEvent) {
        dataSource.saveEvent(tenantId, event)
    }

    override suspend fun listEventsByJob(tenantId: String, executionJobId: String): List<FinalQcPackagingEvent> {
        return dataSource.listEventsByJob(tenantId, executionJobId)
    }
}
