package com.sucharu.sucharupro.data.datasource

import com.sucharu.sucharupro.domain.model.vendorportal.*

/**
 * DataSource contract for Vendor Portal Settlement, Reconciliation & Financial Collaboration Workspace (Module 13 Step 09).
 */
interface VendorPortalSettlementDataSource {

    // Settlement Acknowledgement
    suspend fun saveAcknowledgement(acknowledgement: VendorPortalSettlementAcknowledgement): VendorPortalSettlementAcknowledgement
    suspend fun findAcknowledgementById(tenantId: String, projectId: String, vendorId: String, acknowledgementId: String): VendorPortalSettlementAcknowledgement?
    suspend fun findAcknowledgementBySettlementId(tenantId: String, projectId: String, vendorId: String, settlementId: String): VendorPortalSettlementAcknowledgement?
    suspend fun findAcknowledgementByIdempotencyKey(tenantId: String, projectId: String, vendorId: String, idempotencyKey: String): VendorPortalSettlementAcknowledgement?

    // Reconciliation Cases
    suspend fun saveReconciliationCase(reconciliationCase: VendorPortalReconciliationCase): VendorPortalReconciliationCase
    suspend fun findReconciliationCaseById(tenantId: String, projectId: String, vendorId: String, caseId: String): VendorPortalReconciliationCase?
    suspend fun listReconciliationCases(
        tenantId: String,
        projectId: String,
        vendorId: String,
        settlementId: String? = null,
        invoiceId: String? = null,
        status: VendorPortalReconciliationCaseStatus? = null
    ): List<VendorPortalReconciliationCase>
    suspend fun appendReconciliationEvent(tenantId: String, projectId: String, vendorId: String, caseId: String, event: VendorPortalReconciliationEvent)

    // Financial Disputes
    suspend fun saveFinancialDispute(dispute: VendorPortalFinancialDispute): VendorPortalFinancialDispute
    suspend fun findFinancialDisputeById(tenantId: String, projectId: String, vendorId: String, disputeId: String): VendorPortalFinancialDispute?
    suspend fun listFinancialDisputes(
        tenantId: String,
        projectId: String,
        vendorId: String,
        settlementId: String? = null,
        invoiceId: String? = null,
        status: VendorPortalFinancialDisputeStatus? = null
    ): List<VendorPortalFinancialDispute>
    suspend fun appendFinancialDisputeEvent(tenantId: String, projectId: String, vendorId: String, disputeId: String, event: VendorPortalFinancialDisputeEvent)

    // Financial Evidence
    suspend fun saveEvidence(evidence: VendorPortalFinancialSettlementEvidence): VendorPortalFinancialSettlementEvidence
    suspend fun findEvidenceById(tenantId: String, projectId: String, vendorId: String, evidenceId: String): VendorPortalFinancialSettlementEvidence?
    suspend fun listEvidence(tenantId: String, projectId: String, vendorId: String, entityType: String? = null, entityId: String? = null): List<VendorPortalFinancialSettlementEvidence>

    // Financial Threads & Messages
    suspend fun saveThread(thread: VendorPortalFinancialThread): VendorPortalFinancialThread
    suspend fun findThreadById(tenantId: String, projectId: String, vendorId: String, threadId: String): VendorPortalFinancialThread?
    suspend fun listThreads(tenantId: String, projectId: String, vendorId: String, contextType: String? = null, contextId: String? = null): List<VendorPortalFinancialThread>
    suspend fun saveMessage(message: VendorPortalFinancialMessage): VendorPortalFinancialMessage
    suspend fun listMessages(tenantId: String, projectId: String, vendorId: String, threadId: String): List<VendorPortalFinancialMessage>

    // Activity Events
    suspend fun recordActivity(activity: VendorPortalFinancialActivityEvent)
    suspend fun listActivities(tenantId: String, projectId: String, vendorId: String, entityType: String? = null, entityId: String? = null): List<VendorPortalFinancialActivityEvent>
}
