package com.sucharu.sucharupro.data.repository.profitability

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.profitability.*

/**
 * Repository interface for Customer Profitability & Contribution Analysis (Module 16 Step 04).
 */
interface CustomerProfitabilityRepository {

    suspend fun saveSnapshot(snapshot: CustomerProfitabilitySnapshot): DomainResult<CustomerProfitabilitySnapshot>

    suspend fun getSnapshotById(tenantId: String, projectId: String, snapshotId: String): DomainResult<CustomerProfitabilitySnapshot>

    suspend fun getLatestSnapshotByCustomer(tenantId: String, projectId: String, customerId: String): DomainResult<CustomerProfitabilitySnapshot?>

    suspend fun listSnapshots(tenantId: String, projectId: String, filter: CustomerProfitabilityFilter): DomainResult<List<CustomerProfitabilitySnapshot>>

    suspend fun saveRevenueAttributions(attributions: List<CustomerRevenueAttribution>): DomainResult<Unit>

    suspend fun getRevenueAttributions(tenantId: String, projectId: String, customerId: String): DomainResult<List<CustomerRevenueAttribution>>

    suspend fun saveCostAttributions(attributions: List<CustomerCostAttribution>): DomainResult<Unit>

    suspend fun getCostAttributions(tenantId: String, projectId: String, customerId: String): DomainResult<List<CustomerCostAttribution>>

    suspend fun saveReconciliationEvent(event: CustomerProfitabilityReconciliationEvent): DomainResult<CustomerProfitabilityReconciliationEvent>

    suspend fun getReconciliationEvents(tenantId: String, projectId: String, customerId: String): DomainResult<List<CustomerProfitabilityReconciliationEvent>>

    suspend fun recordAuditEvent(event: CustomerProfitabilityAuditEvent): DomainResult<Unit>

    suspend fun getAuditEvents(tenantId: String, projectId: String, customerId: String): DomainResult<List<CustomerProfitabilityAuditEvent>>

    suspend fun saveUnattributedItems(items: List<UnattributedProfitabilityItem>): DomainResult<Unit>

    suspend fun getUnattributedItems(tenantId: String, projectId: String): DomainResult<List<UnattributedProfitabilityItem>>
}
