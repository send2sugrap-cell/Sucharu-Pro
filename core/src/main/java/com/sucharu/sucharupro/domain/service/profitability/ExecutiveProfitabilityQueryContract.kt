package com.sucharu.sucharupro.domain.service.profitability

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.profitability.*

/**
 * Executive Profitability Service Interface.
 * Canonical Orchestration Contract for Module 16 Step 10.
 */
interface ExecutiveProfitabilityService {

    suspend fun calculateSnapshot(
        tenantId: String,
        projectId: String,
        periodId: String? = null,
        idempotencyKey: String? = null,
        actorId: String = "SYSTEM",
        actorRole: String = "SYSTEM"
    ): DomainResult<ExecutiveProfitabilitySnapshot>

    suspend fun getLatestSnapshot(
        tenantId: String,
        projectId: String,
        periodId: String? = null
    ): DomainResult<ExecutiveProfitabilitySnapshot>

    suspend fun getSnapshotById(
        tenantId: String,
        snapshotId: String
    ): DomainResult<ExecutiveProfitabilitySnapshot>

    suspend fun listSnapshots(
        tenantId: String,
        projectId: String,
        limit: Int = 20
    ): DomainResult<List<ExecutiveProfitabilitySnapshot>>

    suspend fun getKpis(
        tenantId: String,
        projectId: String,
        periodId: String? = null
    ): DomainResult<List<ExecutiveKpi>>

    suspend fun getScorecard(
        tenantId: String,
        projectId: String,
        periodId: String? = null
    ): DomainResult<ExecutiveManagementScorecard>

    suspend fun getRankings(
        tenantId: String,
        projectId: String,
        periodId: String? = null
    ): DomainResult<ExecutiveRankingsPayload>

    suspend fun getPriorities(
        tenantId: String,
        projectId: String,
        periodId: String? = null
    ): DomainResult<List<ExecutivePriorityItem>>

    suspend fun getDrivers(
        tenantId: String,
        projectId: String,
        periodId: String? = null
    ): DomainResult<List<ExecutiveProfitabilityDriver>>

    suspend fun getLeakages(
        tenantId: String,
        projectId: String,
        periodId: String? = null
    ): DomainResult<ExecutiveLeakageSummary>

    suspend fun getConcentration(
        tenantId: String,
        projectId: String,
        periodId: String? = null
    ): DomainResult<ExecutiveConcentrationSummary>

    suspend fun getReconciliation(
        tenantId: String,
        projectId: String,
        periodId: String? = null
    ): DomainResult<ExecutiveReconciliationResult>

    suspend fun getProvenance(
        tenantId: String,
        snapshotId: String
    ): DomainResult<List<ExecutiveProvenanceRecord>>

    suspend fun getFullReport(
        tenantId: String,
        projectId: String,
        periodId: String? = null
    ): DomainResult<ExecutiveProfitabilityReport>

    suspend fun exportHandoffContract(
        tenantId: String,
        projectId: String,
        periodId: String? = null
    ): DomainResult<Module16Step10ExecutiveProfitabilityHandoffContract>
}
