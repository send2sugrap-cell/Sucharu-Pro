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
import com.sucharu.sucharupro.domain.model.delivery.verification.DeliveryItemVerificationStatus
import com.sucharu.sucharupro.domain.model.user.UserRole
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class DeliveryItemVerificationConcurrencyTest {

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
                dispatchExecutionId = "DISP-CONCUR",
                projectId = "PRJ-01",
                dispatchNo = "DN-CONCUR",
                deliveryOrderId = "DO-01",
                deliveryChallanId = "CH-01",
                customerId = "CUST-01",
                sourceWarehouseId = "WH-01",
                sourceLocationId = "LOC-01",
                dispatchType = DispatchExecutionType.STANDARD,
                status = DispatchExecutionStatus.DISPATCHED,
                stockOutId = "SO-CONCUR",
                dispatchDate = 1000L,
                notes = null,
                createdBy = "user-1",
                createdAt = 1000L,
                updatedAt = 1000L,
                dispatchedBy = "operator",
                dispatchedAt = 1000L
            )
            val dLine1 = DispatchExecutionLine("DL-1", "PRJ-01", "DISP-CONCUR", "CL-1", "DOL-1", "PROD-1", 100.0, 100.0, null, null, "LOC-01", 1000L)
            val dLine2 = DispatchExecutionLine("DL-2", "PRJ-01", "DISP-CONCUR", "CL-2", "DOL-2", "PROD-2", 200.0, 200.0, null, null, "LOC-01", 1000L)
            dispatchDataSource.insertDispatch(dispatch, listOf(dLine1, dLine2))

            val verification = DeliveryItemVerification(
                verificationId = "VERIF-CONCUR",
                projectId = "PRJ-01",
                verificationNo = "V-CONCUR",
                deliveryOrderId = "DO-01",
                deliveryChallanId = "CH-01",
                dispatchExecutionId = "DISP-CONCUR",
                status = DeliveryItemVerificationStatus.IN_PROGRESS,
                remarks = null,
                createdBy = "user-1",
                createdAt = 1000L,
                updatedAt = 1000L
            )
            val vLine1 = DeliveryItemVerificationLine("VL-1", "VERIF-CONCUR", "PRJ-01", "DL-1", "CL-1", "DOL-1", "PROD-1", null, null, 100.0, 0.0, 0.0, createdAt = 1000L)
            val vLine2 = DeliveryItemVerificationLine("VL-2", "VERIF-CONCUR", "PRJ-01", "DL-2", "CL-2", "DOL-2", "PROD-2", null, null, 200.0, 0.0, 0.0, createdAt = 1000L)
            verificationDataSource.insertVerification(verification, listOf(vLine1, vLine2))
        }
    }

    @Test
    fun `concurrent line verifications update safely without data loss`() = runBlocking {
        val jobs = listOf(
            async(Dispatchers.IO) {
                repository.verifyLine(
                    verificationId = "VERIF-CONCUR",
                    verificationLineId = "VL-1",
                    verifiedQuantity = 100.0,
                    isDamaged = false,
                    damagedQuantity = 0.0,
                    isMissing = false,
                    isProductMismatch = false,
                    isBatchMismatch = false,
                    isLotMismatch = false,
                    remarks = null,
                    actorId = "op-1",
                    callerRole = UserRole.WAREHOUSE
                )
            },
            async(Dispatchers.IO) {
                repository.verifyLine(
                    verificationId = "VERIF-CONCUR",
                    verificationLineId = "VL-2",
                    verifiedQuantity = 190.0,
                    isDamaged = false,
                    damagedQuantity = 0.0,
                    isMissing = false,
                    isProductMismatch = false,
                    isBatchMismatch = false,
                    isLotMismatch = false,
                    remarks = null,
                    actorId = "op-2",
                    callerRole = UserRole.WAREHOUSE
                )
            }
        )

        val results = jobs.awaitAll()
        assertTrue(results.all { it is DomainResult.Success })

        val linesResult = repository.getVerificationLines("VERIF-CONCUR", UserRole.ADMIN)
        assertTrue(linesResult is DomainResult.Success)
        val lines = (linesResult as DomainResult.Success).data
        assertEquals(2, lines.size)

        val l1 = lines.find { it.verificationLineId == "VL-1" }
        val l2 = lines.find { it.verificationLineId == "VL-2" }

        assertEquals(100.0, l1?.verifiedQuantity ?: 0.0, 0.001)
        assertEquals(190.0, l2?.verifiedQuantity ?: 0.0, 0.001)
    }
}
