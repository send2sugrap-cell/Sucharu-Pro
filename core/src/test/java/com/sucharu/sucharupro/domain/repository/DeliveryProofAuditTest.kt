package com.sucharu.sucharupro.domain.repository

import com.sucharu.sucharupro.data.datasource.FakeDeliveryProofDataSource
import com.sucharu.sucharupro.data.datasource.FakeDeliveryShipmentDataSource
import com.sucharu.sucharupro.data.repository.DeliveryProofRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.delivery.pod.DeliveryProof
import com.sucharu.sucharupro.domain.model.delivery.pod.DeliveryProofActivityType
import com.sucharu.sucharupro.domain.model.delivery.pod.DeliveryProofEvidence
import com.sucharu.sucharupro.domain.model.delivery.pod.DeliveryProofEvidenceType
import com.sucharu.sucharupro.domain.model.delivery.pod.DeliveryProofRecipient
import com.sucharu.sucharupro.domain.model.delivery.pod.DeliveryProofStatus
import com.sucharu.sucharupro.domain.model.delivery.pod.DeliveryProofType
import com.sucharu.sucharupro.domain.model.delivery.shipment.DeliveryShipment
import com.sucharu.sucharupro.domain.model.delivery.shipment.DeliveryShipmentStatus
import com.sucharu.sucharupro.domain.model.user.UserRole
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class DeliveryProofAuditTest {

    private lateinit var proofDataSource: FakeDeliveryProofDataSource
    private lateinit var shipmentDataSource: FakeDeliveryShipmentDataSource
    private lateinit var repository: DeliveryProofRepository

    @Before
    fun setUp() = runBlocking {
        proofDataSource = FakeDeliveryProofDataSource()
        shipmentDataSource = FakeDeliveryShipmentDataSource()
        repository = DeliveryProofRepositoryImpl(proofDataSource, shipmentDataSource)

        val shipment = DeliveryShipment(
            shipmentId = "SHP-AUDIT",
            projectId = "PRJ-01",
            shipmentNo = "S-AUDIT",
            deliveryOrderId = "DO-01",
            deliveryChallanId = "CH-01",
            dispatchExecutionId = "DISP-01",
            currentStatus = DeliveryShipmentStatus.IN_TRANSIT,
            createdBy = "user-1",
            createdAt = 1000L,
            updatedAt = 1000L
        )
        shipmentDataSource.insertShipment(shipment)
    }

    @Test
    fun `proof operations generate full structured audit trail events`() = runBlocking {
        val proof = DeliveryProof(
            proofId = "POD-AUDIT",
            projectId = "PRJ-01",
            deliveryOrderId = "DO-01",
            deliveryChallanId = "CH-01",
            dispatchExecutionId = "DISP-01",
            deliveryShipmentId = "SHP-AUDIT",
            proofNo = "POD-NO-AUDIT",
            proofType = DeliveryProofType.SIGNATURE,
            proofStatus = DeliveryProofStatus.DRAFT,
            createdBy = "operator-1",
            createdAt = 1000L,
            updatedAt = 1000L
        )

        // 1. Create
        repository.createProof(proof, "operator-1", UserRole.WAREHOUSE)

        // 2. Add Evidence
        val evd = DeliveryProofEvidence(
            evidenceId = "EVD-AUDIT",
            proofId = "POD-AUDIT",
            projectId = "PRJ-01",
            evidenceType = DeliveryProofEvidenceType.SIGNATURE_IMAGE,
            storageReference = "gs://bucket/sig.png",
            fileName = "sig.png",
            mimeType = "image/png",
            uploadedBy = "operator-1",
            uploadedAt = 1000L
        )
        repository.addEvidence(evd, "operator-1", UserRole.WAREHOUSE)

        // 3. Confirm Recipient
        val rec = DeliveryProofRecipient(
            recipientId = "REC-AUDIT",
            proofId = "POD-AUDIT",
            projectId = "PRJ-01",
            recipientName = "John Doe",
            confirmedAt = 1000L,
            confirmedBy = "operator-1"
        )
        repository.confirmRecipient("POD-AUDIT", rec, "operator-1", UserRole.WAREHOUSE)

        // 4. Submit
        repository.submitProof("POD-AUDIT", "operator-1", null, UserRole.WAREHOUSE)

        // 5. Review
        repository.startReview("POD-AUDIT", "qc-1", null, UserRole.QC_INSPECTOR)

        // 6. Verify
        repository.verifyProof("POD-AUDIT", "qc-1", null, UserRole.QC_INSPECTOR)

        // 7. Accept
        repository.acceptProof("POD-AUDIT", "manager-1", null, UserRole.MANAGER)

        val eventsRes = repository.getActivityEvents("POD-AUDIT", UserRole.ADMIN)
        assertTrue(eventsRes is DomainResult.Success)
        val eventTypes = (eventsRes as DomainResult.Success).data.map { it.activityType }

        assertTrue(eventTypes.contains(DeliveryProofActivityType.CREATED))
        assertTrue(eventTypes.contains(DeliveryProofActivityType.EVIDENCE_ADDED))
        assertTrue(eventTypes.contains(DeliveryProofActivityType.RECIPIENT_CONFIRMED))
        assertTrue(eventTypes.contains(DeliveryProofActivityType.SUBMITTED))
        assertTrue(eventTypes.contains(DeliveryProofActivityType.REVIEW_STARTED))
        assertTrue(eventTypes.contains(DeliveryProofActivityType.VERIFIED))
        assertTrue(eventTypes.contains(DeliveryProofActivityType.ACCEPTED))
    }
}
