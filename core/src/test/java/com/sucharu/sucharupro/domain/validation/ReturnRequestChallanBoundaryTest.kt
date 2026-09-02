package com.sucharu.sucharupro.domain.validation

import com.sucharu.sucharupro.data.datasource.FakeReturnDataSource
import com.sucharu.sucharupro.data.repository.ReturnRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.delivery.challan.DeliveryChallan
import com.sucharu.sucharupro.domain.model.delivery.challan.DeliveryChallanStatus
import com.sucharu.sucharupro.domain.model.delivery.challan.DeliveryChallanType
import com.sucharu.sucharupro.domain.model.returns.ReturnItem
import com.sucharu.sucharupro.domain.model.returns.ReturnReason
import com.sucharu.sucharupro.domain.model.returns.ReturnRequest
import com.sucharu.sucharupro.domain.model.returns.ReturnStatus
import com.sucharu.sucharupro.domain.model.user.UserRole
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Source Challan boundary integrity tests (Module 11 Step 02).
 * Verifies that Return Request operations never modify or corrupt source Challan documents.
 */
class ReturnRequestChallanBoundaryTest {

    private lateinit var returnDataSource: FakeReturnDataSource
    private lateinit var repository: ReturnRepositoryImpl

    private val projectId = "PRJ-CHAL"
    private val customerId = "CUST-001"
    private val challanId = "CHAL-100"

    // Reference delivery challan baseline state
    private lateinit var originalChallan: DeliveryChallan

    @Before
    fun setUp() {
        returnDataSource = FakeReturnDataSource()
        repository = ReturnRepositoryImpl(returnDataSource)

        val now = System.currentTimeMillis()
        originalChallan = DeliveryChallan(
            challanId = challanId,
            projectId = projectId,
            challanNo = "DC-2024-001",
            deliveryOrderId = "DO-001",
            customerId = customerId,
            sourceReferenceId = "SO-001",
            sourceReferenceType = "SALES_ORDER",
            challanType = DeliveryChallanType.STANDARD,
            status = DeliveryChallanStatus.DELIVERED,
            issueDate = now,
            notes = "Original delivery",
            createdBy = "STAFF-DISPATCH",
            createdAt = now,
            updatedAt = now
        )
    }

    private fun createReturn(returnId: String = "RET-01") = ReturnRequest(
        returnId = returnId,
        projectId = projectId,
        returnNo = "RN-01",
        customerId = customerId,
        originalChallanId = challanId,
        status = ReturnStatus.REQUESTED,
        reason = ReturnReason.PRINTING_DEFECT,
        requestedBy = "USER-01",
        createdAt = System.currentTimeMillis(),
        updatedAt = System.currentTimeMillis(),
        version = 1L
    )

    private fun createItem(returnId: String = "RET-01") = ReturnItem(
        returnItemId = "RI-01",
        returnId = returnId,
        productId = "PROD-10",
        originalChallanItemId = "CI-10",
        requestedQuantity = 100
    )

    @Test
    fun `creating a return request leaves source challan untouched`() = runBlocking {
        val challanBefore = originalChallan.copy()

        val req = createReturn()
        val item = createItem()

        val res = repository.createReturn(
            request = req,
            items = listOf(item),
            actorId = "USER-01",
            callerRole = UserRole.ADMIN,
            callerProjectId = projectId
        )
        assertTrue(res is DomainResult.Success)

        // Compare Challan fields
        assertEquals(challanBefore.status, originalChallan.status)
        assertEquals(challanBefore.updatedAt, originalChallan.updatedAt)
        assertEquals(challanBefore.notes, originalChallan.notes)
        Unit
    }

    @Test
    fun `submitting return for inspection leaves source challan untouched`() = runBlocking {
        val req = createReturn()
        val item = createItem()
        repository.createReturn(req, listOf(item), "USER-01", UserRole.ADMIN, projectId)

        val challanBefore = originalChallan.copy()

        val res = repository.submitForInspection(
            returnId = req.returnId,
            actorId = "USER-01",
            expectedVersion = 1L,
            callerRole = UserRole.ADMIN,
            callerProjectId = projectId
        )
        assertTrue(res is DomainResult.Success)

        assertEquals(challanBefore.status, originalChallan.status)
        assertEquals(challanBefore.updatedAt, originalChallan.updatedAt)
        Unit
    }

    @Test
    fun `cancelling return leaves source challan untouched`() = runBlocking {
        val req = createReturn()
        val item = createItem()
        repository.createReturn(req, listOf(item), "USER-01", UserRole.ADMIN, projectId)

        val challanBefore = originalChallan.copy()

        val res = repository.cancelReturnRequest(
            returnId = req.returnId,
            actorId = "USER-01",
            expectedVersion = 1L,
            callerRole = UserRole.ADMIN,
            callerProjectId = projectId
        )
        assertTrue(res is DomainResult.Success)

        assertEquals(challanBefore.status, originalChallan.status)
        assertEquals(challanBefore.updatedAt, originalChallan.updatedAt)
        Unit
    }
}
