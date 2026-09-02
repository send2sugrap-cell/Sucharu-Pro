package com.sucharu.sucharupro.data.datasource

import com.sucharu.sucharupro.domain.model.vendor.*

/**
 * Data source contract for Vendor Settlement & Analytics (Module 12 Step 10).
 */
interface VendorSettlementDataSource {

    suspend fun insertSettlement(settlement: VendorSettlement): VendorSettlement
    suspend fun findSettlementById(settlementId: String, tenantId: String): VendorSettlement?
    suspend fun findSettlementByNumber(settlementNumber: String, tenantId: String): VendorSettlement?
    suspend fun updateSettlement(settlement: VendorSettlement): VendorSettlement
    suspend fun listSettlements(
        vendorId: String?,
        status: VendorSettlementStatus?,
        projectId: String?,
        tenantId: String
    ): List<VendorSettlement>

    suspend fun insertReconciliationResult(result: VendorReconciliationResult): VendorReconciliationResult
    suspend fun listReconciliationResults(
        vendorId: String?,
        status: ReconciliationStatus?,
        tenantId: String
    ): List<VendorReconciliationResult>

    suspend fun insertAnalyticsSnapshot(snapshot: VendorAnalyticsSnapshot): VendorAnalyticsSnapshot
    suspend fun listAnalyticsSnapshots(
        vendorId: String,
        period: AnalyticsPeriod?,
        tenantId: String
    ): List<VendorAnalyticsSnapshot>

    suspend fun appendAuditEvent(event: VendorSettlementAuditEvent): VendorSettlementAuditEvent
    suspend fun listAuditEvents(settlementId: String?, vendorId: String?, tenantId: String): List<VendorSettlementAuditEvent>
}
