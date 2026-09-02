package com.sucharu.sucharupro.data.repository.profitability

import com.sucharu.sucharupro.data.datasource.profitability.CustomerProfitabilityDataSource
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.profitability.*

/**
 * Production implementation of CustomerProfitabilityRepository.
 */
class CustomerProfitabilityRepositoryImpl(
    private val dataSource: CustomerProfitabilityDataSource
) : CustomerProfitabilityRepository {

    override suspend fun saveSnapshot(snapshot: CustomerProfitabilitySnapshot): DomainResult<CustomerProfitabilitySnapshot> {
        return try {
            dataSource.saveSnapshot(snapshot)
            DomainResult.Success(snapshot)
        } catch (e: Exception) {
            DomainResult.Error(message = "Failed to save customer profitability snapshot: ${e.message}")
        }
    }

    override suspend fun getSnapshotById(tenantId: String, projectId: String, snapshotId: String): DomainResult<CustomerProfitabilitySnapshot> {
        return try {
            val snap = dataSource.getSnapshotById(tenantId, projectId, snapshotId)
            if (snap != null) {
                DomainResult.Success(snap)
            } else {
                DomainResult.Error(message = "Customer profitability snapshot '$snapshotId' not found.")
            }
        } catch (e: Exception) {
            DomainResult.Error(message = "Failed to retrieve snapshot: ${e.message}")
        }
    }

    override suspend fun getLatestSnapshotByCustomer(tenantId: String, projectId: String, customerId: String): DomainResult<CustomerProfitabilitySnapshot?> {
        return try {
            val snap = dataSource.getLatestSnapshotByCustomer(tenantId, projectId, customerId)
            DomainResult.Success(snap)
        } catch (e: Exception) {
            DomainResult.Error(message = "Failed to retrieve latest customer snapshot: ${e.message}")
        }
    }

    override suspend fun listSnapshots(tenantId: String, projectId: String, filter: CustomerProfitabilityFilter): DomainResult<List<CustomerProfitabilitySnapshot>> {
        return try {
            val list = dataSource.listSnapshots(tenantId, projectId, filter)
            DomainResult.Success(list)
        } catch (e: Exception) {
            DomainResult.Error(message = "Failed to list customer profitability snapshots: ${e.message}")
        }
    }

    override suspend fun saveRevenueAttributions(attributions: List<CustomerRevenueAttribution>): DomainResult<Unit> {
        return try {
            dataSource.saveRevenueAttributions(attributions)
            DomainResult.Success(Unit)
        } catch (e: Exception) {
            DomainResult.Error(message = "Failed to save customer revenue attributions: ${e.message}")
        }
    }

    override suspend fun getRevenueAttributions(tenantId: String, projectId: String, customerId: String): DomainResult<List<CustomerRevenueAttribution>> {
        return try {
            val list = dataSource.getRevenueAttributions(tenantId, projectId, customerId)
            DomainResult.Success(list)
        } catch (e: Exception) {
            DomainResult.Error(message = "Failed to get customer revenue attributions: ${e.message}")
        }
    }

    override suspend fun saveCostAttributions(attributions: List<CustomerCostAttribution>): DomainResult<Unit> {
        return try {
            dataSource.saveCostAttributions(attributions)
            DomainResult.Success(Unit)
        } catch (e: Exception) {
            DomainResult.Error(message = "Failed to save customer cost attributions: ${e.message}")
        }
    }

    override suspend fun getCostAttributions(tenantId: String, projectId: String, customerId: String): DomainResult<List<CustomerCostAttribution>> {
        return try {
            val list = dataSource.getCostAttributions(tenantId, projectId, customerId)
            DomainResult.Success(list)
        } catch (e: Exception) {
            DomainResult.Error(message = "Failed to get customer cost attributions: ${e.message}")
        }
    }

    override suspend fun saveReconciliationEvent(event: CustomerProfitabilityReconciliationEvent): DomainResult<CustomerProfitabilityReconciliationEvent> {
        return try {
            dataSource.saveReconciliationEvent(event)
            DomainResult.Success(event)
        } catch (e: Exception) {
            DomainResult.Error(message = "Failed to save customer reconciliation event: ${e.message}")
        }
    }

    override suspend fun getReconciliationEvents(tenantId: String, projectId: String, customerId: String): DomainResult<List<CustomerProfitabilityReconciliationEvent>> {
        return try {
            val list = dataSource.getReconciliationEvents(tenantId, projectId, customerId)
            DomainResult.Success(list)
        } catch (e: Exception) {
            DomainResult.Error(message = "Failed to get customer reconciliation events: ${e.message}")
        }
    }

    override suspend fun recordAuditEvent(event: CustomerProfitabilityAuditEvent): DomainResult<Unit> {
        return try {
            dataSource.recordAuditEvent(event)
            DomainResult.Success(Unit)
        } catch (e: Exception) {
            DomainResult.Error(message = "Failed to record customer audit event: ${e.message}")
        }
    }

    override suspend fun getAuditEvents(tenantId: String, projectId: String, customerId: String): DomainResult<List<CustomerProfitabilityAuditEvent>> {
        return try {
            val list = dataSource.getAuditEvents(tenantId, projectId, customerId)
            DomainResult.Success(list)
        } catch (e: Exception) {
            DomainResult.Error(message = "Failed to get customer audit events: ${e.message}")
        }
    }

    override suspend fun saveUnattributedItems(items: List<UnattributedProfitabilityItem>): DomainResult<Unit> {
        return try {
            dataSource.saveUnattributedItems(items)
            DomainResult.Success(Unit)
        } catch (e: Exception) {
            DomainResult.Error(message = "Failed to save unattributed items: ${e.message}")
        }
    }

    override suspend fun getUnattributedItems(tenantId: String, projectId: String): DomainResult<List<UnattributedProfitabilityItem>> {
        return try {
            val list = dataSource.getUnattributedItems(tenantId, projectId)
            DomainResult.Success(list)
        } catch (e: Exception) {
            DomainResult.Error(message = "Failed to get unattributed items: ${e.message}")
        }
    }
}
