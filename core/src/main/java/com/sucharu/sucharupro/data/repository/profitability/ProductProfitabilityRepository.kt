package com.sucharu.sucharupro.data.repository.profitability

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.profitability.*

/**
 * Repository interface for Product Profitability & Unit Economics analytics (Module 16 Step 03).
 */
interface ProductProfitabilityRepository {

    suspend fun saveSnapshot(snapshot: ProductProfitabilitySnapshot): DomainResult<ProductProfitabilitySnapshot>

    suspend fun getSnapshotById(tenantId: String, projectId: String, snapshotId: String): DomainResult<ProductProfitabilitySnapshot>

    suspend fun getLatestSnapshotByProduct(tenantId: String, projectId: String, productId: String): DomainResult<ProductProfitabilitySnapshot?>

    suspend fun listSnapshots(tenantId: String, projectId: String, filter: ProductProfitabilityFilter): DomainResult<List<ProductProfitabilitySnapshot>>

    suspend fun saveRevenueAttributions(attributions: List<ProductRevenueAttribution>): DomainResult<Unit>

    suspend fun getRevenueAttributions(tenantId: String, projectId: String, productId: String): DomainResult<List<ProductRevenueAttribution>>

    suspend fun saveCostAttributions(attributions: List<ProductCostAttribution>): DomainResult<Unit>

    suspend fun getCostAttributions(tenantId: String, projectId: String, productId: String): DomainResult<List<ProductCostAttribution>>

    suspend fun saveReconciliationEvent(event: ProductProfitabilityReconciliationEvent): DomainResult<ProductProfitabilityReconciliationEvent>

    suspend fun getReconciliationEvents(tenantId: String, projectId: String, productId: String): DomainResult<List<ProductProfitabilityReconciliationEvent>>

    suspend fun recordAuditEvent(event: ProductProfitabilityAuditEvent): DomainResult<Unit>

    suspend fun getAuditEvents(tenantId: String, projectId: String, productId: String): DomainResult<List<ProductProfitabilityAuditEvent>>
}
