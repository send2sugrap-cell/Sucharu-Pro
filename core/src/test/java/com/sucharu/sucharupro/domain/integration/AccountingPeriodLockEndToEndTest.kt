package com.sucharu.sucharupro.domain.integration

import com.sucharu.sucharupro.data.datasource.FakeAccountingPeriodDataSource
import com.sucharu.sucharupro.data.datasource.FakeFinancialClosingSnapshotDataSource
import com.sucharu.sucharupro.data.datasource.FakeFinancialDiscrepancyDataSource
import com.sucharu.sucharupro.data.datasource.FakeFinancialReconciliationDataSource
import com.sucharu.sucharupro.data.repository.AccountingPeriodRepositoryImpl
import com.sucharu.sucharupro.data.repository.FinancialReconciliationRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.common.Money
import com.sucharu.sucharupro.domain.model.finance.AccountingPeriodStatus
import com.sucharu.sucharupro.domain.model.finance.FinancialClosingReadinessStatus
import com.sucharu.sucharupro.domain.model.finance.FinancialPeriodReopenStatus
import com.sucharu.sucharupro.domain.model.user.UserRole
import com.sucharu.sucharupro.domain.validation.FinancialPeriodLockValidator
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AccountingPeriodLockEndToEndTest {

    private lateinit var periodDataSource: FakeAccountingPeriodDataSource
    private lateinit var snapshotDataSource: FakeFinancialClosingSnapshotDataSource
    private lateinit var discrepancyDataSource: FakeFinancialDiscrepancyDataSource
    private lateinit var reconciliationDataSource: FakeFinancialReconciliationDataSource
    private lateinit var periodRepository: AccountingPeriodRepositoryImpl
    private lateinit var reconciliationRepository: FinancialReconciliationRepositoryImpl

    private val projectId = "PRJ-LOCK-E2E"
    private val adminId = "ADMIN_USER"
    private val accountsId = "ACCOUNTS_USER"

    @Before
    fun setup() {
        periodDataSource = FakeAccountingPeriodDataSource()
        snapshotDataSource = FakeFinancialClosingSnapshotDataSource()
        discrepancyDataSource = FakeFinancialDiscrepancyDataSource()
        reconciliationDataSource = FakeFinancialReconciliationDataSource()

        periodRepository = AccountingPeriodRepositoryImpl(
            periodDataSource = periodDataSource,
            snapshotDataSource = snapshotDataSource,
            discrepancyDataSource = discrepancyDataSource,
            reconciliationDataSource = reconciliationDataSource
        )

        reconciliationRepository = FinancialReconciliationRepositoryImpl(
            reconciliationDataSource = reconciliationDataSource,
            discrepancyDataSource = discrepancyDataSource,
            periodDataSource = periodDataSource
        )
    }

    @Test
    fun `full end to end accounting period closing, period lock enforcement, and controlled reopen workflow`() = runBlocking {
        // 1. Create Accounting Period
        val createResult = periodRepository.createAccountingPeriod(
            projectId = projectId,
            periodName = "March 2026",
            startDate = 1000L,
            endDate = 5000L,
            actorId = adminId,
            callerRole = UserRole.ADMIN
        )
        assertTrue(createResult is DomainResult.Success)
        val period = (createResult as DomainResult.Success).data
        assertEquals(AccountingPeriodStatus.OPEN, period.status)

        // 2. Introduce a critical discrepancy
        reconciliationRepository.executeCashReconciliation(
            projectId = projectId,
            periodId = period.periodId,
            openingCash = Money(10000.0),
            cashReceipts = Money.ZERO,
            cashPayments = Money.ZERO,
            actualClosingCash = Money(2000.0), // -8000 difference (CRITICAL)
            actorId = accountsId,
            callerRole = UserRole.ACCOUNTS
        )

        // 3. Evaluate readiness -> Should be BLOCKED due to critical discrepancy
        val readinessBlocked = periodRepository.evaluateClosingReadiness(period.periodId, UserRole.ADMIN)
        assertTrue(readinessBlocked is DomainResult.Success)
        assertEquals(FinancialClosingReadinessStatus.BLOCKED, (readinessBlocked as DomainResult.Success).data.status)

        // Attempting to close period while blocked must fail
        val closeBlocked = periodRepository.closeAccountingPeriod(period.periodId, adminId, UserRole.ADMIN)
        assertTrue(closeBlocked is DomainResult.Error)

        // 4. Admin waives the critical discrepancy with audit reason
        val disc = (reconciliationRepository.getDiscrepancies(projectId, period.periodId, UserRole.ADMIN) as DomainResult.Success).data[0]
        val waiveResult = reconciliationRepository.waiveDiscrepancy(disc.discrepancyId, "Approved theft insurance settlement claim filed.", adminId, UserRole.ADMIN)
        assertTrue(waiveResult is DomainResult.Success)

        // 5. Re-evaluate readiness -> Now READY
        val readinessReady = periodRepository.evaluateClosingReadiness(period.periodId, UserRole.ADMIN)
        assertTrue(readinessReady is DomainResult.Success)
        assertEquals(FinancialClosingReadinessStatus.READY, (readinessReady as DomainResult.Success).data.status)

        // 6. Submit and Close Accounting Period
        periodRepository.submitPeriodForClosing(period.periodId, accountsId, UserRole.ACCOUNTS)
        val closeResult = periodRepository.closeAccountingPeriod(period.periodId, adminId, UserRole.ADMIN)
        assertTrue(closeResult is DomainResult.Success)
        val snapshot1 = (closeResult as DomainResult.Success).data
        assertNotNull(snapshot1.snapshotHash)
        assertEquals(1, snapshot1.version)

        // 7. Verify Period Lock enforcement on financial transactions
        val closedPeriod = (periodRepository.getAccountingPeriod(period.periodId, UserRole.ADMIN) as DomainResult.Success).data
        assertEquals(AccountingPeriodStatus.CLOSED, closedPeriod.status)

        val lockValidation = FinancialPeriodLockValidator.validateMutationAllowed(
            period = closedPeriod,
            transactionDate = 2500L
        )
        assertTrue(lockValidation is DomainResult.Error)
        assertTrue((lockValidation as DomainResult.Error).message.contains("CLOSED and LOCKED"))

        // 8. Submit Reopen Request
        val reqResult = periodRepository.createReopenRequest(
            projectId = projectId,
            periodId = period.periodId,
            reason = "Late tax adjustment required for Q1 audit",
            actorId = accountsId,
            callerRole = UserRole.ACCOUNTS
        )
        assertTrue(reqResult is DomainResult.Success)
        val request = (reqResult as DomainResult.Success).data
        assertEquals(FinancialPeriodReopenStatus.PENDING, request.status)

        // 9. Admin Approves Reopen Request & Executes Reopen
        periodRepository.approveReopenRequest(request.requestId, adminId, UserRole.ADMIN)
        val reopenResult = periodRepository.reopenAccountingPeriod(period.periodId, request.requestId, adminId, UserRole.ADMIN)
        assertTrue(reopenResult is DomainResult.Success)
        val reopenedPeriod = (reopenResult as DomainResult.Success).data
        assertEquals(AccountingPeriodStatus.REOPENED, reopenedPeriod.status)
        assertEquals(2, reopenedPeriod.version)

        // 10. Financial mutation is now allowed during REOPENED audit state
        val reopenedLockValidation = FinancialPeriodLockValidator.validateMutationAllowed(
            period = reopenedPeriod,
            transactionDate = 2500L
        )
        assertTrue(reopenedLockValidation is DomainResult.Success)

        // 11. Close second cycle -> Generates version 2 snapshot without overwriting snapshot 1
        val secondClose = periodRepository.closeAccountingPeriod(period.periodId, adminId, UserRole.ADMIN)
        assertTrue(secondClose is DomainResult.Success)
        val snapshot2 = (secondClose as DomainResult.Success).data
        assertEquals(2, snapshot2.version)

        // Verify snapshot 1 is still preserved in historical records
        val allSnapshots = snapshotDataSource.getSnapshotsByProject(projectId)
        assertEquals(2, allSnapshots.size)
    }
}
