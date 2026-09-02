package com.sucharu.sucharupro.domain.model

import com.sucharu.sucharupro.domain.model.common.Money
import com.sucharu.sucharupro.domain.model.finance.*
import org.junit.Assert.*
import org.junit.Test

class FinancialReportCalculationTest {

    @Test
    fun `calculateBalanceSheetVariance returns zero when balanced`() {
        val assets = Money(150000)
        val liabilities = Money(50000)
        val equity = Money(100000)

        val variance = FinancialReportCalculator.calculateBalanceSheetVariance(assets, liabilities, equity)
        assertEquals(Money.ZERO, variance)
    }

    @Test
    fun `calculateBalanceSheetVariance returns non-zero when unbalanced`() {
        val assets = Money(155000)
        val liabilities = Money(50000)
        val equity = Money(100000)

        val variance = FinancialReportCalculator.calculateBalanceSheetVariance(assets, liabilities, equity)
        assertEquals(Money(5000), variance)
    }

    @Test
    fun `calculateNetProfit without adjustments equals revenue minus expenses`() {
        val revenue = Money(100000)
        val expenses = Money(40000)
        val adjustments = Money.ZERO

        val netProfit = FinancialReportCalculator.calculateNetProfit(revenue, expenses, adjustments)
        assertEquals(Money(60000), netProfit)
    }

    @Test
    fun `calculateNetProfit includes adjustments impact correctly`() {
        val revenue = Money(10000)
        val expenses = Money(3000)
        val adjustments = Money(-500)

        val netProfit = FinancialReportCalculator.calculateNetProfit(revenue, expenses, adjustments)
        assertEquals(Money(6500), netProfit)
    }

    @Test
    fun `calculateTrialBalanceTotals correctly aggregates debit and credit`() {
        val lines = listOf(
            TrialBalanceLine("SALES", "Sales Account", totalDebit = Money.ZERO, totalCredit = Money(10000)),
            TrialBalanceLine("CASH", "Cash Account", totalDebit = Money(7000), totalCredit = Money.ZERO),
            TrialBalanceLine("EXPENSE", "Expense Account", totalDebit = Money(3000), totalCredit = Money.ZERO)
        )
        val (totalDebit, totalCredit) = FinancialReportCalculator.calculateTrialBalanceTotals(lines)
        assertEquals(Money(10000), totalDebit)
        assertEquals(Money(10000), totalCredit)
    }

    @Test
    fun `buildGeneralLedgerEntries calculates running balance deterministically`() {
        val entry1 = FinancialLedgerEntry(
            entryId = "e-1",
            entryNo = "LE-001",
            transactionId = "tx-1",
            projectId = "PRJ-001",
            accountHead = "CASH",
            entryType = FinancialEntryType.DEBIT,
            amount = Money(5000),
            referenceType = FinancialReferenceType.MANUAL,
            referenceId = "ref-1",
            narration = "Opening Cash",
            entryDate = 1000L,
            createdBy = "user-1"
        )
        val entry2 = FinancialLedgerEntry(
            entryId = "e-2",
            entryNo = "LE-002",
            transactionId = "tx-2",
            projectId = "PRJ-001",
            accountHead = "CASH",
            entryType = FinancialEntryType.CREDIT,
            amount = Money(2000),
            referenceType = FinancialReferenceType.EXPENSE,
            referenceId = "ref-2",
            narration = "Rent payment",
            entryDate = 2000L,
            createdBy = "user-1"
        )

        val glEntries = FinancialReportCalculator.buildGeneralLedgerEntries(
            ledgerEntries = listOf(entry1, entry2),
            transactions = emptyList(),
            openingBalance = Money.ZERO,
            startDate = 0L,
            endDate = 5000L
        )

        assertEquals(2, glEntries.size)
        assertEquals(Money(5000), glEntries[0].runningBalance)
        assertEquals(Money(3000), glEntries[1].runningBalance)
    }

    @Test
    fun `FinancialComparisonCalculator calculates absolute and percentage change correctly`() {
        val valA = Money(10000)
        val valB = Money(12000)

        val (diff, pct) = FinancialComparisonCalculator.calculateChange(valA, valB)
        assertEquals(Money(2000), diff)
        assertEquals(20.0, pct ?: 0.0, 0.01)
    }

    @Test
    fun `FinancialComparisonCalculator handles zero base value gracefully`() {
        val valA = Money.ZERO
        val valB = Money(5000)

        val (diff, pct) = FinancialComparisonCalculator.calculateChange(valA, valB)
        assertEquals(Money(5000), diff)
        assertNull(pct)
    }

    @Test
    fun `FinancialKpiCalculator collection rate computation`() {
        val collected = Money(80000)
        val revenue = Money(100000)

        val rate = FinancialKpiCalculator.calculateCollectionRate(collected, revenue)
        assertEquals(80.0, rate ?: 0.0, 0.01)
    }

    @Test
    fun `FinancialKpiCalculator net profit margin computation`() {
        val netProfit = Money(25000)
        val revenue = Money(100000)

        val margin = FinancialKpiCalculator.calculateNetProfitMargin(netProfit, revenue)
        assertEquals(25.0, margin ?: 0.0, 0.01)
    }

    @Test
    fun `FinancialKpiCalculator expense ratio computation`() {
        val expenses = Money(60000)
        val revenue = Money(100000)

        val ratio = FinancialKpiCalculator.calculateExpenseRatio(expenses, revenue)
        assertEquals(60.0, ratio ?: 0.0, 0.01)
    }

    @Test
    fun `FinancialKpiCalculator overdue receivable ratio computation`() {
        val overdue = Money(15000)
        val totalOutstanding = Money(50000)

        val ratio = FinancialKpiCalculator.calculateOverdueReceivableRatio(overdue, totalOutstanding)
        assertEquals(30.0, ratio ?: 0.0, 0.01)
    }

    @Test
    fun `buildReceivableAgingBuckets classifies aging into correct intervals`() {
        val now = 100000000L
        val oneDay = 86400000L

        val rCurrent = createTestReceivable("r1", now + oneDay, Money(1000))
        val r1To30 = createTestReceivable("r2", now - (15 * oneDay), Money(2000))
        val r31To60 = createTestReceivable("r3", now - (45 * oneDay), Money(3000))
        val r61To90 = createTestReceivable("r4", now - (75 * oneDay), Money(4000))
        val r90Plus = createTestReceivable("r5", now - (100 * oneDay), Money(5000))

        val summary = FinancialReportCalculator.buildReceivableAgingBuckets(
            listOf(rCurrent, r1To30, r31To60, r61To90, r90Plus),
            now
        )

        assertEquals(Money(1000), summary.current)
        assertEquals(Money(2000), summary.overdue1To30)
        assertEquals(Money(3000), summary.overdue31To60)
        assertEquals(Money(4000), summary.overdue61To90)
        assertEquals(Money(5000), summary.overdue90Plus)
    }

    private fun createTestReceivable(id: String, dueDate: Long, outstanding: Money): CustomerReceivable {
        return CustomerReceivable(
            receivableId = id,
            receivableNo = "REC-$id",
            projectId = "PRJ-1",
            customerId = "CUST-1",
            referenceType = FinancialReferenceType.INVOICE,
            referenceId = "inv-$id",
            originalAmount = outstanding,
            settledAmount = Money.ZERO,
            dueDate = dueDate,
            status = CustomerReceivableStatus.OPEN,
            description = "Test receivable $id",
            createdBy = "user-1"
        )
    }
}
