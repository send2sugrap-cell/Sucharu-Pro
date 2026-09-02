package com.sucharu.sucharupro.domain.integration

import com.sucharu.sucharupro.data.datasource.FakeDeliveryItemVerificationDataSource
import com.sucharu.sucharupro.data.datasource.FakeDeliveryShipmentDataSource
import com.sucharu.sucharupro.data.datasource.FakeDispatchExecutionDataSource
import com.sucharu.sucharupro.data.repository.DeliveryShipmentRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.delivery.dispatch.DispatchExecution
import com.sucharu.sucharupro.domain.model.delivery.dispatch.DispatchExecutionLine
import com.sucharu.sucharupro.domain.model.delivery.dispatch.DispatchExecutionStatus
import com.sucharu.sucharupro.domain.model.delivery.dispatch.DispatchExecutionType
import com.sucharu.sucharupro.domain.model.delivery.shipment.DeliveryShipment
import com.sucharu.sucharupro.domain.model.delivery.shipment.DeliveryShipmentStatus
import com.sucharu.sucharupro.domain.model.delivery.verification.DeliveryItemVerification
import com.sucharu.sucharupro.domain.model.delivery.verification.DeliveryItemVerificationLine
import com.sucharu.sucharupro.domain.model.delivery.verification.DeliveryItemVerificationStatus
import com.sucharu.sucharupro.domain.model.user.UserRole
import com.sucharu.sucharupro.domain.repository.DeliveryShipmentRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class DeliveryShipmentVerificationReferenceTest {

    private lateinit var shipmentDataSource: FakeDeliveryShipmentDataSource
    private lateinit var dispatchDataSource: FakeDispatchExecutionDataSource
    private lateinit var verificationDataSource: FakeDeliveryItemVerificationDataSource
    private lateinit var repository: DeliveryShipmentRepository

    @Before
    fun setUp() {
        runBlocking {
            shipmentDataSource = FakeDeliveryShipmentDataSource()
            dispatchDataSource = FakeDispatchExecutionDataSource()
            verificationDataSource = FakeDeliveryItemVerificationDataSource()

            repository = DeliveryShipmentRepositoryImpl(
                shipmentDataSource = shipmentDataSource,
                dispatchDataSource = dispatchDataSource,
                verificationDataSource = verificationDataSource
            )

            val dispatch = DispatchExecution(
                dispatchExecutionId = "DISP-VER",
                projectId = "PRJ-01",
                dispatchNo = "DN-VER",
                deliveryOrderId = "DO-01",
                deliveryChallanId = "CH-01",
                customerId = null,
                sourceWarehouseId = "WH-01",
                sourceLocationId = "LOC-01",
                dispatchType = DispatchExecutionType.STANDARD,
                status = DispatchExecutionStatus.DISPATCHED,
                stockOutId = "SO-VER",
                dispatchDate = 1000L,
                notes = null,
                createdBy = "user-1",
                createdAt = 1000L,
                updatedAt = 1000L,
                dispatchedBy = "operator",
                dispatchedAt = 1000L
            )
            val dLine = DispatchExecutionLine("DL-1", "PRJ-01", "DISP-VER", "CL-1", "DOL-1", "PROD-1", 100.0, 100.0, null, null, "LOC-01", 1000L)
            dispatchDataSource.insertDispatch(dispatch, listOf(dLine))

            val verification = DeliveryItemVerification("V-1", "PRJ-01", "VN-1", "DO-01", "CH-01", "DISP-VER", DeliveryItemVerificationStatus.VERIFIED, null, null, null, "user-1", 1000L, null, 1000L)
            val vLine = DeliveryItemVerificationLine("VL-1", "V-1", "PRJ-01", "DL-1", "CL-1", "DOL-1", "PROD-1", null, null, 100.0, 100.0, 0.0, createdAt = 1000L)
            verificationDataSource.insertVerification(verification, listOf(vLine))
        }
    }

    @Test
    fun `shipment referencing valid verification succeeds and persists reference`() = runBlocking {
        val s = DeliveryShipment(
            shipmentId = "SHP-VER",
            projectId = "PRJ-01",
            shipmentNo = "S-VER",
            deliveryOrderId = "DO-01",
            deliveryChallanId = "CH-01",
            dispatchExecutionId = "DISP-VER",
            verificationId = "V-1",
            currentStatus = DeliveryShipmentStatus.DRAFT,
            createdBy = "user-1",
            createdAt = 1000L,
            updatedAt = 1000L
        )
        val res = repository.createShipment(s, UserRole.ADMIN)
        assertTrue(res is DomainResult.Success)

        val fetched = (repository.getShipment("SHP-VER", UserRole.ADMIN) as DomainResult.Success).data
        assertEquals("V-1", fetched.verificationId)
    }

    @Test
    fun `shipment referencing non-existent verification fails`() = runBlocking {
        val s = DeliveryShipment(
            shipmentId = "SHP-VER-FAIL",
            projectId = "PRJ-01",
            shipmentNo = "S-VER-FAIL",
            deliveryOrderId = "DO-01",
            deliveryChallanId = "CH-01",
            dispatchExecutionId = "DISP-VER",
            verificationId = "V-NON-EXISTENT",
            currentStatus = DeliveryShipmentStatus.DRAFT,
            createdBy = "user-1",
            createdAt = 1000L,
            updatedAt = 1000L
        )
        val res = repository.createShipment(s, UserRole.ADMIN)
        assertTrue(res is DomainResult.Error)
        assertTrue((res as DomainResult.Error).message.contains("Referenced Delivery Verification 'V-NON-EXISTENT' not found"))
    }
}
