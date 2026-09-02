package com.sucharu.sucharupro.data.datasource

import com.sucharu.sucharupro.domain.model.vendor.*
import java.util.concurrent.ConcurrentHashMap

/**
 * Thread-safe In-Memory Data Source for Vendor Settlement & Analytics (Module 12 Step 10).
 */
class FakeVendorSettlementDataSource : VendorSettlementDataSource {

    private val settlements = ConcurrentHashMap<String, VendorSettlement>()
    private val reconciliationResults = ConcurrentHashMap<String, VendorReconciliationResult>()
    private val analyticsSnapshots = ConcurrentHashMap<String, VendorAnalyticsSnapshot>()
    private val auditEvents = mutableListOf<VendorSettlementAuditEvent>()

    override suspend fun insertSettlement(settlement: VendorSettlement): VendorSettlement = synchronized(this) {
        if (settlements.values.any { it.tenantId == settlement.tenantId && it.settlementNumber == settlement.settlementNumber }) {
            throw IllegalArgumentException("Settlement number '${settlement.settlementNumber}' already exists for tenant '${settlement.tenantId}'")
        }
        settlements[settlement.settlementId] = settlement
        settlement
    }

    override suspend fun findSettlementById(settlementId: String, tenantId: String): VendorSettlement? = synchronized(this) {
        val s = settlements[settlementId]
        if (s != null && s.tenantId == tenantId) s else null
    }

    override suspend fun findSettlementByNumber(settlementNumber: String, tenantId: String): VendorSettlement? = synchronized(this) {
        settlements.values.firstOrNull { it.settlementNumber == settlementNumber && it.tenantId == tenantId }
    }

    override suspend fun updateSettlement(settlement: VendorSettlement): VendorSettlement = synchronized(this) {
        val existing = settlements[settlement.settlementId]
            ?: throw IllegalArgumentException("Settlement '${settlement.settlementId}' not found")
        if (existing.tenantId != settlement.tenantId) {
            throw IllegalArgumentException("Tenant mismatch on settlement update")
        }
        if (existing.version != settlement.version - 1) {
            throw IllegalStateException("Optimistic concurrency error: Expected version ${existing.version + 1} but got ${settlement.version}")
        }
        settlements[settlement.settlementId] = settlement
        settlement
    }

    override suspend fun listSettlements(
        vendorId: String?,
        status: VendorSettlementStatus?,
        projectId: String?,
        tenantId: String
    ): List<VendorSettlement> = synchronized(this) {
        settlements.values.filter {
            it.tenantId == tenantId &&
            (vendorId == null || it.vendorId == vendorId) &&
            (status == null || it.status == status) &&
            (projectId == null || it.projectId == projectId)
        }.sortedByDescending { it.createdAt }
    }

    override suspend fun insertReconciliationResult(result: VendorReconciliationResult): VendorReconciliationResult = synchronized(this) {
        reconciliationResults[result.reconciliationId] = result
        result
    }

    override suspend fun listReconciliationResults(
        vendorId: String?,
        status: ReconciliationStatus?,
        tenantId: String
    ): List<VendorReconciliationResult> = synchronized(this) {
        reconciliationResults.values.filter {
            it.tenantId == tenantId &&
            (vendorId == null || it.vendorId == vendorId) &&
            (status == null || it.status == status)
        }.sortedByDescending { it.reconciledAt }
    }

    override suspend fun insertAnalyticsSnapshot(snapshot: VendorAnalyticsSnapshot): VendorAnalyticsSnapshot = synchronized(this) {
        analyticsSnapshots[snapshot.snapshotId] = snapshot
        snapshot
    }

    override suspend fun listAnalyticsSnapshots(
        vendorId: String,
        period: AnalyticsPeriod?,
        tenantId: String
    ): List<VendorAnalyticsSnapshot> = synchronized(this) {
        analyticsSnapshots.values.filter {
            it.tenantId == tenantId &&
            it.vendorId == vendorId &&
            (period == null || it.period == period)
        }.sortedByDescending { it.generatedAt }
    }

    override suspend fun appendAuditEvent(event: VendorSettlementAuditEvent): VendorSettlementAuditEvent = synchronized(this) {
        auditEvents.add(event)
        event
    }

    override suspend fun listAuditEvents(
        settlementId: String?,
        vendorId: String?,
        tenantId: String
    ): List<VendorSettlementAuditEvent> = synchronized(this) {
        auditEvents.filter {
            it.tenantId == tenantId &&
            (settlementId == null || it.settlementId == settlementId) &&
            (vendorId == null || it.vendorId == vendorId)
        }.sortedByDescending { it.timestamp }
    }
}
