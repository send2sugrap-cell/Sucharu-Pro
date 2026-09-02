package com.sucharu.sucharupro.data.repository.profitability

import com.sucharu.sucharupro.data.datasource.profitability.ProductProfitabilityDataSource
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.profitability.*

/**
 * Production implementation of ProductProfitabilityRepository.
 */
class ProductProfitabilityRepositoryImpl(
    private val dataSource: ProductProfitabilityDataSource
) : ProductProfitabilityRepository {

    override suspend fun saveSnapshot(snapshot: ProductProfitabilitySnapshot): DomainResult<ProductProfitabilitySnapshot> {
        return try {
            dataSource.saveSnapshot(snapshot)
            DomainResult.Success(snapshot)
        } catch (e: Exception) {
            DomainResult.Error(message = "Failed to save product profitability snapshot: ${e.message}")
        }
    }

    override suspend fun getSnapshotById(tenantId: String, projectId: String, snapshotId: String): DomainResult<ProductProfitabilitySnapshot> {
        return try {
            val snap = dataSource.getSnapshotById(tenantId, projectId, snapshotId)
            if (snap != null) {
                DomainResult.Success(snap)
            } else {
                DomainResult.Error(message = "Product profitability snapshot '$snapshotId' not found.")
            }
        } catch (e: Exception) {
            DomainResult.Error(message = "Failed to retrieve snapshot: ${e.message}")
        }
    }

    override suspend fun getLatestSnapshotByProduct(tenantId: String, projectId: String, productId: String): DomainResult<ProductProfitabilitySnapshot?> {
        return try {
            val snap = dataSource.getLatestSnapshotByProduct(tenantId, projectId, productId)
            DomainResult.Success(snap)
        } catch (e: Exception) {
            DomainResult.Error(message = "Failed to retrieve latest product profitability snapshot: ${e.message}")
        }
    }

    override suspend fun listSnapshots(tenantId: String, projectId: String, filter: ProductProfitabilityFilter): DomainResult<List<ProductProfitabilitySnapshot>> {
        return try {
            val list = dataSource.listSnapshots(tenantId, projectId, filter)
            DomainResult.Success(list)
        } catch (e: Exception) {
            DomainResult.Error(message = "Failed to list product profitability snapshots: ${e.message}")
        }
    }

    override suspend fun saveRevenueAttributions(attributions: List<ProductRevenueAttribution>): DomainResult<Unit> {
        return try {
            dataSource.saveRevenueAttributions(attributions)
            DomainResult.Success(Unit)
        } catch (e: Exception) {
            DomainResult.Error(message = "Failed to save revenue attributions: ${e.message}")
        }
    }

    override suspend fun getRevenueAttributions(tenantId: String, projectId: String, productId: String): DomainResult<List<ProductRevenueAttribution>> {
        return try {
            val list = dataSource.getRevenueAttributions(tenantId, projectId, productId)
            DomainResult.Success(list)
        } catch (e: Exception) {
            DomainResult.Error(message = "Failed to get revenue attributions: ${e.message}")
        }
    }

    override suspend fun saveCostAttributions(attributions: List<ProductCostAttribution>): DomainResult<Unit> {
        return try {
            dataSource.saveCostAttributions(attributions)
            DomainResult.Success(Unit)
        } catch (e: Exception) {
            DomainResult.Error(message = "Failed to save cost attributions: ${e.message}")
        }
    }

    override suspend fun getCostAttributions(tenantId: String, projectId: String, productId: String): DomainResult<List<ProductCostAttribution>> {
        return try {
            val list = dataSource.getCostAttributions(tenantId, projectId, productId)
            DomainResult.Success(list)
        } catch (e: Exception) {
            DomainResult.Error(message = "Failed to get cost attributions: ${e.message}")
        }
    }

    override suspend fun saveReconciliationEvent(event: ProductProfitabilityReconciliationEvent): DomainResult<ProductProfitabilityReconciliationEvent> {
        return try {
            dataSource.saveReconciliationEvent(event)
            DomainResult.Success(event)
        } catch (e: Exception) {
            DomainResult.Error(message = "Failed to save reconciliation event: ${e.message}")
        }
    }

    override suspend fun getReconciliationEvents(tenantId: String, projectId: String, productId: String): DomainResult<List<ProductProfitabilityReconciliationEvent>> {
        return try {
            val list = dataSource.getReconciliationEvents(tenantId, projectId, productId)
            DomainResult.Success(list)
        } catch (e: Exception) {
            DomainResult.Error(message = "Failed to get reconciliation events: ${e.message}")
        }
    }

    override suspend fun recordAuditEvent(event: ProductProfitabilityAuditEvent): DomainResult<Unit> {
        return try {
            dataSource.recordAuditEvent(event)
            DomainResult.Success(Unit)
        } catch (e: Exception) {
            DomainResult.Error(message = "Failed to record audit event: ${e.message}")
        }
    }

    override suspend fun getAuditEvents(tenantId: String, projectId: String, productId: String): DomainResult<List<ProductProfitabilityAuditEvent>> {
        return try {
            val list = dataSource.getAuditEvents(tenantId, projectId, productId)
            DomainResult.Success(list)
        } catch (e: Exception) {
            DomainResult.Error(message = "Failed to get audit events: ${e.message}")
        }
    }
}
