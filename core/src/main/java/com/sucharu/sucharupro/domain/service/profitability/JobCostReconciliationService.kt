package com.sucharu.sucharupro.domain.service.profitability

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.profitability.JobCostReconciliationEvent
import com.sucharu.sucharupro.domain.model.profitability.JobCostSnapshot

/**
 * Non-mutating Job Cost Reconciliation service interface.
 */
interface JobCostReconciliationService {

    /**
     * Reconciles an analytical Job cost snapshot against its underlying components and provenances.
     */
    suspend fun reconcileJobCostSnapshot(
        snapshot: JobCostSnapshot,
        actor: String = "SYSTEM"
    ): DomainResult<JobCostReconciliationEvent>
}
