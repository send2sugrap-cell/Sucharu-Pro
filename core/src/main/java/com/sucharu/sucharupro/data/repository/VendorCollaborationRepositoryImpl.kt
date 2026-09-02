package com.sucharu.sucharupro.data.repository

import com.sucharu.sucharupro.data.datasource.VendorCollaborationDataSource
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.vendorportal.*
import com.sucharu.sucharupro.domain.repository.VendorCollaborationRepository

class VendorCollaborationRepositoryImpl(
    private val dataSource: VendorCollaborationDataSource
) : VendorCollaborationRepository {

    override suspend fun savePoAcknowledgement(ack: VendorPoAcknowledgement): DomainResult<VendorPoAcknowledgement> {
        return try {
            DomainResult.Success(dataSource.savePoAcknowledgement(ack))
        } catch (e: Exception) {
            DomainResult.Error(e)
        }
    }

    override suspend fun findPoAcknowledgement(purchaseOrderId: String, tenantId: String): DomainResult<VendorPoAcknowledgement?> {
        return try {
            DomainResult.Success(dataSource.findPoAcknowledgement(purchaseOrderId, tenantId))
        } catch (e: Exception) {
            DomainResult.Error(e)
        }
    }

    override suspend fun saveWoAcknowledgement(ack: VendorWoAcknowledgement): DomainResult<VendorWoAcknowledgement> {
        return try {
            DomainResult.Success(dataSource.saveWoAcknowledgement(ack))
        } catch (e: Exception) {
            DomainResult.Error(e)
        }
    }

    override suspend fun findWoAcknowledgement(workOrderId: String, tenantId: String): DomainResult<VendorWoAcknowledgement?> {
        return try {
            DomainResult.Success(dataSource.findWoAcknowledgement(workOrderId, tenantId))
        } catch (e: Exception) {
            DomainResult.Error(e)
        }
    }

    override suspend fun recordProgressUpdate(update: VendorProgressUpdate): DomainResult<VendorProgressUpdate> {
        return try {
            DomainResult.Success(dataSource.saveProgressUpdate(update))
        } catch (e: Exception) {
            DomainResult.Error(e)
        }
    }

    override suspend fun listProgressUpdates(workOrderId: String, tenantId: String): DomainResult<List<VendorProgressUpdate>> {
        return try {
            DomainResult.Success(dataSource.listProgressUpdates(workOrderId, tenantId))
        } catch (e: Exception) {
            DomainResult.Error(e)
        }
    }

    override suspend fun getLatestProgressUpdate(workOrderId: String, tenantId: String): DomainResult<VendorProgressUpdate?> {
        return try {
            DomainResult.Success(dataSource.getLatestProgressUpdate(workOrderId, tenantId))
        } catch (e: Exception) {
            DomainResult.Error(e)
        }
    }

    override suspend fun saveBlocker(blocker: VendorBlocker): DomainResult<VendorBlocker> {
        return try {
            DomainResult.Success(dataSource.saveBlocker(blocker))
        } catch (e: Exception) {
            DomainResult.Error(e)
        }
    }

    override suspend fun findBlockerById(blockerId: String, tenantId: String): DomainResult<VendorBlocker> {
        return try {
            val blocker = dataSource.findBlockerById(blockerId, tenantId)
                ?: return DomainResult.Error(NoSuchElementException("Blocker $blockerId not found."))
            DomainResult.Success(blocker)
        } catch (e: Exception) {
            DomainResult.Error(e)
        }
    }

    override suspend fun listBlockers(
        tenantId: String,
        projectId: String,
        vendorId: String?,
        workOrderId: String?,
        status: VendorBlockerStatus?
    ): DomainResult<List<VendorBlocker>> {
        return try {
            DomainResult.Success(dataSource.listBlockers(tenantId, projectId, vendorId, workOrderId, status))
        } catch (e: Exception) {
            DomainResult.Error(e)
        }
    }

    override suspend fun saveThread(thread: VendorCollaborationThread): DomainResult<VendorCollaborationThread> {
        return try {
            DomainResult.Success(dataSource.saveThread(thread))
        } catch (e: Exception) {
            DomainResult.Error(e)
        }
    }

    override suspend fun findThreadById(threadId: String, tenantId: String): DomainResult<VendorCollaborationThread> {
        return try {
            val thread = dataSource.findThreadById(threadId, tenantId)
                ?: return DomainResult.Error(NoSuchElementException("Thread $threadId not found."))
            DomainResult.Success(thread)
        } catch (e: Exception) {
            DomainResult.Error(e)
        }
    }

    override suspend fun findThreadByResource(
        resourceType: VendorThreadResourceType,
        resourceId: String,
        tenantId: String
    ): DomainResult<VendorCollaborationThread?> {
        return try {
            DomainResult.Success(dataSource.findThreadByResource(resourceType, resourceId, tenantId))
        } catch (e: Exception) {
            DomainResult.Error(e)
        }
    }

    override suspend fun recordMessage(message: VendorCollaborationMessage): DomainResult<VendorCollaborationMessage> {
        return try {
            DomainResult.Success(dataSource.saveMessage(message))
        } catch (e: Exception) {
            DomainResult.Error(e)
        }
    }

    override suspend fun listMessages(threadId: String, tenantId: String): DomainResult<List<VendorCollaborationMessage>> {
        return try {
            DomainResult.Success(dataSource.listMessages(threadId, tenantId))
        } catch (e: Exception) {
            DomainResult.Error(e)
        }
    }

    override suspend fun saveEvidence(evidence: VendorCollaborationEvidence): DomainResult<VendorCollaborationEvidence> {
        return try {
            DomainResult.Success(dataSource.saveEvidence(evidence))
        } catch (e: Exception) {
            DomainResult.Error(e)
        }
    }

    override suspend fun listEvidence(
        resourceType: VendorThreadResourceType,
        resourceId: String,
        tenantId: String
    ): DomainResult<List<VendorCollaborationEvidence>> {
        return try {
            DomainResult.Success(dataSource.listEvidence(resourceType, resourceId, tenantId))
        } catch (e: Exception) {
            DomainResult.Error(e)
        }
    }

    override suspend fun findEvidenceById(evidenceId: String, tenantId: String): DomainResult<VendorCollaborationEvidence> {
        return try {
            val evidence = dataSource.findEvidenceById(evidenceId, tenantId)
                ?: return DomainResult.Error(NoSuchElementException("Evidence $evidenceId not found."))
            DomainResult.Success(evidence)
        } catch (e: Exception) {
            DomainResult.Error(e)
        }
    }

    override suspend fun saveCompletionRequest(req: VendorCompletionRequest): DomainResult<VendorCompletionRequest> {
        return try {
            DomainResult.Success(dataSource.saveCompletionRequest(req))
        } catch (e: Exception) {
            DomainResult.Error(e)
        }
    }

    override suspend fun findCompletionRequest(workOrderId: String, tenantId: String): DomainResult<VendorCompletionRequest?> {
        return try {
            DomainResult.Success(dataSource.findCompletionRequest(workOrderId, tenantId))
        } catch (e: Exception) {
            DomainResult.Error(e)
        }
    }

    override suspend fun listCompletionRequests(
        tenantId: String,
        projectId: String,
        vendorId: String?,
        status: VendorCompletionStatus?
    ): DomainResult<List<VendorCompletionRequest>> {
        return try {
            DomainResult.Success(dataSource.listCompletionRequests(tenantId, projectId, vendorId, status))
        } catch (e: Exception) {
            DomainResult.Error(e)
        }
    }

    override suspend fun recordAuditEvent(event: VendorCollaborationAuditEvent): DomainResult<VendorCollaborationAuditEvent> {
        return try {
            DomainResult.Success(dataSource.saveAuditEvent(event))
        } catch (e: Exception) {
            DomainResult.Error(e)
        }
    }

    override suspend fun listAuditEvents(
        tenantId: String,
        resourceType: String,
        resourceId: String
    ): DomainResult<List<VendorCollaborationAuditEvent>> {
        return try {
            DomainResult.Success(dataSource.listAuditEvents(tenantId, resourceType, resourceId))
        } catch (e: Exception) {
            DomainResult.Error(e)
        }
    }
}
