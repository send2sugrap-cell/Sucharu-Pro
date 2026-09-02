package com.sucharu.sucharupro.data.datasource.profitability

import com.sucharu.sucharupro.domain.model.profitability.*
import java.util.concurrent.ConcurrentHashMap

/**
 * In-memory thread-safe fake datasource for deterministic testing of Product Profitability.
 */
class FakeProductProfitabilityDataSource : ProductProfitabilityDataSource {

    private val snapshots = ConcurrentHashMap<String, ProductProfitabilitySnapshot>() // key: tenantId:projectId:snapshotId
    private val revenueAttributions = ConcurrentHashMap<String, MutableList<ProductRevenueAttribution>>() // key: tenantId:projectId:productId
    private val costAttributions = ConcurrentHashMap<String, MutableList<ProductCostAttribution>>() // key: tenantId:projectId:productId
    private val reconciliationEvents = ConcurrentHashMap<String, MutableList<ProductProfitabilityReconciliationEvent>>() // key: tenantId:projectId:productId
    private val auditEvents = ConcurrentHashMap<String, MutableList<ProductProfitabilityAuditEvent>>() // key: tenantId:projectId:productId

    override suspend fun saveSnapshot(snapshot: ProductProfitabilitySnapshot) {
        val key = "${snapshot.tenantId}:${snapshot.projectId}:${snapshot.snapshotId}"
        snapshots[key] = snapshot
    }

    override suspend fun getSnapshotById(tenantId: String, projectId: String, snapshotId: String): ProductProfitabilitySnapshot? {
        val key = "$tenantId:$projectId:$snapshotId"
        return snapshots[key]
    }

    override suspend fun getLatestSnapshotByProduct(tenantId: String, projectId: String, productId: String): ProductProfitabilitySnapshot? {
        return snapshots.values
            .filter { it.tenantId == tenantId && it.projectId == projectId && it.productId == productId }
            .maxByOrNull { it.generatedAt }
    }

    override suspend fun listSnapshots(tenantId: String, projectId: String, filter: ProductProfitabilityFilter): List<ProductProfitabilitySnapshot> {
        return snapshots.values
            .filter { it.tenantId == tenantId && it.projectId == projectId }
            .filter { filter.productId == null || it.productId == filter.productId }
            .filter { filter.sku == null || it.sku == filter.sku }
            .filter { filter.editionId == null || it.editionId == filter.editionId }
            .filter { filter.versionId == null || it.versionId == filter.versionId }
            .filter { filter.customerId == null || it.customerId == filter.customerId }
            .filter { filter.periodId == null || it.periodId == filter.periodId }
            .filter { filter.classification == null || it.profitabilityClassification == filter.classification }
            .filter { filter.varianceClassification == null || it.varianceClassification == filter.varianceClassification }
            .filter { filter.minMargin == null || (it.grossMarginPercentage != null && it.grossMarginPercentage >= filter.minMargin) }
            .filter { filter.maxMargin == null || (it.grossMarginPercentage != null && it.grossMarginPercentage <= filter.maxMargin) }
            .sortedByDescending { it.generatedAt }
            .drop(filter.offset)
            .take(filter.limit)
    }

    override suspend fun saveRevenueAttributions(attributions: List<ProductRevenueAttribution>) {
        attributions.forEach { rev ->
            val key = "${rev.tenantId}:${rev.projectId}:${rev.productId}"
            revenueAttributions.computeIfAbsent(key) { mutableListOf() }.add(rev)
        }
    }

    override suspend fun getRevenueAttributions(tenantId: String, projectId: String, productId: String): List<ProductRevenueAttribution> {
        val key = "$tenantId:$projectId:$productId"
        return revenueAttributions[key]?.toList() ?: emptyList()
    }

    override suspend fun saveCostAttributions(attributions: List<ProductCostAttribution>) {
        attributions.forEach { cost ->
            val key = "${cost.tenantId}:${cost.projectId}:${cost.productId}"
            costAttributions.computeIfAbsent(key) { mutableListOf() }.add(cost)
        }
    }

    override suspend fun getCostAttributions(tenantId: String, projectId: String, productId: String): List<ProductCostAttribution> {
        val key = "$tenantId:$projectId:$productId"
        return costAttributions[key]?.toList() ?: emptyList()
    }

    override suspend fun saveReconciliationEvent(event: ProductProfitabilityReconciliationEvent) {
        val key = "${event.tenantId}:${event.projectId}:${event.productId}"
        reconciliationEvents.computeIfAbsent(key) { mutableListOf() }.add(event)
    }

    override suspend fun getReconciliationEvents(tenantId: String, projectId: String, productId: String): List<ProductProfitabilityReconciliationEvent> {
        val key = "$tenantId:$projectId:$productId"
        return reconciliationEvents[key]?.toList() ?: emptyList()
    }

    override suspend fun recordAuditEvent(event: ProductProfitabilityAuditEvent) {
        val key = "${event.tenantId}:${event.projectId}:${event.productId}"
        auditEvents.computeIfAbsent(key) { mutableListOf() }.add(event)
    }

    override suspend fun getAuditEvents(tenantId: String, projectId: String, productId: String): List<ProductProfitabilityAuditEvent> {
        val key = "$tenantId:$projectId:$productId"
        return auditEvents[key]?.toList() ?: emptyList()
    }

    fun clear() {
        snapshots.clear()
        revenueAttributions.clear()
        costAttributions.clear()
        reconciliationEvents.clear()
        auditEvents.clear()
    }
}
