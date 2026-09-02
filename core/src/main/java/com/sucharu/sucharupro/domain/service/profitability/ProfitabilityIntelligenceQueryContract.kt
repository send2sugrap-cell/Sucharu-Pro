package com.sucharu.sucharupro.domain.service.profitability

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.profitability.*

/**
 * AI-Agent and Automation Ready Read-Only Query Contract for Profitability Intelligence.
 * Exposes deterministic, mathematically explainable querying interfaces for Sucharu AI, n8n, and external integrations.
 * Module 16 Step 07.
 */
interface ProfitabilityIntelligenceQueryContract {

    suspend fun getExecutiveOverview(
        tenantId: String,
        periodId: String
    ): DomainResult<ProfitabilityIntelligenceSnapshot?>

    suspend fun getMostProfitableEntities(
        tenantId: String,
        periodId: String,
        dimensionType: ProfitabilityDimensionType?,
        limit: Int = 10
    ): DomainResult<List<DimensionInsight>>

    suspend fun getLossMakingEntities(
        tenantId: String,
        periodId: String,
        limit: Int = 10
    ): DomainResult<List<DimensionInsight>>

    suspend fun getTopProfitDrivers(
        tenantId: String,
        periodId: String,
        driverType: ProfitabilityDriverType? = null,
        limit: Int = 10
    ): DomainResult<List<ProfitabilityDriver>>

    suspend fun getTopProfitLeakages(
        tenantId: String,
        periodId: String,
        limit: Int = 10
    ): DomainResult<List<ProfitLeakageItem>>

    suspend fun getManagementActionQueue(
        tenantId: String,
        periodId: String,
        priorityLevel: ManagementPriorityLevel? = null
    ): DomainResult<List<ManagementPriorityItem>>

    suspend fun getHealthScoreSummary(
        tenantId: String,
        periodId: String
    ): DomainResult<ProfitabilityHealthScore?>

    suspend fun getRelationshipInsights(
        tenantId: String,
        periodId: String,
        fromDimension: ProfitabilityDimensionType? = null,
        toDimension: ProfitabilityDimensionType? = null
    ): DomainResult<List<ProfitabilityRelationshipInsight>>

    suspend fun exportHandoffContract(
        tenantId: String,
        projectId: String,
        periodId: String
    ): DomainResult<Module16Step07ProfitabilityIntelligenceHandoffContract>
}
