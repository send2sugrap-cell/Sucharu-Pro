package com.sucharu.sucharupro.domain.model.finance

import com.sucharu.sucharupro.domain.model.common.Money
import java.math.BigDecimal

/**
 * Deterministic calculation engine for Financial Health Scoring (Module 09 Step 10).
 *
 * Provides a management indicator from 0 to 100 based on 7 dimensions:
 * 1. Liquidity (Cash Position vs Monthly Burn)
 * 2. Profitability (Net Profit & Margins)
 * 3. Receivable Health (Collection Rate & Overdue Exposure)
 * 4. Payable Health (Settlement Rate & Overdue Exposure)
 * 5. Expense Control (Expense to Revenue Ratio)
 * 6. Reconciliation Health (Discrepancies & Unreconciled Variance)
 * 7. Governance Control Health (Passed vs Open Control Exceptions)
 */
object FinancialHealthEngine {

    fun calculateHealthScore(
        revenue: Money,
        expenses: Money,
        netProfit: Money,
        cashPosition: Money,
        totalReceivable: Money,
        overdueReceivable: Money,
        totalPayable: Money,
        overduePayable: Money,
        collectionRate: Double?,
        settlementRate: Double?,
        discrepancyCount: Int,
        isTrialBalanced: Boolean,
        isBalanceSheetBalanced: Boolean
    ): FinancialHealthScore {
        val warnings = mutableListOf<String>()
        val criticals = mutableListOf<String>()

        // 1. Liquidity Score (0 - 100)
        val liquidityScore: Int = when {
            cashPosition.isNegative() -> {
                criticals.add("Negative cash position detected.")
                10
            }
            cashPosition.isZero() && expenses.isPositive() -> {
                warnings.add("Zero cash reserve with active expenses.")
                30
            }
            expenses.isPositive() && cashPosition.amount >= expenses.amount -> 95
            expenses.isPositive() && cashPosition.amount >= expenses.amount.divide(BigDecimal(2), 2, java.math.RoundingMode.HALF_EVEN) -> 75
            else -> 60
        }

        // 2. Profitability Score (0 - 100)
        val profitabilityScore: Int = when {
            netProfit.isNegative() -> {
                warnings.add("Operating at a net financial loss.")
                20
            }
            revenue.isPositive() -> {
                val margin = netProfit.amount.multiply(BigDecimal(100)).divide(revenue.amount, 2, java.math.RoundingMode.HALF_EVEN).toDouble()
                when {
                    margin >= 25.0 -> 100
                    margin >= 15.0 -> 85
                    margin >= 5.0 -> 70
                    margin > 0.0 -> 50
                    else -> 40
                }
            }
            else -> 50
        }

        // 3. Receivable Health Score (0 - 100)
        val receivableScore: Int = when {
            totalReceivable.isZero() -> 90
            else -> {
                val overdueRatio = overdueReceivable.amount.multiply(BigDecimal(100))
                    .divide(totalReceivable.amount, 2, java.math.RoundingMode.HALF_EVEN).toDouble()
                when {
                    overdueRatio > 50.0 -> {
                        criticals.add("Over 50% of receivables are overdue.")
                        25
                    }
                    overdueRatio > 25.0 -> {
                        warnings.add("Overdue receivables exceed 25% of total due.")
                        50
                    }
                    overdueRatio > 10.0 -> 75
                    else -> 95
                }
            }
        }

        // 4. Payable Health Score (0 - 100)
        val payableScore: Int = when {
            totalPayable.isZero() -> 95
            else -> {
                val overdueRatio = overduePayable.amount.multiply(BigDecimal(100))
                    .divide(totalPayable.amount, 2, java.math.RoundingMode.HALF_EVEN).toDouble()
                when {
                    overdueRatio > 50.0 -> {
                        warnings.add("Over 50% of vendor payables are overdue.")
                        35
                    }
                    overdueRatio > 20.0 -> 60
                    else -> 90
                }
            }
        }

        // 5. Expense Control Score (0 - 100)
        val expenseControlScore: Int = when {
            revenue.isPositive() -> {
                val expRatio = expenses.amount.multiply(BigDecimal(100))
                    .divide(revenue.amount, 2, java.math.RoundingMode.HALF_EVEN).toDouble()
                when {
                    expRatio > 100.0 -> {
                        criticals.add("Operating expenses exceed total revenue ($expRatio%).")
                        20
                    }
                    expRatio > 85.0 -> 50
                    expRatio > 65.0 -> 75
                    else -> 95
                }
            }
            expenses.isPositive() -> 30
            else -> 80
        }

        // 6. Reconciliation Health Score (0 - 100)
        val reconciliationScore: Int = when {
            discrepancyCount > 5 -> {
                criticals.add("High volume of unresolved financial discrepancies ($discrepancyCount).")
                20
            }
            discrepancyCount > 0 -> {
                warnings.add("Unresolved financial discrepancies exist ($discrepancyCount).")
                55
            }
            else -> 100
        }

        // 7. Governance Control Score (0 - 100)
        val governanceScore: Int = when {
            !isTrialBalanced || !isBalanceSheetBalanced -> {
                criticals.add("Financial statements contain unbalanced equations.")
                15
            }
            else -> 100
        }

        // Overall Weighted Score
        val weightedSum = (liquidityScore * 0.20) +
                (profitabilityScore * 0.20) +
                (receivableScore * 0.15) +
                (payableScore * 0.10) +
                (expenseControlScore * 0.10) +
                (reconciliationScore * 0.10) +
                (governanceScore * 0.15)

        val totalScore = weightedSum.toInt().coerceIn(0, 100)

        val status = when {
            totalScore >= 85 && criticals.isEmpty() -> FinancialHealthStatus.EXCELLENT
            totalScore >= 70 && criticals.isEmpty() -> FinancialHealthStatus.HEALTHY
            totalScore >= 50 && criticals.isEmpty() -> FinancialHealthStatus.WATCH
            criticals.isNotEmpty() || totalScore < 35 -> FinancialHealthStatus.CRITICAL
            else -> FinancialHealthStatus.AT_RISK
        }

        return FinancialHealthScore(
            score = totalScore,
            status = status,
            liquidityScore = liquidityScore,
            profitabilityScore = profitabilityScore,
            receivableHealthScore = receivableScore,
            payableHealthScore = payableScore,
            expenseControlScore = expenseControlScore,
            reconciliationHealthScore = reconciliationScore,
            governanceControlScore = governanceScore,
            warningIndicators = warnings,
            criticalIndicators = criticals
        )
    }
}
