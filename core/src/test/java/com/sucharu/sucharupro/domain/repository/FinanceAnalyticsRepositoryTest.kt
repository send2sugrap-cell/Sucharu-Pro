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

class FinanceAnalyticsRepositoryTest {

    private lateinit var analyticsDataSource: FakeFinanceAnalyticsDataSource
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

    private lateinit var analyticsRepository: FinanceAnalyticsRepository

    private val projectId = "PRJ-TEST"
    private val actorId = "USER-ADMIN"
    private val adminRole = UserRole.ADMIN

    @Before
    fun setUp() {
        analyticsDataSource = FakeFinanceAnalyticsDataSource()
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

        analyticsRepository = FinanceAnalyticsRepositoryImpl(
            analyticsDataSource = analyticsDataSource,
            financialTransactionRepository = financialTransactionRepository,
            customerReceivableRepository = customerReceivableRepository,
            customerPaymentRepository = customerPaymentRepository,
            vendorPayableRepository = vendorPayableRepository,
            supplierPaymentRepository = supplierPaymentRepository,
            expenseRepository = expenseRepository,
            financialAdjustmentRepository = financialAdjustmentRepository,
            accountingPeriodRepository = accountingPeriodRepository,
            financialReconciliationRepository = financialReconciliationRepository,
            financialReportingRepository = reportingRepository
        )
    }

    @Test
    fun `getDashboard generates complete executive analytics without error`() = runBlocking {
        // Setup some sales transactions
        val txRes = financialTransactionRepository.createTransaction(
            projectId = projectId,
            transactionType = FinancialTransactionType.SALE,
            entryType = FinancialEntryType.DEBIT,
            amount = Money(75000),
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

        val filter = AnalyticsFilter(projectId = projectId, reportPeriod = FinancialReportPeriod.CurrentMonth)
        val res = analyticsRepository.getDashboard(projectId, filter, actorId, adminRole)
        assertTrue(res is DomainResult.Success)
        val dash = (res as DomainResult.Success).data

        assertEquals(projectId, dash.projectId)
        assertEquals(Money(75000), dash.summary.totalRevenue)
        assertEquals(Money(75000), dash.summary.netProfit)
        assertTrue(dash.healthScore.score >= 50)
    }

    @Test
    fun `createSnapshot generates immutable snapshot with SHA-256 seal and idempotency`() = runBlocking {
        val filter = AnalyticsFilter(projectId = projectId, reportPeriod = FinancialReportPeriod.CurrentMonth)
        val res = analyticsRepository.createSnapshot(
            projectId = projectId,
            filter = filter,
            snapshotRequestId = "snap-req-101",
            actorId = actorId,
            callerRole = adminRole
        )
        assertTrue(res is DomainResult.Success)
        val snapshot = (res as DomainResult.Success).data

        assertEquals(projectId, snapshot.projectId)
        assertEquals(64, snapshot.snapshotHash.length) // SHA-256 length

        // Idempotency: re-request with same requestId returns identical snapshot
        val idempotentRes = analyticsRepository.createSnapshot(
            projectId = projectId,
            filter = filter,
            snapshotRequestId = "snap-req-101",
            actorId = actorId,
            callerRole = adminRole
        )
        assertTrue(idempotentRes is DomainResult.Success)
        assertEquals(snapshot.snapshotId, (idempotentRes as DomainResult.Success).data.snapshotId)
    }

    @Test
    fun `project isolation prevents cross-tenant data contamination`() = runBlocking {
        // Post data in Project A
        val txRes = financialTransactionRepository.createTransaction(
            projectId = "PRJ-AAA",
            transactionType = FinancialTransactionType.SALE,
            entryType = FinancialEntryType.DEBIT,
            amount = Money(100000),
            referenceType = FinancialReferenceType.ORDER,
            referenceId = "ord-AAA",
            description = "Sales A",
            actorId = "creator-1",
            callerRole = adminRole
        )
        val tx = (txRes as DomainResult.Success).data
        financialTransactionRepository.submitTransaction(tx.transactionId, "creator-1", adminRole)
        financialTransactionRepository.postTransaction(tx.transactionId, "SALES", actorId, adminRole)

        // Query Project B
        val filterB = AnalyticsFilter(projectId = "PRJ-BBB", reportPeriod = FinancialReportPeriod.CurrentMonth)
        val resB = analyticsRepository.getSummary("PRJ-BBB", filterB, actorId, adminRole)
        assertTrue(resB is DomainResult.Success)
        val summaryB = (resB as DomainResult.Success).data

        assertEquals(Money.ZERO, summaryB.totalRevenue)
        assertEquals(Money.ZERO, summaryB.netProfit)
    }

    @Test
    fun `RBAC forbids STAFF from accessing executive finance governance`() = runBlocking {
        val filter = AnalyticsFilter(projectId = projectId)
        val res = analyticsRepository.getDashboard(projectId, filter, "STAFF-01", UserRole.STAFF)
        assertTrue(res is DomainResult.Error)
    }

    @Test
    fun `Zero mutation boundary test ensures no financial transactions or entities are altered`() = runBlocking {
        val initialTxCount = financialTransactionRepository.observeTransactions(projectId, adminRole).first().size
        val initialRecCount = customerReceivableRepository.observeReceivables(projectId, adminRole).first().size
        val initialPayCount = vendorPayableRepository.observePayables(projectId, adminRole).first().size
        val initialExpCount = expenseRepository.observeExpenses(projectId, adminRole).first().size

        // Run full suite of analytics operations
        val filter = AnalyticsFilter(projectId = projectId)
        analyticsRepository.getDashboard(projectId, filter, actorId, adminRole)
        analyticsRepository.getSummary(projectId, filter, actorId, adminRole)
        analyticsRepository.getProfitabilityAnalytics(projectId, filter, actorId, adminRole)
        analyticsRepository.getCashFlowAnalytics(projectId, filter, actorId, adminRole)
        analyticsRepository.getReceivableAnalytics(projectId, filter, actorId, adminRole)
        analyticsRepository.getPayableAnalytics(projectId, filter, actorId, adminRole)
        analyticsRepository.getExpenseAnalytics(projectId, filter, actorId, adminRole)
        analyticsRepository.getCollectionPerformance(projectId, filter, actorId, adminRole)
        analyticsRepository.getSupplierPaymentAnalytics(projectId, filter, actorId, adminRole)
        analyticsRepository.calculateFinancialHealth(projectId, filter, actorId, adminRole)
        analyticsRepository.detectRisks(projectId, filter, actorId, adminRole)
        analyticsRepository.detectAnomalies(projectId, filter, actorId, adminRole)
        analyticsRepository.runGovernanceControls(projectId, filter, actorId, adminRole)
        analyticsRepository.generateForecast(projectId, ForecastMethod.MOVING_AVERAGE, actorId, adminRole)

        val finalTxCount = financialTransactionRepository.observeTransactions(projectId, adminRole).first().size
        val finalRecCount = customerReceivableRepository.observeReceivables(projectId, adminRole).first().size
        val finalPayCount = vendorPayableRepository.observePayables(projectId, adminRole).first().size
        val finalExpCount = expenseRepository.observeExpenses(projectId, adminRole).first().size

        assertEquals(initialTxCount, finalTxCount)
        assertEquals(initialRecCount, finalRecCount)
        assertEquals(initialPayCount, finalPayCount)
        assertEquals(initialExpCount, finalExpCount)
    }
}
