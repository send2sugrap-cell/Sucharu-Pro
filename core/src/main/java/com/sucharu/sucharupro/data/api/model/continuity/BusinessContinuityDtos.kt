package com.sucharu.sucharupro.data.api.model.continuity

import kotlinx.serialization.Serializable

@Serializable
data class ContinuityRpoRtoTargetsDto(
    val targetRpoHours: Int = 1,
    val targetRtoHours: Int = 2,
    val backupFrequencyHours: Int = 24,
    val retentionDays: Int = 30
)

@Serializable
data class BusinessContinuitySummaryDto(
    val healthStatus: String = "HEALTHY",
    val rpoRtoTargets: ContinuityRpoRtoTargetsDto = ContinuityRpoRtoTargetsDto(),
    val lastBackupTimestamp: String,
    val isBackupVerified: Boolean = true,
    val activeFlywayMigrationVersion: String = "V20261219",
    val outboxPendingEventCount: Int = 0,
    val isPostgresRlsSecurityActive: Boolean = true,
    val readinessStatusMessage: String,
    val generatedAt: String
)
