package com.sucharu.sucharupro.domain.repository

import com.sucharu.sucharupro.data.datasource.FakeDeliveryOrderDataSource
import com.sucharu.sucharupro.data.datasource.FakeDeliveryProofDataSource
import com.sucharu.sucharupro.data.datasource.FakeDeliveryShipmentDataSource
import com.sucharu.sucharupro.data.repository.DeliveryProofRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.delivery.pod.DeliveryProof
import com.sucharu.sucharupro.domain.model.delivery.pod.DeliveryProofEvidence
import com.sucharu.sucharupro.domain.model.delivery.pod.DeliveryProofEvidenceType
import com.sucharu.sucharupro.domain.model.delivery.pod.DeliveryProofRecipient
import com.sucharu.sucharupro.domain.model.delivery.pod.DeliveryProofStatus
import com.sucharu.sucharupro.domain.model.delivery.pod.DeliveryProofType
import com.sucharu.sucharupro.domain.model.delivery.shipment.DeliveryShipment
import com.sucharu.sucharupro.domain.model.delivery.shipment.DeliveryShipmentStatus
import com.sucharu.sucharupro.domain.model.user.UserRole
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class DeliveryProofRepositoryTest {

    private lateinit var proofDataSource: FakeDeliveryProofDataSource
    private lateinit var shipmentDataSource: FakeDeliveryShipmentDataSource
    private lateinit var orderDataSource: FakeDeliveryOrderDataSource
    private lateinit var repository: DeliveryProofRepository

    @Before
    fun setUp() = runBlocking {
        proofDataSource = FakeDeliveryProofDataSource()
        shipmentDataSource = FakeDeliveryShipmentDataSource()
        orderDataSource = FakeDeliveryOrderDataSource()

        repository = DeliveryProofRepositoryImpl(
            proofDataSource = proofDataSource,
            shipmentDataSource = shipmentDataSource,
            orderDataSource = orderDataSource
        )

        val shipment = DeliveryShipment(
            shipmentId = "SHP-01",
            projectId = "PRJ-01",
            shipmentNo = "SHP-NO-01",
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

    private fun sampleProof(
        id: String = "POD-01",
        no: String = "POD-NO-01",
        type: DeliveryProofType = DeliveryProofType.SIGNATURE,
        status: DeliveryProofStatus = DeliveryProofStatus.DRAFT
    ) = DeliveryProof(
        proofId = id,
        projectId = "PRJ-01",
        deliveryOrderId = "DO-01",
        deliveryChallanId = "CH-01",
        dispatchExecutionId = "DISP-01",
        deliveryShipmentId = "SHP-01",
        proofNo = no,
        proofType = type,
        proofStatus = status,
        recipientName = "Customer Representative",
        createdBy = "operator-1",
        createdAt = 1000L,
        updatedAt = 1000L
    )

    private fun sampleEvidence(
        evidenceId: String = "EVD-01",
        proofId: String = "POD-01",
        type: DeliveryProofEvidenceType = DeliveryProofEvidenceType.SIGNATURE_IMAGE
    ) = DeliveryProofEvidence(
        evidenceId = evidenceId,
        proofId = proofId,
        projectId = "PRJ-01",
        evidenceType = type,
        storageReference = "gs://bucket/signatures/pod-01.png",
        fileName = "signature.png",
        mimeType = "image/png",
        uploadedBy = "operator-1",
        uploadedAt = 1000L
    )

    private fun sampleRecipient(
        recipientId: String = "REC-01",
        proofId: String = "POD-01"
    ) = DeliveryProofRecipient(
        recipientId = recipientId,
        proofId = proofId,
        projectId = "PRJ-01",
        recipientName = "Customer Representative",
        recipientPhone = "+8801700000000",
        confirmedAt = 1000L,
        confirmedBy = "operator-1"
    )

    @Test
    fun `createProof creates record and records CREATED event`() = runBlocking {
        val proof = sampleProof()
        val result = repository.createProof(proof, "operator-1", UserRole.WAREHOUSE)
        assertTrue(result is DomainResult.Success)

        val fetched = repository.getProof(proof.proofId, UserRole.ADMIN)
        assertTrue(fetched is DomainResult.Success)
        assertEquals(proof.proofNo, (fetched as DomainResult.Success).data.proofNo)

        val eventsRes = repository.getActivityEvents(proof.proofId, UserRole.ADMIN)
        assertTrue(eventsRes is DomainResult.Success)
        val events = (eventsRes as DomainResult.Success).data
        assertEquals(1, events.size)
        assertEquals(DeliveryProofStatus.DRAFT, events[0].newStatus)
    }

    @Test
    fun `addEvidence and removeEvidence updates evidence store and logs activity`() = runBlocking {
        val proof = sampleProof()
        repository.createProof(proof, "operator-1", UserRole.WAREHOUSE)

        val evidence = sampleEvidence()
        val addRes = repository.addEvidence(evidence, "operator-1", UserRole.WAREHOUSE)
        assertTrue(addRes is DomainResult.Success)

        val listRes = repository.getEvidenceList(proof.proofId, UserRole.ADMIN)
        assertTrue(listRes is DomainResult.Success)
        assertEquals(1, (listRes as DomainResult.Success).data.size)

        val removeRes = repository.removeEvidence(proof.proofId, evidence.evidenceId, "operator-1", UserRole.WAREHOUSE)
        assertTrue(removeRes is DomainResult.Success)

        val listAfter = repository.getEvidenceList(proof.proofId, UserRole.ADMIN)
        assertTrue(listAfter is DomainResult.Success)
        assertEquals(0, (listAfter as DomainResult.Success).data.size)
    }

    @Test
    fun `confirmRecipient updates proof recipient info and stores snapshot`() = runBlocking {
        val proof = sampleProof()
        repository.createProof(proof, "operator-1", UserRole.WAREHOUSE)

        val recipient = sampleRecipient()
        val confirmRes = repository.confirmRecipient(proof.proofId, recipient, "operator-1", UserRole.WAREHOUSE)
        assertTrue(confirmRes is DomainResult.Success)

        val fetchedRec = repository.getRecipient(proof.proofId, UserRole.ADMIN)
        assertTrue(fetchedRec is DomainResult.Success)
        assertEquals("Customer Representative", (fetchedRec as DomainResult.Success).data.recipientName)
    }

    @Test
    fun `full lifecycle workflow from draft to acceptance succeeds and updates shipment`() = runBlocking {
        val proof = sampleProof()
        repository.createProof(proof, "operator-1", UserRole.WAREHOUSE)

        // 1. Attach Evidence
        val evidence = sampleEvidence()
        repository.addEvidence(evidence, "operator-1", UserRole.WAREHOUSE)

        // 2. Confirm Recipient
        val recipient = sampleRecipient()
        repository.confirmRecipient(proof.proofId, recipient, "operator-1", UserRole.WAREHOUSE)

        // 3. Submit POD
        val submitRes = repository.submitProof(proof.proofId, "operator-1", "Ready for review", UserRole.WAREHOUSE)
        assertTrue(submitRes is DomainResult.Success)
        assertEquals(DeliveryProofStatus.SUBMITTED, (submitRes as DomainResult.Success).data.proofStatus)

        // 4. Start Review
        val reviewRes = repository.startReview(proof.proofId, "inspector-1", "Reviewing signature", UserRole.QC_INSPECTOR)
        assertTrue(reviewRes is DomainResult.Success)
        assertEquals(DeliveryProofStatus.PENDING_REVIEW, (reviewRes as DomainResult.Success).data.proofStatus)

        // 5. Verify POD
        val verifyRes = repository.verifyProof(proof.proofId, "inspector-1", "Signature matches receiver", UserRole.QC_INSPECTOR)
        assertTrue(verifyRes is DomainResult.Success)
        assertEquals(DeliveryProofStatus.VERIFIED, (verifyRes as DomainResult.Success).data.proofStatus)

        // 6. Accept POD (by Manager / Admin)
        val acceptRes = repository.acceptProof(proof.proofId, "manager-1", "Accepted officially", UserRole.MANAGER)
        assertTrue(acceptRes is DomainResult.Success)
        assertEquals(DeliveryProofStatus.ACCEPTED, (acceptRes as DomainResult.Success).data.proofStatus)

        // Verify that shipment status was updated to DELIVERED
        val updatedShipment = shipmentDataSource.getShipment("SHP-01")
        assertNotNull(updatedShipment)
        assertEquals(DeliveryShipmentStatus.DELIVERED, updatedShipment?.currentStatus)
    }

    @Test
    fun `rejectProof transitions to REJECTED with mandatory reason`() = runBlocking {
        val proof = sampleProof()
        repository.createProof(proof, "operator-1", UserRole.WAREHOUSE)

        val evidence = sampleEvidence()
        repository.addEvidence(evidence, "operator-1", UserRole.WAREHOUSE)

        repository.submitProof(proof.proofId, "operator-1", null, UserRole.WAREHOUSE)

        val rejectRes = repository.rejectProof(proof.proofId, "Blurry signature", "manager-1", UserRole.MANAGER)
        assertTrue(rejectRes is DomainResult.Success)
        assertEquals(DeliveryProofStatus.REJECTED, (rejectRes as DomainResult.Success).data.proofStatus)
        assertEquals("Blurry signature", (rejectRes as DomainResult.Success).data.rejectionReason)
    }

    @Test
    fun `cancelProof cancels draft proof with valid reason`() = runBlocking {
        val proof = sampleProof()
        repository.createProof(proof, "operator-1", UserRole.WAREHOUSE)

        val cancelRes = repository.cancelProof(proof.proofId, "Shipment recalled", "operator-1", UserRole.WAREHOUSE)
        assertTrue(cancelRes is DomainResult.Success)
        assertEquals(DeliveryProofStatus.CANCELLED, (cancelRes as DomainResult.Success).data.proofStatus)
    }

    @Test
    fun `observeProofSummary computes correct status aggregation`() = runBlocking {
        val proof1 = sampleProof("P-1", "P-NO-1")
        val proof2 = sampleProof("P-2", "P-NO-2", status = DeliveryProofStatus.ACCEPTED)
        proofDataSource.insertProof(proof1)
        proofDataSource.insertProof(proof2)

        val summary = repository.observeProofSummary("PRJ-01").first()
        assertEquals(2, summary.totalProofs)
        assertEquals(1, summary.draftCount)
        assertEquals(1, summary.acceptedCount)
    }
}
