package com.sucharu.sucharupro.data.datasource.finalqc

import com.sucharu.sucharupro.domain.model.finalqc.*
import java.util.concurrent.ConcurrentHashMap

class FakeFinalQcPackagingDataSource : FinalQcPackagingDataSource {

    private val inspections = ConcurrentHashMap<String, MutableMap<String, ProductionFinalQcInspection>>()
    private val defects = ConcurrentHashMap<String, MutableMap<String, ProductionDefectContainmentRecord>>()
    private val packagings = ConcurrentHashMap<String, MutableMap<String, ProductionPackagingRecord>>()
    private val releases = ConcurrentHashMap<String, MutableMap<String, FinishedGoodsReleaseRecord>>()
    private val events = ConcurrentHashMap<String, MutableList<FinalQcPackagingEvent>>()

    override suspend fun saveInspection(tenantId: String, inspection: ProductionFinalQcInspection) {
        inspections.computeIfAbsent(tenantId) { ConcurrentHashMap() }[inspection.inspectionId] = inspection
    }

    override suspend fun getInspection(tenantId: String, inspectionId: String): ProductionFinalQcInspection? {
        return inspections[tenantId]?.get(inspectionId)
    }

    override suspend fun listInspectionsByJob(tenantId: String, executionJobId: String): List<ProductionFinalQcInspection> {
        return inspections[tenantId]?.values?.filter { it.executionJobId == executionJobId }?.sortedBy { it.inspectedAt } ?: emptyList()
    }

    override suspend fun saveDefectContainment(tenantId: String, defect: ProductionDefectContainmentRecord) {
        defects.computeIfAbsent(tenantId) { ConcurrentHashMap() }[defect.containmentId] = defect
    }

    override suspend fun listDefectsByJob(tenantId: String, executionJobId: String): List<ProductionDefectContainmentRecord> {
        return defects[tenantId]?.values?.filter { it.executionJobId == executionJobId }?.sortedBy { it.loggedAt } ?: emptyList()
    }

    override suspend fun savePackagingRecord(tenantId: String, packaging: ProductionPackagingRecord) {
        packagings.computeIfAbsent(tenantId) { ConcurrentHashMap() }[packaging.packagingId] = packaging
    }

    override suspend fun getPackagingRecord(tenantId: String, packagingId: String): ProductionPackagingRecord? {
        return packagings[tenantId]?.get(packagingId)
    }

    override suspend fun listPackagingRecordsByJob(tenantId: String, executionJobId: String): List<ProductionPackagingRecord> {
        return packagings[tenantId]?.values?.filter { it.executionJobId == executionJobId }?.sortedBy { it.packagedAt } ?: emptyList()
    }

    override suspend fun saveReleaseRecord(tenantId: String, release: FinishedGoodsReleaseRecord) {
        releases.computeIfAbsent(tenantId) { ConcurrentHashMap() }[release.releaseId] = release
    }

    override suspend fun getReleaseRecord(tenantId: String, releaseId: String): FinishedGoodsReleaseRecord? {
        return releases[tenantId]?.get(releaseId)
    }

    override suspend fun listReleaseRecordsByJob(tenantId: String, executionJobId: String): List<FinishedGoodsReleaseRecord> {
        return releases[tenantId]?.values?.filter { it.executionJobId == executionJobId }?.sortedBy { it.authorizedAt } ?: emptyList()
    }

    override suspend fun saveEvent(tenantId: String, event: FinalQcPackagingEvent) {
        events.computeIfAbsent(tenantId) { mutableListOf() }.add(event)
    }

    override suspend fun listEventsByJob(tenantId: String, executionJobId: String): List<FinalQcPackagingEvent> {
        return events[tenantId]?.filter { it.executionJobId == executionJobId }?.sortedBy { it.timestamp } ?: emptyList()
    }
}
