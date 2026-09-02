package com.sucharu.sucharupro.domain.repository

import com.sucharu.sucharupro.data.datasource.FakeDeliveryItemVerificationDataSource
import com.sucharu.sucharupro.data.datasource.FakeDispatchExecutionDataSource
import com.sucharu.sucharupro.data.repository.DeliveryItemVerificationRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.delivery.dispatch.DispatchExecution
import com.sucharu.sucharupro.domain.model.delivery.dispatch.DispatchExecutionLine
import com.sucharu.sucharupro.domain.model.delivery.dispatch.DispatchExecutionStatus
import com.sucharu.sucharupro.domain.model.delivery.dispatch.DispatchExecutionType
import com.sucharu.sucharupro.domain.model.delivery.verification.DeliveryItemVerification
import com.sucharu.sucharupro.domain.model.delivery.verification.DeliveryItemVerificationActivityType
import com.sucharu.sucharupro.domain.model.delivery.verification.DeliveryItemVerificationLine
import com.sucharu.sucharupro.domain.model.delivery.verification.DeliveryItemVerificationStatus
import com.sucharu.sucharupro.domain.model.user.UserRole
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class DeliveryItemVerificationAuditTest {

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
                dispatchExecutionId = "DISP-AUDIT",
                projectId = "PRJ-01",
                dispatchNo = "DN-AUDIT",
                deliveryOrderId = "DO-01",
                deliveryChallanId = "CH-01",
                customerId = "CUST-01",
                sourceWarehouseId = "WH-01",
                sourceLocationId = "LOC-01",
                dispatchType = DispatchExecutionType.STANDARD,
                status = DispatchExecutionStatus.DISPATCHED,
                stockOutId = "SO-AUDIT",
                dispatchDate = 1000L,
                notes = null,
                createdBy = "user-1",
                createdAt = 1000L,
                updatedAt = 1000L,
                dispatchedBy = "operator",
                dispatchedAt = 1000L
            )
            val dLine = DispatchExecutionLine(
                dispatchExecutionLineId = "DL-AUDIT",
                projectId = "PRJ-01",
                dispatchExecutionId = "DISP-AUDIT",
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
    fun `full verification workflow records comprehensive audit history`() = runBlocking {
        val verification = DeliveryItemVerification(
            verificationId = "VERIF-AUDIT",
            projectId = "PRJ-01",
            verificationNo = "V-AUDIT",
            deliveryOrderId = "DO-01",
            deliveryChallanId = "CH-01",
            dispatchExecutionId = "DISP-AUDIT",
            status = DeliveryItemVerificationStatus.DRAFT,
            remarks = null,
            createdBy = "user-1",
            createdAt = 1000L,
            updatedAt = 1000L
        )
        val line = DeliveryItemVerificationLine(
            verificationLineId = "VL-AUDIT",
            verificationId = "VERIF-AUDIT",
            projectId = "PRJ-01",
            dispatchExecutionLineId = "DL-AUDIT",
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

        // 1. Create
        repository.createVerification(verification, listOf(line), UserRole.ADMIN)

        // 2. Submit
        repository.submitVerification("VERIF-AUDIT", "submitter", UserRole.ADMIN)

        // 3. Start
        repository.startVerification("VERIF-AUDIT", "operator", UserRole.WAREHOUSE)

        // 4. Verify line
        repository.verifyLine(
            verificationId = "VERIF-AUDIT",
            verificationLineId = "VL-AUDIT",
            verifiedQuantity = 100.0,
            isDamaged = false,
            damagedQuantity = 0.0,
            isMissing = false,
            isProductMismatch = false,
            isBatchMismatch = false,
            isLotMismatch = false,
            remarks = null,
            actorId = "operator",
            callerRole = UserRole.WAREHOUSE
        )

        // 5. Complete
        repository.completeVerification("VERIF-AUDIT", "operator", UserRole.WAREHOUSE)

        // 6. Close
        repository.closeVerification("VERIF-AUDIT", "manager", UserRole.MANAGER)

        val eventsResult = repository.getActivityEvents("VERIF-AUDIT", UserRole.ADMIN)
        assertTrue(eventsResult is DomainResult.Success)
        val events = (eventsResult as DomainResult.Success).data

        val types = events.map { it.activityType }
        assertTrue(types.contains(DeliveryItemVerificationActivityType.CREATED))
        assertTrue(types.contains(DeliveryItemVerificationActivityType.SUBMITTED))
        assertTrue(types.contains(DeliveryItemVerificationActivityType.STARTED))
        assertTrue(types.contains(DeliveryItemVerificationActivityType.LINE_VERIFIED))
        assertTrue(types.contains(DeliveryItemVerificationActivityType.VERIFIED))
        assertTrue(types.contains(DeliveryItemVerificationActivityType.CLOSED))
    }
}
