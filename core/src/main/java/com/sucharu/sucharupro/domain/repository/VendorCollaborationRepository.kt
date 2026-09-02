package com.sucharu.sucharupro.domain.repository

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.vendorportal.*

interface VendorCollaborationRepository {
    suspend fun savePoAcknowledgement(ack: VendorPoAcknowledgement): DomainResult<VendorPoAcknowledgement>
    suspend fun findPoAcknowledgement(purchaseOrderId: String, tenantId: String): DomainResult<VendorPoAcknowledgement?>

    suspend fun saveWoAcknowledgement(ack: VendorWoAcknowledgement): DomainResult<VendorWoAcknowledgement>
    suspend fun findWoAcknowledgement(workOrderId: String, tenantId: String): DomainResult<VendorWoAcknowledgement?>

    suspend fun recordProgressUpdate(update: VendorProgressUpdate): DomainResult<VendorProgressUpdate>
    suspend fun listProgressUpdates(workOrderId: String, tenantId: String): DomainResult<List<VendorProgressUpdate>>
    suspend fun getLatestProgressUpdate(workOrderId: String, tenantId: String): DomainResult<VendorProgressUpdate?>

    suspend fun saveBlocker(blocker: VendorBlocker): DomainResult<VendorBlocker>
    suspend fun findBlockerById(blockerId: String, tenantId: String): DomainResult<VendorBlocker>
    suspend fun listBlockers(
        tenantId: String,
        projectId: String,
        vendorId: String? = null,
        workOrderId: String? = null,
        status: VendorBlockerStatus? = null
    ): DomainResult<List<VendorBlocker>>

    suspend fun saveThread(thread: VendorCollaborationThread): DomainResult<VendorCollaborationThread>
    suspend fun findThreadById(threadId: String, tenantId: String): DomainResult<VendorCollaborationThread>
    suspend fun findThreadByResource(
        resourceType: VendorThreadResourceType,
        resourceId: String,
        tenantId: String
    ): DomainResult<VendorCollaborationThread?>

    suspend fun recordMessage(message: VendorCollaborationMessage): DomainResult<VendorCollaborationMessage>
    suspend fun listMessages(threadId: String, tenantId: String): DomainResult<List<VendorCollaborationMessage>>

    suspend fun saveEvidence(evidence: VendorCollaborationEvidence): DomainResult<VendorCollaborationEvidence>
    suspend fun listEvidence(
        resourceType: VendorThreadResourceType,
        resourceId: String,
        tenantId: String
    ): DomainResult<List<VendorCollaborationEvidence>>
    suspend fun findEvidenceById(evidenceId: String, tenantId: String): DomainResult<VendorCollaborationEvidence>

    suspend fun saveCompletionRequest(req: VendorCompletionRequest): DomainResult<VendorCompletionRequest>
    suspend fun findCompletionRequest(workOrderId: String, tenantId: String): DomainResult<VendorCompletionRequest?>
    suspend fun listCompletionRequests(
        tenantId: String,
        projectId: String,
        vendorId: String? = null,
        status: VendorCompletionStatus? = null
    ): DomainResult<List<VendorCompletionRequest>>

    suspend fun recordAuditEvent(event: VendorCollaborationAuditEvent): DomainResult<VendorCollaborationAuditEvent>
    suspend fun listAuditEvents(
        tenantId: String,
        resourceType: String,
        resourceId: String
    ): DomainResult<List<VendorCollaborationAuditEvent>>
}
