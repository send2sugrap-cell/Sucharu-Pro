package com.sucharu.sucharupro.domain.integration

import com.sucharu.sucharupro.data.datasource.FakeDeliveryOrderDataSource
import com.sucharu.sucharupro.data.datasource.FakeDeliveryProofDataSource
import com.sucharu.sucharupro.data.datasource.FakeDeliveryShipmentDataSource
import com.sucharu.sucharupro.data.repository.DeliveryProofRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.delivery.pod.DeliveryProof
import com.sucharu.sucharupro.domain.model.delivery.pod.DeliveryProofEvidence
import com.sucharu.sucharupro.domain.model.delivery.pod.DeliveryProofEvidenceType
import com.sucharu.sucharupro.domain.model.delivery.pod.DeliveryProofRecipient
import com.sucharu.sucharupro.domain.model.delivery.pod.DeliveryProofRecipientType
import com.sucharu.sucharupro.domain.model.delivery.pod.DeliveryProofStatus
import com.sucharu.sucharupro.domain.model.delivery.pod.DeliveryProofType
import com.sucharu.sucharupro.domain.model.delivery.shipment.DeliveryShipment
import com.sucharu.sucharupro.domain.model.delivery.shipment.DeliveryShipmentStatus
import com.sucharu.sucharupro.domain.model.user.UserRole
import com.sucharu.sucharupro.domain.repository.DeliveryProofRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class DeliveryProofEndToEndTest {

    private lateinit var proofDataSource: FakeDeliveryProofDataSource
    private lateinit var shipmentDataSource: FakeDeliveryShipmentDataSource
    private lateinit var orderDataSource: FakeDeliveryOrderDataSource
    private lateinit var repository: DeliveryProofRepository

    @Before
    fun setUp() = runBlocking {
        proofDataSource = FakeDeliveryProofDataSource()
        shipmentDataSource = FakeDeliveryShipmentDataSource()
        orderDataSource = FakeDeliveryOrderDataSource()
        repository = DeliveryProofRepositoryImpl(proofDataSource, shipmentDataSource, orderDataSource)

        val shipment = DeliveryShipment(
            shipmentId = "SHP-E2E",
            projectId = "PRJ-01",
            shipmentNo = "SHP-NO-E2E",
            deliveryOrderId = "DO-E2E",
            deliveryChallanId = "CH-E2E",
            dispatchExecutionId = "DISP-E2E",
            currentStatus = DeliveryShipmentStatus.OUT_FOR_DELIVERY,
            createdBy = "user-1",
            createdAt = 1000L,
            updatedAt = 1000L
        )
        shipmentDataSource.insertShipment(shipment)
    }

    @Test
    fun `complete end to end proof of delivery scenario with multi-evidence verification and shipment update`() = runBlocking {
        // 1. Create Draft POD
        val proof = DeliveryProof(
            proofId = "POD-E2E-01",
            projectId = "PRJ-01",
            deliveryOrderId = "DO-E2E",
            deliveryChallanId = "CH-E2E",
            dispatchExecutionId = "DISP-E2E",
            deliveryShipmentId = "SHP-E2E",
            proofNo = "POD-2026-001",
            proofType = DeliveryProofType.COMBINED,
            proofStatus = DeliveryProofStatus.DRAFT,
            createdBy = "operator-1",
            createdAt = 1000L,
            updatedAt = 1000L
        )
        val createRes = repository.createProof(proof, "operator-1", UserRole.WAREHOUSE)
        assertTrue(createRes is DomainResult.Success)

        // 2. Attach Signature and Photo Evidence (Multi-factor)
        val sigEvidence = DeliveryProofEvidence(
            evidenceId = "EVD-SIG-01",
            proofId = "POD-E2E-01",
            projectId = "PRJ-01",
            evidenceType = DeliveryProofEvidenceType.SIGNATURE_IMAGE,
            storageReference = "gs://sucharu-pro/pod/signature-01.png",
            fileName = "signature-01.png",
            mimeType = "image/png",
            isPrimary = true,
            uploadedBy = "operator-1",
            uploadedAt = 1000L
        )
        val photoEvidence = DeliveryProofEvidence(
            evidenceId = "EVD-PHT-01",
            proofId = "POD-E2E-01",
            projectId = "PRJ-01",
            evidenceType = DeliveryProofEvidenceType.DELIVERY_PHOTO,
            storageReference = "gs://sucharu-pro/pod/delivery-photo-01.jpg",
            fileName = "delivery-photo-01.jpg",
            mimeType = "image/jpeg",
            uploadedBy = "operator-1",
            uploadedAt = 1000L
        )
        repository.addEvidence(sigEvidence, "operator-1", UserRole.WAREHOUSE)
        repository.addEvidence(photoEvidence, "operator-1", UserRole.WAREHOUSE)

        // 3. Confirm Recipient
        val recipient = DeliveryProofRecipient(
            recipientId = "REC-001",
            proofId = "POD-E2E-01",
            projectId = "PRJ-01",
            recipientName = "Tareq Mahmud",
            recipientPhone = "+8801711223344",
            recipientType = DeliveryProofRecipientType.PRIMARY_CONTACT,
            confirmationMethod = "SIGNATURE",
            confirmedAt = 1000L,
            confirmedBy = "operator-1"
        )
        val confirmRes = repository.confirmRecipient("POD-E2E-01", recipient, "operator-1", UserRole.WAREHOUSE)
        assertTrue(confirmRes is DomainResult.Success)

        // 4. Submit POD
        val submitRes = repository.submitProof("POD-E2E-01", "operator-1", "Delivered directly to client", UserRole.WAREHOUSE)
        assertTrue(submitRes is DomainResult.Success)

        // 5. Review by QC Inspector
        val reviewRes = repository.startReview("POD-E2E-01", "qc-lead", "Inspecting evidence quality", UserRole.QC_INSPECTOR)
        assertTrue(reviewRes is DomainResult.Success)

        // 6. Verify by QC Inspector
        val verifyRes = repository.verifyProof("POD-E2E-01", "qc-lead", "Signature and photo verified valid", UserRole.QC_INSPECTOR)
        assertTrue(verifyRes is DomainResult.Success)

        // 7. Final Acceptance by Manager
        val acceptRes = repository.acceptProof("POD-E2E-01", "manager-1", "Approved for final delivery confirmation", UserRole.MANAGER)
        assertTrue(acceptRes is DomainResult.Success)

        // Verify status and summary metrics
        val finalProof = (repository.getProof("POD-E2E-01", UserRole.ADMIN) as DomainResult.Success).data
        assertEquals(DeliveryProofStatus.ACCEPTED, finalProof.proofStatus)

        val summary = repository.observeProofSummary("PRJ-01").first()
        assertEquals(1, summary.totalProofs)
        assertEquals(1, summary.acceptedCount)

        // Verify upstream shipment state advanced to DELIVERED
        val shipment = shipmentDataSource.getShipment("SHP-E2E")
        assertNotNull(shipment)
        assertEquals(DeliveryShipmentStatus.DELIVERED, shipment?.currentStatus)
    }
}
