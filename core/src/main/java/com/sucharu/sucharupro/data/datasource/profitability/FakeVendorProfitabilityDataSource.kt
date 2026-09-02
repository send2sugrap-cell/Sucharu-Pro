package com.sucharu.sucharupro.data.datasource.profitability

import com.sucharu.sucharupro.domain.model.profitability.*
import java.util.concurrent.ConcurrentHashMap

/**
 * In-memory fake data source for fast unit testing of Vendor Profitability.
 * Module 16 Step 05.
 */
class FakeVendorProfitabilityDataSource : VendorProfitabilityDataSource {

    private val snapshots = ConcurrentHashMap<String, VendorProfitabilitySnapshot>()
    private val costAttributions = ConcurrentHashMap<String, MutableList<VendorCostAttribution>>()
    private val revenueContexts = ConcurrentHashMap<String, MutableList<VendorRevenueContextAttribution>>()
    private val reconciliationEvents = ConcurrentHashMap<String, MutableList<VendorProfitabilityReconciliationEvent>>()
    private val auditEvents = ConcurrentHashMap<String, MutableList<VendorProfitabilityAuditEvent>>()
    private val unattributedItems = ConcurrentHashMap<String, MutableList<VendorUnattributedItem>>()

    override suspend fun saveSnapshot(snapshot: VendorProfitabilitySnapshot): VendorProfitabilitySnapshot {
        val key = "${snapshot.tenantId}:${snapshot.snapshotId}"
        snapshots[key] = snapshot
        return snapshot
    }

    override suspend fun findSnapshotById(tenantId: String, snapshotId: String): VendorProfitabilitySnapshot? {
        val key = "$tenantId:$snapshotId"
        return snapshots[key]
    }

    override suspend fun findLatestSnapshotByVendorId(tenantId: String, vendorId: String): VendorProfitabilitySnapshot? {
        return snapshots.values
            .filter { it.tenantId == tenantId && it.vendorId == vendorId }
            .maxByOrNull { it.generatedAt }
    }

    override suspend fun listSnapshots(tenantId: String, filter: VendorProfitabilityFilter): List<VendorProfitabilitySnapshot> {
        return snapshots.values
            .filter { it.tenantId == tenantId }
            .filter { filter.vendorId == null || it.vendorId == filter.vendorId }
            .filter { filter.serviceCategory == null || it.serviceCategory == filter.serviceCategory }
            .filter { filter.periodId == null || it.periodId == filter.periodId }
            .filter { filter.riskClassification == null || it.riskClassification == filter.riskClassification }
            .filter { filter.dependencyClassification == null || it.dependencyClassification == filter.dependencyClassification }
            .filter { filter.isHighRisk == null || (!filter.isHighRisk || it.riskClassification == VendorRiskClassification.HIGH_RISK || it.riskClassification == VendorRiskClassification.CRITICAL_RISK) }
            .filter { filter.isOverBudget == null || (!filter.isOverBudget || (it.costVariancePercentage != null && it.costVariancePercentage > java.math.BigDecimal.ZERO)) }
            .filter { filter.minSpend == null || it.totalVendorCost >= filter.minSpend }
            .filter { filter.maxSpend == null || it.totalVendorCost <= filter.maxSpend }
            .sortedByDescending { it.generatedAt }
            .drop(filter.offset)
            .take(filter.limit)
    }

    override suspend fun saveCostAttributions(attributions: List<VendorCostAttribution>) {
        for (attr in attributions) {
            val key = "${attr.tenantId}:${attr.vendorId}"
            costAttributions.computeIfAbsent(key) { mutableListOf() }.add(attr)
        }
    }

    override suspend fun listCostAttributionsByVendorId(tenantId: String, vendorId: String): List<VendorCostAttribution> {
        val key = "$tenantId:$vendorId"
        return costAttributions[key]?.toList() ?: emptyList()
    }

    override suspend fun saveRevenueContextAttributions(attributions: List<VendorRevenueContextAttribution>) {
        for (attr in attributions) {
            val key = "${attr.tenantId}:${attr.vendorId}"
            revenueContexts.computeIfAbsent(key) { mutableListOf() }.add(attr)
        }
    }

    override suspend fun listRevenueContextByVendorId(tenantId: String, vendorId: String): List<VendorRevenueContextAttribution> {
        val key = "$tenantId:$vendorId"
        return revenueContexts[key]?.toList() ?: emptyList()
    }

    override suspend fun saveReconciliationEvent(event: VendorProfitabilityReconciliationEvent): VendorProfitabilityReconciliationEvent {
        val key = "${event.tenantId}:${event.vendorId}"
        reconciliationEvents.computeIfAbsent(key) { mutableListOf() }.add(event)
        return event
    }

    override suspend fun listReconciliationEventsByVendorId(tenantId: String, vendorId: String): List<VendorProfitabilityReconciliationEvent> {
        val key = "$tenantId:$vendorId"
        return reconciliationEvents[key]?.toList() ?: emptyList()
    }

    override suspend fun saveAuditEvent(event: VendorProfitabilityAuditEvent): VendorProfitabilityAuditEvent {
        val key = "${event.tenantId}:${event.vendorId}"
        auditEvents.computeIfAbsent(key) { mutableListOf() }.add(event)
        return event
    }

    override suspend fun listAuditEventsByVendorId(tenantId: String, vendorId: String): List<VendorProfitabilityAuditEvent> {
        val key = "$tenantId:$vendorId"
        return auditEvents[key]?.toList() ?: emptyList()
    }

    override suspend fun saveUnattributedItems(items: List<VendorUnattributedItem>) {
        for (item in items) {
            val key = "${item.tenantId}:${item.vendorId}"
            unattributedItems.computeIfAbsent(key) { mutableListOf() }.add(item)
        }
    }

    override suspend fun listUnattributedItems(tenantId: String, vendorId: String?): List<VendorUnattributedItem> {
        return if (vendorId != null) {
            unattributedItems["$tenantId:$vendorId"]?.toList() ?: emptyList()
        } else {
            unattributedItems.entries
                .filter { it.key.startsWith("$tenantId:") }
                .flatMap { it.value }
        }
    }
}
