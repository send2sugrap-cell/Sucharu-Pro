package com.sucharu.sucharupro.data.repository.profitability

import com.sucharu.sucharupro.data.datasource.profitability.VendorProfitabilityDataSource
import com.sucharu.sucharupro.domain.model.profitability.*

/**
 * Production implementation of VendorProfitabilityRepository.
 * Module 16 Step 05.
 */
class VendorProfitabilityRepositoryImpl(
    private val dataSource: VendorProfitabilityDataSource
) : VendorProfitabilityRepository {

    override suspend fun saveSnapshot(snapshot: VendorProfitabilitySnapshot): VendorProfitabilitySnapshot {
        return dataSource.saveSnapshot(snapshot)
    }

    override suspend fun findSnapshotById(tenantId: String, snapshotId: String): VendorProfitabilitySnapshot? {
        return dataSource.findSnapshotById(tenantId, snapshotId)
    }

    override suspend fun findLatestSnapshotByVendorId(tenantId: String, vendorId: String): VendorProfitabilitySnapshot? {
        return dataSource.findLatestSnapshotByVendorId(tenantId, vendorId)
    }

    override suspend fun listSnapshots(tenantId: String, filter: VendorProfitabilityFilter): List<VendorProfitabilitySnapshot> {
        return dataSource.listSnapshots(tenantId, filter)
    }

    override suspend fun saveCostAttributions(attributions: List<VendorCostAttribution>) {
        dataSource.saveCostAttributions(attributions)
    }

    override suspend fun listCostAttributionsByVendorId(tenantId: String, vendorId: String): List<VendorCostAttribution> {
        return dataSource.listCostAttributionsByVendorId(tenantId, vendorId)
    }

    override suspend fun saveRevenueContextAttributions(attributions: List<VendorRevenueContextAttribution>) {
        dataSource.saveRevenueContextAttributions(attributions)
    }

    override suspend fun listRevenueContextByVendorId(tenantId: String, vendorId: String): List<VendorRevenueContextAttribution> {
        return dataSource.listRevenueContextByVendorId(tenantId, vendorId)
    }

    override suspend fun saveReconciliationEvent(event: VendorProfitabilityReconciliationEvent): VendorProfitabilityReconciliationEvent {
        return dataSource.saveReconciliationEvent(event)
    }

    override suspend fun listReconciliationEventsByVendorId(tenantId: String, vendorId: String): List<VendorProfitabilityReconciliationEvent> {
        return dataSource.listReconciliationEventsByVendorId(tenantId, vendorId)
    }

    override suspend fun saveAuditEvent(event: VendorProfitabilityAuditEvent): VendorProfitabilityAuditEvent {
        return dataSource.saveAuditEvent(event)
    }

    override suspend fun listAuditEventsByVendorId(tenantId: String, vendorId: String): List<VendorProfitabilityAuditEvent> {
        return dataSource.listAuditEventsByVendorId(tenantId, vendorId)
    }

    override suspend fun saveUnattributedItems(items: List<VendorUnattributedItem>) {
        dataSource.saveUnattributedItems(items)
    }

    override suspend fun listUnattributedItems(tenantId: String, vendorId: String?): List<VendorUnattributedItem> {
        return dataSource.listUnattributedItems(tenantId, vendorId)
    }
}
