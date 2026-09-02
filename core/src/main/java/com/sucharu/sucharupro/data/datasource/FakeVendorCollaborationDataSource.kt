package com.sucharu.sucharupro.data.datasource

import com.sucharu.sucharupro.domain.model.vendorportal.*
import java.util.concurrent.ConcurrentHashMap

class FakeVendorCollaborationDataSource : VendorCollaborationDataSource {

    private val poAcks = ConcurrentHashMap<String, VendorPoAcknowledgement>()
    private val woAcks = ConcurrentHashMap<String, VendorWoAcknowledgement>()
    private val progressUpdates = ConcurrentHashMap<String, VendorProgressUpdate>()
    private val blockers = ConcurrentHashMap<String, VendorBlocker>()
    private val threads = ConcurrentHashMap<String, VendorCollaborationThread>()
    private val messages = ConcurrentHashMap<String, VendorCollaborationMessage>()
    private val evidenceList = ConcurrentHashMap<String, VendorCollaborationEvidence>()
    private val completionRequests = ConcurrentHashMap<String, VendorCompletionRequest>()
    private val auditEvents = ConcurrentHashMap<String, VendorCollaborationAuditEvent>()

    override suspend fun savePoAcknowledgement(ack: VendorPoAcknowledgement): VendorPoAcknowledgement {
        poAcks[ack.acknowledgementId] = ack
        return ack
    }

    override suspend fun findPoAcknowledgement(purchaseOrderId: String, tenantId: String): VendorPoAcknowledgement? {
        return poAcks.values.find { it.purchaseOrderId == purchaseOrderId && it.tenantId == tenantId }
    }

    override suspend fun saveWoAcknowledgement(ack: VendorWoAcknowledgement): VendorWoAcknowledgement {
        woAcks[ack.acknowledgementId] = ack
        return ack
    }

    override suspend fun findWoAcknowledgement(workOrderId: String, tenantId: String): VendorWoAcknowledgement? {
        return woAcks.values.find { it.workOrderId == workOrderId && it.tenantId == tenantId }
    }

    override suspend fun saveProgressUpdate(update: VendorProgressUpdate): VendorProgressUpdate {
        progressUpdates[update.progressUpdateId] = update
        return update
    }

    override suspend fun listProgressUpdates(workOrderId: String, tenantId: String): List<VendorProgressUpdate> {
        return progressUpdates.values
            .filter { it.workOrderId == workOrderId && it.tenantId == tenantId }
            .sortedBy { it.submittedAt }
    }

    override suspend fun getLatestProgressUpdate(workOrderId: String, tenantId: String): VendorProgressUpdate? {
        return progressUpdates.values
            .filter { it.workOrderId == workOrderId && it.tenantId == tenantId }
            .maxByOrNull { it.submittedAt }
    }

    override suspend fun saveBlocker(blocker: VendorBlocker): VendorBlocker {
        blockers[blocker.blockerId] = blocker
        return blocker
    }

    override suspend fun findBlockerById(blockerId: String, tenantId: String): VendorBlocker? {
        return blockers[blockerId]?.takeIf { it.tenantId == tenantId }
    }

    override suspend fun listBlockers(
        tenantId: String,
        projectId: String,
        vendorId: String?,
        workOrderId: String?,
        status: VendorBlockerStatus?
    ): List<VendorBlocker> {
        return blockers.values.filter {
            it.tenantId == tenantId &&
            it.projectId == projectId &&
            (vendorId == null || it.vendorId == vendorId) &&
            (workOrderId == null || it.workOrderId == workOrderId) &&
            (status == null || it.status == status)
        }.sortedByDescending { it.reportedAt }
    }

    override suspend fun saveThread(thread: VendorCollaborationThread): VendorCollaborationThread {
        threads[thread.threadId] = thread
        return thread
    }

    override suspend fun findThreadById(threadId: String, tenantId: String): VendorCollaborationThread? {
        return threads[threadId]?.takeIf { it.tenantId == tenantId }
    }

    override suspend fun findThreadByResource(
        resourceType: VendorThreadResourceType,
        resourceId: String,
        tenantId: String
    ): VendorCollaborationThread? {
        return threads.values.find {
            it.resourceType == resourceType && it.resourceId == resourceId && it.tenantId == tenantId
        }
    }

    override suspend fun saveMessage(message: VendorCollaborationMessage): VendorCollaborationMessage {
        messages[message.messageId] = message
        return message
    }

    override suspend fun listMessages(threadId: String, tenantId: String): List<VendorCollaborationMessage> {
        return messages.values
            .filter { it.threadId == threadId && it.tenantId == tenantId }
            .sortedBy { it.createdAt }
    }

    override suspend fun saveEvidence(evidence: VendorCollaborationEvidence): VendorCollaborationEvidence {
        evidenceList[evidence.evidenceId] = evidence
        return evidence
    }

    override suspend fun listEvidence(
        resourceType: VendorThreadResourceType,
        resourceId: String,
        tenantId: String
    ): List<VendorCollaborationEvidence> {
        return evidenceList.values
            .filter { it.resourceType == resourceType && it.resourceId == resourceId && it.tenantId == tenantId }
            .sortedByDescending { it.uploadedAt }
    }

    override suspend fun findEvidenceById(evidenceId: String, tenantId: String): VendorCollaborationEvidence? {
        return evidenceList[evidenceId]?.takeIf { it.tenantId == tenantId }
    }

    override suspend fun saveCompletionRequest(req: VendorCompletionRequest): VendorCompletionRequest {
        completionRequests[req.completionRequestId] = req
        return req
    }

    override suspend fun findCompletionRequest(workOrderId: String, tenantId: String): VendorCompletionRequest? {
        return completionRequests.values.find { it.workOrderId == workOrderId && it.tenantId == tenantId }
    }

    override suspend fun listCompletionRequests(
        tenantId: String,
        projectId: String,
        vendorId: String?,
        status: VendorCompletionStatus?
    ): List<VendorCompletionRequest> {
        return completionRequests.values.filter {
            it.tenantId == tenantId &&
            it.projectId == projectId &&
            (vendorId == null || it.vendorId == vendorId) &&
            (status == null || it.status == status)
        }.sortedByDescending { it.submittedAt }
    }

    override suspend fun saveAuditEvent(event: VendorCollaborationAuditEvent): VendorCollaborationAuditEvent {
        auditEvents[event.eventId] = event
        return event
    }

    override suspend fun listAuditEvents(
        tenantId: String,
        resourceType: String,
        resourceId: String
    ): List<VendorCollaborationAuditEvent> {
        return auditEvents.values
            .filter { it.tenantId == tenantId && it.resourceType == resourceType && it.resourceId == resourceId }
            .sortedBy { it.timestamp }
    }
}
