package com.sucharu.sucharupro.domain.service.profitability

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.profitability.*

/**
 * Registry and Provenance Contract for Canonical Sources consumed by Module 16.
 */
interface ProfitabilitySourceRegistry {

    suspend fun evaluateSourceReadiness(
        tenantId: String,
        projectId: String,
        periodId: String? = null
    ): DomainResult<ProfitabilitySourceReadiness>

    fun validateRevenueProvenance(
        provenance: RevenueProvenance
    ): DomainResult<Boolean>

    fun validateCostAttribution(
        attribution: CostAttributionReference
    ): DomainResult<Boolean>

    fun detectDuplicateSources(
        revenues: List<RevenueProvenance>,
        costs: List<CostAttributionReference>
    ): List<String>
}
