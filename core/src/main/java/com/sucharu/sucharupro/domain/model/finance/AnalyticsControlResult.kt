package com.sucharu.sucharupro.domain.model.finance

/**
 * Governance Control check result (Module 09 Step 10).
 */
data class AnalyticsControlResult(
    val controlCode: String,
    val title: String,
    val status: FinancialGovernanceStatus,
    val severity: FinancialGovernanceSeverity,
    val description: String,
    val source: String,
    val detectedAt: Long = System.currentTimeMillis()
) {
    init {
        require(controlCode.isNotBlank()) { "Control code cannot be blank." }
        require(title.isNotBlank()) { "Title cannot be blank." }
    }
}

enum class FinancialGovernanceStatus(val defaultLabel: String) {
    PASSED("Passed"),
    WARNING("Warning"),
    CONTROL_EXCEPTION("Control Exception"),
    CRITICAL_EXCEPTION("Critical Governance Exception")
}

enum class FinancialGovernanceSeverity(val defaultLabel: String) {
    INFO("Info"),
    LOW("Low"),
    MEDIUM("Medium"),
    HIGH("High"),
    CRITICAL("Critical")
}

data class FinancialGovernanceAlert(
    val alertId: String,
    val projectId: String,
    val controlCode: String,
    val title: String,
    val message: String,
    val status: FinancialGovernanceStatus,
    val createdAt: Long = System.currentTimeMillis()
) {
    init {
        require(alertId.isNotBlank()) { "Alert ID cannot be blank." }
        require(projectId.isNotBlank()) { "Project ID cannot be blank." }
    }
}
