package com.sucharu.sucharupro.domain.repository

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.vendor.*

/**
 * Repository interface for Vendor Settlement operations (Module 12 Step 10).
 */
interface VendorSettlementRepository {

    suspend fun createSettlement(settlement: VendorSettlement): DomainResult<VendorSettlement>
    suspend fun getSettlementById(settlementId: String, tenantId: String): DomainResult<VendorSettlement?>
    suspend fun getSettlementByNumber(settlementNumber: String, tenantId: String): DomainResult<VendorSettlement?>
    suspend fun updateSettlement(settlement: VendorSettlement): DomainResult<VendorSettlement>
    suspend fun listSettlements(
        vendorId: String?,
        status: VendorSettlementStatus?,
        projectId: String?,
        tenantId: String
    ): DomainResult<List<VendorSettlement>>

    suspend fun recordReconciliationResult(result: VendorReconciliationResult): DomainResult<VendorReconciliationResult>
    suspend fun listReconciliationResults(
        vendorId: String?,
        status: ReconciliationStatus?,
        tenantId: String
    ): DomainResult<List<VendorReconciliationResult>>

    suspend fun saveAnalyticsSnapshot(snapshot: VendorAnalyticsSnapshot): DomainResult<VendorAnalyticsSnapshot>
    suspend fun listAnalyticsSnapshots(
        vendorId: String,
        period: AnalyticsPeriod?,
        tenantId: String
    ): DomainResult<List<VendorAnalyticsSnapshot>>

    suspend fun appendAuditEvent(event: VendorSettlementAuditEvent): DomainResult<VendorSettlementAuditEvent>
    suspend fun listAuditEvents(settlementId: String?, vendorId: String?, tenantId: String): DomainResult<List<VendorSettlementAuditEvent>>
}
