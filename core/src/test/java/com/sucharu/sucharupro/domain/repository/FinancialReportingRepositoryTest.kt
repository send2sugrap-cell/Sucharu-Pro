package com.sucharu.sucharupro.domain.repository

import com.sucharu.sucharupro.data.datasource.*
import com.sucharu.sucharupro.data.repository.*
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.common.Money
import com.sucharu.sucharupro.domain.model.finance.*
import com.sucharu.sucharupro.domain.model.user.UserRole
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.util.UUID

class FinancialReportingRepositoryTest {

    private lateinit var reportingDataSource: FakeFinancialReportingDataSource
    private lateinit var financialTransactionDataSource: FakeFinancialTransactionDataSource
    private lateinit var customerReceivableDataSource: FakeCustomerReceivableDataSource
    private lateinit var customerPaymentDataSource: FakeCustomerPaymentDataSource
    private lateinit var vendorPayableDataSource: FakeVendorPayableDataSource
    private lateinit var supplierPaymentDataSource: FakeSupplierPaymentDataSource
    private lateinit var expenseDataSource: FakeExpenseDataSource
    private lateinit var expenseCategoryDataSource: FakeExpenseCategoryDataSource
    private lateinit var financialAdjustmentDataSource: FakeFinancialAdjustmentDataSource
    private lateinit var accountingPeriodDataSource: FakeAccountingPeriodDataSource
    private lateinit var closingSnapshotDataSource: FakeFinancialClosingSnapshotDataSource
    private lateinit var discrepancyDataSource: FakeFinancialDiscrepancyDataSource
    private lateinit var reconciliationDataSource: FakeFinancialReconciliationDataSource

    private lateinit var financialTransactionRepository: FinancialTransactionRepository
    private lateinit var customerReceivableRepository: CustomerReceivableRepository
    private lateinit var customerPaymentRepository: CustomerPaymentRepository
    private lateinit var vendorPayableRepository: VendorPayableRepository
    private lateinit var supplierPaymentRepository: SupplierPaymentRepository
    private lateinit var expenseRepository: ExpenseRepository
    private lateinit var financialAdjustmentRepository: FinancialAdjustmentRepository
    private lateinit var accountingPeriodRepository: AccountingPeriodRepository
    private lateinit var financialReconciliationRepository: FinancialReconciliationRepository

    private lateinit var reportingRepository: FinancialReportingRepository

    private val projectId = "PRJ-TEST"
    private val actorId = "USER-ADMIN"
    private val adminRole = UserRole.ADMIN

    @Before
    fun setUp() {
        reportingDataSource = FakeFinancialReportingDataSource()
        financialTransactionDataSource = FakeFinancialTransactionDataSource()
        customerReceivableDataSource = FakeCustomerReceivableDataSource()
        customerPaymentDataSource = FakeCustomerPaymentDataSource()
        vendorPayableDataSource = FakeVendorPayableDataSource()
        supplierPaymentDataSource = FakeSupplierPaymentDataSource()
        expenseDataSource = FakeExpenseDataSource()
        expenseCategoryDataSource = FakeExpenseCategoryDataSource()
        financialAdjustmentDataSource = FakeFinancialAdjustmentDataSource()
        accountingPeriodDataSource = FakeAccountingPeriodDataSource()
        closingSnapshotDataSource = FakeFinancialClosingSnapshotDataSource()
        discrepancyDataSource = FakeFinancialDiscrepancyDataSource()
        reconciliationDataSource = FakeFinancialReconciliationDataSource()

        financialTransactionRepository = FinancialTransactionRepositoryImpl(financialTransactionDataSource)
        customerReceivableRepository = CustomerReceivableRepositoryImpl(customerReceivableDataSource)
        customerPaymentRepository = CustomerPaymentRepositoryImpl(
            customerPaymentDataSource,
            customerReceivableRepository,
            financialTransactionRepository
        )
        vendorPayableRepository = VendorPayableRepositoryImpl(vendorPayableDataSource)
        supplierPaymentRepository = SupplierPaymentRepositoryImpl(
            supplierPaymentDataSource,
            vendorPayableRepository,
            financialTransactionRepository
        )
        expenseRepository = ExpenseRepositoryImpl(
            expenseDataSource,
            expenseCategoryDataSource,
            financialTransactionRepository
        )
        financialAdjustmentRepository = FinancialAdjustmentRepositoryImpl(
            financialAdjustmentDataSource,
            financialTransactionRepository,
            customerReceivableRepository,
            vendorPayableRepository
        )
        accountingPeriodRepository = AccountingPeriodRepositoryImpl(
            accountingPeriodDataSource,
            closingSnapshotDataSource
        )
        financialReconciliationRepository = FinancialReconciliationRepositoryImpl(
            reconciliationDataSource,
            discrepancyDataSource,
            accountingPeriodDataSource
        )

        reportingRepository = FinancialReportingRepositoryImpl(
            reportingDataSource = reportingDataSource,
            financialTransactionRepository = financialTransactionRepository,
            customerReceivableRepository = customerReceivableRepository,
            customerPaymentRepository = customerPaymentRepository,
            vendorPayableRepository = vendorPayableRepository,
            supplierPaymentRepository = supplierPaymentRepository,
            expenseRepository = expenseRepository,
            financialAdjustmentRepository = financialAdjustmentRepository,
            accountingPeriodRepository = accountingPeriodRepository,
            financialReconciliationRepository = financialReconciliationRepository
        )
    }

    @Test
    fun `getProfitLossReport calculates revenue, expenses, and net profit correctly`() = runBlocking {
        val txRes = financialTransactionRepository.createTransaction(
            projectId = projectId,
            transactionType = FinancialTransactionType.SALE,
            entryType = FinancialEntryType.DEBIT,
            amount = Money(20000),
            referenceType = FinancialReferenceType.ORDER,
            referenceId = "ord-1",
            description = "Sales revenue",
            actorId = "creator-1",
            callerRole = adminRole
        )
        assertTrue(txRes is DomainResult.Success)
        val tx = (txRes as DomainResult.Success).data
        financialTransactionRepository.submitTransaction(tx.transactionId, "creator-1", adminRole)
        financialTransactionRepository.postTransaction(tx.transactionId, "SALES", actorId, adminRole)

        val filter = FinancialReportFilter(projectId = projectId, reportPeriod = FinancialReportPeriod.CurrentMonth)
        val res = reportingRepository.getProfitLossReport(projectId, filter, actorId, adminRole)
        assertTrue(res is DomainResult.Success)
        val pnl = (res as DomainResult.Success).data

        assertEquals(Money(20000), pnl.totalRevenue)
        assertEquals(Money(0), pnl.totalExpenses)
        assertEquals(Money(20000), pnl.netProfit)
        assertTrue(pnl.isProfit)
    }

    @Test
    fun `getBalanceSheetReport returns balanced statement`() = runBlocking {
        val filter = FinancialReportFilter(projectId = projectId, reportPeriod = FinancialReportPeriod.CurrentMonth)
        val res = reportingRepository.getBalanceSheetReport(projectId, filter, actorId, adminRole)
        assertTrue(res is DomainResult.Success)
        val bs = (res as DomainResult.Success).data

        assertTrue(bs.isBalanced)
        assertEquals(Money.ZERO, bs.equationVariance)
        assertEquals(FinancialReportStatus.READY, bs.status)
    }

    @Test
    fun `createReportSnapshot creates immutable snapshot with valid SHA-256 hash`() = runBlocking {
        val filter = FinancialReportFilter(projectId = projectId, reportPeriod = FinancialReportPeriod.CurrentMonth)
        val res = reportingRepository.createReportSnapshot(
            projectId = projectId,
            reportType = FinancialReportType.PROFIT_AND_LOSS,
            filter = filter,
            snapshotRequestId = "snap-req-001",
            actorId = actorId,
            callerRole = adminRole
        )
        assertTrue(res is DomainResult.Success)
        val snapshot = (res as DomainResult.Success).data

        assertEquals(projectId, snapshot.projectId)
        assertEquals(FinancialReportType.PROFIT_AND_LOSS, snapshot.reportType)
        assertTrue(snapshot.snapshotHash.isNotBlank())
        assertEquals(64, snapshot.snapshotHash.length)

        val idempotentRes = reportingRepository.createReportSnapshot(
            projectId = projectId,
            reportType = FinancialReportType.PROFIT_AND_LOSS,
            filter = filter,
            snapshotRequestId = "snap-req-001",
            actorId = actorId,
            callerRole = adminRole
        )
        assertTrue(idempotentRes is DomainResult.Success)
        assertEquals(snapshot.snapshotId, (idempotentRes as DomainResult.Success).data.snapshotId)
    }

    @Test
    fun `project isolation protects data between projects`() = runBlocking {
        val txRes = financialTransactionRepository.createTransaction(
            projectId = "PRJ-A",
            transactionType = FinancialTransactionType.SALE,
            entryType = FinancialEntryType.DEBIT,
            amount = Money(50000),
            referenceType = FinancialReferenceType.ORDER,
            referenceId = "ord-A",
            description = "Sales A",
            actorId = "creator-1",
            callerRole = adminRole
        )
        val tx = (txRes as DomainResult.Success).data
        financialTransactionRepository.submitTransaction(tx.transactionId, "creator-1", adminRole)
        financialTransactionRepository.postTransaction(tx.transactionId, "SALES", actorId, adminRole)

        val filterB = FinancialReportFilter(projectId = "PRJ-B", reportPeriod = FinancialReportPeriod.CurrentMonth)
        val resB = reportingRepository.getProfitLossReport("PRJ-B", filterB, actorId, adminRole)
        assertTrue(resB is DomainResult.Success)
        val pnlB = (resB as DomainResult.Success).data

        assertEquals(Money.ZERO, pnlB.totalRevenue)
    }

    @Test
    fun `boundary test verifies reporting does not mutate existing financial data`() = runBlocking {
        val initialTxCount = financialTransactionRepository.observeTransactions(projectId, adminRole).first().size
        val initialRecCount = customerReceivableRepository.observeReceivables(projectId, adminRole).first().size
        val initialPayCount = vendorPayableRepository.observePayables(projectId, adminRole).first().size

        val filter = FinancialReportFilter(projectId = projectId)
        reportingRepository.getProfitLossReport(projectId, filter, actorId, adminRole)
        reportingRepository.getBalanceSheetReport(projectId, filter, actorId, adminRole)
        reportingRepository.getCashFlowReport(projectId, filter, actorId, adminRole)
        reportingRepository.getTrialBalanceReport(projectId, filter, actorId, adminRole)
        reportingRepository.getGeneralLedgerReport(projectId, filter, actorId, adminRole)
        reportingRepository.getAccountsReceivableReport(projectId, filter, actorId, adminRole)
        reportingRepository.getAccountsPayableReport(projectId, filter, actorId, adminRole)
        reportingRepository.getExpenseAnalysisReport(projectId, filter, actorId, adminRole)
        reportingRepository.getFinancialKpiSummary(projectId, filter, actorId, adminRole)

        val finalTxCount = financialTransactionRepository.observeTransactions(projectId, adminRole).first().size
        val finalRecCount = customerReceivableRepository.observeReceivables(projectId, adminRole).first().size
        val finalPayCount = vendorPayableRepository.observePayables(projectId, adminRole).first().size

        assertEquals(initialTxCount, finalTxCount)
        assertEquals(initialRecCount, finalRecCount)
        assertEquals(initialPayCount, finalPayCount)
    }
}
