package com.sucharu.sucharupro.domain.validation

import com.sucharu.sucharupro.data.datasource.FakeReturnDataSource
import com.sucharu.sucharupro.data.repository.ReturnRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.returns.ReturnItem
import com.sucharu.sucharupro.domain.model.returns.ReturnReason
import com.sucharu.sucharupro.domain.model.returns.ReturnRequest
import com.sucharu.sucharupro.domain.model.returns.ReturnStatus
import com.sucharu.sucharupro.domain.model.user.UserRole
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Production-grade creation & validation test suite for Return Requests (Module 11 Step 02).
 */
class ReturnRequestCreationTest {

    private lateinit var dataSource: FakeReturnDataSource
    private lateinit var repository: ReturnRepositoryImpl

    private val testProjectId = "PRJ-001"
    private val testCustomerId = "CUST-100"
    private val testActorId = "STAFF-01"

    @Before
    fun setUp() {
        dataSource = FakeReturnDataSource()
        repository = ReturnRepositoryImpl(dataSource)
    }

    private fun createValidRequest(
        returnId: String = "RET-101",
        projectId: String = testProjectId,
        customerId: String = testCustomerId,
        originalChallanId: String? = "CHAL-500",
        reason: ReturnReason = ReturnReason.PRINTING_DEFECT
    ) = ReturnRequest(
        returnId = returnId,
        projectId = projectId,
        returnNo = "RN-2024-001",
        customerId = customerId,
        originalChallanId = originalChallanId,
        status = ReturnStatus.REQUESTED,
        reason = reason,
        description = "Color discrepancy on front cover",
        requestedBy = testActorId,
        createdAt = System.currentTimeMillis(),
        updatedAt = System.currentTimeMillis(),
        version = 1L
    )

    private fun createValidItem(
        returnItemId: String = "RI-101",
        returnId: String = "RET-101",
        productId: String = "PROD-200",
        originalChallanItemId: String? = "CI-501",
        quantity: Int = 50
    ) = ReturnItem(
        returnItemId = returnItemId,
        returnId = returnId,
        productId = productId,
        originalChallanItemId = originalChallanItemId,
        requestedQuantity = quantity,
        acceptedQuantity = 0,
        rejectedQuantity = 0,
        unit = "copies",
        notes = "Blurred text on pages 3-4"
    )

    @Test
    fun `successful return request creation with initial REQUESTED status`() = runBlocking {
        val request = createValidRequest()
        val item = createValidItem()

        val result = repository.createReturn(
            request = request,
            items = listOf(item),
            actorId = testActorId,
            callerRole = UserRole.STAFF,
            callerProjectId = testProjectId
        )

        assertTrue("Creation should succeed", result is DomainResult.Success)
        val created = (result as DomainResult.Success).data
        assertEquals(ReturnStatus.REQUESTED, created.status)
        assertEquals("RET-101", created.returnId)
        assertEquals(testProjectId, created.projectId)
        assertEquals(testCustomerId, created.customerId)

        // Verify stored in data source
        val stored = dataSource.getReturn("RET-101")
        assertNotNull(stored)
        assertEquals(ReturnStatus.REQUESTED, stored?.status)

        val storedItems = dataSource.getReturnItems("RET-101")
        assertEquals(1, storedItems.size)
        assertEquals("PROD-200", storedItems[0].productId)
        assertEquals(50, storedItems[0].requestedQuantity)
        Unit
    }

    @Test
    fun `creation fails when caller project does not match request project`() = runBlocking {
        val request = createValidRequest(projectId = "PRJ-AAA")
        val item = createValidItem(returnId = "RET-101")

        val result = repository.createReturn(
            request = request,
            items = listOf(item),
            actorId = testActorId,
            callerRole = UserRole.STAFF,
            callerProjectId = "PRJ-BBB"
        )

        assertTrue("Project mismatch should be rejected", result is DomainResult.Error)
        val err = (result as DomainResult.Error).message
        assertTrue(err.contains("Access denied") || err.contains("Caller project"))
        Unit
    }

    @Test
    fun `creation fails when item does not belong to return request`() = runBlocking {
        val request = createValidRequest(returnId = "RET-101")
        val item = createValidItem(returnId = "RET-DIFFERENT")

        val result = repository.createReturn(
            request = request,
            items = listOf(item),
            actorId = testActorId,
            callerRole = UserRole.STAFF,
            callerProjectId = testProjectId
        )

        assertTrue(result is DomainResult.Error)
        val err = (result as DomainResult.Error).message
        assertTrue(err.contains("does not belong to Return"))
        Unit
    }

    @Test
    fun `creation fails for non-positive requested quantity`() {
        try {
            createValidItem(quantity = 0)
            assertTrue("Should fail with 0 quantity", false)
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message!!.contains("Requested quantity must be strictly positive"))
        }

        try {
            createValidItem(quantity = -10)
            assertTrue("Should fail with negative quantity", false)
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message!!.contains("Requested quantity must be strictly positive"))
        }
    }

    @Test
    fun `creation with optional null source challan is supported`() = runBlocking {
        val request = createValidRequest(originalChallanId = null)
        val item = createValidItem(originalChallanItemId = null)

        val result = repository.createReturn(
            request = request,
            items = listOf(item),
            actorId = testActorId,
            callerRole = UserRole.STAFF,
            callerProjectId = testProjectId
        )

        assertTrue(result is DomainResult.Success)
        Unit
    }

    @Test
    fun `all return reason variants are accepted`() = runBlocking {
        for (reason in ReturnReason.entries) {
            val reqId = "RET-REASON-${reason.name}"
            val request = createValidRequest(returnId = reqId, reason = reason)
            val item = createValidItem(returnItemId = "RI-$reqId", returnId = reqId)

            val result = repository.createReturn(
                request = request,
                items = listOf(item),
                actorId = testActorId,
                callerRole = UserRole.ADMIN,
                callerProjectId = testProjectId
            )

            assertTrue("Reason $reason should be accepted", result is DomainResult.Success)
        }
        Unit
    }
}
