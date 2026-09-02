package com.sucharu.sucharupro.data.datasource.profitability

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.profitability.*

/**
 * Data Source interface for Profitability Intelligence persistence.
 * Module 16 Step 07.
 */
interface ProfitabilityIntelligenceDataSource {
    suspend fun saveSnapshot(snapshot: ProfitabilityIntelligenceSnapshot): DomainResult<ProfitabilityIntelligenceSnapshot>
    suspend fun getLatestSnapshot(tenantId: String, periodId: String): DomainResult<ProfitabilityIntelligenceSnapshot?>
    suspend fun getSnapshotById(tenantId: String, snapshotId: String): DomainResult<ProfitabilityIntelligenceSnapshot?>
    suspend fun listSnapshots(tenantId: String, filter: ProfitabilityIntelligenceFilter): DomainResult<List<ProfitabilityIntelligenceSnapshot>>
    suspend fun getDimensionInsights(tenantId: String, periodId: String, dimensionType: ProfitabilityDimensionType? = null): DomainResult<List<DimensionInsight>>
    suspend fun getRelationshipInsights(tenantId: String, periodId: String, fromDimension: ProfitabilityDimensionType? = null, toDimension: ProfitabilityDimensionType? = null): DomainResult<List<ProfitabilityRelationshipInsight>>
    suspend fun getDrivers(tenantId: String, periodId: String, driverType: ProfitabilityDriverType? = null): DomainResult<List<ProfitabilityDriver>>
    suspend fun getLeakages(tenantId: String, periodId: String): DomainResult<List<ProfitLeakageItem>>
    suspend fun getPriorities(tenantId: String, periodId: String): DomainResult<List<ManagementPriorityItem>>
    suspend fun getHealthScore(tenantId: String, periodId: String): DomainResult<ProfitabilityHealthScore?>
    suspend fun getProvenanceRecords(tenantId: String, periodId: String): DomainResult<List<ProfitabilityIntelligenceProvenance>>
    suspend fun saveReconciliationEvent(event: ProfitabilityIntelligenceReconciliationEvent): DomainResult<ProfitabilityIntelligenceReconciliationEvent>
    suspend fun listReconciliationEvents(tenantId: String, periodId: String): DomainResult<List<ProfitabilityIntelligenceReconciliationEvent>>
    suspend fun recordAuditEvent(event: ProfitabilityIntelligenceAuditEvent): DomainResult<Unit>
    suspend fun listAuditEvents(tenantId: String, periodId: String): DomainResult<List<ProfitabilityIntelligenceAuditEvent>>
    suspend fun checkIdempotency(tenantId: String, idempotencyKey: String): DomainResult<String?>
    suspend fun saveIdempotencyRecord(tenantId: String, idempotencyKey: String, snapshotId: String): DomainResult<Unit>
}
