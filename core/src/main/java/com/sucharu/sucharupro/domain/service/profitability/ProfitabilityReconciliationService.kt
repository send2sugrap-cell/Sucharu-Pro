package com.sucharu.sucharupro.domain.service.profitability

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.profitability.ProfitabilityReconciliationEvent
import com.sucharu.sucharupro.domain.model.profitability.ProfitabilitySnapshot

/**
 * Reconciliation contract for validating Module 16 analytical projections against canonical sources.
 * Non-mutating verification engine.
 */
interface ProfitabilityReconciliationService {

    suspend fun reconcileSnapshot(
        snapshot: ProfitabilitySnapshot,
        actor: String = "SYSTEM"
    ): DomainResult<ProfitabilityReconciliationEvent>

    suspend fun verifyCanonicalAlignment(
        tenantId: String,
        projectId: String,
        periodId: String
    ): DomainResult<ProfitabilityReconciliationEvent>
}
