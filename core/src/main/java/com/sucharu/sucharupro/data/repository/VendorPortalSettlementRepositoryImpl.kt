package com.sucharu.sucharupro.data.repository

import com.sucharu.sucharupro.data.datasource.VendorPortalSettlementDataSource
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.vendorportal.*
import com.sucharu.sucharupro.domain.repository.VendorPortalSettlementRepository

/**
 * Production implementation of VendorPortalSettlementRepository (Module 13 Step 09).
 */
class VendorPortalSettlementRepositoryImpl(
    private val dataSource: VendorPortalSettlementDataSource
) : VendorPortalSettlementRepository {

    // --- Settlement Acknowledgement ---

    override suspend fun saveAcknowledgement(acknowledgement: VendorPortalSettlementAcknowledgement): DomainResult<VendorPortalSettlementAcknowledgement> =
        try {
            DomainResult.Success(dataSource.saveAcknowledgement(acknowledgement))
        } catch (e: Exception) {
            DomainResult.Error(exception = e, message = e.message ?: "Failed to save settlement acknowledgement")
        }

    override suspend fun findAcknowledgementById(
        tenantId: String,
        projectId: String,
        vendorId: String,
        acknowledgementId: String
    ): DomainResult<VendorPortalSettlementAcknowledgement?> =
        try {
            DomainResult.Success(dataSource.findAcknowledgementById(tenantId, projectId, vendorId, acknowledgementId))
        } catch (e: Exception) {
            DomainResult.Error(exception = e, message = e.message ?: "Failed to find settlement acknowledgement")
        }

    override suspend fun findAcknowledgementBySettlementId(
        tenantId: String,
        projectId: String,
        vendorId: String,
        settlementId: String
    ): DomainResult<VendorPortalSettlementAcknowledgement?> =
        try {
            DomainResult.Success(dataSource.findAcknowledgementBySettlementId(tenantId, projectId, vendorId, settlementId))
        } catch (e: Exception) {
            DomainResult.Error(exception = e, message = e.message ?: "Failed to find settlement acknowledgement by settlementId")
        }

    override suspend fun findAcknowledgementByIdempotencyKey(
        tenantId: String,
        projectId: String,
        vendorId: String,
        idempotencyKey: String
    ): DomainResult<VendorPortalSettlementAcknowledgement?> =
        try {
            DomainResult.Success(dataSource.findAcknowledgementByIdempotencyKey(tenantId, projectId, vendorId, idempotencyKey))
        } catch (e: Exception) {
            DomainResult.Error(exception = e, message = e.message ?: "Failed to find settlement acknowledgement by idempotency key")
        }

    // --- Reconciliation Cases ---

    override suspend fun saveReconciliationCase(reconciliationCase: VendorPortalReconciliationCase): DomainResult<VendorPortalReconciliationCase> =
        try {
            DomainResult.Success(dataSource.saveReconciliationCase(reconciliationCase))
        } catch (e: Exception) {
            DomainResult.Error(exception = e, message = e.message ?: "Failed to save reconciliation case")
        }

    override suspend fun findReconciliationCaseById(
        tenantId: String,
        projectId: String,
        vendorId: String,
        caseId: String
    ): DomainResult<VendorPortalReconciliationCase?> =
        try {
            DomainResult.Success(dataSource.findReconciliationCaseById(tenantId, projectId, vendorId, caseId))
        } catch (e: Exception) {
            DomainResult.Error(exception = e, message = e.message ?: "Failed to find reconciliation case")
        }

    override suspend fun listReconciliationCases(
        tenantId: String,
        projectId: String,
        vendorId: String,
        settlementId: String?,
        invoiceId: String?,
        status: VendorPortalReconciliationCaseStatus?
    ): DomainResult<List<VendorPortalReconciliationCase>> =
        try {
            DomainResult.Success(dataSource.listReconciliationCases(tenantId, projectId, vendorId, settlementId, invoiceId, status))
        } catch (e: Exception) {
            DomainResult.Error(exception = e, message = e.message ?: "Failed to list reconciliation cases")
        }

    override suspend fun appendReconciliationEvent(
        tenantId: String,
        projectId: String,
        vendorId: String,
        caseId: String,
        event: VendorPortalReconciliationEvent
    ): DomainResult<Unit> =
        try {
            dataSource.appendReconciliationEvent(tenantId, projectId, vendorId, caseId, event)
            DomainResult.Success(Unit)
        } catch (e: Exception) {
            DomainResult.Error(exception = e, message = e.message ?: "Failed to append reconciliation event")
        }

    // --- Financial Disputes ---

    override suspend fun saveFinancialDispute(dispute: VendorPortalFinancialDispute): DomainResult<VendorPortalFinancialDispute> =
        try {
            DomainResult.Success(dataSource.saveFinancialDispute(dispute))
        } catch (e: Exception) {
            DomainResult.Error(exception = e, message = e.message ?: "Failed to save financial dispute")
        }

    override suspend fun findFinancialDisputeById(
        tenantId: String,
        projectId: String,
        vendorId: String,
        disputeId: String
    ): DomainResult<VendorPortalFinancialDispute?> =
        try {
            DomainResult.Success(dataSource.findFinancialDisputeById(tenantId, projectId, vendorId, disputeId))
        } catch (e: Exception) {
            DomainResult.Error(exception = e, message = e.message ?: "Failed to find financial dispute")
        }

    override suspend fun listFinancialDisputes(
        tenantId: String,
        projectId: String,
        vendorId: String,
        settlementId: String?,
        invoiceId: String?,
        status: VendorPortalFinancialDisputeStatus?
    ): DomainResult<List<VendorPortalFinancialDispute>> =
        try {
            DomainResult.Success(dataSource.listFinancialDisputes(tenantId, projectId, vendorId, settlementId, invoiceId, status))
        } catch (e: Exception) {
            DomainResult.Error(exception = e, message = e.message ?: "Failed to list financial disputes")
        }

    override suspend fun appendFinancialDisputeEvent(
        tenantId: String,
        projectId: String,
        vendorId: String,
        disputeId: String,
        event: VendorPortalFinancialDisputeEvent
    ): DomainResult<Unit> =
        try {
            dataSource.appendFinancialDisputeEvent(tenantId, projectId, vendorId, disputeId, event)
            DomainResult.Success(Unit)
        } catch (e: Exception) {
            DomainResult.Error(exception = e, message = e.message ?: "Failed to append financial dispute event")
        }

    // --- Financial Evidence ---

    override suspend fun saveEvidence(evidence: VendorPortalFinancialSettlementEvidence): DomainResult<VendorPortalFinancialSettlementEvidence> =
        try {
            DomainResult.Success(dataSource.saveEvidence(evidence))
        } catch (e: Exception) {
            DomainResult.Error(exception = e, message = e.message ?: "Failed to save financial evidence")
        }

    override suspend fun findEvidenceById(
        tenantId: String,
        projectId: String,
        vendorId: String,
        evidenceId: String
    ): DomainResult<VendorPortalFinancialSettlementEvidence?> =
        try {
            DomainResult.Success(dataSource.findEvidenceById(tenantId, projectId, vendorId, evidenceId))
        } catch (e: Exception) {
            DomainResult.Error(exception = e, message = e.message ?: "Failed to find financial evidence")
        }

    override suspend fun listEvidence(
        tenantId: String,
        projectId: String,
        vendorId: String,
        entityType: String?,
        entityId: String?
    ): DomainResult<List<VendorPortalFinancialSettlementEvidence>> =
        try {
            DomainResult.Success(dataSource.listEvidence(tenantId, projectId, vendorId, entityType, entityId))
        } catch (e: Exception) {
            DomainResult.Error(exception = e, message = e.message ?: "Failed to list financial evidence")
        }

    // --- Financial Threads & Messages ---

    override suspend fun saveThread(thread: VendorPortalFinancialThread): DomainResult<VendorPortalFinancialThread> =
        try {
            DomainResult.Success(dataSource.saveThread(thread))
        } catch (e: Exception) {
            DomainResult.Error(exception = e, message = e.message ?: "Failed to save financial thread")
        }

    override suspend fun findThreadById(
        tenantId: String,
        projectId: String,
        vendorId: String,
        threadId: String
    ): DomainResult<VendorPortalFinancialThread?> =
        try {
            DomainResult.Success(dataSource.findThreadById(tenantId, projectId, vendorId, threadId))
        } catch (e: Exception) {
            DomainResult.Error(exception = e, message = e.message ?: "Failed to find financial thread")
        }

    override suspend fun listThreads(
        tenantId: String,
        projectId: String,
        vendorId: String,
        contextType: String?,
        contextId: String?
    ): DomainResult<List<VendorPortalFinancialThread>> =
        try {
            DomainResult.Success(dataSource.listThreads(tenantId, projectId, vendorId, contextType, contextId))
        } catch (e: Exception) {
            DomainResult.Error(exception = e, message = e.message ?: "Failed to list financial threads")
        }

    override suspend fun saveMessage(message: VendorPortalFinancialMessage): DomainResult<VendorPortalFinancialMessage> =
        try {
            DomainResult.Success(dataSource.saveMessage(message))
        } catch (e: Exception) {
            DomainResult.Error(exception = e, message = e.message ?: "Failed to save financial message")
        }

    override suspend fun listMessages(
        tenantId: String,
        projectId: String,
        vendorId: String,
        threadId: String
    ): DomainResult<List<VendorPortalFinancialMessage>> =
        try {
            DomainResult.Success(dataSource.listMessages(tenantId, projectId, vendorId, threadId))
        } catch (e: Exception) {
            DomainResult.Error(exception = e, message = e.message ?: "Failed to list financial messages")
        }

    // --- Activity Events ---

    override suspend fun recordActivity(activity: VendorPortalFinancialActivityEvent): DomainResult<Unit> =
        try {
            dataSource.recordActivity(activity)
            DomainResult.Success(Unit)
        } catch (e: Exception) {
            DomainResult.Error(exception = e, message = e.message ?: "Failed to record financial activity")
        }

    override suspend fun listActivities(
        tenantId: String,
        projectId: String,
        vendorId: String,
        entityType: String?,
        entityId: String?
    ): DomainResult<List<VendorPortalFinancialActivityEvent>> =
        try {
            DomainResult.Success(dataSource.listActivities(tenantId, projectId, vendorId, entityType, entityId))
        } catch (e: Exception) {
            DomainResult.Error(exception = e, message = e.message ?: "Failed to list financial activities")
        }
}
