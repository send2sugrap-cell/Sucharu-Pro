package com.sucharu.sucharupro.domain.model.continuity

/**
 * BI-11 System Continuity Health Status.
 */
enum class ContinuityHealthStatus {
    HEALTHY,
    DEGRADED,
    RECOVERY_REQUIRED,
    BLOCKED
}

/**
 * BI-11 Target RPO / RTO Policy Configuration.
 */
data class ContinuityRpoRtoTargets(
    val targetRpoHours: Int = 1,
    val targetRtoHours: Int = 2,
    val backupFrequencyHours: Int = 24,
    val retentionDays: Int = 30
)

/**
 * BI-11 Business Continuity & Operational Readiness Summary Model.
 */
data class BusinessContinuitySummary(
    val healthStatus: ContinuityHealthStatus = ContinuityHealthStatus.HEALTHY,
    val rpoRtoTargets: ContinuityRpoRtoTargets = ContinuityRpoRtoTargets(),
    val lastBackupTimestamp: String,
    val isBackupVerified: Boolean = true,
    val activeFlywayMigrationVersion: String = "V20261219",
    val outboxPendingEventCount: Int = 0,
    val isPostgresRlsSecurityActive: Boolean = true, // Critical Invariant: Always true!
    val readinessStatusMessage: String,
    val generatedAt: String
)
