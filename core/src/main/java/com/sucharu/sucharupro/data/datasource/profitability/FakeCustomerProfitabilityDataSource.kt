package com.sucharu.sucharupro.data.datasource.profitability

import com.sucharu.sucharupro.domain.model.profitability.*
import java.util.concurrent.ConcurrentHashMap

/**
 * In-memory thread-safe fake datasource for deterministic testing of Customer Profitability.
 */
class FakeCustomerProfitabilityDataSource : CustomerProfitabilityDataSource {

    private val snapshots = ConcurrentHashMap<String, CustomerProfitabilitySnapshot>() // tenantId:projectId:snapshotId
    private val revenueAttributions = ConcurrentHashMap<String, MutableList<CustomerRevenueAttribution>>() // tenantId:projectId:customerId
    private val costAttributions = ConcurrentHashMap<String, MutableList<CustomerCostAttribution>>() // tenantId:projectId:customerId
    private val reconciliationEvents = ConcurrentHashMap<String, MutableList<CustomerProfitabilityReconciliationEvent>>() // tenantId:projectId:customerId
    private val auditEvents = ConcurrentHashMap<String, MutableList<CustomerProfitabilityAuditEvent>>() // tenantId:projectId:customerId
    private val unattributedItems = ConcurrentHashMap<String, MutableList<UnattributedProfitabilityItem>>() // tenantId:projectId

    override suspend fun saveSnapshot(snapshot: CustomerProfitabilitySnapshot) {
        val key = "${snapshot.tenantId}:${snapshot.projectId}:${snapshot.snapshotId}"
        snapshots[key] = snapshot
    }

    override suspend fun getSnapshotById(tenantId: String, projectId: String, snapshotId: String): CustomerProfitabilitySnapshot? {
        val key = "$tenantId:$projectId:$snapshotId"
        return snapshots[key]
    }

    override suspend fun getLatestSnapshotByCustomer(tenantId: String, projectId: String, customerId: String): CustomerProfitabilitySnapshot? {
        return snapshots.values
            .filter { it.tenantId == tenantId && it.projectId == projectId && it.customerId == customerId }
            .maxByOrNull { it.generatedAt }
    }

    override suspend fun listSnapshots(tenantId: String, projectId: String, filter: CustomerProfitabilityFilter): List<CustomerProfitabilitySnapshot> {
        return snapshots.values
            .filter { it.tenantId == tenantId && it.projectId == projectId }
            .filter { filter.customerId == null || it.customerId == filter.customerId }
            .filter { filter.periodType == null || it.periodType == filter.periodType }
            .filter { filter.classification == null || it.profitabilityClassification == filter.classification }
            .filter { filter.isLossMaking == null || it.isLossMaking == filter.isLossMaking }
            .filter { filter.isLowMargin == null || it.isLowMargin == filter.isLowMargin }
            .filter { filter.minMargin == null || (it.grossMarginPercentage != null && it.grossMarginPercentage >= filter.minMargin) }
            .filter { filter.maxMargin == null || (it.grossMarginPercentage != null && it.grossMarginPercentage <= filter.maxMargin) }
            .sortedByDescending { it.generatedAt }
            .drop(filter.offset)
            .take(filter.limit)
    }

    override suspend fun saveRevenueAttributions(attributions: List<CustomerRevenueAttribution>) {
        attributions.forEach { rev ->
            val key = "${rev.tenantId}:${rev.projectId}:${rev.customerId}"
            revenueAttributions.computeIfAbsent(key) { mutableListOf() }.add(rev)
        }
    }

    override suspend fun getRevenueAttributions(tenantId: String, projectId: String, customerId: String): List<CustomerRevenueAttribution> {
        val key = "$tenantId:$projectId:$customerId"
        return revenueAttributions[key]?.toList() ?: emptyList()
    }

    override suspend fun saveCostAttributions(attributions: List<CustomerCostAttribution>) {
        attributions.forEach { cost ->
            val key = "${cost.tenantId}:${cost.projectId}:${cost.customerId}"
            costAttributions.computeIfAbsent(key) { mutableListOf() }.add(cost)
        }
    }

    override suspend fun getCostAttributions(tenantId: String, projectId: String, customerId: String): List<CustomerCostAttribution> {
        val key = "$tenantId:$projectId:$customerId"
        return costAttributions[key]?.toList() ?: emptyList()
    }

    override suspend fun saveReconciliationEvent(event: CustomerProfitabilityReconciliationEvent) {
        val key = "${event.tenantId}:${event.projectId}:${event.customerId}"
        reconciliationEvents.computeIfAbsent(key) { mutableListOf() }.add(event)
    }

    override suspend fun getReconciliationEvents(tenantId: String, projectId: String, customerId: String): List<CustomerProfitabilityReconciliationEvent> {
        val key = "$tenantId:$projectId:$customerId"
        return reconciliationEvents[key]?.toList() ?: emptyList()
    }

    override suspend fun recordAuditEvent(event: CustomerProfitabilityAuditEvent) {
        val key = "${event.tenantId}:${event.projectId}:${event.customerId}"
        auditEvents.computeIfAbsent(key) { mutableListOf() }.add(event)
    }

    override suspend fun getAuditEvents(tenantId: String, projectId: String, customerId: String): List<CustomerProfitabilityAuditEvent> {
        val key = "$tenantId:$projectId:$customerId"
        return auditEvents[key]?.toList() ?: emptyList()
    }

    override suspend fun saveUnattributedItems(items: List<UnattributedProfitabilityItem>) {
        items.forEach { item ->
            val key = "${item.tenantId}:${item.projectId}"
            unattributedItems.computeIfAbsent(key) { mutableListOf() }.add(item)
        }
    }

    override suspend fun getUnattributedItems(tenantId: String, projectId: String): List<UnattributedProfitabilityItem> {
        val key = "$tenantId:$projectId"
        return unattributedItems[key]?.toList() ?: emptyList()
    }

    fun clear() {
        snapshots.clear()
        revenueAttributions.clear()
        costAttributions.clear()
        reconciliationEvents.clear()
        auditEvents.clear()
        unattributedItems.clear()
    }
}
