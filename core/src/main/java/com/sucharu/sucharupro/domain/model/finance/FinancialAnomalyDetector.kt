package com.sucharu.sucharupro.domain.model.finance

import com.sucharu.sucharupro.domain.model.common.Money
import java.util.UUID

/**
 * Deterministic rule-based financial anomaly detection engine (Module 09 Step 10).
 */
object FinancialAnomalyDetector {

    fun detectAnomalies(
        projectId: String,
        transactions: List<FinancialTransaction>,
        expenses: List<Expense>,
        adjustments: List<FinancialAdjustment>,
        discrepancies: List<FinancialReconciliationDiscrepancy>
    ): List<FinancialAnomaly> {
        val anomalies = mutableListOf<FinancialAnomaly>()

        // 1. Duplicate-like transactions (same amount, same reference, close timestamps within 60s)
        val groupedByRef = transactions.groupBy { "${it.referenceType}_${it.referenceId}_${it.amount.formatted()}" }
        for ((key, txList) in groupedByRef) {
            if (txList.size > 1) {
                val sorted = txList.sortedBy { it.createdAt }
                for (i in 0 until sorted.size - 1) {
                    val diff = Math.abs(sorted[i + 1].createdAt - sorted[i].createdAt)
                    if (diff < 60_000L) {
                        anomalies.add(
                            FinancialAnomaly(
                                anomalyId = "ANOM-${UUID.randomUUID().toString().take(8)}",
                                projectId = projectId,
                                type = FinancialAnomalyType.DUPLICATE_LIKE_ACTIVITY,
                                severity = FinancialAnomalySeverity.HIGH,
                                title = "Potential Duplicate Transaction Detected",
                                description = "Transactions ${sorted[i].transactionNo} and ${sorted[i + 1].transactionNo} share same amount (${sorted[i].amount.formatted()}) and reference.",
                                entityReferenceId = sorted[i + 1].transactionId
                            )
                        )
                    }
                }
            }
        }

        // 2. High-Value Expenses (> 100,000 BDT)
        for (expense in expenses) {
            if (expense.amount > Money(100000)) {
                anomalies.add(
                    FinancialAnomaly(
                        anomalyId = "ANOM-${UUID.randomUUID().toString().take(8)}",
                        projectId = projectId,
                        type = FinancialAnomalyType.EXPENSE_SPIKE,
                        severity = FinancialAnomalySeverity.MEDIUM,
                        title = "High-Value Single Expense Voucher",
                        description = "Expense voucher ${expense.expenseNo} of amount ${expense.amount.formatted()} exceeds standard anomaly threshold.",
                        entityReferenceId = expense.expenseId
                    )
                )
            }
        }

        // 3. Abnormally Large Financial Adjustments (> 50,000 BDT)
        for (adjustment in adjustments) {
            if (adjustment.amount > Money(50000)) {
                anomalies.add(
                    FinancialAnomaly(
                        anomalyId = "ANOM-${UUID.randomUUID().toString().take(8)}",
                        projectId = projectId,
                        type = FinancialAnomalyType.LARGE_ADJUSTMENT,
                        severity = FinancialAnomalySeverity.HIGH,
                        title = "High-Value Financial Adjustment Note",
                        description = "Adjustment ${adjustment.adjustmentNo} (${adjustment.adjustmentType.defaultLabel}) of ${adjustment.amount.formatted()} detected.",
                        entityReferenceId = adjustment.adjustmentId
                    )
                )
            }
        }

        // 4. Critical Discrepancies
        for (discrepancy in discrepancies) {
            if (discrepancy.severity == FinancialDiscrepancySeverity.CRITICAL && discrepancy.status == FinancialDiscrepancyStatus.OPEN) {
                anomalies.add(
                    FinancialAnomaly(
                        anomalyId = "ANOM-${UUID.randomUUID().toString().take(8)}",
                        projectId = projectId,
                        type = FinancialAnomalyType.RECONCILIATION_VARIANCE,
                        severity = FinancialAnomalySeverity.CRITICAL,
                        title = "Critical Reconciliation Variance",
                        description = "Unresolved discrepancy: ${discrepancy.description} (${discrepancy.differenceAmount.formatted()}).",
                        entityReferenceId = discrepancy.discrepancyId
                    )
                )
            }
        }

        return anomalies
    }
}
