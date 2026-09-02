package com.sucharu.sucharupro.domain.repository

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.delivery.pod.DeliveryProof
import com.sucharu.sucharupro.domain.model.delivery.pod.DeliveryProofActivityEvent
import com.sucharu.sucharupro.domain.model.delivery.pod.DeliveryProofEvidence
import com.sucharu.sucharupro.domain.model.delivery.pod.DeliveryProofRecipient
import com.sucharu.sucharupro.domain.model.delivery.pod.DeliveryProofSummary
import com.sucharu.sucharupro.domain.model.user.UserRole
import kotlinx.coroutines.flow.Flow

/**
 * Domain repository contract for Proof of Delivery (Module 08 Step 08).
 */
interface DeliveryProofRepository {

    // Reactive Queries
    fun observeProofs(projectId: String): Flow<List<DeliveryProof>>

    fun observeProof(proofId: String): Flow<DeliveryProof?>

    fun observeEvidence(proofId: String): Flow<List<DeliveryProofEvidence>>

    fun observeRecipient(proofId: String): Flow<DeliveryProofRecipient?>

    fun observeActivityEvents(proofId: String): Flow<List<DeliveryProofActivityEvent>>

    fun observeProofSummary(projectId: String): Flow<DeliveryProofSummary>

    // Synchronous Queries
    suspend fun getProof(
        proofId: String,
        callerRole: UserRole? = null,
        callerProjectId: String? = null
    ): DomainResult<DeliveryProof>

    suspend fun getProofByShipment(
        shipmentId: String,
        callerRole: UserRole? = null,
        callerProjectId: String? = null
    ): DomainResult<DeliveryProof>

    suspend fun getProofsByDeliveryOrder(
        deliveryOrderId: String,
        callerRole: UserRole? = null,
        callerProjectId: String? = null
    ): DomainResult<List<DeliveryProof>>

    suspend fun getEvidenceList(
        proofId: String,
        callerRole: UserRole? = null,
        callerProjectId: String? = null
    ): DomainResult<List<DeliveryProofEvidence>>

    suspend fun getRecipient(
        proofId: String,
        callerRole: UserRole? = null,
        callerProjectId: String? = null
    ): DomainResult<DeliveryProofRecipient>

    suspend fun getActivityEvents(
        proofId: String,
        callerRole: UserRole? = null,
        callerProjectId: String? = null
    ): DomainResult<List<DeliveryProofActivityEvent>>

    // Mutations
    suspend fun createProof(
        proof: DeliveryProof,
        actorId: String,
        callerRole: UserRole? = null,
        callerProjectId: String? = null
    ): DomainResult<DeliveryProof>

    suspend fun updateDraftProof(
        proof: DeliveryProof,
        actorId: String,
        callerRole: UserRole? = null,
        callerProjectId: String? = null
    ): DomainResult<DeliveryProof>

    suspend fun addEvidence(
        evidence: DeliveryProofEvidence,
        actorId: String,
        callerRole: UserRole? = null,
        callerProjectId: String? = null
    ): DomainResult<DeliveryProofEvidence>

    suspend fun removeEvidence(
        proofId: String,
        evidenceId: String,
        actorId: String,
        callerRole: UserRole? = null,
        callerProjectId: String? = null
    ): DomainResult<Unit>

    suspend fun confirmRecipient(
        proofId: String,
        recipient: DeliveryProofRecipient,
        actorId: String,
        callerRole: UserRole? = null,
        callerProjectId: String? = null
    ): DomainResult<DeliveryProofRecipient>

    suspend fun submitProof(
        proofId: String,
        actorId: String,
        notes: String? = null,
        callerRole: UserRole? = null,
        callerProjectId: String? = null
    ): DomainResult<DeliveryProof>

    suspend fun startReview(
        proofId: String,
        actorId: String,
        notes: String? = null,
        callerRole: UserRole? = null,
        callerProjectId: String? = null
    ): DomainResult<DeliveryProof>

    suspend fun verifyProof(
        proofId: String,
        actorId: String,
        notes: String? = null,
        callerRole: UserRole? = null,
        callerProjectId: String? = null
    ): DomainResult<DeliveryProof>

    suspend fun acceptProof(
        proofId: String,
        actorId: String,
        notes: String? = null,
        callerRole: UserRole? = null,
        callerProjectId: String? = null
    ): DomainResult<DeliveryProof>

    suspend fun rejectProof(
        proofId: String,
        reason: String,
        actorId: String,
        callerRole: UserRole? = null,
        callerProjectId: String? = null
    ): DomainResult<DeliveryProof>

    suspend fun cancelProof(
        proofId: String,
        reason: String,
        actorId: String,
        callerRole: UserRole? = null,
        callerProjectId: String? = null
    ): DomainResult<DeliveryProof>
}
