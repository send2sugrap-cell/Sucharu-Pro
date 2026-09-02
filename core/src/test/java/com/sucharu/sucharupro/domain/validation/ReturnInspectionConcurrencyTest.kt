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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ReturnInspectionConcurrencyTest {

    private lateinit var dataSource: FakeReturnDataSource
    private lateinit var repository: ReturnRepositoryImpl

    private val testReturn = ReturnRequest(
        returnId = "RET-CONC-01",
        projectId = "PRJ-01",
        returnNo = "RET-2026-001",
        customerId = "CUST-01",
        originalChallanId = null,
        status = ReturnStatus.UNDER_INSPECTION,
        reason = ReturnReason.PRINTING_DEFECT,
        requestedBy = "user-1",
        version = 3L
    )

    private val testItem = ReturnItem(
        returnItemId = "RI-01",
        returnId = "RET-CONC-01",
        productId = "PROD-01",
        originalChallanItemId = null,
        requestedQuantity = 5,
        acceptedQuantity = 5,
        rejectedQuantity = 0
    )

    @Before
    fun setup() = runBlocking {
        dataSource = FakeReturnDataSource()
        repository = ReturnRepositoryImpl(dataSource)
        dataSource.insertReturn(testReturn, listOf(testItem))
    }

    @Test
    fun `approveReturn fails if expectedVersion does not match current version`() = runBlocking {
        val res = repository.approveReturn(
            returnId = testReturn.returnId,
            actorId = "admin-1",
            expectedVersion = 1L, // Mismatch (actual is 3L)
            callerRole = UserRole.ADMIN,
            callerProjectId = testReturn.projectId
        )

        assertTrue(res is DomainResult.Error)
        val msg = (res as DomainResult.Error).message
        assertTrue(msg.contains("Concurrency conflict"))
    }

    @Test
    fun `rejectReturn fails if expectedVersion does not match current version`() = runBlocking {
        val res = repository.rejectReturn(
            returnId = testReturn.returnId,
            actorId = "manager-1",
            expectedVersion = 2L, // Mismatch (actual is 3L)
            rejectionReason = "Item damaged",
            callerRole = UserRole.MANAGER,
            callerProjectId = testReturn.projectId
        )

        assertTrue(res is DomainResult.Error)
        val msg = (res as DomainResult.Error).message
        assertTrue(msg.contains("Concurrency conflict"))
    }
}
