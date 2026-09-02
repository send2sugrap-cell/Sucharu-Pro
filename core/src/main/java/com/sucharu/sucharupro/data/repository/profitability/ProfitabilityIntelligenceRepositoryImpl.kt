package com.sucharu.sucharupro.data.repository.profitability

import com.sucharu.sucharupro.data.datasource.profitability.ProfitabilityIntelligenceDataSource
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.profitability.*

/**
 * Implementation of ProfitabilityIntelligenceRepository delegating to ProfitabilityIntelligenceDataSource.
 * Module 16 Step 07.
 */
class ProfitabilityIntelligenceRepositoryImpl(
    private val dataSource: ProfitabilityIntelligenceDataSource
) : ProfitabilityIntelligenceRepository {

    override suspend fun saveSnapshot(snapshot: ProfitabilityIntelligenceSnapshot): DomainResult<ProfitabilityIntelligenceSnapshot> {
        return dataSource.saveSnapshot(snapshot)
    }

    override suspend fun getLatestSnapshot(tenantId: String, periodId: String): DomainResult<ProfitabilityIntelligenceSnapshot?> {
        return dataSource.getLatestSnapshot(tenantId, periodId)
    }

    override suspend fun getSnapshotById(tenantId: String, snapshotId: String): DomainResult<ProfitabilityIntelligenceSnapshot?> {
        return dataSource.getSnapshotById(tenantId, snapshotId)
    }

    override suspend fun listSnapshots(tenantId: String, filter: ProfitabilityIntelligenceFilter): DomainResult<List<ProfitabilityIntelligenceSnapshot>> {
        return dataSource.listSnapshots(tenantId, filter)
    }

    override suspend fun getDimensionInsights(tenantId: String, periodId: String, dimensionType: ProfitabilityDimensionType?): DomainResult<List<DimensionInsight>> {
        return dataSource.getDimensionInsights(tenantId, periodId, dimensionType)
    }

    override suspend fun getRelationshipInsights(
        tenantId: String,
        periodId: String,
        fromDimension: ProfitabilityDimensionType?,
        toDimension: ProfitabilityDimensionType?
    ): DomainResult<List<ProfitabilityRelationshipInsight>> {
        return dataSource.getRelationshipInsights(tenantId, periodId, fromDimension, toDimension)
    }

    override suspend fun getDrivers(tenantId: String, periodId: String, driverType: ProfitabilityDriverType?): DomainResult<List<ProfitabilityDriver>> {
        return dataSource.getDrivers(tenantId, periodId, driverType)
    }

    override suspend fun getLeakages(tenantId: String, periodId: String): DomainResult<List<ProfitLeakageItem>> {
        return dataSource.getLeakages(tenantId, periodId)
    }

    override suspend fun getPriorities(tenantId: String, periodId: String): DomainResult<List<ManagementPriorityItem>> {
        return dataSource.getPriorities(tenantId, periodId)
    }

    override suspend fun getHealthScore(tenantId: String, periodId: String): DomainResult<ProfitabilityHealthScore?> {
        return dataSource.getHealthScore(tenantId, periodId)
    }

    override suspend fun getProvenanceRecords(tenantId: String, periodId: String): DomainResult<List<ProfitabilityIntelligenceProvenance>> {
        return dataSource.getProvenanceRecords(tenantId, periodId)
    }

    override suspend fun saveReconciliationEvent(event: ProfitabilityIntelligenceReconciliationEvent): DomainResult<ProfitabilityIntelligenceReconciliationEvent> {
        return dataSource.saveReconciliationEvent(event)
    }

    override suspend fun listReconciliationEvents(tenantId: String, periodId: String): DomainResult<List<ProfitabilityIntelligenceReconciliationEvent>> {
        return dataSource.listReconciliationEvents(tenantId, periodId)
    }

    override suspend fun recordAuditEvent(event: ProfitabilityIntelligenceAuditEvent): DomainResult<Unit> {
        return dataSource.recordAuditEvent(event)
    }

    override suspend fun listAuditEvents(tenantId: String, periodId: String): DomainResult<List<ProfitabilityIntelligenceAuditEvent>> {
        return dataSource.listAuditEvents(tenantId, periodId)
    }

    override suspend fun checkIdempotency(tenantId: String, idempotencyKey: String): DomainResult<String?> {
        return dataSource.checkIdempotency(tenantId, idempotencyKey)
    }

    override suspend fun saveIdempotencyRecord(tenantId: String, idempotencyKey: String, snapshotId: String): DomainResult<Unit> {
        return dataSource.saveIdempotencyRecord(tenantId, idempotencyKey, snapshotId)
    }
}
