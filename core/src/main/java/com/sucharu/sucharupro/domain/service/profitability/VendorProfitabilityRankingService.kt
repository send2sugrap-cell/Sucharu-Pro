package com.sucharu.sucharupro.domain.service.profitability

import com.sucharu.sucharupro.domain.model.profitability.*

/**
 * Ranking, Concentration Analysis and Vendor Comparison Service.
 * Module 16 Step 05.
 */
interface VendorProfitabilityRankingService {

    fun rankVendors(
        snapshots: List<VendorProfitabilitySnapshot>,
        criteria: VendorRankingCriteria = VendorRankingCriteria.TOTAL_COST,
        ascending: Boolean = false,
        limit: Int = 10
    ): List<VendorRankingItem>

    fun analyzeConcentration(
        tenantId: String,
        projectId: String,
        snapshots: List<VendorProfitabilitySnapshot>,
        periodId: String? = null
    ): VendorConcentrationAnalysis

    fun compareVendors(
        snapshots: List<VendorProfitabilitySnapshot>,
        vendorIds: List<String>
    ): List<VendorComparisonItem>
}
