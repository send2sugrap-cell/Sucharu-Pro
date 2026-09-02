package com.sucharu.sucharupro.data.datasource

import com.sucharu.sucharupro.domain.model.vendorportal.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Thread-safe in-memory data source for Vendor Settlement, Reconciliation & Financial Collaboration Workspace (Module 13 Step 09).
 */
class FakeVendorPortalSettlementDataSource : VendorPortalSettlementDataSource {

    private val acknowledgements = ConcurrentHashMap<String, VendorPortalSettlementAcknowledgement>()
    private val reconciliationCases = ConcurrentHashMap<String, VendorPortalReconciliationCase>()
    private val financialDisputes = ConcurrentHashMap<String, VendorPortalFinancialDispute>()
    private val evidenceRecords = ConcurrentHashMap<String, VendorPortalFinancialSettlementEvidence>()
    private val threads = ConcurrentHashMap<String, VendorPortalFinancialThread>()
    private val messages = ConcurrentHashMap<String, MutableList<VendorPortalFinancialMessage>>()
    private val activities = CopyOnWriteArrayList<VendorPortalFinancialActivityEvent>()

    // --- Settlement Acknowledgement ---

    override suspend fun saveAcknowledgement(acknowledgement: VendorPortalSettlementAcknowledgement): VendorPortalSettlementAcknowledgement {
        val key = "${acknowledgement.tenantId}:${acknowledgement.acknowledgementId}"
        acknowledgements[key] = acknowledgement
        return acknowledgement
    }

    override suspend fun findAcknowledgementById(
        tenantId: String,
        projectId: String,
        vendorId: String,
        acknowledgementId: String
    ): VendorPortalSettlementAcknowledgement? {
        val ack = acknowledgements["$tenantId:$acknowledgementId"] ?: return null
        return if (ack.projectId == projectId && ack.vendorId == vendorId) ack else null
    }

    override suspend fun findAcknowledgementBySettlementId(
        tenantId: String,
        projectId: String,
        vendorId: String,
        settlementId: String
    ): VendorPortalSettlementAcknowledgement? {
        return acknowledgements.values.firstOrNull {
            it.tenantId == tenantId && it.projectId == projectId && it.vendorId == vendorId && it.settlementId == settlementId
        }
    }

    override suspend fun findAcknowledgementByIdempotencyKey(
        tenantId: String,
        projectId: String,
        vendorId: String,
        idempotencyKey: String
    ): VendorPortalSettlementAcknowledgement? {
        return acknowledgements.values.firstOrNull {
            it.tenantId == tenantId && it.projectId == projectId && it.vendorId == vendorId && it.idempotencyKey == idempotencyKey
        }
    }

    // --- Reconciliation Cases ---

    override suspend fun saveReconciliationCase(reconciliationCase: VendorPortalReconciliationCase): VendorPortalReconciliationCase {
        val key = "${reconciliationCase.tenantId}:${reconciliationCase.caseId}"
        reconciliationCases[key] = reconciliationCase
        return reconciliationCase
    }

    override suspend fun findReconciliationCaseById(
        tenantId: String,
        projectId: String,
        vendorId: String,
        caseId: String
    ): VendorPortalReconciliationCase? {
        val c = reconciliationCases["$tenantId:$caseId"] ?: return null
        return if (c.projectId == projectId && c.vendorId == vendorId) c else null
    }

    override suspend fun listReconciliationCases(
        tenantId: String,
        projectId: String,
        vendorId: String,
        settlementId: String?,
        invoiceId: String?,
        status: VendorPortalReconciliationCaseStatus?
    ): List<VendorPortalReconciliationCase> {
        return reconciliationCases.values
            .filter { it.tenantId == tenantId && it.projectId == projectId && it.vendorId == vendorId }
            .filter { settlementId == null || it.settlementId == settlementId }
            .filter { invoiceId == null || it.invoiceId == invoiceId }
            .filter { status == null || it.status == status }
            .sortedByDescending { it.createdAt }
    }

    override suspend fun appendReconciliationEvent(
        tenantId: String,
        projectId: String,
        vendorId: String,
        caseId: String,
        event: VendorPortalReconciliationEvent
    ) {
        val key = "$tenantId:$caseId"
        val existing = reconciliationCases[key] ?: return
        if (existing.projectId != projectId || existing.vendorId != vendorId) return
        if (existing.events.any { it.eventId == event.eventId }) return
        val updated = existing.copy(
            events = existing.events + event,
            updatedAt = System.currentTimeMillis()
        )
        reconciliationCases[key] = updated
    }

    // --- Financial Disputes ---

    override suspend fun saveFinancialDispute(dispute: VendorPortalFinancialDispute): VendorPortalFinancialDispute {
        val key = "${dispute.tenantId}:${dispute.disputeId}"
        financialDisputes[key] = dispute
        return dispute
    }

    override suspend fun findFinancialDisputeById(
        tenantId: String,
        projectId: String,
        vendorId: String,
        disputeId: String
    ): VendorPortalFinancialDispute? {
        val d = financialDisputes["$tenantId:$disputeId"] ?: return null
        return if (d.projectId == projectId && d.vendorId == vendorId) d else null
    }

    override suspend fun listFinancialDisputes(
        tenantId: String,
        projectId: String,
        vendorId: String,
        settlementId: String?,
        invoiceId: String?,
        status: VendorPortalFinancialDisputeStatus?
    ): List<VendorPortalFinancialDispute> {
        return financialDisputes.values
            .filter { it.tenantId == tenantId && it.projectId == projectId && it.vendorId == vendorId }
            .filter { settlementId == null || it.settlementId == settlementId }
            .filter { invoiceId == null || it.invoiceId == invoiceId }
            .filter { status == null || it.status == status }
            .sortedByDescending { it.createdAt }
    }

    override suspend fun appendFinancialDisputeEvent(
        tenantId: String,
        projectId: String,
        vendorId: String,
        disputeId: String,
        event: VendorPortalFinancialDisputeEvent
    ) {
        val key = "$tenantId:$disputeId"
        val existing = financialDisputes[key] ?: return
        if (existing.projectId != projectId || existing.vendorId != vendorId) return
        if (existing.events.any { it.eventId == event.eventId }) return
        val updated = existing.copy(
            events = existing.events + event,
            updatedAt = System.currentTimeMillis()
        )
        financialDisputes[key] = updated
    }

    // --- Financial Evidence ---

    override suspend fun saveEvidence(evidence: VendorPortalFinancialSettlementEvidence): VendorPortalFinancialSettlementEvidence {
        val key = "${evidence.tenantId}:${evidence.evidenceId}"
        evidenceRecords[key] = evidence
        return evidence
    }

    override suspend fun findEvidenceById(
        tenantId: String,
        projectId: String,
        vendorId: String,
        evidenceId: String
    ): VendorPortalFinancialSettlementEvidence? {
        val ev = evidenceRecords["$tenantId:$evidenceId"] ?: return null
        return if (ev.projectId == projectId && ev.vendorId == vendorId) ev else null
    }

    override suspend fun listEvidence(
        tenantId: String,
        projectId: String,
        vendorId: String,
        entityType: String?,
        entityId: String?
    ): List<VendorPortalFinancialSettlementEvidence> {
        return evidenceRecords.values
            .filter { it.tenantId == tenantId && it.projectId == projectId && it.vendorId == vendorId }
            .filter { entityType == null || it.entityType == entityType }
            .filter { entityId == null || it.entityId == entityId }
            .sortedByDescending { it.uploadedAt }
    }

    // --- Financial Threads & Messages ---

    override suspend fun saveThread(thread: VendorPortalFinancialThread): VendorPortalFinancialThread {
        val key = "${thread.tenantId}:${thread.threadId}"
        threads[key] = thread
        return thread
    }

    override suspend fun findThreadById(
        tenantId: String,
        projectId: String,
        vendorId: String,
        threadId: String
    ): VendorPortalFinancialThread? {
        val t = threads["$tenantId:$threadId"] ?: return null
        return if (t.projectId == projectId && t.vendorId == vendorId) t else null
    }

    override suspend fun listThreads(
        tenantId: String,
        projectId: String,
        vendorId: String,
        contextType: String?,
        contextId: String?
    ): List<VendorPortalFinancialThread> {
        return threads.values
            .filter { it.tenantId == tenantId && it.projectId == projectId && it.vendorId == vendorId }
            .filter { contextType == null || it.contextType == contextType }
            .filter { contextId == null || it.contextId == contextId }
            .sortedByDescending { it.updatedAt }
    }

    override suspend fun saveMessage(message: VendorPortalFinancialMessage): VendorPortalFinancialMessage {
        val list = messages.computeIfAbsent(message.threadId) { CopyOnWriteArrayList() }
        list.add(message)
        val threadKey = "${message.tenantId}:${message.threadId}"
        threads[threadKey]?.let { t ->
            threads[threadKey] = t.copy(
                messageCount = list.size,
                updatedAt = System.currentTimeMillis()
            )
        }
        return message
    }

    override suspend fun listMessages(
        tenantId: String,
        projectId: String,
        vendorId: String,
        threadId: String
    ): List<VendorPortalFinancialMessage> {
        val t = findThreadById(tenantId, projectId, vendorId, threadId) ?: return emptyList()
        return (messages[threadId] ?: emptyList())
            .filter { it.tenantId == tenantId && it.projectId == projectId && it.vendorId == vendorId }
            .sortedBy { it.timestamp }
    }

    // --- Activity Events ---

    override suspend fun recordActivity(activity: VendorPortalFinancialActivityEvent) {
        activities.add(activity)
    }

    override suspend fun listActivities(
        tenantId: String,
        projectId: String,
        vendorId: String,
        entityType: String?,
        entityId: String?
    ): List<VendorPortalFinancialActivityEvent> {
        return activities
            .filter { it.tenantId == tenantId && it.projectId == projectId && it.vendorId == vendorId }
            .filter { entityType == null || it.entityType == entityType }
            .filter { entityId == null || it.entityId == entityId }
            .sortedByDescending { it.occurredAt }
    }
}
