package com.sucharu.sucharupro.domain.validation

import com.sucharu.sucharupro.data.datasource.FakeReturnDataSource
import com.sucharu.sucharupro.data.repository.ReturnRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.returns.ReturnReason
import com.sucharu.sucharupro.domain.model.returns.ReturnRequest
import com.sucharu.sucharupro.domain.model.returns.ReturnStatus
import com.sucharu.sucharupro.domain.model.user.UserRole
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Concurrency safety tests for the Return domain (Module 11 Step 01).
 *
 * Verifies that concurrent lifecycle transitions cannot produce an invalid
 * final state — all state mutation is serialized through the repository Mutex.
 *
 * Uses [runBlocking] because [kotlinx.coroutines.test] is not in the
 * project's test dependencies; coroutines-core is available transitively.
 */
class ReturnConcurrencyTest {

    private lateinit var dataSource: FakeReturnDataSource
    private lateinit var repository: ReturnRepositoryImpl

    @Before
    fun setUp() {
        dataSource = FakeReturnDataSource()
        repository = ReturnRepositoryImpl(dataSource)
    }

    private fun seedRequest(
        returnId: String = "RET-CONC-001",
        projectId: String = "PRJ-A"
    ): ReturnRequest {
        val now = System.currentTimeMillis()
        return ReturnRequest(
            returnId = returnId,
            projectId = projectId,
            returnNo = "RN-CONC-001",
            customerId = "CUST-001",
            originalChallanId = null,
            status = ReturnStatus.REQUESTED,
            reason = ReturnReason.QUANTITY_ISSUE,
            requestedBy = "USER-01",
            requestedAt = now,
            createdAt = now,
            updatedAt = now,
            version = 1L
        )
    }

    // =========================================================================
    // Optimistic concurrency — version mismatch protection
    // =========================================================================

    @Test
    fun `concurrent transitions with stale version fail gracefully`() = runBlocking {
        val request = seedRequest()
        dataSource.insertReturn(request, emptyList())

        // First transition succeeds (version 1 → 2)
        val first = repository.transitionReturnStatus(
            returnId = request.returnId,
            targetStatus = ReturnStatus.UNDER_INSPECTION,
            actorId = "USER-01",
            expectedVersion = 1L,
            callerRole = UserRole.ADMIN,
            callerProjectId = request.projectId
        )
        assertTrue("First transition should succeed", first is DomainResult.Success)
        assertEquals(2L, (first as DomainResult.Success).data.version)

        // Second transition with stale version 1 must fail
        val second = repository.transitionReturnStatus(
            returnId = request.returnId,
            targetStatus = ReturnStatus.UNDER_INSPECTION,
            actorId = "USER-02",
            expectedVersion = 1L,   // stale — version is now 2
            callerRole = UserRole.ADMIN,
            callerProjectId = request.projectId
        )
        assertTrue("Stale version must be rejected", second is DomainResult.Error)
        assertTrue((second as DomainResult.Error).message.contains("Concurrency conflict"))
        Unit
    }

    // =========================================================================
    // Parallel creates — duplicate guard
    // =========================================================================

    @Test
    fun `duplicate create with same returnId is rejected`() = runBlocking {
        val request = seedRequest()

        val first = repository.createReturn(
            request = request,
            items = emptyList(),
            actorId = "USER-01",
            callerRole = UserRole.ADMIN,
            callerProjectId = request.projectId
        )
        assertTrue(first is DomainResult.Success)

        val second = repository.createReturn(
            request = request,  // same returnId
            items = emptyList(),
            actorId = "USER-01",
            callerRole = UserRole.ADMIN,
            callerProjectId = request.projectId
        )
        assertTrue("Duplicate create must be rejected", second is DomainResult.Error)
        assertEquals(1, dataSource.countReturns())  // Only one record persisted
        Unit
    }

    // =========================================================================
    // Concurrent coroutines — single winner
    // =========================================================================

    @Test
    fun `concurrent coroutines racing to transition same return produce exactly one success`() =
        runBlocking {
            val request = seedRequest()
            dataSource.insertReturn(request, emptyList())

            // 10 coroutines all try to transition REQUESTED → UNDER_INSPECTION with version 1
            val results = (1..10).map { idx ->
                async(Dispatchers.Default) {
                    repository.transitionReturnStatus(
                        returnId = request.returnId,
                        targetStatus = ReturnStatus.UNDER_INSPECTION,
                        actorId = "USER-$idx",
                        expectedVersion = 1L,
                        callerRole = UserRole.ADMIN,
                        callerProjectId = request.projectId
                    )
                }
            }.awaitAll()

            val successes = results.count { it is DomainResult.Success }
            val errors = results.count { it is DomainResult.Error }

            // Exactly one coroutine should win; all others hit version conflict
            assertEquals("Exactly one transition should succeed", 1, successes)
            assertEquals("All others should fail with concurrency error", 9, errors)
            Unit
        }

    // =========================================================================
    // Version increments correctly through lifecycle
    // =========================================================================

    @Test
    fun `version increments by one for each successful transition`() = runBlocking {
        val request = seedRequest()
        dataSource.insertReturn(request, emptyList())

        // REQUESTED → UNDER_INSPECTION (v1 → v2)
        val step1 = repository.transitionReturnStatus(
            returnId = request.returnId,
            targetStatus = ReturnStatus.UNDER_INSPECTION,
            actorId = "USER-01",
            expectedVersion = 1L,
            callerRole = UserRole.ADMIN,
            callerProjectId = request.projectId
        ) as DomainResult.Success
        assertEquals(2L, step1.data.version)

        // UNDER_INSPECTION → APPROVED (v2 → v3)
        val step2 = repository.transitionReturnStatus(
            returnId = request.returnId,
            targetStatus = ReturnStatus.APPROVED,
            actorId = "USER-01",
            expectedVersion = 2L,
            callerRole = UserRole.ADMIN,
            callerProjectId = request.projectId
        ) as DomainResult.Success
        assertEquals(3L, step2.data.version)
        Unit
    }
}
