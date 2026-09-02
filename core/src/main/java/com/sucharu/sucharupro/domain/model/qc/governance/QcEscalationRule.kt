package com.sucharu.sucharupro.domain.model.qc.governance

import com.sucharu.sucharupro.domain.model.user.UserRole

/**
 * Configurable escalation rule for quality alerts (Module 06 Step 10).
 */
data class QcEscalationRule(
    val ruleId: String,
    val projectId: String,
    val kpiType: QcGovernanceKpi? = null,
    val severity: QcAlertSeverity,
    val escalationLevel: QcEscalationLevel,
    val maxResponseTimeMinutes: Long = 60L,
    val responsibleRole: UserRole = escalationLevel.responsibleRole ?: UserRole.MANAGER,
    val description: String? = null,
    val enabled: Boolean = true
) {
    init {
        require(ruleId.isNotBlank()) { "Rule ID cannot be blank" }
        require(projectId.isNotBlank()) { "Project ID cannot be blank" }
        require(maxResponseTimeMinutes > 0) { "Response time must be positive" }
    }
}
