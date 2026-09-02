package com.sucharu.sucharupro.domain.model.qc.governance

import com.sucharu.sucharupro.domain.model.user.UserRole

/**
 * Escalation level hierarchy for quality alerts and governance reviews (Module 06 Step 10).
 */
enum class QcEscalationLevel(
    val defaultLabel: String,
    val rank: Int,
    val responsibleRole: UserRole?
) {
    NONE("None", 0, null),
    QC_INSPECTOR("QC Inspector Level", 1, UserRole.QC_INSPECTOR),
    MANAGER("Manager Level", 2, UserRole.MANAGER),
    ADMIN("Executive / Admin Level", 3, UserRole.ADMIN);

    companion object {
        fun fromString(value: String?): QcEscalationLevel? {
            if (value.isNullOrBlank()) return null
            return entries.find { it.name.equals(value.trim(), ignoreCase = true) }
        }
    }
}
