package com.sucharu.sucharupro.domain.model

import com.sucharu.sucharupro.domain.model.common.Money
import com.sucharu.sucharupro.domain.model.finance.*
import org.junit.Assert.*
import org.junit.Test

class FinanceAnalyticsRiskAnomalyTest {

    @Test
    fun `FinancialRiskEngine detects negative cash and overdue debt risks`() {
        val now = 100000000L
        val overdueReceivable = CustomerReceivable(
            receivableId = "r-1",
            receivableNo = "REC-01",
            projectId = "PRJ-01",
            customerId = "CUST-01",
            referenceType = FinancialReferenceType.INVOICE,
            referenceId = "inv-1",
            originalAmount = Money(50000),
            settledAmount = Money.ZERO,
            dueDate = now - 100000L,
            status = CustomerReceivableStatus.OVERDUE,
            description = "Overdue bill",
            createdBy = "user-1"
        )

        val risks = FinancialRiskEngine.detectRisks(
            projectId = "PRJ-01",
            revenue = Money(100000),
            expenses = Money(120000),
            cashPosition = Money(-10000),
            receivables = listOf(overdueReceivable),
            payables = emptyList(),
            discrepancies = emptyList(),
            collectionRate = 40.0
        )

        assertTrue(risks.any { it.type == FinancialRiskType.NEGATIVE_CASH_TREND })
        assertTrue(risks.any { it.type == FinancialRiskType.OVERDUE_RECEIVABLE_GROWTH })
        assertTrue(risks.any { it.type == FinancialRiskType.COLLECTION_RATE_DECLINE })
        assertTrue(risks.any { it.type == FinancialRiskType.ABNORMAL_EXPENSE_INCREASE })
    }

    @Test
    fun `FinancialAnomalyDetector detects duplicate-like transactions within time threshold`() {
        val tx1 = FinancialTransaction(
            transactionId = "tx-1",
            projectId = "PRJ-01",
            transactionNo = "TXN-001",
            transactionType = FinancialTransactionType.SALE,
            entryType = FinancialEntryType.DEBIT,
            amount = Money(25000),
            referenceType = FinancialReferenceType.ORDER,
            referenceId = "ord-123",
            transactionDate = 100000L,
            description = "Sale order",
            createdBy = "user-1",
            createdAt = 100000L
        )

        val tx2 = FinancialTransaction(
            transactionId = "tx-2",
            projectId = "PRJ-01",
            transactionNo = "TXN-002",
            transactionType = FinancialTransactionType.SALE,
            entryType = FinancialEntryType.DEBIT,
            amount = Money(25000),
            referenceType = FinancialReferenceType.ORDER,
            referenceId = "ord-123",
            transactionDate = 100020L,
            description = "Sale order duplicate",
            createdBy = "user-1",
            createdAt = 100020L // 20s later (within 60s window)
        )

        val anomalies = FinancialAnomalyDetector.detectAnomalies(
            projectId = "PRJ-01",
            transactions = listOf(tx1, tx2),
            expenses = emptyList(),
            adjustments = emptyList(),
            discrepancies = emptyList()
        )

        assertTrue(anomalies.any { it.type == FinancialAnomalyType.DUPLICATE_LIKE_ACTIVITY })
    }

    @Test
    fun `FinanceGovernanceEngine flags critical exception on unbalanced statements`() {
        val controls = FinanceGovernanceEngine.executeGovernanceAudit(
            isTrialBalanced = false,
            trialBalanceVariance = Money(1500),
            isBalanceSheetBalanced = true,
            balanceSheetVariance = Money.ZERO,
            hasOpenAccountingPeriod = true,
            discrepancyCount = 2,
            criticalDiscrepancyCount = 0
        )

        val trialCtrl = controls.first { it.controlCode == "GOV-CTRL-01" }
        assertEquals(FinancialGovernanceStatus.CRITICAL_EXCEPTION, trialCtrl.status)

        val bsCtrl = controls.first { it.controlCode == "GOV-CTRL-02" }
        assertEquals(FinancialGovernanceStatus.PASSED, bsCtrl.status)

        val reconCtrl = controls.first { it.controlCode == "GOV-CTRL-03" }
        assertEquals(FinancialGovernanceStatus.WARNING, reconCtrl.status)
    }
}
