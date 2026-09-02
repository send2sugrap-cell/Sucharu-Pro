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
import com.sucharu.sucharupro.domain.model.delivery.verification.DeliveryItemVerificationLine
import com.sucharu.sucharupro.domain.model.delivery.verification.DeliveryItemVerificationResultType
import com.sucharu.sucharupro.domain.model.delivery.verification.DeliveryItemVerificationStatus
import com.sucharu.sucharupro.domain.model.user.UserRole
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class DeliveryItemVerificationRepositoryTest {

    private lateinit var verificationDataSource: FakeDeliveryItemVerificationDataSource
    private lateinit var dispatchDataSource: FakeDispatchExecutionDataSource
    private lateinit var repository: DeliveryItemVerificationRepository

    @Before
    fun setUp() {
        runBlocking {
            verificationDataSource = FakeDeliveryItemVerificationDataSource()
            dispatchDataSource = FakeDispatchExecutionDataSource()
            repository = DeliveryItemVerificationRepositoryImpl(
                verificationDataSource = verificationDataSource,
                dispatchDataSource = dispatchDataSource
            )

            val dispatch = DispatchExecution(
                dispatchExecutionId = "DISP-01",
                projectId = "PRJ-01",
                dispatchNo = "DN-01",
                deliveryOrderId = "DO-01",
                deliveryChallanId = "CH-01",
                customerId = "CUST-01",
                sourceWarehouseId = "WH-01",
                sourceLocationId = "LOC-01",
                dispatchType = DispatchExecutionType.STANDARD,
                status = DispatchExecutionStatus.DISPATCHED,
                stockOutId = "SO-01",
                dispatchDate = 1000L,
                notes = null,
                createdBy = "user-1",
                createdAt = 1000L,
                updatedAt = 1000L,
                dispatchedBy = "operator",
                dispatchedAt = 1000L
            )
            val dLine = DispatchExecutionLine(
                dispatchExecutionLineId = "DLINE-01",
                projectId = "PRJ-01",
                dispatchExecutionId = "DISP-01",
                deliveryChallanLineId = "CLINE-01",
                deliveryOrderLineId = "DOLINE-01",
                productId = "PROD-01",
                requestedQuantity = 100.0,
                dispatchQuantity = 100.0,
                sourceLocationId = "LOC-01",
                createdAt = 1000L
            )
            dispatchDataSource.insertDispatch(dispatch, listOf(dLine))
        }
    }

    private fun sampleVerification(id: String = "VERIF-01", no: String = "V-001") = DeliveryItemVerification(
        verificationId = id,
        projectId = "PRJ-01",
        verificationNo = no,
        deliveryOrderId = "DO-01",
        deliveryChallanId = "CH-01",
        dispatchExecutionId = "DISP-01",
        status = DeliveryItemVerificationStatus.DRAFT,
        remarks = null,
        createdBy = "user-1",
        createdAt = 1000L,
        updatedAt = 1000L
    )

    private fun sampleLine(lineId: String = "VLINE-01", verifId: String = "VERIF-01", qty: Double = 100.0) = DeliveryItemVerificationLine(
        verificationLineId = lineId,
        verificationId = verifId,
        projectId = "PRJ-01",
        dispatchExecutionLineId = "DLINE-01",
        challanLineId = "CLINE-01",
        deliveryOrderLineId = "DOLINE-01",
        productId = "PROD-01",
        expectedQuantity = 100.0,
        verifiedQuantity = qty,
        createdAt = 1000L
    )

    @Test
    fun `createVerification successfully creates verification and lines`() = runBlocking {
        val verification = sampleVerification()
        val lines = listOf(sampleLine())

        val result = repository.createVerification(verification, lines, UserRole.ADMIN)
        assertTrue(result is DomainResult.Success)

        val fetched = repository.getVerification(verification.verificationId, UserRole.ADMIN)
        assertTrue(fetched is DomainResult.Success)
        assertEquals(verification.verificationNo, (fetched as DomainResult.Success).data.verificationNo)
    }

    @Test
    fun `full verification workflow transitions properly`() = runBlocking {
        val verification = sampleVerification()
        val lines = listOf(sampleLine())
        repository.createVerification(verification, lines, UserRole.ADMIN)

        // 1. Submit
        val submitRes = repository.submitVerification(verification.verificationId, "user-1", UserRole.ADMIN)
        assertTrue(submitRes is DomainResult.Success)
        assertEquals(DeliveryItemVerificationStatus.PENDING, (submitRes as DomainResult.Success).data.status)

        // 2. Start
        val startRes = repository.startVerification(verification.verificationId, "operator", UserRole.WAREHOUSE)
        assertTrue(startRes is DomainResult.Success)
        assertEquals(DeliveryItemVerificationStatus.IN_PROGRESS, (startRes as DomainResult.Success).data.status)

        // 3. Verify Line with shortage
        val verifyLineRes = repository.verifyLine(
            verificationId = verification.verificationId,
            verificationLineId = "VLINE-01",
            verifiedQuantity = 90.0,
            isDamaged = false,
            damagedQuantity = 0.0,
            isMissing = false,
            isProductMismatch = false,
            isBatchMismatch = false,
            isLotMismatch = false,
            remarks = "Short 10 units",
            actorId = "operator",
            callerRole = UserRole.WAREHOUSE
        )
        assertTrue(verifyLineRes is DomainResult.Success)
        assertEquals(DeliveryItemVerificationResultType.SHORT, (verifyLineRes as DomainResult.Success).data.resultType)
        assertEquals(10.0, (verifyLineRes as DomainResult.Success).data.issueQuantity, 0.001)

        // 4. Complete Verification
        val completeRes = repository.completeVerification(verification.verificationId, "operator", UserRole.WAREHOUSE)
        assertTrue(completeRes is DomainResult.Success)
        assertEquals(DeliveryItemVerificationStatus.VERIFIED, (completeRes as DomainResult.Success).data.status)

        // 5. Close Verification
        val closeRes = repository.closeVerification(verification.verificationId, "manager", UserRole.MANAGER)
        assertTrue(closeRes is DomainResult.Success)
        assertEquals(DeliveryItemVerificationStatus.CLOSED, (closeRes as DomainResult.Success).data.status)
    }
}
