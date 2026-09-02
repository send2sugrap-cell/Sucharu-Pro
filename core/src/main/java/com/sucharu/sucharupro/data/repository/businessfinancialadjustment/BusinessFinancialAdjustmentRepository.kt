package com.sucharu.sucharupro.data.repository.businessfinancialadjustment

import com.sucharu.sucharupro.data.datasource.businessfinancialadjustment.AdjustmentFilter
import com.sucharu.sucharupro.data.datasource.businessfinancialadjustment.RefundFilter
import com.sucharu.sucharupro.data.datasource.businessfinancialadjustment.WriteOffFilter
import com.sucharu.sucharupro.domain.model.businessfinancialadjustment.*

interface BusinessFinancialAdjustmentRepository {

    // --- Adjustments ---
    suspend fun saveAdjustment(adjustment: BusinessFinancialAdjustment): BusinessFinancialAdjustment
    suspend fun updateAdjustment(adjustment: BusinessFinancialAdjustment): BusinessFinancialAdjustment
    suspend fun findAdjustmentById(id: String, tenantId: String, projectId: String): BusinessFinancialAdjustment?
    suspend fun findAdjustmentByNumber(number: String, tenantId: String, projectId: String): BusinessFinancialAdjustment?
    suspend fun listAdjustments(tenantId: String, projectId: String, filter: AdjustmentFilter = AdjustmentFilter()): List<BusinessFinancialAdjustment>

    // --- Refunds ---
    suspend fun saveRefund(refund: BusinessFinancialRefund): BusinessFinancialRefund
    suspend fun updateRefund(refund: BusinessFinancialRefund): BusinessFinancialRefund
    suspend fun findRefundById(id: String, tenantId: String, projectId: String): BusinessFinancialRefund?
    suspend fun findRefundByNumber(number: String, tenantId: String, projectId: String): BusinessFinancialRefund?
    suspend fun listRefunds(tenantId: String, projectId: String, filter: RefundFilter = RefundFilter()): List<BusinessFinancialRefund>

    // --- Write-Offs ---
    suspend fun saveWriteOff(writeOff: BusinessFinancialWriteOff): BusinessFinancialWriteOff
    suspend fun updateWriteOff(writeOff: BusinessFinancialWriteOff): BusinessFinancialWriteOff
    suspend fun findWriteOffById(id: String, tenantId: String, projectId: String): BusinessFinancialWriteOff?
    suspend fun findWriteOffByNumber(number: String, tenantId: String, projectId: String): BusinessFinancialWriteOff?
    suspend fun listWriteOffs(tenantId: String, projectId: String, filter: WriteOffFilter = WriteOffFilter()): List<BusinessFinancialWriteOff>

    // --- Compensating Postings ---
    suspend fun savePosting(posting: BusinessFinancialAdjustmentPosting): BusinessFinancialAdjustmentPosting
    suspend fun listPostingsByAdjustmentId(adjustmentId: String, tenantId: String, projectId: String): List<BusinessFinancialAdjustmentPosting>

    // --- Audit Trail ---
    suspend fun recordAuditEvent(event: BusinessFinancialAdjustmentAuditEvent): BusinessFinancialAdjustmentAuditEvent
    suspend fun listAuditEvents(tenantId: String, projectId: String, entityId: String? = null, entityType: String? = null): List<BusinessFinancialAdjustmentAuditEvent>
}
