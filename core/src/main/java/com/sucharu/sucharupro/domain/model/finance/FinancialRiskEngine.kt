package com.sucharu.sucharupro.domain.model.finance

import com.sucharu.sucharupro.domain.model.common.Money
import java.math.BigDecimal
import java.util.UUID

/**
 * Early Warning Risk Detection Engine (Module 09 Step 10).
 */
object FinancialRiskEngine {

    fun detectRisks(
        projectId: String,
        revenue: Money,
        expenses: Money,
        cashPosition: Money,
        receivables: List<CustomerReceivable>,
        payables: List<VendorPayable>,
        discrepancies: List<FinancialReconciliationDiscrepancy>,
        collectionRate: Double?
    ): List<FinancialRiskIndicator> {
        val risks = mutableListOf<FinancialRiskIndicator>()
        val now = System.currentTimeMillis()

        // 1. Cash Drain / Negative Cash
        if (cashPosition.isNegative()) {
            risks.add(
                FinancialRiskIndicator(
                    riskId = "RISK-${UUID.randomUUID().toString().take(8)}",
                    projectId = projectId,
                    type = FinancialRiskType.NEGATIVE_CASH_TREND,
                    severity = FinancialRiskSeverity.CRITICAL,
                    title = "Negative Liquidity Position",
                    description = "Operating cash is negative (${cashPosition.formatted()}). Immediate liquidity injection required.",
                    metricValue = cashPosition.formatted(),
                    threshold = "৳ 0.00"
                )
            )
        }

        // 2. Overdue Receivables
        val totalRec = receivables.fold(Money.ZERO) { acc, r -> acc.plus(r.outstandingAmount) }
        val overdueRec = receivables
            .filter { it.status == CustomerReceivableStatus.OVERDUE || (it.dueDate < now && !it.outstandingAmount.isZero()) }
            .fold(Money.ZERO) { acc, r -> acc.plus(r.outstandingAmount) }

        if (totalRec.isPositive()) {
            val overduePct = overdueRec.amount.multiply(BigDecimal(100))
                .divide(totalRec.amount, 2, java.math.RoundingMode.HALF_EVEN).toDouble()
            if (overduePct > 40.0) {
                risks.add(
                    FinancialRiskIndicator(
                        riskId = "RISK-${UUID.randomUUID().toString().take(8)}",
                        projectId = projectId,
                        type = FinancialRiskType.OVERDUE_RECEIVABLE_GROWTH,
                        severity = if (overduePct > 60.0) FinancialRiskSeverity.CRITICAL else FinancialRiskSeverity.HIGH,
                        title = "Elevated Overdue Customer Debt",
                        description = "Overdue receivables account for $overduePct% of total outstanding receivables (${overdueRec.formatted()}).",
                        metricValue = "$overduePct%",
                        threshold = "40.0%"
                    )
                )
            }
        }

        // 3. Collection Rate Drop
        if (collectionRate != null && collectionRate < 50.0 && totalRec.isPositive()) {
            risks.add(
                FinancialRiskIndicator(
                    riskId = "RISK-${UUID.randomUUID().toString().take(8)}",
                    projectId = projectId,
                    type = FinancialRiskType.COLLECTION_RATE_DECLINE,
                    severity = FinancialRiskSeverity.MEDIUM,
                    title = "Low Collection Efficiency",
                    description = "Current collection rate is $collectionRate%, falling below the operational benchmark of 50%.",
                    metricValue = "$collectionRate%",
                    threshold = "50.0%"
                )
            )
        }

        // 4. Expense Ratio Exceeding Revenue
        if (revenue.isPositive()) {
            val expRatio = expenses.amount.multiply(BigDecimal(100))
                .divide(revenue.amount, 2, java.math.RoundingMode.HALF_EVEN).toDouble()
            if (expRatio > 100.0) {
                risks.add(
                    FinancialRiskIndicator(
                        riskId = "RISK-${UUID.randomUUID().toString().take(8)}",
                        projectId = projectId,
                        type = FinancialRiskType.ABNORMAL_EXPENSE_INCREASE,
                        severity = FinancialRiskSeverity.HIGH,
                        title = "Expenses Exceed Operational Revenue",
                        description = "Total expenses (${expenses.formatted()}) surpass revenue (${revenue.formatted()}) by ${(expRatio - 100).toInt()}%.",
                        metricValue = "$expRatio%",
                        threshold = "100.0%"
                    )
                )
            }
        }

        // 5. Unresolved Discrepancies
        val openDiscrepancies = discrepancies.filter { it.status == FinancialDiscrepancyStatus.OPEN }
        if (openDiscrepancies.isNotEmpty()) {
            risks.add(
                FinancialRiskIndicator(
                    riskId = "RISK-${UUID.randomUUID().toString().take(8)}",
                    projectId = projectId,
                    type = FinancialRiskType.RECONCILIATION_DISCREPANCY,
                    severity = if (openDiscrepancies.size > 3) FinancialRiskSeverity.HIGH else FinancialRiskSeverity.MEDIUM,
                    title = "Unresolved Financial Discrepancies",
                    description = "${openDiscrepancies.size} financial discrepancies remain unresolved across accounts.",
                    metricValue = "${openDiscrepancies.size} discrepancies",
                    threshold = "0"
                )
            )
        }

        return risks
    }
}
