package com.sucharu.sucharupro.data.observability.alert

import com.sucharu.sucharupro.domain.observability.AlertSeverity
import com.sucharu.sucharupro.domain.observability.AlertStatus
import com.sucharu.sucharupro.domain.observability.OperationalAlert
import java.util.concurrent.ConcurrentHashMap

/**
 * Production-grade Operational Alert Engine with Strict Anti-Noise Deduplication (INFRA-04 Step 09).
 */
class OperationalAlertEngine(
    private val alertCooldownMs: Long = 300_000L // 5 minutes
) {

    private val alertsByDedupKey = ConcurrentHashMap<String, OperationalAlert>()

    fun recordCondition(
        projectId: String,
        subsystem: String,
        alertKey: String,
        title: String,
        summary: String,
        severity: AlertSeverity,
        failureClass: String? = null
    ): OperationalAlert {
        val dedupKey = "$projectId:$subsystem:$alertKey"
        val existing = alertsByDedupKey[dedupKey]

        val now = System.currentTimeMillis()
        if (existing != null && existing.status != AlertStatus.RESOLVED) {
            // Update existing active alert occurrences without spamming new alert instances
            val updated = existing.copy(
                occurrences = existing.occurrences + 1,
                lastOccurredAt = now,
                summary = summary,
                severity = if (severity == AlertSeverity.CRITICAL) AlertSeverity.CRITICAL else existing.severity
            )
            alertsByDedupKey[dedupKey] = updated
            return updated
        }

        // New active alert
        val newAlert = OperationalAlert(
            projectId = projectId,
            alertKey = alertKey,
            deduplicationKey = dedupKey,
            title = title,
            summary = summary,
            severity = severity,
            status = AlertStatus.OPEN,
            subsystem = subsystem,
            failureClass = failureClass,
            occurrences = 1,
            firstOccurredAt = now,
            lastOccurredAt = now
        )
        alertsByDedupKey[dedupKey] = newAlert
        return newAlert
    }

    fun resolveCondition(projectId: String, subsystem: String, alertKey: String, resolutionNotes: String = "Condition returned to normal"): OperationalAlert? {
        val dedupKey = "$projectId:$subsystem:$alertKey"
        val existing = alertsByDedupKey[dedupKey] ?: return null
        if (existing.status == AlertStatus.RESOLVED) return existing

        val resolved = existing.copy(
            status = AlertStatus.RESOLVED,
            resolvedAt = System.currentTimeMillis(),
            resolutionNotes = resolutionNotes
        )
        alertsByDedupKey[dedupKey] = resolved
        return resolved
    }

    fun acknowledgeAlert(alertId: String, acknowledgedBy: String): OperationalAlert? {
        val entry = alertsByDedupKey.entries.find { it.value.alertId == alertId } ?: return null
        val acked = entry.value.copy(
            status = AlertStatus.ACKNOWLEDGED,
            acknowledgedBy = acknowledgedBy
        )
        alertsByDedupKey[entry.key] = acked
        return acked
    }

    fun getActiveAlerts(projectId: String? = null): List<OperationalAlert> {
        return alertsByDedupKey.values
            .filter { it.status != AlertStatus.RESOLVED }
            .filter { projectId == null || it.projectId == projectId }
            .sortedByDescending { it.lastOccurredAt }
    }

    fun getAllAlerts(projectId: String? = null): List<OperationalAlert> {
        return alertsByDedupKey.values
            .filter { projectId == null || it.projectId == projectId }
            .sortedByDescending { it.lastOccurredAt }
    }

    fun clear() = alertsByDedupKey.clear()
}
