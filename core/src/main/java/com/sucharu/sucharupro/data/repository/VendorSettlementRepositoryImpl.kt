package com.sucharu.sucharupro.data.repository

import com.sucharu.sucharupro.data.datasource.VendorSettlementDataSource
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.vendor.*
import com.sucharu.sucharupro.domain.repository.VendorSettlementRepository

/**
 * Implementation of VendorSettlementRepository (Module 12 Step 10).
 */
class VendorSettlementRepositoryImpl(
    private val dataSource: VendorSettlementDataSource
) : VendorSettlementRepository {

    override suspend fun createSettlement(settlement: VendorSettlement): DomainResult<VendorSettlement> {
        return try {
            val created = dataSource.insertSettlement(settlement)
            DomainResult.Success(created)
        } catch (e: Exception) {
            DomainResult.Error(e)
        }
    }

    override suspend fun getSettlementById(settlementId: String, tenantId: String): DomainResult<VendorSettlement?> {
        return try {
            val settlement = dataSource.findSettlementById(settlementId, tenantId)
            DomainResult.Success(settlement)
        } catch (e: Exception) {
            DomainResult.Error(e)
        }
    }

    override suspend fun getSettlementByNumber(settlementNumber: String, tenantId: String): DomainResult<VendorSettlement?> {
        return try {
            val settlement = dataSource.findSettlementByNumber(settlementNumber, tenantId)
            DomainResult.Success(settlement)
        } catch (e: Exception) {
            DomainResult.Error(e)
        }
    }

    override suspend fun updateSettlement(settlement: VendorSettlement): DomainResult<VendorSettlement> {
        return try {
            val updated = dataSource.updateSettlement(settlement)
            DomainResult.Success(updated)
        } catch (e: Exception) {
            DomainResult.Error(e)
        }
    }

    override suspend fun listSettlements(
        vendorId: String?,
        status: VendorSettlementStatus?,
        projectId: String?,
        tenantId: String
    ): DomainResult<List<VendorSettlement>> {
        return try {
            val list = dataSource.listSettlements(vendorId, status, projectId, tenantId)
            DomainResult.Success(list)
        } catch (e: Exception) {
            DomainResult.Error(e)
        }
    }

    override suspend fun recordReconciliationResult(result: VendorReconciliationResult): DomainResult<VendorReconciliationResult> {
        return try {
            val recorded = dataSource.insertReconciliationResult(result)
            DomainResult.Success(recorded)
        } catch (e: Exception) {
            DomainResult.Error(e)
        }
    }

    override suspend fun listReconciliationResults(
        vendorId: String?,
        status: ReconciliationStatus?,
        tenantId: String
    ): DomainResult<List<VendorReconciliationResult>> {
        return try {
            val list = dataSource.listReconciliationResults(vendorId, status, tenantId)
            DomainResult.Success(list)
        } catch (e: Exception) {
            DomainResult.Error(e)
        }
    }

    override suspend fun saveAnalyticsSnapshot(snapshot: VendorAnalyticsSnapshot): DomainResult<VendorAnalyticsSnapshot> {
        return try {
            val saved = dataSource.insertAnalyticsSnapshot(snapshot)
            DomainResult.Success(saved)
        } catch (e: Exception) {
            DomainResult.Error(e)
        }
    }

    override suspend fun listAnalyticsSnapshots(
        vendorId: String,
        period: AnalyticsPeriod?,
        tenantId: String
    ): DomainResult<List<VendorAnalyticsSnapshot>> {
        return try {
            val list = dataSource.listAnalyticsSnapshots(vendorId, period, tenantId)
            DomainResult.Success(list)
        } catch (e: Exception) {
            DomainResult.Error(e)
        }
    }

    override suspend fun appendAuditEvent(event: VendorSettlementAuditEvent): DomainResult<VendorSettlementAuditEvent> {
        return try {
            val saved = dataSource.appendAuditEvent(event)
            DomainResult.Success(saved)
        } catch (e: Exception) {
            DomainResult.Error(e)
        }
    }

    override suspend fun listAuditEvents(
        settlementId: String?,
        vendorId: String?,
        tenantId: String
    ): DomainResult<List<VendorSettlementAuditEvent>> {
        return try {
            val list = dataSource.listAuditEvents(settlementId, vendorId, tenantId)
            DomainResult.Success(list)
        } catch (e: Exception) {
            DomainResult.Error(e)
        }
    }
}
