package com.sucharu.sucharupro.data.datasource.businessfinancialadjustment

import com.sucharu.sucharupro.domain.model.businessfinancialadjustment.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

class FakeBusinessFinancialAdjustmentDataSource : BusinessFinancialAdjustmentDataSource {

    private val adjustments = ConcurrentHashMap<String, BusinessFinancialAdjustment>()
    private val refunds = ConcurrentHashMap<String, BusinessFinancialRefund>()
    private val writeOffs = ConcurrentHashMap<String, BusinessFinancialWriteOff>()
    private val postings = CopyOnWriteArrayList<BusinessFinancialAdjustmentPosting>()
    private val auditEvents = CopyOnWriteArrayList<BusinessFinancialAdjustmentAuditEvent>()

    // --- Adjustments ---

    override suspend fun insertAdjustment(adjustment: BusinessFinancialAdjustment): BusinessFinancialAdjustment {
        val key = "${adjustment.tenantId}:${adjustment.projectId}:${adjustment.id}"
        adjustments[key] = adjustment
        return adjustment
    }

    override suspend fun updateAdjustment(adjustment: BusinessFinancialAdjustment): BusinessFinancialAdjustment {
        val key = "${adjustment.tenantId}:${adjustment.projectId}:${adjustment.id}"
        adjustments[key] = adjustment
        return adjustment
    }

    override suspend fun findAdjustmentById(id: String, tenantId: String, projectId: String): BusinessFinancialAdjustment? {
        val key = "$tenantId:$projectId:$id"
        return adjustments[key]
    }

    override suspend fun findAdjustmentByNumber(number: String, tenantId: String, projectId: String): BusinessFinancialAdjustment? {
        return adjustments.values.find {
            it.tenantId == tenantId && it.projectId == projectId && it.adjustmentNumber == number
        }
    }

    override suspend fun listAdjustments(tenantId: String, projectId: String, filter: AdjustmentFilter): List<BusinessFinancialAdjustment> {
        return adjustments.values
            .filter { it.tenantId == tenantId && it.projectId == projectId }
            .filter { filter.adjustmentType == null || it.adjustmentType == filter.adjustmentType }
            .filter { filter.sourceType == null || it.sourceType == filter.sourceType }
            .filter { filter.sourceId == null || it.sourceId == filter.sourceId }
            .filter { filter.status == null || it.status == filter.status }
            .filter { filter.periodId == null || it.periodId == filter.periodId }
            .filter { filter.customerId == null || it.customerId == filter.customerId }
            .filter { filter.vendorId == null || it.vendorId == filter.vendorId }
            .filter { filter.jobId == null || it.jobId == filter.jobId }
            .filter { filter.costCenterId == null || it.costCenterId == filter.costCenterId }
            .sortedByDescending { it.createdAt }
    }

    // --- Refunds ---

    override suspend fun insertRefund(refund: BusinessFinancialRefund): BusinessFinancialRefund {
        val key = "${refund.tenantId}:${refund.projectId}:${refund.id}"
        refunds[key] = refund
        return refund
    }

    override suspend fun updateRefund(refund: BusinessFinancialRefund): BusinessFinancialRefund {
        val key = "${refund.tenantId}:${refund.projectId}:${refund.id}"
        refunds[key] = refund
        return refund
    }

    override suspend fun findRefundById(id: String, tenantId: String, projectId: String): BusinessFinancialRefund? {
        val key = "$tenantId:$projectId:$id"
        return refunds[key]
    }

    override suspend fun findRefundByNumber(number: String, tenantId: String, projectId: String): BusinessFinancialRefund? {
        return refunds.values.find {
            it.tenantId == tenantId && it.projectId == projectId && it.refundNumber == number
        }
    }

    override suspend fun listRefunds(tenantId: String, projectId: String, filter: RefundFilter): List<BusinessFinancialRefund> {
        return refunds.values
            .filter { it.tenantId == tenantId && it.projectId == projectId }
            .filter { filter.sourceType == null || it.sourceType == filter.sourceType }
            .filter { filter.sourceId == null || it.sourceId == filter.sourceId }
            .filter { filter.status == null || it.status == filter.status }
            .filter { filter.periodId == null || it.periodId == filter.periodId }
            .filter { filter.customerId == null || it.customerId == filter.customerId }
            .filter { filter.vendorId == null || it.vendorId == filter.vendorId }
            .sortedByDescending { it.createdAt }
    }

    // --- Write-Offs ---

    override suspend fun insertWriteOff(writeOff: BusinessFinancialWriteOff): BusinessFinancialWriteOff {
        val key = "${writeOff.tenantId}:${writeOff.projectId}:${writeOff.id}"
        writeOffs[key] = writeOff
        return writeOff
    }

    override suspend fun updateWriteOff(writeOff: BusinessFinancialWriteOff): BusinessFinancialWriteOff {
        val key = "${writeOff.tenantId}:${writeOff.projectId}:${writeOff.id}"
        writeOffs[key] = writeOff
        return writeOff
    }

    override suspend fun findWriteOffById(id: String, tenantId: String, projectId: String): BusinessFinancialWriteOff? {
        val key = "$tenantId:$projectId:$id"
        return writeOffs[key]
    }

    override suspend fun findWriteOffByNumber(number: String, tenantId: String, projectId: String): BusinessFinancialWriteOff? {
        return writeOffs.values.find {
            it.tenantId == tenantId && it.projectId == projectId && it.writeOffNumber == number
        }
    }

    override suspend fun listWriteOffs(tenantId: String, projectId: String, filter: WriteOffFilter): List<BusinessFinancialWriteOff> {
        return writeOffs.values
            .filter { it.tenantId == tenantId && it.projectId == projectId }
            .filter { filter.writeOffType == null || it.writeOffType == filter.writeOffType }
            .filter { filter.sourceType == null || it.sourceType == filter.sourceType }
            .filter { filter.sourceId == null || it.sourceId == filter.sourceId }
            .filter { filter.status == null || it.status == filter.status }
            .filter { filter.periodId == null || it.periodId == filter.periodId }
            .filter { filter.customerId == null || it.customerId == filter.customerId }
            .filter { filter.vendorId == null || it.vendorId == filter.vendorId }
            .sortedByDescending { it.createdAt }
    }

    // --- Compensating Postings ---

    override suspend fun insertPosting(posting: BusinessFinancialAdjustmentPosting): BusinessFinancialAdjustmentPosting {
        postings.add(posting)
        return posting
    }

    override suspend fun listPostingsByAdjustmentId(adjustmentId: String, tenantId: String, projectId: String): List<BusinessFinancialAdjustmentPosting> {
        return postings.filter {
            it.tenantId == tenantId && it.projectId == projectId && it.adjustmentId == adjustmentId
        }.sortedByDescending { it.createdAt }
    }

    // --- Audit Trail ---

    override suspend fun recordAuditEvent(event: BusinessFinancialAdjustmentAuditEvent): BusinessFinancialAdjustmentAuditEvent {
        auditEvents.add(event)
        return event
    }

    override suspend fun listAuditEvents(tenantId: String, projectId: String, entityId: String?, entityType: String?): List<BusinessFinancialAdjustmentAuditEvent> {
        return auditEvents
            .filter { it.tenantId == tenantId && it.projectId == projectId }
            .filter { entityId == null || it.entityId == entityId }
            .filter { entityType == null || it.entityType == entityType }
            .sortedBy { it.timestamp }
    }
}
