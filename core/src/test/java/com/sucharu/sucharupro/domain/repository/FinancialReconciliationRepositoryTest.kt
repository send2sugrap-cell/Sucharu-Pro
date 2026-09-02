package com.sucharu.sucharupro.domain.repository

import com.sucharu.sucharupro.data.datasource.FakeAccountingPeriodDataSource
import com.sucharu.sucharupro.data.datasource.FakeFinancialDiscrepancyDataSource
import com.sucharu.sucharupro.data.datasource.FakeFinancialReconciliationDataSource
import com.sucharu.sucharupro.data.repository.FinancialReconciliationRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.common.Money
import com.sucharu.sucharupro.domain.model.finance.FinancialDiscrepancyStatus
import com.sucharu.sucharupro.domain.model.finance.FinancialReconciliationStatus
import com.sucharu.sucharupro.domain.model.finance.FinancialReconciliationType
import com.sucharu.sucharupro.domain.model.user.UserRole
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class FinancialReconciliationRepositoryTest {

    private lateinit var reconciliationDataSource: FakeFinancialReconciliationDataSource
    private lateinit var discrepancyDataSource: FakeFinancialDiscrepancyDataSource
    private lateinit var periodDataSource: FakeAccountingPeriodDataSource
    private lateinit var repository: FinancialReconciliationRepository

    @Before
    fun setup() {
        reconciliationDataSource = FakeFinancialReconciliationDataSource()
        discrepancyDataSource = FakeFinancialDiscrepancyDataSource()
        periodDataSource = FakeAccountingPeriodDataSource()
        repository = FinancialReconciliationRepositoryImpl(
            reconciliationDataSource = reconciliationDataSource,
            discrepancyDataSource = discrepancyDataSource,
            periodDataSource = periodDataSource
        )
    }

    @Test
    fun `createReconciliation with idempotency returns existing record on retry`() = runBlocking {
        val result1 = repository.createReconciliation(
            projectId = "PRJ-01",
            periodId = "PER-01",
            type = FinancialReconciliationType.CASH,
            expectedAmount = Money(1000.0),
            actualAmount = Money(1000.0),
            idempotencyKey = "KEY-REC-01",
            actorId = "USER-01",
            callerRole = UserRole.ACCOUNTS
        )
        assertTrue(result1 is DomainResult.Success)
        val rec1 = (result1 as DomainResult.Success).data

        // Re-call with same idempotency key
        val result2 = repository.createReconciliation(
            projectId = "PRJ-01",
            periodId = "PER-01",
            type = FinancialReconciliationType.CASH,
            expectedAmount = Money(1000.0),
            actualAmount = Money(1000.0),
            idempotencyKey = "KEY-REC-01",
            actorId = "USER-01",
            callerRole = UserRole.ACCOUNTS
        )
        assertTrue(result2 is DomainResult.Success)
        val rec2 = (result2 as DomainResult.Success).data
        assertEquals(rec1.reconciliationId, rec2.reconciliationId)
    }

    @Test
    fun `executeCashReconciliation with discrepancy automatically creates discrepancy record`() = runBlocking {
        val result = repository.executeCashReconciliation(
            projectId = "PRJ-01",
            periodId = "PER-01",
            openingCash = Money(10000.0),
            cashReceipts = Money(5000.0),
            cashPayments = Money(2000.0),
            cashAdjustments = Money.ZERO,
            actualClosingCash = Money(12000.0), // Expected is 13000 -> Diff is -1000
            notes = "Shortage in vault",
            actorId = "CASHIER_01",
            callerRole = UserRole.ACCOUNTS
        )

        assertTrue(result is DomainResult.Success)
        val cashRec = (result as DomainResult.Success).data
        assertEquals(FinancialReconciliationStatus.MISMATCHED, cashRec.status)

        // Verify discrepancy was inserted
        val discrepancies = (repository.getDiscrepancies("PRJ-01", "PER-01", UserRole.ACCOUNTS) as DomainResult.Success).data
        assertEquals(1, discrepancies.size)
        assertEquals(Money(-1000.0), discrepancies[0].differenceAmount)
        assertEquals(FinancialDiscrepancyStatus.OPEN, discrepancies[0].status)
    }

    @Test
    fun `resolveDiscrepancy updates status and notes`() = runBlocking {
        repository.executeCashReconciliation(
            projectId = "PRJ-01",
            periodId = "PER-01",
            openingCash = Money(1000.0),
            cashReceipts = Money.ZERO,
            cashPayments = Money.ZERO,
            actualClosingCash = Money(900.0),
            actorId = "USER_01",
            callerRole = UserRole.ACCOUNTS
        )

        val discrepancies = (repository.getDiscrepancies("PRJ-01", "PER-01", UserRole.ACCOUNTS) as DomainResult.Success).data
        val discId = discrepancies[0].discrepancyId

        val resolveResult = repository.resolveDiscrepancy(
            discrepancyId = discId,
            resolutionNote = "Petty cash voucher #123 was recorded in subsequent batch.",
            actorId = "MANAGER_01",
            callerRole = UserRole.MANAGER
        )

        assertTrue(resolveResult is DomainResult.Success)
        val resolved = (resolveResult as DomainResult.Success).data
        assertEquals(FinancialDiscrepancyStatus.RESOLVED, resolved.status)
    }

    @Test
    fun `waiveDiscrepancy requires ADMIN role`() = runBlocking {
        repository.executeCashReconciliation(
            projectId = "PRJ-01",
            periodId = "PER-01",
            openingCash = Money(1000.0),
            cashReceipts = Money.ZERO,
            cashPayments = Money.ZERO,
            actualClosingCash = Money(500.0),
            actorId = "USER_01",
            callerRole = UserRole.ACCOUNTS
        )

        val discId = (repository.getDiscrepancies("PRJ-01", "PER-01", UserRole.ACCOUNTS) as DomainResult.Success).data[0].discrepancyId

        // Non-admin waiver fails
        val managerWaive = repository.waiveDiscrepancy(discId, "Authorized write-off", "MGR_01", UserRole.MANAGER)
        assertTrue(managerWaive is DomainResult.Error)

        // Admin waiver succeeds
        val adminWaive = repository.waiveDiscrepancy(discId, "Authorized administrative write-off", "ADMIN_01", UserRole.ADMIN)
        assertTrue(adminWaive is DomainResult.Success)
        assertEquals(FinancialDiscrepancyStatus.WAIVED, (adminWaive as DomainResult.Success).data.status)
    }

    @Test
    fun `concurrency test with 20 parallel reconciliation creations executes safely`() = runBlocking {
        val jobs = (1..20).map { i ->
            async {
                repository.createReconciliation(
                    projectId = "PRJ-CONCURRENT",
                    periodId = "PER-CONCURRENT",
                    type = FinancialReconciliationType.CASH,
                    expectedAmount = Money(100.0 * i),
                    actualAmount = Money(100.0 * i),
                    idempotencyKey = "IDEMP-$i",
                    actorId = "USER-$i",
                    callerRole = UserRole.ADMIN
                )
            }
        }
        val results = jobs.awaitAll()
        assertTrue(results.all { it is DomainResult.Success })
    }
}
