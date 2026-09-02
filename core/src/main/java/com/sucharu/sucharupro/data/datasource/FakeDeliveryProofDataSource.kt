package com.sucharu.sucharupro.data.datasource

import com.sucharu.sucharupro.domain.model.delivery.pod.DeliveryProof
import com.sucharu.sucharupro.domain.model.delivery.pod.DeliveryProofActivityEvent
import com.sucharu.sucharupro.domain.model.delivery.pod.DeliveryProofEvidence
import com.sucharu.sucharupro.domain.model.delivery.pod.DeliveryProofRecipient
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Thread-safe in-memory data source for Proof of Delivery (Module 08 Step 08).
 */
class FakeDeliveryProofDataSource : DeliveryProofDataSource {

    private val mutex = Mutex()
    private val proofsFlow = MutableStateFlow<Map<String, DeliveryProof>>(emptyMap())
    private val evidenceFlow = MutableStateFlow<Map<String, DeliveryProofEvidence>>(emptyMap())
    private val recipientFlow = MutableStateFlow<Map<String, DeliveryProofRecipient>>(emptyMap())
    private val eventsFlow = MutableStateFlow<List<DeliveryProofActivityEvent>>(emptyList())

    override fun observeProofs(projectId: String): Flow<List<DeliveryProof>> {
        return proofsFlow.map { map ->
            map.values.filter { it.projectId == projectId }.sortedByDescending { it.createdAt }
        }
    }

    override fun observeProof(proofId: String): Flow<DeliveryProof?> {
        return proofsFlow.map { it[proofId] }
    }

    override suspend fun getProof(proofId: String): DeliveryProof? = mutex.withLock {
        proofsFlow.value[proofId]
    }

    override suspend fun getProofByNo(projectId: String, proofNo: String): DeliveryProof? = mutex.withLock {
        proofsFlow.value.values.find { it.projectId == projectId && it.proofNo == proofNo }
    }

    override suspend fun getProofByShipment(shipmentId: String): DeliveryProof? = mutex.withLock {
        proofsFlow.value.values.find { it.deliveryShipmentId == shipmentId }
    }

    override suspend fun getProofsByDeliveryOrder(deliveryOrderId: String): List<DeliveryProof> = mutex.withLock {
        proofsFlow.value.values.filter { it.deliveryOrderId == deliveryOrderId }
    }

    override suspend fun insertProof(proof: DeliveryProof) = mutex.withLock {
        proofsFlow.update { it + (proof.proofId to proof) }
    }

    override suspend fun updateProof(proof: DeliveryProof) = mutex.withLock {
        proofsFlow.update { it + (proof.proofId to proof) }
    }

    override fun observeEvidence(proofId: String): Flow<List<DeliveryProofEvidence>> {
        return evidenceFlow.map { map ->
            map.values.filter { it.proofId == proofId }.sortedByDescending { it.uploadedAt }
        }
    }

    override suspend fun getEvidenceList(proofId: String): List<DeliveryProofEvidence> = mutex.withLock {
        evidenceFlow.value.values.filter { it.proofId == proofId }
    }

    override suspend fun getEvidence(evidenceId: String): DeliveryProofEvidence? = mutex.withLock {
        evidenceFlow.value[evidenceId]
    }

    override suspend fun insertEvidence(evidence: DeliveryProofEvidence) = mutex.withLock {
        evidenceFlow.update { it + (evidence.evidenceId to evidence) }
    }

    override suspend fun removeEvidence(evidenceId: String) = mutex.withLock {
        evidenceFlow.update { it - evidenceId }
    }

    override fun observeRecipient(proofId: String): Flow<DeliveryProofRecipient?> {
        return recipientFlow.map { it[proofId] }
    }

    override suspend fun getRecipient(proofId: String): DeliveryProofRecipient? = mutex.withLock {
        recipientFlow.value[proofId]
    }

    override suspend fun insertRecipient(recipient: DeliveryProofRecipient) = mutex.withLock {
        recipientFlow.update { it + (recipient.proofId to recipient) }
    }

    override suspend fun updateRecipient(recipient: DeliveryProofRecipient) = mutex.withLock {
        recipientFlow.update { it + (recipient.proofId to recipient) }
    }

    override fun observeActivityEvents(proofId: String): Flow<List<DeliveryProofActivityEvent>> {
        return eventsFlow.map { list ->
            list.filter { it.proofId == proofId }.sortedBy { it.timestamp }
        }
    }

    override suspend fun insertActivityEvent(event: DeliveryProofActivityEvent) = mutex.withLock {
        eventsFlow.update { it + event }
    }
}
