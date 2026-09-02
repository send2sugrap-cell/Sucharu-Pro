package com.sucharu.sucharupro.data.datasource.profitability

import com.sucharu.sucharupro.domain.model.profitability.*

/**
 * Data Source interface for Vendor Profitability and Supplier Economics.
 * Module 16 Step 05.
 */
interface VendorProfitabilityDataSource {

    suspend fun saveSnapshot(snapshot: VendorProfitabilitySnapshot): VendorProfitabilitySnapshot

    suspend fun findSnapshotById(tenantId: String, snapshotId: String): VendorProfitabilitySnapshot?

    suspend fun findLatestSnapshotByVendorId(tenantId: String, vendorId: String): VendorProfitabilitySnapshot?

    suspend fun listSnapshots(tenantId: String, filter: VendorProfitabilityFilter): List<VendorProfitabilitySnapshot>

    suspend fun saveCostAttributions(attributions: List<VendorCostAttribution>)

    suspend fun listCostAttributionsByVendorId(tenantId: String, vendorId: String): List<VendorCostAttribution>

    suspend fun saveRevenueContextAttributions(attributions: List<VendorRevenueContextAttribution>)

    suspend fun listRevenueContextByVendorId(tenantId: String, vendorId: String): List<VendorRevenueContextAttribution>

    suspend fun saveReconciliationEvent(event: VendorProfitabilityReconciliationEvent): VendorProfitabilityReconciliationEvent

    suspend fun listReconciliationEventsByVendorId(tenantId: String, vendorId: String): List<VendorProfitabilityReconciliationEvent>

    suspend fun saveAuditEvent(event: VendorProfitabilityAuditEvent): VendorProfitabilityAuditEvent

    suspend fun listAuditEventsByVendorId(tenantId: String, vendorId: String): List<VendorProfitabilityAuditEvent>

    suspend fun saveUnattributedItems(items: List<VendorUnattributedItem>)

    suspend fun listUnattributedItems(tenantId: String, vendorId: String? = null): List<VendorUnattributedItem>
}
