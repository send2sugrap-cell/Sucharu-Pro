package com.sucharu.sucharupro.domain.service.vendorportal

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.common.Money
import com.sucharu.sucharupro.domain.model.vendorportal.*

/**
 * Service interface for Vendor Portal Settlement, Reconciliation & Financial Collaboration Workspace (Module 13 Step 09).
 */
interface VendorPortalSettlementService {

    // Settlements
    suspend fun listSettlements(tenantId: String, projectId: String, vendorId: String): DomainResult<List<VendorPortalSettlementSummary>>
    suspend fun getSettlementById(tenantId: String, projectId: String, vendorId: String, settlementId: String): DomainResult<VendorPortalSettlementSummary>
    suspend fun getSettlementAllocations(tenantId: String, projectId: String, vendorId: String, settlementId: String): DomainResult<List<VendorPortalSettlementAllocationProjection>>
    suspend fun acknowledgeSettlement(
        tenantId: String,
        projectId: String,
        vendorId: String,
        settlementId: String,
        status: VendorPortalSettlementViewStatus = VendorPortalSettlementViewStatus.ACKNOWLEDGED,
        idempotencyKey: String,
        discrepancyFlag: Boolean = false,
        discrepancyNotes: String? = null,
        evidenceReferences: List<String> = emptyList(),
        actorId: String
    ): DomainResult<VendorPortalSettlementAcknowledgement>

    // Reconciliation Cases
    suspend fun listReconciliationCases(
        tenantId: String,
        projectId: String,
        vendorId: String,
        settlementId: String? = null,
        invoiceId: String? = null,
        status: VendorPortalReconciliationCaseStatus? = null
    ): DomainResult<List<VendorPortalReconciliationCase>>
    suspend fun getReconciliationCaseById(tenantId: String, projectId: String, vendorId: String, caseId: String): DomainResult<VendorPortalReconciliationCase>
    suspend fun createReconciliationQuery(
        tenantId: String,
        projectId: String,
        vendorId: String,
        settlementId: String? = null,
        invoiceId: String? = null,
        subject: String,
        claimedAmount: Money,
        systemAmount: Money,
        notes: String? = null,
        actorId: String
    ): DomainResult<VendorPortalReconciliationCase>
    suspend fun respondToReconciliation(
        tenantId: String,
        projectId: String,
        vendorId: String,
        caseId: String,
        remarks: String,
        actorId: String,
        actorRole: String
    ): DomainResult<VendorPortalReconciliationCase>

    // Financial Disputes
    suspend fun listFinancialDisputes(
        tenantId: String,
        projectId: String,
        vendorId: String,
        settlementId: String? = null,
        invoiceId: String? = null,
        status: VendorPortalFinancialDisputeStatus? = null
    ): DomainResult<List<VendorPortalFinancialDispute>>
    suspend fun getFinancialDisputeById(tenantId: String, projectId: String, vendorId: String, disputeId: String): DomainResult<VendorPortalFinancialDispute>
    suspend fun createFinancialDispute(
        tenantId: String,
        projectId: String,
        vendorId: String,
        settlementId: String? = null,
        invoiceId: String? = null,
        category: String,
        priority: String = "NORMAL",
        disputedAmount: Money,
        proposedResolutionAmount: Money? = null,
        reason: String,
        actorId: String
    ): DomainResult<VendorPortalFinancialDispute>
    suspend fun respondToFinancialDispute(
        tenantId: String,
        projectId: String,
        vendorId: String,
        disputeId: String,
        remarks: String,
        actorId: String,
        actorRole: String,
        proposedResolutionAmount: Money? = null
    ): DomainResult<VendorPortalFinancialDispute>

    // Payments
    suspend fun listPaymentHistory(tenantId: String, projectId: String, vendorId: String): DomainResult<List<VendorPortalPaymentSummary>>

    // Financial Evidence
    suspend fun uploadEvidence(
        tenantId: String,
        projectId: String,
        vendorId: String,
        entityType: String,
        entityId: String,
        evidenceType: VendorPortalSettlementEvidenceType = VendorPortalSettlementEvidenceType.SETTLEMENT_STATEMENT,
        fileName: String,
        fileUrl: String,
        checksum: String? = null,
        fileSizeBytes: Long = 0L,
        mimeType: String? = null,
        description: String? = null,
        actorId: String
    ): DomainResult<VendorPortalFinancialSettlementEvidence>
    suspend fun listEvidence(
        tenantId: String,
        projectId: String,
        vendorId: String,
        entityType: String? = null,
        entityId: String? = null
    ): DomainResult<List<VendorPortalFinancialSettlementEvidence>>

    // Financial Collaboration Threads & Messages
    suspend fun listThreads(
        tenantId: String,
        projectId: String,
        vendorId: String,
        contextType: String? = null,
        contextId: String? = null
    ): DomainResult<List<VendorPortalFinancialThread>>
    suspend fun getThreadById(tenantId: String, projectId: String, vendorId: String, threadId: String): DomainResult<VendorPortalFinancialThread>
    suspend fun postMessage(
        tenantId: String,
        projectId: String,
        vendorId: String,
        threadId: String,
        content: String,
        evidenceReferences: List<String> = emptyList(),
        actorId: String,
        actorRole: String
    ): DomainResult<VendorPortalFinancialMessage>
    suspend fun listMessages(
        tenantId: String,
        projectId: String,
        vendorId: String,
        threadId: String
    ): DomainResult<List<VendorPortalFinancialMessage>>

    // Activity, Analytics & Workspace
    suspend fun listFinancialActivity(
        tenantId: String,
        projectId: String,
        vendorId: String,
        entityType: String? = null,
        entityId: String? = null
    ): DomainResult<List<VendorPortalFinancialActivityEvent>>
    suspend fun getFinancialAnalytics(tenantId: String, projectId: String, vendorId: String): DomainResult<VendorPortalSettlementAnalyticsSummary>
    suspend fun getFinancialWorkspace(tenantId: String, projectId: String, vendorId: String): DomainResult<VendorPortalFinancialWorkspace>
}
