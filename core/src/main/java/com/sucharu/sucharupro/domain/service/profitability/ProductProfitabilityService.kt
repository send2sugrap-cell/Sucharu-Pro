package com.sucharu.sucharupro.domain.service.profitability

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.profitability.*
import java.math.BigDecimal

/**
 * Product Profitability & Unit Economics Engine Service Interface (Module 16 Step 03).
 */
interface ProductProfitabilityService {

    suspend fun calculateProductProfitability(
        tenantId: String,
        projectId: String,
        productId: String,
        sku: String? = null,
        productName: String? = null,
        editionId: String? = null,
        versionId: String? = null,
        periodId: String? = null,
        customerId: String? = null,
        customRevenue: List<ProductRevenueAttribution>? = null,
        customCosts: List<ProductCostAttribution>? = null,
        customBaselineCost: BigDecimal? = null,
        idempotencyKey: String? = null,
        actor: String = "SYSTEM"
    ): DomainResult<ProductProfitabilitySnapshot>

    suspend fun getLatestSnapshot(tenantId: String, projectId: String, productId: String): DomainResult<ProductProfitabilitySnapshot?>

    suspend fun getSnapshotById(tenantId: String, projectId: String, snapshotId: String): DomainResult<ProductProfitabilitySnapshot>

    suspend fun listSnapshots(tenantId: String, projectId: String, filter: ProductProfitabilityFilter): DomainResult<List<ProductProfitabilitySnapshot>>

    suspend fun getCostBreakdown(tenantId: String, projectId: String, productId: String): DomainResult<List<ProductCostBreakdownItem>>

    suspend fun getProvenance(tenantId: String, projectId: String, productId: String): DomainResult<Pair<List<ProductRevenueAttribution>, List<ProductCostAttribution>>>

    suspend fun getUnitEconomics(tenantId: String, projectId: String, productId: String): DomainResult<ProductUnitEconomics>

    suspend fun getVariance(tenantId: String, projectId: String, productId: String): DomainResult<Pair<ProductVarianceClassification, Pair<BigDecimal?, BigDecimal?>>>

    suspend fun reconcileProductProfitability(
        tenantId: String,
        projectId: String,
        productId: String,
        snapshotId: String?,
        actor: String = "SYSTEM"
    ): DomainResult<ProductProfitabilityReconciliationEvent>

    suspend fun getAuditHistory(tenantId: String, projectId: String, productId: String): DomainResult<List<ProductProfitabilityAuditEvent>>

    suspend fun compareProducts(tenantId: String, projectId: String, productIds: List<String>): DomainResult<List<ProductProfitabilityComparisonItem>>
}
