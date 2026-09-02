package com.sucharu.sucharupro.data.repository

import com.sucharu.sucharupro.data.datasource.DeliveryOrderDataSource
import com.sucharu.sucharupro.data.datasource.DeliveryProofDataSource
import com.sucharu.sucharupro.data.datasource.DeliveryShipmentDataSource
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.delivery.pod.DeliveryProof
import com.sucharu.sucharupro.domain.model.delivery.pod.DeliveryProofActivityEvent
import com.sucharu.sucharupro.domain.model.delivery.pod.DeliveryProofActivityType
import com.sucharu.sucharupro.domain.model.delivery.pod.DeliveryProofEvidence
import com.sucharu.sucharupro.domain.model.delivery.pod.DeliveryProofRecipient
import com.sucharu.sucharupro.domain.model.delivery.pod.DeliveryProofStatus
import com.sucharu.sucharupro.domain.model.delivery.pod.DeliveryProofSummary
import com.sucharu.sucharupro.domain.model.delivery.shipment.DeliveryShipmentStatus
import com.sucharu.sucharupro.domain.model.user.UserRole
import com.sucharu.sucharupro.domain.repository.DeliveryProofRepository
import com.sucharu.sucharupro.domain.service.delivery.DeliveryProofCompletionService
import com.sucharu.sucharupro.domain.validation.DeliveryProofAuthorizationValidator
import com.sucharu.sucharupro.domain.validation.DeliveryProofEvidenceValidator
import com.sucharu.sucharupro.domain.validation.DeliveryProofLifecycleValidator
import com.sucharu.sucharupro.domain.validation.DeliveryProofOperation
import com.sucharu.sucharupro.domain.validation.DeliveryProofValidator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID

/**
 * Production-grade, thread-safe implementation of DeliveryProofRepository (Module 08 Step 08).
 */
class DeliveryProofRepositoryImpl(
    private val proofDataSource: DeliveryProofDataSource,
    private val shipmentDataSource: DeliveryShipmentDataSource? = null,
    private val orderDataSource: DeliveryOrderDataSource? = null
) : DeliveryProofRepository {

    private val mutex = Mutex()

    override fun observeProofs(projectId: String): Flow<List<DeliveryProof>> {
        return proofDataSource.observeProofs(projectId)
    }

    override fun observeProof(proofId: String): Flow<DeliveryProof?> {
        return proofDataSource.observeProof(proofId)
    }

    override fun observeEvidence(proofId: String): Flow<List<DeliveryProofEvidence>> {
        return proofDataSource.observeEvidence(proofId)
    }

    override fun observeRecipient(proofId: String): Flow<DeliveryProofRecipient?> {
        return proofDataSource.observeRecipient(proofId)
    }

    override fun observeActivityEvents(proofId: String): Flow<List<DeliveryProofActivityEvent>> {
        return proofDataSource.observeActivityEvents(proofId)
    }

    override fun observeProofSummary(projectId: String): Flow<DeliveryProofSummary> {
        return combine(proofDataSource.observeProofs(projectId)) { proofsArray ->
            val proofs = proofsArray[0]
            DeliveryProofSummary(
                totalProofs = proofs.size,
                draftCount = proofs.count { it.proofStatus == DeliveryProofStatus.DRAFT },
                pendingReviewCount = proofs.count { it.proofStatus == DeliveryProofStatus.PENDING_REVIEW },
                submittedCount = proofs.count { it.proofStatus == DeliveryProofStatus.SUBMITTED },
                verifiedCount = proofs.count { it.proofStatus == DeliveryProofStatus.VERIFIED },
                acceptedCount = proofs.count { it.proofStatus == DeliveryProofStatus.ACCEPTED },
                rejectedCount = proofs.count { it.proofStatus == DeliveryProofStatus.REJECTED },
                cancelledCount = proofs.count { it.proofStatus == DeliveryProofStatus.CANCELLED },
                totalEvidenceCount = 0
            )
        }
    }

    override suspend fun getProof(
        proofId: String,
        callerRole: UserRole?,
        callerProjectId: String?
    ): DomainResult<DeliveryProof> = mutex.withLock {
        val proof = proofDataSource.getProof(proofId)
            ?: return DomainResult.Error(message = "Proof of Delivery '$proofId' not found.")

        if (callerRole != null) {
            val authCheck = DeliveryProofAuthorizationValidator.validateOperation(
                callerRole = callerRole,
                operation = DeliveryProofOperation.VIEW,
                targetProjectId = proof.projectId,
                callerProjectId = callerProjectId
            )
            if (authCheck is DomainResult.Error) return authCheck
        }

        DomainResult.Success(proof)
    }

    override suspend fun getProofByShipment(
        shipmentId: String,
        callerRole: UserRole?,
        callerProjectId: String?
    ): DomainResult<DeliveryProof> = mutex.withLock {
        val proof = proofDataSource.getProofByShipment(shipmentId)
            ?: return DomainResult.Error(message = "No Proof of Delivery found for shipment '$shipmentId'.")

        if (callerRole != null) {
            val authCheck = DeliveryProofAuthorizationValidator.validateOperation(
                callerRole = callerRole,
                operation = DeliveryProofOperation.VIEW,
                targetProjectId = proof.projectId,
                callerProjectId = callerProjectId
            )
            if (authCheck is DomainResult.Error) return authCheck
        }

        DomainResult.Success(proof)
    }

    override suspend fun getProofsByDeliveryOrder(
        deliveryOrderId: String,
        callerRole: UserRole?,
        callerProjectId: String?
    ): DomainResult<List<DeliveryProof>> = mutex.withLock {
        val proofs = proofDataSource.getProofsByDeliveryOrder(deliveryOrderId)
        if (callerRole != null && proofs.isNotEmpty()) {
            val targetProjectId = proofs.first().projectId
            val authCheck = DeliveryProofAuthorizationValidator.validateOperation(
                callerRole = callerRole,
                operation = DeliveryProofOperation.VIEW,
                targetProjectId = targetProjectId,
                callerProjectId = callerProjectId
            )
            if (authCheck is DomainResult.Error) return authCheck
        }
        DomainResult.Success(proofs)
    }

    override suspend fun getEvidenceList(
        proofId: String,
        callerRole: UserRole?,
        callerProjectId: String?
    ): DomainResult<List<DeliveryProofEvidence>> = mutex.withLock {
        val proof = proofDataSource.getProof(proofId)
            ?: return DomainResult.Error(message = "Proof of Delivery '$proofId' not found.")

        if (callerRole != null) {
            val authCheck = DeliveryProofAuthorizationValidator.validateOperation(
                callerRole = callerRole,
                operation = DeliveryProofOperation.VIEW,
                targetProjectId = proof.projectId,
                callerProjectId = callerProjectId
            )
            if (authCheck is DomainResult.Error) return authCheck
        }

        val list = proofDataSource.getEvidenceList(proofId)
        DomainResult.Success(list)
    }

    override suspend fun getRecipient(
        proofId: String,
        callerRole: UserRole?,
        callerProjectId: String?
    ): DomainResult<DeliveryProofRecipient> = mutex.withLock {
        val proof = proofDataSource.getProof(proofId)
            ?: return DomainResult.Error(message = "Proof of Delivery '$proofId' not found.")

        if (callerRole != null) {
            val authCheck = DeliveryProofAuthorizationValidator.validateOperation(
                callerRole = callerRole,
                operation = DeliveryProofOperation.VIEW,
                targetProjectId = proof.projectId,
                callerProjectId = callerProjectId
            )
            if (authCheck is DomainResult.Error) return authCheck
        }

        val recipient = proofDataSource.getRecipient(proofId)
            ?: return DomainResult.Error(message = "Recipient acknowledgment not recorded for POD '$proofId'.")

        DomainResult.Success(recipient)
    }

    override suspend fun getActivityEvents(
        proofId: String,
        callerRole: UserRole?,
        callerProjectId: String?
    ): DomainResult<List<DeliveryProofActivityEvent>> = mutex.withLock {
        val proof = proofDataSource.getProof(proofId)
            ?: return DomainResult.Error(message = "Proof of Delivery '$proofId' not found.")

        if (callerRole != null) {
            val authCheck = DeliveryProofAuthorizationValidator.validateOperation(
                callerRole = callerRole,
                operation = DeliveryProofOperation.VIEW,
                targetProjectId = proof.projectId,
                callerProjectId = callerProjectId
            )
            if (authCheck is DomainResult.Error) return authCheck
        }

        val snapshot = proofDataSource.observeActivityEvents(proofId).first()
        DomainResult.Success(snapshot)
    }

    override suspend fun createProof(
        proof: DeliveryProof,
        actorId: String,
        callerRole: UserRole?,
        callerProjectId: String?
    ): DomainResult<DeliveryProof> = mutex.withLock {
        if (callerRole != null) {
            val authCheck = DeliveryProofAuthorizationValidator.validateOperation(
                callerRole = callerRole,
                operation = DeliveryProofOperation.CREATE,
                targetProjectId = proof.projectId,
                callerProjectId = callerProjectId,
                actorId = actorId
            )
            if (authCheck is DomainResult.Error) return authCheck
        }

        val validation = DeliveryProofValidator.validateProof(proof, proof.projectId)
        if (validation is DomainResult.Error) return validation

        // Check duplicate proof for the same shipment
        val existingForShipment = proofDataSource.getProofByShipment(proof.deliveryShipmentId)
        if (existingForShipment != null && existingForShipment.proofStatus != DeliveryProofStatus.CANCELLED) {
            return DomainResult.Error(
                message = "An active or accepted Proof of Delivery ('${existingForShipment.proofNo}') already exists for shipment '${proof.deliveryShipmentId}'."
            )
        }

        // Validate upstream shipment if dataSource available
        if (shipmentDataSource != null) {
            val shipment = shipmentDataSource.getShipment(proof.deliveryShipmentId)
                ?: return DomainResult.Error(message = "Upstream delivery shipment '${proof.deliveryShipmentId}' does not exist.")

            if (shipment.projectId != proof.projectId) {
                return DomainResult.Error(
                    message = "Project mismatch: Shipment belongs to '${shipment.projectId}', but POD is created for '${proof.projectId}'."
                )
            }

            if (shipment.currentStatus == DeliveryShipmentStatus.CANCELLED) {
                return DomainResult.Error(message = "Cannot create Proof of Delivery for a CANCELLED shipment.")
            }
        }

        proofDataSource.insertProof(proof)

        val event = DeliveryProofActivityEvent(
            eventId = UUID.randomUUID().toString(),
            projectId = proof.projectId,
            proofId = proof.proofId,
            activityType = DeliveryProofActivityType.CREATED,
            actorId = actorId,
            actorRole = callerRole,
            previousStatus = null,
            newStatus = proof.proofStatus,
            metadata = mapOf("proofNo" to proof.proofNo, "shipmentId" to proof.deliveryShipmentId),
            notes = "Proof of Delivery created in '${proof.proofStatus.defaultLabel}' state."
        )
        proofDataSource.insertActivityEvent(event)

        DomainResult.Success(proof)
    }

    override suspend fun updateDraftProof(
        proof: DeliveryProof,
        actorId: String,
        callerRole: UserRole?,
        callerProjectId: String?
    ): DomainResult<DeliveryProof> = mutex.withLock {
        val existing = proofDataSource.getProof(proof.proofId)
            ?: return DomainResult.Error(message = "Proof of Delivery '${proof.proofId}' not found.")

        if (callerRole != null) {
            val authCheck = DeliveryProofAuthorizationValidator.validateOperation(
                callerRole = callerRole,
                operation = DeliveryProofOperation.UPDATE,
                targetProjectId = existing.projectId,
                callerProjectId = callerProjectId,
                actorId = actorId
            )
            if (authCheck is DomainResult.Error) return authCheck
        }

        if (!existing.proofStatus.canEdit) {
            return DomainResult.Error(
                message = "Cannot edit POD in '${existing.proofStatus.defaultLabel}' status."
            )
        }

        val validation = DeliveryProofValidator.validateProof(proof, existing.projectId)
        if (validation is DomainResult.Error) return validation

        val updated = proof.copy(
            updatedBy = actorId,
            updatedAt = System.currentTimeMillis()
        )
        proofDataSource.updateProof(updated)

        val event = DeliveryProofActivityEvent(
            eventId = UUID.randomUUID().toString(),
            projectId = updated.projectId,
            proofId = updated.proofId,
            activityType = DeliveryProofActivityType.UPDATED,
            actorId = actorId,
            actorRole = callerRole,
            previousStatus = existing.proofStatus,
            newStatus = updated.proofStatus,
            notes = "Proof of Delivery details updated."
        )
        proofDataSource.insertActivityEvent(event)

        DomainResult.Success(updated)
    }

    override suspend fun addEvidence(
        evidence: DeliveryProofEvidence,
        actorId: String,
        callerRole: UserRole?,
        callerProjectId: String?
    ): DomainResult<DeliveryProofEvidence> = mutex.withLock {
        val proof = proofDataSource.getProof(evidence.proofId)
            ?: return DomainResult.Error(message = "Proof of Delivery '${evidence.proofId}' not found.")

        if (callerRole != null) {
            val authCheck = DeliveryProofAuthorizationValidator.validateOperation(
                callerRole = callerRole,
                operation = DeliveryProofOperation.ADD_EVIDENCE,
                targetProjectId = proof.projectId,
                callerProjectId = callerProjectId,
                actorId = actorId
            )
            if (authCheck is DomainResult.Error) return authCheck
        }

        if (!proof.proofStatus.canAddEvidence) {
            return DomainResult.Error(
                message = "Cannot attach evidence to POD in '${proof.proofStatus.defaultLabel}' status."
            )
        }

        val validation = DeliveryProofEvidenceValidator.validateEvidence(evidence, proof.projectId, proof.proofId)
        if (validation is DomainResult.Error) return validation

        proofDataSource.insertEvidence(evidence)

        val event = DeliveryProofActivityEvent(
            eventId = UUID.randomUUID().toString(),
            projectId = proof.projectId,
            proofId = proof.proofId,
            activityType = DeliveryProofActivityType.EVIDENCE_ADDED,
            actorId = actorId,
            actorRole = callerRole,
            previousStatus = proof.proofStatus,
            newStatus = proof.proofStatus,
            metadata = mapOf("evidenceId" to evidence.evidenceId, "evidenceType" to evidence.evidenceType.name),
            notes = "Evidence '${evidence.fileName}' (${evidence.evidenceType.defaultLabel}) attached."
        )
        proofDataSource.insertActivityEvent(event)

        DomainResult.Success(evidence)
    }

    override suspend fun removeEvidence(
        proofId: String,
        evidenceId: String,
        actorId: String,
        callerRole: UserRole?,
        callerProjectId: String?
    ): DomainResult<Unit> = mutex.withLock {
        val proof = proofDataSource.getProof(proofId)
            ?: return DomainResult.Error(message = "Proof of Delivery '$proofId' not found.")

        if (callerRole != null) {
            val authCheck = DeliveryProofAuthorizationValidator.validateOperation(
                callerRole = callerRole,
                operation = DeliveryProofOperation.REMOVE_EVIDENCE,
                targetProjectId = proof.projectId,
                callerProjectId = callerProjectId,
                actorId = actorId
            )
            if (authCheck is DomainResult.Error) return authCheck
        }

        if (proof.proofStatus.isTerminal) {
            return DomainResult.Error(
                message = "Cannot remove evidence from terminal POD in '${proof.proofStatus.defaultLabel}' status."
            )
        }

        val existing = proofDataSource.getEvidence(evidenceId)
            ?: return DomainResult.Error(message = "Evidence item '$evidenceId' not found.")

        proofDataSource.removeEvidence(evidenceId)

        val event = DeliveryProofActivityEvent(
            eventId = UUID.randomUUID().toString(),
            projectId = proof.projectId,
            proofId = proof.proofId,
            activityType = DeliveryProofActivityType.EVIDENCE_REMOVED,
            actorId = actorId,
            actorRole = callerRole,
            previousStatus = proof.proofStatus,
            newStatus = proof.proofStatus,
            metadata = mapOf("evidenceId" to evidenceId, "fileName" to existing.fileName),
            notes = "Evidence '${existing.fileName}' removed."
        )
        proofDataSource.insertActivityEvent(event)

        DomainResult.Success(Unit)
    }

    override suspend fun confirmRecipient(
        proofId: String,
        recipient: DeliveryProofRecipient,
        actorId: String,
        callerRole: UserRole?,
        callerProjectId: String?
    ): DomainResult<DeliveryProofRecipient> = mutex.withLock {
        val proof = proofDataSource.getProof(proofId)
            ?: return DomainResult.Error(message = "Proof of Delivery '$proofId' not found.")

        if (callerRole != null) {
            val authCheck = DeliveryProofAuthorizationValidator.validateOperation(
                callerRole = callerRole,
                operation = DeliveryProofOperation.CONFIRM_RECIPIENT,
                targetProjectId = proof.projectId,
                callerProjectId = callerProjectId,
                actorId = actorId
            )
            if (authCheck is DomainResult.Error) return authCheck
        }

        if (proof.proofStatus.isTerminal) {
            return DomainResult.Error(
                message = "Cannot record recipient acknowledgment for terminal POD in '${proof.proofStatus.defaultLabel}' status."
            )
        }

        val now = System.currentTimeMillis()
        val confirmedRecipient = recipient.copy(
            proofId = proofId,
            projectId = proof.projectId,
            confirmedAt = recipient.confirmedAt ?: now,
            confirmedBy = actorId
        )
        proofDataSource.insertRecipient(confirmedRecipient)

        val updatedProof = proof.copy(
            recipientName = confirmedRecipient.recipientName,
            recipientPhone = confirmedRecipient.recipientPhone ?: proof.recipientPhone,
            recipientType = confirmedRecipient.recipientType,
            receivedAt = confirmedRecipient.confirmedAt ?: now,
            updatedBy = actorId,
            updatedAt = now
        )
        proofDataSource.updateProof(updatedProof)

        val event = DeliveryProofActivityEvent(
            eventId = UUID.randomUUID().toString(),
            projectId = proof.projectId,
            proofId = proof.proofId,
            activityType = DeliveryProofActivityType.RECIPIENT_CONFIRMED,
            actorId = actorId,
            actorRole = callerRole,
            previousStatus = proof.proofStatus,
            newStatus = proof.proofStatus,
            metadata = mapOf("recipientName" to confirmedRecipient.recipientName),
            notes = "Recipient acknowledgment recorded for '${confirmedRecipient.recipientName}'."
        )
        proofDataSource.insertActivityEvent(event)

        DomainResult.Success(confirmedRecipient)
    }

    override suspend fun submitProof(
        proofId: String,
        actorId: String,
        notes: String?,
        callerRole: UserRole?,
        callerProjectId: String?
    ): DomainResult<DeliveryProof> = mutex.withLock {
        val proof = proofDataSource.getProof(proofId)
            ?: return DomainResult.Error(message = "Proof of Delivery '$proofId' not found.")

        if (callerRole != null) {
            val authCheck = DeliveryProofAuthorizationValidator.validateOperation(
                callerRole = callerRole,
                operation = DeliveryProofOperation.SUBMIT,
                targetProjectId = proof.projectId,
                callerProjectId = callerProjectId,
                actorId = actorId
            )
            if (authCheck is DomainResult.Error) return authCheck
        }

        val lifecycleCheck = DeliveryProofLifecycleValidator.validateTransition(
            currentStatus = proof.proofStatus,
            targetStatus = DeliveryProofStatus.SUBMITTED
        )
        if (lifecycleCheck is DomainResult.Error) return lifecycleCheck

        val evidenceList = proofDataSource.getEvidenceList(proofId)
        val recipient = proofDataSource.getRecipient(proofId)
        val completenessCheck = DeliveryProofCompletionService.validateEvidenceCompleteness(proof, evidenceList, recipient)
        if (completenessCheck is DomainResult.Error) return completenessCheck

        val now = System.currentTimeMillis()
        val updated = proof.copy(
            proofStatus = DeliveryProofStatus.SUBMITTED,
            submittedAt = now,
            notes = notes ?: proof.notes,
            updatedBy = actorId,
            updatedAt = now
        )
        proofDataSource.updateProof(updated)

        val event = DeliveryProofActivityEvent(
            eventId = UUID.randomUUID().toString(),
            projectId = proof.projectId,
            proofId = proof.proofId,
            activityType = DeliveryProofActivityType.SUBMITTED,
            actorId = actorId,
            actorRole = callerRole,
            previousStatus = proof.proofStatus,
            newStatus = DeliveryProofStatus.SUBMITTED,
            notes = notes ?: "Proof of Delivery submitted for review."
        )
        proofDataSource.insertActivityEvent(event)

        DomainResult.Success(updated)
    }

    override suspend fun startReview(
        proofId: String,
        actorId: String,
        notes: String?,
        callerRole: UserRole?,
        callerProjectId: String?
    ): DomainResult<DeliveryProof> = mutex.withLock {
        val proof = proofDataSource.getProof(proofId)
            ?: return DomainResult.Error(message = "Proof of Delivery '$proofId' not found.")

        if (callerRole != null) {
            val authCheck = DeliveryProofAuthorizationValidator.validateOperation(
                callerRole = callerRole,
                operation = DeliveryProofOperation.REVIEW,
                targetProjectId = proof.projectId,
                callerProjectId = callerProjectId,
                actorId = actorId
            )
            if (authCheck is DomainResult.Error) return authCheck
        }

        val lifecycleCheck = DeliveryProofLifecycleValidator.validateTransition(
            currentStatus = proof.proofStatus,
            targetStatus = DeliveryProofStatus.PENDING_REVIEW
        )
        if (lifecycleCheck is DomainResult.Error) return lifecycleCheck

        val now = System.currentTimeMillis()
        val updated = proof.copy(
            proofStatus = DeliveryProofStatus.PENDING_REVIEW,
            reviewedAt = now,
            reviewedBy = actorId,
            reviewNotes = notes ?: proof.reviewNotes,
            updatedBy = actorId,
            updatedAt = now
        )
        proofDataSource.updateProof(updated)

        val event = DeliveryProofActivityEvent(
            eventId = UUID.randomUUID().toString(),
            projectId = proof.projectId,
            proofId = proof.proofId,
            activityType = DeliveryProofActivityType.REVIEW_STARTED,
            actorId = actorId,
            actorRole = callerRole,
            previousStatus = proof.proofStatus,
            newStatus = DeliveryProofStatus.PENDING_REVIEW,
            notes = notes ?: "Proof of Delivery review started."
        )
        proofDataSource.insertActivityEvent(event)

        DomainResult.Success(updated)
    }

    override suspend fun verifyProof(
        proofId: String,
        actorId: String,
        notes: String?,
        callerRole: UserRole?,
        callerProjectId: String?
    ): DomainResult<DeliveryProof> = mutex.withLock {
        val proof = proofDataSource.getProof(proofId)
            ?: return DomainResult.Error(message = "Proof of Delivery '$proofId' not found.")

        if (callerRole != null) {
            val authCheck = DeliveryProofAuthorizationValidator.validateOperation(
                callerRole = callerRole,
                operation = DeliveryProofOperation.VERIFY,
                targetProjectId = proof.projectId,
                callerProjectId = callerProjectId,
                actorId = actorId
            )
            if (authCheck is DomainResult.Error) return authCheck
        }

        val lifecycleCheck = DeliveryProofLifecycleValidator.validateTransition(
            currentStatus = proof.proofStatus,
            targetStatus = DeliveryProofStatus.VERIFIED
        )
        if (lifecycleCheck is DomainResult.Error) return lifecycleCheck

        val evidenceList = proofDataSource.getEvidenceList(proofId)
        val recipient = proofDataSource.getRecipient(proofId)
        val completenessCheck = DeliveryProofCompletionService.validateEvidenceCompleteness(proof, evidenceList, recipient)
        if (completenessCheck is DomainResult.Error) return completenessCheck

        val now = System.currentTimeMillis()
        val updated = proof.copy(
            proofStatus = DeliveryProofStatus.VERIFIED,
            verifiedAt = now,
            verifiedBy = actorId,
            reviewNotes = notes ?: proof.reviewNotes,
            updatedBy = actorId,
            updatedAt = now
        )
        proofDataSource.updateProof(updated)

        val event = DeliveryProofActivityEvent(
            eventId = UUID.randomUUID().toString(),
            projectId = proof.projectId,
            proofId = proof.proofId,
            activityType = DeliveryProofActivityType.VERIFIED,
            actorId = actorId,
            actorRole = callerRole,
            previousStatus = proof.proofStatus,
            newStatus = DeliveryProofStatus.VERIFIED,
            notes = notes ?: "Proof of Delivery successfully verified."
        )
        proofDataSource.insertActivityEvent(event)

        DomainResult.Success(updated)
    }

    override suspend fun acceptProof(
        proofId: String,
        actorId: String,
        notes: String?,
        callerRole: UserRole?,
        callerProjectId: String?
    ): DomainResult<DeliveryProof> = mutex.withLock {
        val proof = proofDataSource.getProof(proofId)
            ?: return DomainResult.Error(message = "Proof of Delivery '$proofId' not found.")

        if (callerRole != null) {
            val authCheck = DeliveryProofAuthorizationValidator.validateOperation(
                callerRole = callerRole,
                operation = DeliveryProofOperation.ACCEPT,
                targetProjectId = proof.projectId,
                callerProjectId = callerProjectId,
                actorId = actorId,
                creatorId = proof.createdBy
            )
            if (authCheck is DomainResult.Error) return authCheck
        }

        val lifecycleCheck = DeliveryProofLifecycleValidator.validateTransition(
            currentStatus = proof.proofStatus,
            targetStatus = DeliveryProofStatus.ACCEPTED
        )
        if (lifecycleCheck is DomainResult.Error) return lifecycleCheck

        val evidenceList = proofDataSource.getEvidenceList(proofId)
        val recipient = proofDataSource.getRecipient(proofId)
        val completenessCheck = DeliveryProofCompletionService.validateEvidenceCompleteness(proof, evidenceList, recipient)
        if (completenessCheck is DomainResult.Error) return completenessCheck

        val now = System.currentTimeMillis()
        val updated = proof.copy(
            proofStatus = DeliveryProofStatus.ACCEPTED,
            acceptedAt = now,
            acceptedBy = actorId,
            reviewNotes = notes ?: proof.reviewNotes,
            updatedBy = actorId,
            updatedAt = now
        )
        proofDataSource.updateProof(updated)

        val event = DeliveryProofActivityEvent(
            eventId = UUID.randomUUID().toString(),
            projectId = proof.projectId,
            proofId = proof.proofId,
            activityType = DeliveryProofActivityType.ACCEPTED,
            actorId = actorId,
            actorRole = callerRole,
            previousStatus = proof.proofStatus,
            newStatus = DeliveryProofStatus.ACCEPTED,
            notes = notes ?: "Proof of Delivery officially accepted."
        )
        proofDataSource.insertActivityEvent(event)

        // Integrate with DeliveryShipment if available
        if (shipmentDataSource != null) {
            val shipment = shipmentDataSource.getShipment(proof.deliveryShipmentId)
            if (shipment != null && shipment.currentStatus != DeliveryShipmentStatus.DELIVERED) {
                val updatedShipment = shipment.copy(
                    currentStatus = DeliveryShipmentStatus.DELIVERED,
                    actualDeliveryAt = proof.deliveredAt ?: now,
                    updatedBy = actorId,
                    updatedAt = now
                )
                shipmentDataSource.updateShipment(updatedShipment)
            }
        }

        DomainResult.Success(updated)
    }

    override suspend fun rejectProof(
        proofId: String,
        reason: String,
        actorId: String,
        callerRole: UserRole?,
        callerProjectId: String?
    ): DomainResult<DeliveryProof> = mutex.withLock {
        if (reason.isBlank()) {
            return DomainResult.Error(message = "Rejection reason cannot be blank.")
        }

        val proof = proofDataSource.getProof(proofId)
            ?: return DomainResult.Error(message = "Proof of Delivery '$proofId' not found.")

        if (callerRole != null) {
            val authCheck = DeliveryProofAuthorizationValidator.validateOperation(
                callerRole = callerRole,
                operation = DeliveryProofOperation.REJECT,
                targetProjectId = proof.projectId,
                callerProjectId = callerProjectId,
                actorId = actorId
            )
            if (authCheck is DomainResult.Error) return authCheck
        }

        val lifecycleCheck = DeliveryProofLifecycleValidator.validateTransition(
            currentStatus = proof.proofStatus,
            targetStatus = DeliveryProofStatus.REJECTED
        )
        if (lifecycleCheck is DomainResult.Error) return lifecycleCheck

        val now = System.currentTimeMillis()
        val updated = proof.copy(
            proofStatus = DeliveryProofStatus.REJECTED,
            rejectionReason = reason,
            rejectedAt = now,
            rejectedBy = actorId,
            updatedBy = actorId,
            updatedAt = now
        )
        proofDataSource.updateProof(updated)

        val event = DeliveryProofActivityEvent(
            eventId = UUID.randomUUID().toString(),
            projectId = proof.projectId,
            proofId = proof.proofId,
            activityType = DeliveryProofActivityType.REJECTED,
            actorId = actorId,
            actorRole = callerRole,
            previousStatus = proof.proofStatus,
            newStatus = DeliveryProofStatus.REJECTED,
            notes = "Proof of Delivery rejected: $reason"
        )
        proofDataSource.insertActivityEvent(event)

        DomainResult.Success(updated)
    }

    override suspend fun cancelProof(
        proofId: String,
        reason: String,
        actorId: String,
        callerRole: UserRole?,
        callerProjectId: String?
    ): DomainResult<DeliveryProof> = mutex.withLock {
        val proof = proofDataSource.getProof(proofId)
            ?: return DomainResult.Error(message = "Proof of Delivery '$proofId' not found.")

        if (callerRole != null) {
            val authCheck = DeliveryProofAuthorizationValidator.validateOperation(
                callerRole = callerRole,
                operation = DeliveryProofOperation.CANCEL,
                targetProjectId = proof.projectId,
                callerProjectId = callerProjectId,
                actorId = actorId
            )
            if (authCheck is DomainResult.Error) return authCheck
        }

        val lifecycleCheck = DeliveryProofLifecycleValidator.validateTransition(
            currentStatus = proof.proofStatus,
            targetStatus = DeliveryProofStatus.CANCELLED
        )
        if (lifecycleCheck is DomainResult.Error) return lifecycleCheck

        val now = System.currentTimeMillis()
        val updated = proof.copy(
            proofStatus = DeliveryProofStatus.CANCELLED,
            cancelledAt = now,
            notes = if (reason.isNotBlank()) "${proof.notes ?: ""}\nCancellation reason: $reason".trim() else proof.notes,
            updatedBy = actorId,
            updatedAt = now
        )
        proofDataSource.updateProof(updated)

        val event = DeliveryProofActivityEvent(
            eventId = UUID.randomUUID().toString(),
            projectId = proof.projectId,
            proofId = proof.proofId,
            activityType = DeliveryProofActivityType.CANCELLED,
            actorId = actorId,
            actorRole = callerRole,
            previousStatus = proof.proofStatus,
            newStatus = DeliveryProofStatus.CANCELLED,
            notes = "Proof of Delivery cancelled: $reason"
        )
        proofDataSource.insertActivityEvent(event)

        DomainResult.Success(updated)
    }
}
