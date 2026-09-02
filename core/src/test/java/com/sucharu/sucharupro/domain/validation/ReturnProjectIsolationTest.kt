package com.sucharu.sucharupro.domain.validation

import com.sucharu.sucharupro.data.datasource.FakeReturnDataSource
import com.sucharu.sucharupro.data.repository.ReturnRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
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
 * Project isolation tests for the Return domain (Module 11 Step 01).
 *
 * Proves that Return records from Project A are completely inaccessible
 * from Project B — at both the domain validator and repository levels.
 *
 * Uses [runBlocking] because [kotlinx.coroutines.test] is not in the
 * project's test dependencies; coroutines-core is available transitively.
 */
class ReturnProjectIsolationTest {

    private lateinit var dataSource: FakeReturnDataSource
    private lateinit var repository: ReturnRepositoryImpl

    // =========================================================================
    // Setup
    // =========================================================================

    @Before
    fun setUp() {
        dataSource = FakeReturnDataSource()
        repository = ReturnRepositoryImpl(dataSource)
    }

    private fun returnRequestFor(
        projectId: String,
        returnId: String = "RET-$projectId-001"
    ) = ReturnRequest(
        returnId = returnId,
        projectId = projectId,
        returnNo = "RN-$projectId-001",
        customerId = "CUST-001",
        originalChallanId = "CHAL-001",
        status = ReturnStatus.REQUESTED,
        reason = ReturnReason.WRONG_PRODUCT,
        requestedBy = "USER-01",
        requestedAt = System.currentTimeMillis(),
        createdAt = System.currentTimeMillis(),
        updatedAt = System.currentTimeMillis(),
        version = 1L
    )

    // =========================================================================
    // Domain-level isolation (ReturnDomainValidator)
    // =========================================================================

    @Test
    fun `validateProjectIsolation passes when project matches`() {
        val request = returnRequestFor("PRJ-A")
        val result = com.sucharu.sucharupro.domain.validation.returns.ReturnDomainValidator
            .validateProjectIsolation(request, "PRJ-A")
        assertTrue(result is DomainResult.Success)
    }

    @Test
    fun `validateProjectIsolation fails when project does not match`() {
        val request = returnRequestFor("PRJ-A")
        val result = com.sucharu.sucharupro.domain.validation.returns.ReturnDomainValidator
            .validateProjectIsolation(request, "PRJ-B")
        assertTrue("Project B should not access Project A return", result is DomainResult.Error)
    }

    @Test
    fun `project A cannot access project B return via validator`() {
        val requestB = returnRequestFor("PRJ-B")
        val result = com.sucharu.sucharupro.domain.validation.returns.ReturnDomainValidator
            .validateProjectIsolation(requestB, "PRJ-A")
        assertTrue("Project A should not access Project B return", result is DomainResult.Error)
    }

    // =========================================================================
    // Repository-level isolation
    // =========================================================================

    @Test
    fun `project A cannot read project B return via repository`() = runBlocking {
        val requestB = returnRequestFor("PRJ-B", returnId = "RET-B-001")
        repository.createReturn(
            request = requestB,
            items = emptyList(),
            actorId = "USER-01",
            callerRole = UserRole.ADMIN,
            callerProjectId = "PRJ-B"
        )

        // Attempt to access from Project A context
        val result = repository.getReturn(
            returnId = "RET-B-001",
            callerRole = UserRole.ADMIN,
            callerProjectId = "PRJ-A"
        )
        assertTrue("Project A must not access Project B return", result is DomainResult.Error)
        Unit
    }

    @Test
    fun `project B cannot read project A return via repository`() = runBlocking {
        val requestA = returnRequestFor("PRJ-A", returnId = "RET-A-001")
        repository.createReturn(
            request = requestA,
            items = emptyList(),
            actorId = "USER-01",
            callerRole = UserRole.ADMIN,
            callerProjectId = "PRJ-A"
        )

        val result = repository.getReturn(
            returnId = "RET-A-001",
            callerRole = UserRole.ADMIN,
            callerProjectId = "PRJ-B"
        )
        assertTrue("Project B must not access Project A return", result is DomainResult.Error)
        Unit
    }

    @Test
    fun `listReturns only returns records for the requested project`() = runBlocking {
        val requestA = returnRequestFor("PRJ-A", returnId = "RET-A-001")
        val requestB = returnRequestFor("PRJ-B", returnId = "RET-B-001")

        // Seed both projects directly through dataSource
        dataSource.insertReturn(requestA, emptyList())
        dataSource.insertReturn(requestB, emptyList())

        val result = repository.listReturns(
            projectId = "PRJ-A",
            callerProjectId = "PRJ-A"
        )
        assertTrue(result is DomainResult.Success)
        val list = (result as DomainResult.Success).data
        assertEquals(1, list.size)
        assertEquals("PRJ-A", list[0].projectId)
        Unit
    }

    @Test
    fun `listReturns cross-project request is rejected`() = runBlocking {
        val result = repository.listReturns(
            projectId = "PRJ-A",
            callerProjectId = "PRJ-B"
        )
        assertTrue("Cross-project listReturns must be denied", result is DomainResult.Error)
        Unit
    }

    @Test
    fun `createReturn cross-project request is rejected`() = runBlocking {
        val requestA = returnRequestFor("PRJ-A", returnId = "RET-A-002")
        val result = repository.createReturn(
            request = requestA,
            items = emptyList(),
            actorId = "USER-01",
            callerRole = UserRole.ADMIN,
            callerProjectId = "PRJ-B"   // different project than requestA.projectId
        )
        assertTrue("Cross-project create must be denied", result is DomainResult.Error)
        Unit
    }
}
