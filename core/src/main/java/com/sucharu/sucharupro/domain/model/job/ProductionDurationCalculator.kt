package com.sucharu.sucharupro.domain.model.job

import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

/**
 * Utility to calculate duration in seconds between ISO-8601 timestamps safely.
 */
object ProductionDurationCalculator {

    fun calculateDurationSeconds(startedAt: String?, completedAt: String?): Long? {
        if (startedAt.isNullOrBlank() || completedAt.isNullOrBlank()) return null
        return try {
            val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
            sdf.timeZone = TimeZone.getTimeZone("UTC")
            val startDate = sdf.parse(startedAt) ?: return null
            val completeDate = sdf.parse(completedAt) ?: return null
            val diffMs = completeDate.time - startDate.time
            if (diffMs >= 0) diffMs / 1000 else null
        } catch (_: Exception) {
            null
        }
    }

    fun formatDuration(seconds: Long): String {
        if (seconds <= 0L) return "0m"
        val hours = seconds / 3600
        val minutes = (seconds % 3600) / 60
        return when {
            hours > 0 && minutes > 0 -> "${hours}h ${minutes}m"
            hours > 0 -> "${hours}h"
            else -> "${minutes}m"
        }
    }
}
