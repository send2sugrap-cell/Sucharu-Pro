package com.sucharu.sucharupro.domain.repository

import com.sucharu.sucharupro.data.datasource.FakeDeliveryProofDataSource
import com.sucharu.sucharupro.data.datasource.FakeDeliveryShipmentDataSource
import com.sucharu.sucharupro.data.repository.DeliveryProofRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.delivery.pod.DeliveryProof
import com.sucharu.sucharupro.domain.model.delivery.pod.DeliveryProofEvidence
import com.sucharu.sucharupro.domain.model.delivery.pod.DeliveryProofEvidenceType
import com.sucharu.sucharupro.domain.model.delivery.pod.DeliveryProofStatus
import com.sucharu.sucharupro.domain.model.delivery.pod.DeliveryProofType
import com.sucharu.sucharupro.domain.model.delivery.shipment.DeliveryShipment
import com.sucharu.sucharupro.domain.model.delivery.shipment.DeliveryShipmentStatus
import com.sucharu.sucharupro.domain.model.user.UserRole
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class DeliveryProofConcurrencyTest {

    private lateinit var proofDataSource: FakeDeliveryProofDataSource
    private lateinit var shipmentDataSource: FakeDeliveryShipmentDataSource
    private lateinit var repository: DeliveryProofRepository

    @Before
    fun setUp() = runBlocking {
        proofDataSource = FakeDeliveryProofDataSource()
        shipmentDataSource = FakeDeliveryShipmentDataSource()
        repository = DeliveryProofRepositoryImpl(proofDataSource, shipmentDataSource)

        val shipment = DeliveryShipment(
            shipmentId = "SHP-CONCUR",
            projectId = "PRJ-01",
            shipmentNo = "S-CONCUR",
            deliveryOrderId = "DO-01",
            deliveryChallanId = "CH-01",
            dispatchExecutionId = "DISP-01",
            currentStatus = DeliveryShipmentStatus.IN_TRANSIT,
            createdBy = "user-1",
            createdAt = 1000L,
            updatedAt = 1000L
        )
        shipmentDataSource.insertShipment(shipment)

        val proof = DeliveryProof(
            proofId = "POD-CONCUR",
            projectId = "PRJ-01",
            deliveryOrderId = "DO-01",
            deliveryChallanId = "CH-01",
            dispatchExecutionId = "DISP-01",
            deliveryShipmentId = "SHP-CONCUR",
            proofNo = "POD-NO-CONCUR",
            proofType = DeliveryProofType.COMBINED,
            proofStatus = DeliveryProofStatus.DRAFT,
            createdBy = "operator-1",
            createdAt = 1000L,
            updatedAt = 1000L
        )
        proofDataSource.insertProof(proof)
    }

    @Test
    fun `concurrent evidence additions maintain atomic state and complete log`() = runBlocking {
        val jobs = (1..5).map { index ->
            async(Dispatchers.IO) {
                val evd = DeliveryProofEvidence(
                    evidenceId = "EVD-C-$index",
                    proofId = "POD-CONCUR",
                    projectId = "PRJ-01",
                    evidenceType = DeliveryProofEvidenceType.DELIVERY_PHOTO,
                    storageReference = "gs://bucket/photo_$index.jpg",
                    fileName = "photo_$index.jpg",
                    mimeType = "image/jpeg",
                    uploadedBy = "operator-$index",
                    uploadedAt = 1000L + index
                )
                repository.addEvidence(evd, "operator-$index", UserRole.WAREHOUSE)
            }
        }

        val results = jobs.awaitAll()
        assertTrue(results.all { it is DomainResult.Success })

        val evidenceList = (repository.getEvidenceList("POD-CONCUR", UserRole.ADMIN) as DomainResult.Success).data
        assertEquals(5, evidenceList.size)
    }
}
