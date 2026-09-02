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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class DeliveryItemVerificationProjectIsolationTest {

    private lateinit var verificationDataSource: FakeDeliveryItemVerificationDataSource
    private lateinit var dispatchDataSource: FakeDispatchExecutionDataSource
    private lateinit var repository: DeliveryItemVerificationRepository

    @Before
    fun setUp() {
        runBlocking {
            verificationDataSource = FakeDeliveryItemVerificationDataSource()
            dispatchDataSource = FakeDispatchExecutionDataSource()
            repository = DeliveryItemVerificationRepositoryImpl(verificationDataSource, dispatchDataSource)

            // Dispatch in PRJ-A
            val dispA = DispatchExecution(
                dispatchExecutionId = "DISP-A",
                projectId = "PRJ-A",
                dispatchNo = "DN-A",
                deliveryOrderId = "DO-A",
                deliveryChallanId = "CH-A",
                customerId = null,
                sourceWarehouseId = "WH-A",
                sourceLocationId = "LOC-A",
                dispatchType = DispatchExecutionType.STANDARD,
                status = DispatchExecutionStatus.DISPATCHED,
                stockOutId = "SO-A",
                dispatchDate = 1000L,
                notes = null,
                createdBy = "user-1",
                createdAt = 1000L,
                updatedAt = 1000L,
                dispatchedBy = "operator",
                dispatchedAt = 1000L
            )
            val dLineA = DispatchExecutionLine(
                dispatchExecutionLineId = "DLA",
                projectId = "PRJ-A",
                dispatchExecutionId = "DISP-A",
                deliveryChallanLineId = "CLA",
                deliveryOrderLineId = "DOLA",
                productId = "PROD-A",
                requestedQuantity = 10.0,
                dispatchQuantity = 10.0,
                batchId = null,
                lotId = null,
                sourceLocationId = "LOC-A",
                createdAt = 1000L
            )
            dispatchDataSource.insertDispatch(dispA, listOf(dLineA))

            // Dispatch in PRJ-B
            val dispB = DispatchExecution(
                dispatchExecutionId = "DISP-B",
                projectId = "PRJ-B",
                dispatchNo = "DN-B",
                deliveryOrderId = "DO-B",
                deliveryChallanId = "CH-B",
                customerId = null,
                sourceWarehouseId = "WH-B",
                sourceLocationId = "LOC-B",
                dispatchType = DispatchExecutionType.STANDARD,
                status = DispatchExecutionStatus.DISPATCHED,
                stockOutId = "SO-B",
                dispatchDate = 1000L,
                notes = null,
                createdBy = "user-2",
                createdAt = 1000L,
                updatedAt = 1000L,
                dispatchedBy = "operator",
                dispatchedAt = 1000L
            )
            val dLineB = DispatchExecutionLine(
                dispatchExecutionLineId = "DLB",
                projectId = "PRJ-B",
                dispatchExecutionId = "DISP-B",
                deliveryChallanLineId = "CLB",
                deliveryOrderLineId = "DOLB",
                productId = "PROD-B",
                requestedQuantity = 20.0,
                dispatchQuantity = 20.0,
                batchId = null,
                lotId = null,
                sourceLocationId = "LOC-B",
                createdAt = 1000L
            )
            dispatchDataSource.insertDispatch(dispB, listOf(dLineB))
        }
    }

    @Test
    fun `observeVerifications returns strictly project scoped records`() = runBlocking {
        val verifA = DeliveryItemVerification("V-A", "PRJ-A", "VN-A", "DO-A", "CH-A", "DISP-A", DeliveryItemVerificationStatus.DRAFT, null, null, null, "user-1", 1000L, null, 1000L)
        val vLineA = DeliveryItemVerificationLine("VLA", "V-A", "PRJ-A", "DLA", "CLA", "DOLA", "PROD-A", null, null, 10.0, 10.0, 0.0, createdAt = 1000L)
        repository.createVerification(verifA, listOf(vLineA), UserRole.ADMIN, "PRJ-A")

        val verifB = DeliveryItemVerification("V-B", "PRJ-B", "VN-B", "DO-B", "CH-B", "DISP-B", DeliveryItemVerificationStatus.DRAFT, null, null, null, "user-2", 1000L, null, 1000L)
        val vLineB = DeliveryItemVerificationLine("VLB", "V-B", "PRJ-B", "DLB", "CLB", "DOLB", "PROD-B", null, null, 20.0, 20.0, 0.0, createdAt = 1000L)
        repository.createVerification(verifB, listOf(vLineB), UserRole.ADMIN, "PRJ-B")

        val listA = repository.observeVerifications("PRJ-A").first()
        val listB = repository.observeVerifications("PRJ-B").first()

        assertEquals(1, listA.size)
        assertEquals("V-A", listA[0].verificationId)

        assertEquals(1, listB.size)
        assertEquals("V-B", listB[0].verificationId)
    }

    @Test
    fun `cross project getVerification is blocked`() = runBlocking {
        val verifA = DeliveryItemVerification("V-A", "PRJ-A", "VN-A", "DO-A", "CH-A", "DISP-A", DeliveryItemVerificationStatus.DRAFT, null, null, null, "user-1", 1000L, null, 1000L)
        val vLineA = DeliveryItemVerificationLine("VLA", "V-A", "PRJ-A", "DLA", "CLA", "DOLA", "PROD-A", null, null, 10.0, 10.0, 0.0, createdAt = 1000L)
        repository.createVerification(verifA, listOf(vLineA), UserRole.ADMIN, "PRJ-A")

        val result = repository.getVerification("V-A", UserRole.ADMIN, "PRJ-B")
        assertTrue(result is DomainResult.Error)
        assertTrue((result as DomainResult.Error).message.contains("Access denied"))
    }
}
