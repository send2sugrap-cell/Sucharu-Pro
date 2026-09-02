package com.sucharu.sucharupro.domain.service.profitability

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.profitability.*
import java.math.BigDecimal

/**
 * Interface for collecting canonical vendor costs, work orders, jobs, payables, revenue contexts, and QC metrics.
 * Module 16 Step 05.
 */
interface VendorProfitabilitySourceCollector {

    suspend fun collectVendorData(
        tenantId: String,
        projectId: String,
        vendorId: String,
        customCosts: List<VendorCostAttribution>? = null,
        customRevenueContext: List<VendorRevenueContextAttribution>? = null,
        periodStart: Long? = null,
        periodEnd: Long? = null
    ): DomainResult<VendorSourceCollectionResult>
}
