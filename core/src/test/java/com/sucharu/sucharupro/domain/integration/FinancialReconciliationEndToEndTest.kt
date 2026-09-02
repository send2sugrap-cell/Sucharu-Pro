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
import com.sucharu.sucharupro.domain.model.finance.FinancialDiscrepancyStatus
import com.sucharu.sucharupro.domain.model.finance.FinancialEntryType
import com.sucharu.sucharupro.domain.model.finance.FinancialLedgerEntry
import com.sucharu.sucharupro.domain.model.finance.FinancialReconciliationStatus
import com.sucharu.sucharupro.domain.model.finance.FinancialReferenceType
import com.sucharu.sucharupro.domain.model.finance.FinancialTransaction
import com.sucharu.sucharupro.domain.model.finance.FinancialTransactionStatus
import com.sucharu.sucharupro.domain.model.finance.FinancialTransactionType
import com.sucharu.sucharupro.domain.model.user.UserRole
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class FinancialReconciliationEndToEndTest {

    private lateinit var periodDataSource: FakeAccountingPeriodDataSource
    private lateinit var snapshotDataSource: FakeFinancialClosingSnapshotDataSource
    private lateinit var discrepancyDataSource: FakeFinancialDiscrepancyDataSource
    private lateinit var reconciliationDataSource: FakeFinancialReconciliationDataSource
    private lateinit var periodRepository: AccountingPeriodRepositoryImpl
    private lateinit var reconciliationRepository: FinancialReconciliationRepositoryImpl

    private val projectId = "PRJ-E2E-RECON"
    private val actorId = "ADMIN_E2E"

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
    fun `full end to end financial reconciliation and discrepancy resolution lifecycle`() = runBlocking {
        // Step 1: Create Accounting Period
        val periodResult = periodRepository.createAccountingPeriod(
            projectId = projectId,
            periodName = "February 2026",
            startDate = 1770000000000L,
            endDate = 1772400000000L,
            actorId = actorId,
            callerRole = UserRole.ADMIN
        )
        assertTrue(periodResult is DomainResult.Success)
        val period = (periodResult as DomainResult.Success).data
        assertEquals(AccountingPeriodStatus.OPEN, period.status)

        // Step 2: Execute Physical Cash Reconciliation with Variance
        val cashRecResult = reconciliationRepository.executeCashReconciliation(
            projectId = projectId,
            periodId = period.periodId,
            openingCash = Money(50000.0),
            cashReceipts = Money(25000.0),
            cashPayments = Money(10000.0),
            actualClosingCash = Money(64000.0), // Expected is 65000 -> Diff is -1000
            notes = "Cash counted in vault",
            actorId = actorId,
            callerRole = UserRole.ACCOUNTS
        )
        assertTrue(cashRecResult is DomainResult.Success)
        val cashRec = (cashRecResult as DomainResult.Success).data
        assertEquals(FinancialReconciliationStatus.MISMATCHED, cashRec.status)

        // Step 3: Verify Discrepancy was Automatically Detected and Logged
        val discrepancies = (reconciliationRepository.getDiscrepancies(projectId, period.periodId, UserRole.ADMIN) as DomainResult.Success).data
        assertEquals(1, discrepancies.size)
        val disc = discrepancies[0]
        assertEquals(FinancialDiscrepancyStatus.OPEN, disc.status)
        assertEquals(Money(-1000.0), disc.differenceAmount)

        // Step 4: Resolve the Discrepancy
        val resolveResult = reconciliationRepository.resolveDiscrepancy(
            discrepancyId = disc.discrepancyId,
            resolutionNote = "Discrepancy explained: Unrecorded courier cash expense voucher #992 settled.",
            actorId = actorId,
            callerRole = UserRole.MANAGER
        )
        assertTrue(resolveResult is DomainResult.Success)
        assertEquals(FinancialDiscrepancyStatus.RESOLVED, (resolveResult as DomainResult.Success).data.status)

        // Step 5: Execute Bank Reconciliation (Balanced)
        val bankRecResult = reconciliationRepository.executeBankReconciliation(
            projectId = projectId,
            periodId = period.periodId,
            bankAccountId = "BANK-01",
            bankName = "Eastern Bank Ltd",
            openingBankBalance = Money(200000.0),
            ledgerDeposits = Money(50000.0),
            ledgerWithdrawals = Money(30000.0),
            bankStatementBalance = Money(220000.0),
            notes = "Statement matches reconciled book balance",
            actorId = actorId,
            callerRole = UserRole.ACCOUNTS
        )
        assertTrue(bankRecResult is DomainResult.Success)
        assertEquals(FinancialReconciliationStatus.MATCHED, (bankRecResult as DomainResult.Success).data.status)

        // Step 6: Execute Ledger Diagnostic
        val txn = FinancialTransaction(
            transactionId = "TXN-01",
            projectId = projectId,
            transactionNo = "FTX-001",
            transactionType = FinancialTransactionType.SALE,
            transactionStatus = FinancialTransactionStatus.POSTED,
            entryType = FinancialEntryType.DEBIT,
            amount = Money(1000.0),
            referenceType = FinancialReferenceType.INVOICE,
            referenceId = "INV-001",
            transactionDate = 1770500000000L,
            description = "Sales Invoice",
            createdBy = actorId
        )

        val entry1 = FinancialLedgerEntry(
            entryId = "LED-01",
            transactionId = "TXN-01",
            projectId = projectId,
            entryNo = "LED-001",
            entryType = FinancialEntryType.DEBIT,
            amount = Money(1000.0),
            currency = "BDT",
            accountHead = "ACCOUNTS_RECEIVABLE",
            referenceType = FinancialReferenceType.INVOICE,
            referenceId = "INV-001",
            entryDate = 1770500000000L,
            narration = "Debit AR",
            createdBy = actorId
        )

        val entry2 = FinancialLedgerEntry(
            entryId = "LED-02",
            transactionId = "TXN-01",
            projectId = projectId,
            entryNo = "LED-002",
            entryType = FinancialEntryType.CREDIT,
            amount = Money(1000.0),
            currency = "BDT",
            accountHead = "SALES_REVENUE",
            referenceType = FinancialReferenceType.INVOICE,
            referenceId = "INV-001",
            entryDate = 1770500000000L,
            narration = "Credit Revenue",
            createdBy = actorId
        )

        val ledgerReportResult = reconciliationRepository.executeLedgerReconciliation(
            projectId = projectId,
            periodId = period.periodId,
            transactions = listOf(txn),
            ledgerEntries = listOf(entry1, entry2),
            actorId = actorId,
            callerRole = UserRole.ADMIN
        )
        assertTrue(ledgerReportResult is DomainResult.Success)
        val ledgerReport = (ledgerReportResult as DomainResult.Success).data
        assertTrue(ledgerReport.isBalanced)
        assertTrue(ledgerReport.isClean)

        // Step 7: Verify Financial Control Summary reflects complete state
        val summaryResult = reconciliationRepository.getFinancialControlSummary(projectId, period.periodId, UserRole.ADMIN)
        assertTrue(summaryResult is DomainResult.Success)
        val summary = (summaryResult as DomainResult.Success).data
        assertEquals(0, summary.openDiscrepanciesCount)
        assertNotNull(summary.activePeriod)
    }
}
