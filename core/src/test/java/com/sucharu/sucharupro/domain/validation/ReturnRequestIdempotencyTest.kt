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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Idempotency test suite for Return Request creation (Module 11 Step 02).
 */
class ReturnRequestIdempotencyTest {

    private lateinit var dataSource: FakeReturnDataSource
    private lateinit var repository: ReturnRepositoryImpl

    private val projectId = "PRJ-IDEM"

    @Before
    fun setUp() {
        dataSource = FakeReturnDataSource()
        repository = ReturnRepositoryImpl(dataSource)
    }

    private fun createRequest(returnId: String = "RET-DUP-01") = ReturnRequest(
        returnId = returnId,
        projectId = projectId,
        returnNo = "RN-DUP-01",
        customerId = "CUST-01",
        originalChallanId = "CHAL-01",
        status = ReturnStatus.REQUESTED,
        reason = ReturnReason.QUANTITY_ISSUE,
        requestedBy = "USER-01",
        createdAt = System.currentTimeMillis(),
        updatedAt = System.currentTimeMillis(),
        version = 1L
    )

    private fun createItem(returnId: String = "RET-DUP-01") = ReturnItem(
        returnItemId = "RI-DUP-01",
        returnId = returnId,
        productId = "PROD-01",
        originalChallanItemId = "CI-01",
        requestedQuantity = 15
    )

    @Test
    fun `submitting identical return request twice rejects duplicate without creating second record`() = runBlocking {
        val req = createRequest()
        val item = createItem()

        val firstAttempt = repository.createReturn(
            request = req,
            items = listOf(item),
            actorId = "USER-01",
            callerRole = UserRole.ADMIN,
            callerProjectId = projectId
        )
        assertTrue("First create should succeed", firstAttempt is DomainResult.Success)
        assertEquals(1, dataSource.countReturns())

        // Duplicate submission with same returnId
        val secondAttempt = repository.createReturn(
            request = req,
            items = listOf(item),
            actorId = "USER-01",
            callerRole = UserRole.ADMIN,
            callerProjectId = projectId
        )
        assertTrue("Duplicate create should be rejected", secondAttempt is DomainResult.Error)
        val err = (secondAttempt as DomainResult.Error).message
        assertTrue(err.contains("already exists") || err.contains("duplicate"))

        // Total count in database must still be exactly 1
        assertEquals(1, dataSource.countReturns())
        assertEquals(1, dataSource.countItems())
        Unit
    }
}
