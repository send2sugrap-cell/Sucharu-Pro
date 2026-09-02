package com.sucharu.sucharupro.data.datasource.profitability

import com.sucharu.sucharupro.domain.model.profitability.*

/**
 * Underlying DataSource interface for Product Profitability persistence.
 */
interface ProductProfitabilityDataSource {

    suspend fun saveSnapshot(snapshot: ProductProfitabilitySnapshot)

    suspend fun getSnapshotById(tenantId: String, projectId: String, snapshotId: String): ProductProfitabilitySnapshot?

    suspend fun getLatestSnapshotByProduct(tenantId: String, projectId: String, productId: String): ProductProfitabilitySnapshot?

    suspend fun listSnapshots(tenantId: String, projectId: String, filter: ProductProfitabilityFilter): List<ProductProfitabilitySnapshot>

    suspend fun saveRevenueAttributions(attributions: List<ProductRevenueAttribution>)

    suspend fun getRevenueAttributions(tenantId: String, projectId: String, productId: String): List<ProductRevenueAttribution>

    suspend fun saveCostAttributions(attributions: List<ProductCostAttribution>)

    suspend fun getCostAttributions(tenantId: String, projectId: String, productId: String): List<ProductCostAttribution>

    suspend fun saveReconciliationEvent(event: ProductProfitabilityReconciliationEvent)

    suspend fun getReconciliationEvents(tenantId: String, projectId: String, productId: String): List<ProductProfitabilityReconciliationEvent>

    suspend fun recordAuditEvent(event: ProductProfitabilityAuditEvent)

    suspend fun getAuditEvents(tenantId: String, projectId: String, productId: String): List<ProductProfitabilityAuditEvent>
}
