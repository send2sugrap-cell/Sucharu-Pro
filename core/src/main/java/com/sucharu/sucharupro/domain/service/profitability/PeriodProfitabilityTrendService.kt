package com.sucharu.sucharupro.domain.service.profitability

import com.sucharu.sucharupro.domain.model.profitability.PeriodComparisonResult
import com.sucharu.sucharupro.domain.model.profitability.PeriodProfitabilitySnapshot

/**
 * Period trend and comparison service contract.
 * Module 16 Step 06.
 */
interface PeriodProfitabilityTrendService {
    fun comparePeriods(
        currentSnapshot: PeriodProfitabilitySnapshot,
        previousSnapshot: PeriodProfitabilitySnapshot
    ): PeriodComparisonResult
}
