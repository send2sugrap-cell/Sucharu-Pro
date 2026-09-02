package com.sucharu.sucharupro.domain.repository

import com.sucharu.sucharupro.data.datasource.FakeAccountingPeriodDataSource
import com.sucharu.sucharupro.data.datasource.FakeFinancialClosingSnapshotDataSource
import com.sucharu.sucharupro.data.datasource.FakeFinancialDiscrepancyDataSource
import com.sucharu.sucharupro.data.datasource.FakeFinancialReconciliationDataSource
import com.sucharu.sucharupro.data.repository.AccountingPeriodRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.finance.AccountingPeriodStatus
import com.sucharu.sucharupro.domain.model.finance.FinancialPeriodReopenStatus
import com.sucharu.sucharupro.domain.model.user.UserRole
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AccountingPeriodRepositoryTest {

    private lateinit var periodDataSource: FakeAccountingPeriodDataSource
    private lateinit var snapshotDataSource: FakeFinancialClosingSnapshotDataSource
    private lateinit var discrepancyDataSource: FakeFinancialDiscrepancyDataSource
    private lateinit var reconciliationDataSource: FakeFinancialReconciliationDataSource
    private lateinit var repository: AccountingPeriodRepository

    @Before
    fun setup() {
        periodDataSource = FakeAccountingPeriodDataSource()
        snapshotDataSource = FakeFinancialClosingSnapshotDataSource()
        discrepancyDataSource = FakeFinancialDiscrepancyDataSource()
        reconciliationDataSource = FakeFinancialReconciliationDataSource()
        repository = AccountingPeriodRepositoryImpl(
            periodDataSource = periodDataSource,
            snapshotDataSource = snapshotDataSource,
            discrepancyDataSource = discrepancyDataSource,
            reconciliationDataSource = reconciliationDataSource
        )
    }

    @Test
    fun `createAccountingPeriod succeeds and generates unique period number`() = runBlocking {
        val result = repository.createAccountingPeriod(
            projectId = "PRJ-01",
            periodName = "January 2026",
            startDate = 1000L,
            endDate = 2000L,
            actorId = "ADMIN_01",
            callerRole = UserRole.ADMIN
        )

        assertTrue(result is DomainResult.Success)
        val period = (result as DomainResult.Success).data
        assertEquals("January 2026", period.periodName)
        assertEquals(AccountingPeriodStatus.OPEN, period.status)
        assertTrue(period.periodNo.startsWith("PER-2026-"))
    }

    @Test
    fun `period closing workflow generates snapshot and locks period`() = runBlocking {
        val createResult = repository.createAccountingPeriod(
            projectId = "PRJ-01",
            periodName = "January 2026",
            startDate = 1000L,
            endDate = 2000L,
            actorId = "ADMIN_01",
            callerRole = UserRole.ADMIN
        )
        val period = (createResult as DomainResult.Success).data

        val submitResult = repository.submitPeriodForClosing(
            periodId = period.periodId,
            actorId = "ACCOUNTS_01",
            callerRole = UserRole.ACCOUNTS
        )
        assertTrue(submitResult is DomainResult.Success)

        val closeResult = repository.closeAccountingPeriod(
            periodId = period.periodId,
            actorId = "ADMIN_01",
            callerRole = UserRole.ADMIN
        )
        assertTrue(closeResult is DomainResult.Success)
        val snapshot = (closeResult as DomainResult.Success).data
        assertNotNull(snapshot.snapshotHash)
        assertEquals(period.periodId, snapshot.periodId)

        // Verify period is now CLOSED
        val fetchedPeriod = (repository.getAccountingPeriod(period.periodId, UserRole.ADMIN) as DomainResult.Success).data
        assertEquals(AccountingPeriodStatus.CLOSED, fetchedPeriod.status)
    }

    @Test
    fun `controlled reopen workflow through Admin approval works properly`() = runBlocking {
        val createResult = repository.createAccountingPeriod(
            projectId = "PRJ-01",
            periodName = "January 2026",
            startDate = 1000L,
            endDate = 2000L,
            actorId = "ADMIN_01",
            callerRole = UserRole.ADMIN
        )
        val period = (createResult as DomainResult.Success).data

        repository.closeAccountingPeriod(period.periodId, "ADMIN_01", UserRole.ADMIN)

        // Create reopen request
        val reqResult = repository.createReopenRequest(
            projectId = "PRJ-01",
            periodId = period.periodId,
            reason = "Auditor requested adjustment entries",
            actorId = "ACCOUNTS_01",
            callerRole = UserRole.ACCOUNTS
        )
        assertTrue(reqResult is DomainResult.Success)
        val request = (reqResult as DomainResult.Success).data
        assertEquals(FinancialPeriodReopenStatus.PENDING, request.status)

        // Admin approves reopen request
        val approveResult = repository.approveReopenRequest(
            requestId = request.requestId,
            actorId = "ADMIN_01",
            callerRole = UserRole.ADMIN
        )
        assertTrue(approveResult is DomainResult.Success)

        // Execute controlled reopen
        val reopenResult = repository.reopenAccountingPeriod(
            periodId = period.periodId,
            requestId = request.requestId,
            actorId = "ADMIN_01",
            callerRole = UserRole.ADMIN
        )
        assertTrue(reopenResult is DomainResult.Success)
        val reopenedPeriod = (reopenResult as DomainResult.Success).data
        assertEquals(AccountingPeriodStatus.REOPENED, reopenedPeriod.status)
        assertEquals(2, reopenedPeriod.version)
    }

    @Test
    fun `project isolation prevents cross-project period access`() = runBlocking {
        val createResult = repository.createAccountingPeriod(
            projectId = "PRJ-01",
            periodName = "Jan 2026",
            startDate = 1000L,
            endDate = 2000L,
            actorId = "ADMIN_01",
            callerRole = UserRole.ADMIN
        )
        val period = (createResult as DomainResult.Success).data

        // Project 2 requesting reopen on Project 1's period should fail
        val crossProjectReopen = repository.createReopenRequest(
            projectId = "PRJ-02",
            periodId = period.periodId,
            reason = "Tampering attempt",
            actorId = "ADMIN_02",
            callerRole = UserRole.ADMIN
        )
        assertTrue(crossProjectReopen is DomainResult.Error)
    }

    @Test
    fun `concurrency test with 20 parallel period requests maintains thread safety`() = runBlocking {
        val jobs = (1..20).map { i ->
            async {
                repository.createAccountingPeriod(
                    projectId = "PRJ-CONCURRENCY",
                    periodName = "Period $i",
                    startDate = i * 10000L,
                    endDate = (i * 10000L) + 5000L,
                    actorId = "ADMIN_CONCURRENCY",
                    callerRole = UserRole.ADMIN
                )
            }
        }
        val results = jobs.awaitAll()
        assertTrue(results.all { it is DomainResult.Success })
    }
}
