package com.sucharu.sucharupro.data.repository.businessfinancialadjustment

import com.sucharu.sucharupro.data.datasource.businessfinancialadjustment.AdjustmentFilter
import com.sucharu.sucharupro.data.datasource.businessfinancialadjustment.BusinessFinancialAdjustmentDataSource
import com.sucharu.sucharupro.data.datasource.businessfinancialadjustment.RefundFilter
import com.sucharu.sucharupro.data.datasource.businessfinancialadjustment.WriteOffFilter
import com.sucharu.sucharupro.domain.model.businessfinancialadjustment.*

class BusinessFinancialAdjustmentRepositoryImpl(
    private val dataSource: BusinessFinancialAdjustmentDataSource
) : BusinessFinancialAdjustmentRepository {

    override suspend fun saveAdjustment(adjustment: BusinessFinancialAdjustment): BusinessFinancialAdjustment {
        return dataSource.insertAdjustment(adjustment)
    }

    override suspend fun updateAdjustment(adjustment: BusinessFinancialAdjustment): BusinessFinancialAdjustment {
        return dataSource.updateAdjustment(adjustment)
    }

    override suspend fun findAdjustmentById(id: String, tenantId: String, projectId: String): BusinessFinancialAdjustment? {
        return dataSource.findAdjustmentById(id, tenantId, projectId)
    }

    override suspend fun findAdjustmentByNumber(number: String, tenantId: String, projectId: String): BusinessFinancialAdjustment? {
        return dataSource.findAdjustmentByNumber(number, tenantId, projectId)
    }

    override suspend fun listAdjustments(tenantId: String, projectId: String, filter: AdjustmentFilter): List<BusinessFinancialAdjustment> {
        return dataSource.listAdjustments(tenantId, projectId, filter)
    }

    override suspend fun saveRefund(refund: BusinessFinancialRefund): BusinessFinancialRefund {
        return dataSource.insertRefund(refund)
    }

    override suspend fun updateRefund(refund: BusinessFinancialRefund): BusinessFinancialRefund {
        return dataSource.updateRefund(refund)
    }

    override suspend fun findRefundById(id: String, tenantId: String, projectId: String): BusinessFinancialRefund? {
        return dataSource.findRefundById(id, tenantId, projectId)
    }

    override suspend fun findRefundByNumber(number: String, tenantId: String, projectId: String): BusinessFinancialRefund? {
        return dataSource.findRefundByNumber(number, tenantId, projectId)
    }

    override suspend fun listRefunds(tenantId: String, projectId: String, filter: RefundFilter): List<BusinessFinancialRefund> {
        return dataSource.listRefunds(tenantId, projectId, filter)
    }

    override suspend fun saveWriteOff(writeOff: BusinessFinancialWriteOff): BusinessFinancialWriteOff {
        return dataSource.insertWriteOff(writeOff)
    }

    override suspend fun updateWriteOff(writeOff: BusinessFinancialWriteOff): BusinessFinancialWriteOff {
        return dataSource.updateWriteOff(writeOff)
    }

    override suspend fun findWriteOffById(id: String, tenantId: String, projectId: String): BusinessFinancialWriteOff? {
        return dataSource.findWriteOffById(id, tenantId, projectId)
    }

    override suspend fun findWriteOffByNumber(number: String, tenantId: String, projectId: String): BusinessFinancialWriteOff? {
        return dataSource.findWriteOffByNumber(number, tenantId, projectId)
    }

    override suspend fun listWriteOffs(tenantId: String, projectId: String, filter: WriteOffFilter): List<BusinessFinancialWriteOff> {
        return dataSource.listWriteOffs(tenantId, projectId, filter)
    }

    override suspend fun savePosting(posting: BusinessFinancialAdjustmentPosting): BusinessFinancialAdjustmentPosting {
        return dataSource.insertPosting(posting)
    }

    override suspend fun listPostingsByAdjustmentId(adjustmentId: String, tenantId: String, projectId: String): List<BusinessFinancialAdjustmentPosting> {
        return dataSource.listPostingsByAdjustmentId(adjustmentId, tenantId, projectId)
    }

    override suspend fun recordAuditEvent(event: BusinessFinancialAdjustmentAuditEvent): BusinessFinancialAdjustmentAuditEvent {
        return dataSource.recordAuditEvent(event)
    }

    override suspend fun listAuditEvents(tenantId: String, projectId: String, entityId: String?, entityType: String?): List<BusinessFinancialAdjustmentAuditEvent> {
        return dataSource.listAuditEvents(tenantId, projectId, entityId, entityType)
    }
}
