package com.sucharu.sucharupro.data.datasource

import com.sucharu.sucharupro.domain.model.delivery.pod.DeliveryProof
import com.sucharu.sucharupro.domain.model.delivery.pod.DeliveryProofActivityEvent
import com.sucharu.sucharupro.domain.model.delivery.pod.DeliveryProofEvidence
import com.sucharu.sucharupro.domain.model.delivery.pod.DeliveryProofRecipient
import kotlinx.coroutines.flow.Flow

/**
 * Data source contract for Proof of Delivery subsystem (Module 08 Step 08).
 */
interface DeliveryProofDataSource {

    fun observeProofs(projectId: String): Flow<List<DeliveryProof>>

    fun observeProof(proofId: String): Flow<DeliveryProof?>

    suspend fun getProof(proofId: String): DeliveryProof?

    suspend fun getProofByNo(projectId: String, proofNo: String): DeliveryProof?

    suspend fun getProofByShipment(shipmentId: String): DeliveryProof?

    suspend fun getProofsByDeliveryOrder(deliveryOrderId: String): List<DeliveryProof>

    suspend fun insertProof(proof: DeliveryProof)

    suspend fun updateProof(proof: DeliveryProof)

    fun observeEvidence(proofId: String): Flow<List<DeliveryProofEvidence>>

    suspend fun getEvidenceList(proofId: String): List<DeliveryProofEvidence>

    suspend fun getEvidence(evidenceId: String): DeliveryProofEvidence?

    suspend fun insertEvidence(evidence: DeliveryProofEvidence)

    suspend fun removeEvidence(evidenceId: String)

    fun observeRecipient(proofId: String): Flow<DeliveryProofRecipient?>

    suspend fun getRecipient(proofId: String): DeliveryProofRecipient?

    suspend fun insertRecipient(recipient: DeliveryProofRecipient)

    suspend fun updateRecipient(recipient: DeliveryProofRecipient)

    fun observeActivityEvents(proofId: String): Flow<List<DeliveryProofActivityEvent>>

    suspend fun insertActivityEvent(event: DeliveryProofActivityEvent)
}
