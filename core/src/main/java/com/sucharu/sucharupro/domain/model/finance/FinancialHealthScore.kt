package com.sucharu.sucharupro.domain.model.finance

/**
 * Deterministic Financial Health Score for management indicators (Module 09 Step 10).
 *
 * MANAGEMENT INDICATOR ONLY. Never alters accounting books.
 */
data class FinancialHealthScore(
    val score: Int, // 0 to 100
    val status: FinancialHealthStatus,
    val liquidityScore: Int, // 0 to 100
    val profitabilityScore: Int, // 0 to 100
    val receivableHealthScore: Int, // 0 to 100
    val payableHealthScore: Int, // 0 to 100
    val expenseControlScore: Int, // 0 to 100
    val reconciliationHealthScore: Int, // 0 to 100
    val governanceControlScore: Int, // 0 to 100
    val warningIndicators: List<String> = emptyList(),
    val criticalIndicators: List<String> = emptyList(),
    val calculatedAt: Long = System.currentTimeMillis()
) {
    init {
        require(score in 0..100) { "Overall score must be between 0 and 100." }
        require(liquidityScore in 0..100) { "Liquidity score must be between 0 and 100." }
        require(profitabilityScore in 0..100) { "Profitability score must be between 0 and 100." }
        require(receivableHealthScore in 0..100) { "Receivable health score must be between 0 and 100." }
        require(payableHealthScore in 0..100) { "Payable health score must be between 0 and 100." }
        require(expenseControlScore in 0..100) { "Expense control score must be between 0 and 100." }
        require(reconciliationHealthScore in 0..100) { "Reconciliation health score must be between 0 and 100." }
        require(governanceControlScore in 0..100) { "Governance control score must be between 0 and 100." }
    }
}

enum class FinancialHealthStatus(val defaultLabel: String) {
    EXCELLENT("Excellent"),
    HEALTHY("Healthy"),
    WATCH("Watch / Needs Attention"),
    AT_RISK("At Risk"),
    CRITICAL("Critical Risk")
}
