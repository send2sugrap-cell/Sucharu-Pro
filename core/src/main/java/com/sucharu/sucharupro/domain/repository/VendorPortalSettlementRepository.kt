package com.sucharu.sucharupro.domain.repository

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.vendorportal.*

/**
 * Repository interface for Vendor Portal Settlement, Reconciliation & Financial Collaboration Workspace (Module 13 Step 09).
 */
interface VendorPortalSettlementRepository {

    // Settlement Acknowledgements
    suspend fun saveAcknowledgement(acknowledgement: VendorPortalSettlementAcknowledgement): DomainResult<VendorPortalSettlementAcknowledgement>
    suspend fun findAcknowledgementById(tenantId: String, projectId: String, vendorId: String, acknowledgementId: String): DomainResult<VendorPortalSettlementAcknowledgement?>
    suspend fun findAcknowledgementBySettlementId(tenantId: String, projectId: String, vendorId: String, settlementId: String): DomainResult<VendorPortalSettlementAcknowledgement?>
    suspend fun findAcknowledgementByIdempotencyKey(tenantId: String, projectId: String, vendorId: String, idempotencyKey: String): DomainResult<VendorPortalSettlementAcknowledgement?>

    // Reconciliation Cases
    suspend fun saveReconciliationCase(reconciliationCase: VendorPortalReconciliationCase): DomainResult<VendorPortalReconciliationCase>
    suspend fun findReconciliationCaseById(tenantId: String, projectId: String, vendorId: String, caseId: String): DomainResult<VendorPortalReconciliationCase?>
    suspend fun listReconciliationCases(
        tenantId: String,
        projectId: String,
        vendorId: String,
        settlementId: String? = null,
        invoiceId: String? = null,
        status: VendorPortalReconciliationCaseStatus? = null
    ): DomainResult<List<VendorPortalReconciliationCase>>
    suspend fun appendReconciliationEvent(
        tenantId: String,
        projectId: String,
        vendorId: String,
        caseId: String,
        event: VendorPortalReconciliationEvent
    ): DomainResult<Unit>

    // Financial Disputes
    suspend fun saveFinancialDispute(dispute: VendorPortalFinancialDispute): DomainResult<VendorPortalFinancialDispute>
    suspend fun findFinancialDisputeById(tenantId: String, projectId: String, vendorId: String, disputeId: String): DomainResult<VendorPortalFinancialDispute?>
    suspend fun listFinancialDisputes(
        tenantId: String,
        projectId: String,
        vendorId: String,
        settlementId: String? = null,
        invoiceId: String? = null,
        status: VendorPortalFinancialDisputeStatus? = null
    ): DomainResult<List<VendorPortalFinancialDispute>>
    suspend fun appendFinancialDisputeEvent(
        tenantId: String,
        projectId: String,
        vendorId: String,
        disputeId: String,
        event: VendorPortalFinancialDisputeEvent
    ): DomainResult<Unit>

    // Financial Evidence
    suspend fun saveEvidence(evidence: VendorPortalFinancialSettlementEvidence): DomainResult<VendorPortalFinancialSettlementEvidence>
    suspend fun findEvidenceById(tenantId: String, projectId: String, vendorId: String, evidenceId: String): DomainResult<VendorPortalFinancialSettlementEvidence?>
    suspend fun listEvidence(
        tenantId: String,
        projectId: String,
        vendorId: String,
        entityType: String? = null,
        entityId: String? = null
    ): DomainResult<List<VendorPortalFinancialSettlementEvidence>>

    // Financial Threads & Messages
    suspend fun saveThread(thread: VendorPortalFinancialThread): DomainResult<VendorPortalFinancialThread>
    suspend fun findThreadById(tenantId: String, projectId: String, vendorId: String, threadId: String): DomainResult<VendorPortalFinancialThread?>
    suspend fun listThreads(
        tenantId: String,
        projectId: String,
        vendorId: String,
        contextType: String? = null,
        contextId: String? = null
    ): DomainResult<List<VendorPortalFinancialThread>>
    suspend fun saveMessage(message: VendorPortalFinancialMessage): DomainResult<VendorPortalFinancialMessage>
    suspend fun listMessages(
        tenantId: String,
        projectId: String,
        vendorId: String,
        threadId: String
    ): DomainResult<List<VendorPortalFinancialMessage>>

    // Activity Events
    suspend fun recordActivity(activity: VendorPortalFinancialActivityEvent): DomainResult<Unit>
    suspend fun listActivities(
        tenantId: String,
        projectId: String,
        vendorId: String,
        entityType: String? = null,
        entityId: String? = null
    ): DomainResult<List<VendorPortalFinancialActivityEvent>>
}
