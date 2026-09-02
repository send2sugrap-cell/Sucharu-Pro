package com.sucharu.sucharupro.domain.service.profitability

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.profitability.*
import java.math.BigDecimal

/**
 * Service interface for Vendor Profitability, Cost Contribution & Supplier Economics.
 * Module 16 Step 05.
 */
interface VendorProfitabilityService {

    suspend fun calculateVendorProfitability(
        tenantId: String,
        projectId: String,
        vendorId: String,
        vendorName: String? = null,
        vendorCode: String? = null,
        serviceCategory: String? = null,
        periodId: String? = null,
        periodStart: Long? = null,
        periodEnd: Long? = null,
        customCosts: List<VendorCostAttribution>? = null,
        customRevenueContext: List<VendorRevenueContextAttribution>? = null,
        customBaselineCost: BigDecimal? = null,
        idempotencyKey: String? = null
    ): DomainResult<VendorProfitabilitySnapshot>

    suspend fun getLatestSnapshot(
        tenantId: String,
        vendorId: String
    ): DomainResult<VendorProfitabilitySnapshot?>

    suspend fun getSnapshotById(
        tenantId: String,
        snapshotId: String
    ): DomainResult<VendorProfitabilitySnapshot?>

    suspend fun listSnapshots(
        tenantId: String,
        filter: VendorProfitabilityFilter
    ): DomainResult<List<VendorProfitabilitySnapshot>>

    suspend fun getCostBreakdown(
        tenantId: String,
        vendorId: String
    ): DomainResult<List<VendorCostBreakdownItem>>

    suspend fun getCostAttributions(
        tenantId: String,
        vendorId: String
    ): DomainResult<List<VendorCostAttribution>>

    suspend fun getRevenueContextAttributions(
        tenantId: String,
        vendorId: String
    ): DomainResult<List<VendorRevenueContextAttribution>>

    suspend fun getProvenance(
        tenantId: String,
        vendorId: String
    ): DomainResult<Pair<List<VendorCostAttribution>, List<VendorRevenueContextAttribution>>>

    suspend fun reconcile(
        tenantId: String,
        projectId: String,
        vendorId: String,
        snapshotId: String? = null
    ): DomainResult<VendorProfitabilityReconciliationEvent>

    suspend fun listAuditEvents(
        tenantId: String,
        vendorId: String
    ): DomainResult<List<VendorProfitabilityAuditEvent>>

    suspend fun listUnattributedItems(
        tenantId: String,
        vendorId: String? = null
    ): DomainResult<List<VendorUnattributedItem>>

    suspend fun rankVendors(
        tenantId: String,
        criteria: VendorRankingCriteria = VendorRankingCriteria.TOTAL_COST,
        ascending: Boolean = false,
        limit: Int = 10
    ): DomainResult<List<VendorRankingItem>>

    suspend fun analyzeConcentration(
        tenantId: String,
        projectId: String,
        periodId: String? = null
    ): DomainResult<VendorConcentrationAnalysis>

    suspend fun compareVendors(
        tenantId: String,
        vendorIds: List<String>
    ): DomainResult<List<VendorComparisonItem>>
}
