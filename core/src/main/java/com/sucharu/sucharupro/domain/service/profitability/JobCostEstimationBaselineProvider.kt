package com.sucharu.sucharupro.domain.service.profitability

import com.sucharu.sucharupro.domain.model.common.DomainResult
import java.math.BigDecimal

/**
 * Future Module 17 (Smart Printing Calculator) estimation handoff contract.
 *
 * Allows Module 16 (Profit & Cost Analysis) to consume planned/estimated baselines
 * for variance calculation without granting Module 17 authority over actual financial ledger records.
 */
interface JobCostEstimationBaselineProvider {

    /**
     * Retrieves the authoritative estimated/baseline cost for a production Job.
     */
    suspend fun getEstimatedCostBaseline(
        tenantId: String,
        projectId: String,
        jobId: String
    ): DomainResult<BigDecimal?>
}

/**
 * Default / Fallback implementation of JobCostEstimationBaselineProvider.
 */
class DefaultJobCostEstimationBaselineProvider : JobCostEstimationBaselineProvider {
    override suspend fun getEstimatedCostBaseline(
        tenantId: String,
        projectId: String,
        jobId: String
    ): DomainResult<BigDecimal?> {
        return DomainResult.Success(null)
    }
}
