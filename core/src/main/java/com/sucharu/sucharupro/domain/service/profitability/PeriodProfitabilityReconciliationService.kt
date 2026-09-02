package com.sucharu.sucharupro.domain.service.profitability

import com.sucharu.sucharupro.domain.model.profitability.PeriodProfitabilityReconciliationEvent
import com.sucharu.sucharupro.domain.model.profitability.PeriodProfitabilitySnapshot
import com.sucharu.sucharupro.domain.model.profitability.PeriodSourceCollectionResult

/**
 * Non-mutating invariant reconciliation service for Period Profitability.
 * Module 16 Step 06.
 */
interface PeriodProfitabilityReconciliationService {
    fun reconcile(
        snapshot: PeriodProfitabilitySnapshot,
        sourceData: PeriodSourceCollectionResult,
        childSnapshots: List<PeriodProfitabilitySnapshot> = emptyList()
    ): PeriodProfitabilityReconciliationEvent
}
