package com.sucharu.sucharupro.data.datasource.businessfinancialadjustment

import com.sucharu.sucharupro.domain.model.businessfinancialadjustment.*

data class AdjustmentFilter(
    val adjustmentType: BusinessFinancialAdjustmentType? = null,
    val sourceType: AdjustmentSourceType? = null,
    val sourceId: String? = null,
    val status: AdjustmentStatus? = null,
    val periodId: String? = null,
    val customerId: String? = null,
    val vendorId: String? = null,
    val jobId: String? = null,
    val costCenterId: String? = null
)

data class RefundFilter(
    val sourceType: AdjustmentSourceType? = null,
    val sourceId: String? = null,
    val status: RefundStatus? = null,
    val periodId: String? = null,
    val customerId: String? = null,
    val vendorId: String? = null
)

data class WriteOffFilter(
    val writeOffType: BusinessFinancialWriteOffType? = null,
    val sourceType: AdjustmentSourceType? = null,
    val sourceId: String? = null,
    val status: WriteOffStatus? = null,
    val periodId: String? = null,
    val customerId: String? = null,
    val vendorId: String? = null
)

interface BusinessFinancialAdjustmentDataSource {

    // --- Adjustments ---
    suspend fun insertAdjustment(adjustment: BusinessFinancialAdjustment): BusinessFinancialAdjustment
    suspend fun updateAdjustment(adjustment: BusinessFinancialAdjustment): BusinessFinancialAdjustment
    suspend fun findAdjustmentById(id: String, tenantId: String, projectId: String): BusinessFinancialAdjustment?
    suspend fun findAdjustmentByNumber(number: String, tenantId: String, projectId: String): BusinessFinancialAdjustment?
    suspend fun listAdjustments(tenantId: String, projectId: String, filter: AdjustmentFilter = AdjustmentFilter()): List<BusinessFinancialAdjustment>

    // --- Refunds ---
    suspend fun insertRefund(refund: BusinessFinancialRefund): BusinessFinancialRefund
    suspend fun updateRefund(refund: BusinessFinancialRefund): BusinessFinancialRefund
    suspend fun findRefundById(id: String, tenantId: String, projectId: String): BusinessFinancialRefund?
    suspend fun findRefundByNumber(number: String, tenantId: String, projectId: String): BusinessFinancialRefund?
    suspend fun listRefunds(tenantId: String, projectId: String, filter: RefundFilter = RefundFilter()): List<BusinessFinancialRefund>

    // --- Write-Offs ---
    suspend fun insertWriteOff(writeOff: BusinessFinancialWriteOff): BusinessFinancialWriteOff
    suspend fun updateWriteOff(writeOff: BusinessFinancialWriteOff): BusinessFinancialWriteOff
    suspend fun findWriteOffById(id: String, tenantId: String, projectId: String): BusinessFinancialWriteOff?
    suspend fun findWriteOffByNumber(number: String, tenantId: String, projectId: String): BusinessFinancialWriteOff?
    suspend fun listWriteOffs(tenantId: String, projectId: String, filter: WriteOffFilter = WriteOffFilter()): List<BusinessFinancialWriteOff>

    // --- Compensating Postings ---
    suspend fun insertPosting(posting: BusinessFinancialAdjustmentPosting): BusinessFinancialAdjustmentPosting
    suspend fun listPostingsByAdjustmentId(adjustmentId: String, tenantId: String, projectId: String): List<BusinessFinancialAdjustmentPosting>

    // --- Audit Trail ---
    suspend fun recordAuditEvent(event: BusinessFinancialAdjustmentAuditEvent): BusinessFinancialAdjustmentAuditEvent
    suspend fun listAuditEvents(tenantId: String, projectId: String, entityId: String? = null, entityType: String? = null): List<BusinessFinancialAdjustmentAuditEvent>
}
