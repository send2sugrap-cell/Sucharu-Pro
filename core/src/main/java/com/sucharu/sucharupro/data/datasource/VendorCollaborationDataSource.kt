package com.sucharu.sucharupro.data.datasource

import com.sucharu.sucharupro.domain.model.vendorportal.*

interface VendorCollaborationDataSource {
    suspend fun savePoAcknowledgement(ack: VendorPoAcknowledgement): VendorPoAcknowledgement
    suspend fun findPoAcknowledgement(purchaseOrderId: String, tenantId: String): VendorPoAcknowledgement?

    suspend fun saveWoAcknowledgement(ack: VendorWoAcknowledgement): VendorWoAcknowledgement
    suspend fun findWoAcknowledgement(workOrderId: String, tenantId: String): VendorWoAcknowledgement?

    suspend fun saveProgressUpdate(update: VendorProgressUpdate): VendorProgressUpdate
    suspend fun listProgressUpdates(workOrderId: String, tenantId: String): List<VendorProgressUpdate>
    suspend fun getLatestProgressUpdate(workOrderId: String, tenantId: String): VendorProgressUpdate?

    suspend fun saveBlocker(blocker: VendorBlocker): VendorBlocker
    suspend fun findBlockerById(blockerId: String, tenantId: String): VendorBlocker?
    suspend fun listBlockers(
        tenantId: String,
        projectId: String,
        vendorId: String? = null,
        workOrderId: String? = null,
        status: VendorBlockerStatus? = null
    ): List<VendorBlocker>

    suspend fun saveThread(thread: VendorCollaborationThread): VendorCollaborationThread
    suspend fun findThreadById(threadId: String, tenantId: String): VendorCollaborationThread?
    suspend fun findThreadByResource(resourceType: VendorThreadResourceType, resourceId: String, tenantId: String): VendorCollaborationThread?

    suspend fun saveMessage(message: VendorCollaborationMessage): VendorCollaborationMessage
    suspend fun listMessages(threadId: String, tenantId: String): List<VendorCollaborationMessage>

    suspend fun saveEvidence(evidence: VendorCollaborationEvidence): VendorCollaborationEvidence
    suspend fun listEvidence(resourceType: VendorThreadResourceType, resourceId: String, tenantId: String): List<VendorCollaborationEvidence>
    suspend fun findEvidenceById(evidenceId: String, tenantId: String): VendorCollaborationEvidence?

    suspend fun saveCompletionRequest(req: VendorCompletionRequest): VendorCompletionRequest
    suspend fun findCompletionRequest(workOrderId: String, tenantId: String): VendorCompletionRequest?
    suspend fun listCompletionRequests(
        tenantId: String,
        projectId: String,
        vendorId: String? = null,
        status: VendorCompletionStatus? = null
    ): List<VendorCompletionRequest>

    suspend fun saveAuditEvent(event: VendorCollaborationAuditEvent): VendorCollaborationAuditEvent
    suspend fun listAuditEvents(tenantId: String, resourceType: String, resourceId: String): List<VendorCollaborationAuditEvent>
}
