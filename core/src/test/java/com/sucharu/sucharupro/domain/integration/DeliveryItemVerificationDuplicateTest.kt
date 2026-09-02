package com.sucharu.sucharupro.domain.integration

import com.sucharu.sucharupro.data.datasource.FakeDeliveryItemVerificationDataSource
import com.sucharu.sucharupro.data.datasource.FakeDispatchExecutionDataSource
import com.sucharu.sucharupro.data.repository.DeliveryItemVerificationRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.delivery.dispatch.DispatchExecution
import com.sucharu.sucharupro.domain.model.delivery.dispatch.DispatchExecutionLine
import com.sucharu.sucharupro.domain.model.delivery.dispatch.DispatchExecutionStatus
import com.sucharu.sucharupro.domain.model.delivery.dispatch.DispatchExecutionType
import com.sucharu.sucharupro.domain.model.delivery.verification.DeliveryItemVerification
import com.sucharu.sucharupro.domain.model.delivery.verification.DeliveryItemVerificationLine
import com.sucharu.sucharupro.domain.model.delivery.verification.DeliveryItemVerificationStatus
import com.sucharu.sucharupro.domain.model.user.UserRole
import com.sucharu.sucharupro.domain.repository.DeliveryItemVerificationRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class DeliveryItemVerificationDuplicateTest {

    private lateinit var verificationDataSource: FakeDeliveryItemVerificationDataSource
    private lateinit var dispatchDataSource: FakeDispatchExecutionDataSource
    private lateinit var repository: DeliveryItemVerificationRepository

    @Before
    fun setUp() {
        runBlocking {
            verificationDataSource = FakeDeliveryItemVerificationDataSource()
            dispatchDataSource = FakeDispatchExecutionDataSource()
            repository = DeliveryItemVerificationRepositoryImpl(verificationDataSource, dispatchDataSource)

            val dispatch = DispatchExecution(
                dispatchExecutionId = "DISP-DUP",
                projectId = "PRJ-01",
                dispatchNo = "DN-DUP",
                deliveryOrderId = "DO-01",
                deliveryChallanId = "CH-01",
                customerId = null,
                sourceWarehouseId = "WH-01",
                sourceLocationId = "LOC-01",
                dispatchType = DispatchExecutionType.STANDARD,
                status = DispatchExecutionStatus.DISPATCHED,
                stockOutId = "SO-DUP",
                dispatchDate = 1000L,
                notes = null,
                createdBy = "user-1",
                createdAt = 1000L,
                updatedAt = 1000L,
                dispatchedBy = "operator",
                dispatchedAt = 1000L
            )
            val dLine = DispatchExecutionLine(
                dispatchExecutionLineId = "DL-DUP",
                projectId = "PRJ-01",
                dispatchExecutionId = "DISP-DUP",
                deliveryChallanLineId = "CL-1",
                deliveryOrderLineId = "DOL-1",
                productId = "PROD-1",
                requestedQuantity = 100.0,
                dispatchQuantity = 100.0,
                batchId = null,
                lotId = null,
                sourceLocationId = "LOC-01",
                createdAt = 1000L
            )
            dispatchDataSource.insertDispatch(dispatch, listOf(dLine))
        }
    }

    @Test
    fun `duplicate active verification for same dispatch execution is rejected`() = runBlocking {
        val v1 = DeliveryItemVerification(
            verificationId = "V-1",
            projectId = "PRJ-01",
            verificationNo = "VN-01",
            deliveryOrderId = "DO-01",
            deliveryChallanId = "CH-01",
            dispatchExecutionId = "DISP-DUP",
            status = DeliveryItemVerificationStatus.DRAFT,
            remarks = null,
            createdBy = "user-1",
            createdAt = 1000L,
            updatedAt = 1000L
        )
        val line1 = DeliveryItemVerificationLine(
            verificationLineId = "VL-1",
            verificationId = "V-1",
            projectId = "PRJ-01",
            dispatchExecutionLineId = "DL-DUP",
            challanLineId = "CL-1",
            deliveryOrderLineId = "DOL-1",
            productId = "PROD-1",
            batchId = null,
            lotId = null,
            expectedQuantity = 100.0,
            verifiedQuantity = 100.0,
            issueQuantity = 0.0,
            createdAt = 1000L
        )
        val res1 = repository.createVerification(v1, listOf(line1), UserRole.ADMIN)
        assertTrue(res1 is DomainResult.Success)

        val v2 = DeliveryItemVerification(
            verificationId = "V-2",
            projectId = "PRJ-01",
            verificationNo = "VN-02",
            deliveryOrderId = "DO-01",
            deliveryChallanId = "CH-01",
            dispatchExecutionId = "DISP-DUP",
            status = DeliveryItemVerificationStatus.DRAFT,
            remarks = null,
            createdBy = "user-2",
            createdAt = 1000L,
            updatedAt = 1000L
        )
        val line2 = DeliveryItemVerificationLine(
            verificationLineId = "VL-2",
            verificationId = "V-2",
            projectId = "PRJ-01",
            dispatchExecutionLineId = "DL-DUP",
            challanLineId = "CL-1",
            deliveryOrderLineId = "DOL-1",
            productId = "PROD-1",
            batchId = null,
            lotId = null,
            expectedQuantity = 100.0,
            verifiedQuantity = 100.0,
            issueQuantity = 0.0,
            createdAt = 1000L
        )
        val res2 = repository.createVerification(v2, listOf(line2), UserRole.ADMIN)

        assertTrue(res2 is DomainResult.Error)
        assertTrue((res2 as DomainResult.Error).message.contains("already exists for dispatch"))
    }
}
