package com.sucharu.sucharupro.domain.service.profitability

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.profitability.*

/**
 * Service interface for Module 16 Profitability & Cost Analysis Foundation.
 */
interface ProfitabilityFoundationService {

    suspend fun generateProfitabilitySnapshot(
        tenantId: String,
        projectId: String,
        scope: ProfitabilityScope,
        targetEntityId: String? = null,
        periodId: String? = null,
        currency: String = "BDT",
        customRevenue: java.math.BigDecimal? = null,
        customDirectCost: java.math.BigDecimal? = null,
        customIndirectCost: java.math.BigDecimal? = null,
        baselineCost: java.math.BigDecimal? = null,
        baselineRevenue: java.math.BigDecimal? = null,
        revenueProvenances: List<RevenueProvenance> = emptyList(),
        costAttributions: List<CostAttributionReference> = emptyList(),
        idempotencyKey: String? = null,
        actor: String = "SYSTEM"
    ): DomainResult<ProfitabilitySnapshot>

    suspend fun getSnapshotById(
        tenantId: String,
        projectId: String,
        id: String
    ): DomainResult<ProfitabilitySnapshot>

    suspend fun listSnapshots(
        tenantId: String,
        projectId: String,
        scope: ProfitabilityScope? = null,
        targetEntityId: String? = null,
        periodId: String? = null,
        limit: Int = 100,
        offset: Int = 0
    ): DomainResult<List<ProfitabilitySnapshot>>

    suspend fun reconcileSnapshot(
        tenantId: String,
        projectId: String,
        snapshotId: String,
        actor: String = "SYSTEM"
    ): DomainResult<ProfitabilityReconciliationEvent>

    suspend fun listReconciliationEvents(
        tenantId: String,
        projectId: String,
        snapshotId: String? = null,
        limit: Int = 100,
        offset: Int = 0
    ): DomainResult<List<ProfitabilityReconciliationEvent>>

    suspend fun getSourceReadiness(
        tenantId: String,
        projectId: String,
        periodId: String? = null
    ): DomainResult<ProfitabilitySourceReadiness>

    suspend fun getFinancialHandoffStatus(
        tenantId: String,
        projectId: String,
        periodId: String
    ): DomainResult<ValidatedFinancialHandoff>

    suspend fun listAuditEvents(
        tenantId: String,
        projectId: String,
        snapshotId: String? = null,
        limit: Int = 100,
        offset: Int = 0
    ): DomainResult<List<ProfitabilityAuditEvent>>
}
