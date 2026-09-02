package com.sucharu.sucharupro.data.datasource.profitability

import com.sucharu.sucharupro.domain.model.profitability.*

/**
 * Underlying DataSource interface for Customer Profitability persistence.
 */
interface CustomerProfitabilityDataSource {

    suspend fun saveSnapshot(snapshot: CustomerProfitabilitySnapshot)

    suspend fun getSnapshotById(tenantId: String, projectId: String, snapshotId: String): CustomerProfitabilitySnapshot?

    suspend fun getLatestSnapshotByCustomer(tenantId: String, projectId: String, customerId: String): CustomerProfitabilitySnapshot?

    suspend fun listSnapshots(tenantId: String, projectId: String, filter: CustomerProfitabilityFilter): List<CustomerProfitabilitySnapshot>

    suspend fun saveRevenueAttributions(attributions: List<CustomerRevenueAttribution>)

    suspend fun getRevenueAttributions(tenantId: String, projectId: String, customerId: String): List<CustomerRevenueAttribution>

    suspend fun saveCostAttributions(attributions: List<CustomerCostAttribution>)

    suspend fun getCostAttributions(tenantId: String, projectId: String, customerId: String): List<CustomerCostAttribution>

    suspend fun saveReconciliationEvent(event: CustomerProfitabilityReconciliationEvent)

    suspend fun getReconciliationEvents(tenantId: String, projectId: String, customerId: String): List<CustomerProfitabilityReconciliationEvent>

    suspend fun recordAuditEvent(event: CustomerProfitabilityAuditEvent)

    suspend fun getAuditEvents(tenantId: String, projectId: String, customerId: String): List<CustomerProfitabilityAuditEvent>

    suspend fun saveUnattributedItems(items: List<UnattributedProfitabilityItem>)

    suspend fun getUnattributedItems(tenantId: String, projectId: String): List<UnattributedProfitabilityItem>
}
