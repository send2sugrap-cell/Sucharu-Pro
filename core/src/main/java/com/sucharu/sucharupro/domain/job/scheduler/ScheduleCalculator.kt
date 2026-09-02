package com.sucharu.sucharupro.domain.job.scheduler

import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit

/**
 * Production-grade server-authoritative schedule calculator with TimeZone and cron support (INFRA-04 Step 04).
 */
object ScheduleCalculator {

    /**
     * Calculates the next execution timestamp after [afterTimestamp] for a fixed interval.
     */
    fun calculateNextIntervalRun(
        afterTimestamp: Long,
        intervalMs: Long
    ): Long {
        require(intervalMs > 0) { "intervalMs must be positive" }
        return afterTimestamp + intervalMs
    }

    /**
     * Calculates the next execution timestamp after [afterTimestamp] for a 5-field cron expression.
     * Supports:
     * - Wildcard: `*`
     * - Step values: `* / 15`, `* / 5`
     * - Explicit values: `0`, `30`, `5`
     * - TimeZone awareness: e.g. `Asia/Dhaka`, `UTC`
     */
    fun calculateNextCronRun(
        cronExpression: String,
        afterTimestamp: Long = System.currentTimeMillis(),
        timezoneStr: String = "Asia/Dhaka"
    ): Long {
        val zone = try {
            ZoneId.of(timezoneStr)
        } catch (_: Exception) {
            ZoneId.of("UTC")
        }

        val parts = cronExpression.trim().split(Regex("\\s+"))
        require(parts.size == 5) {
            "Invalid cron expression: '$cronExpression'. Expected 5 fields: (minute hour day-of-month month day-of-week)"
        }

        val minuteField = parts[0]
        val hourField = parts[1]
        val dayOfMonthField = parts[2]
        val monthField = parts[3]
        val dayOfWeekField = parts[4]

        var current = ZonedDateTime.ofInstant(Instant.ofEpochMilli(afterTimestamp), zone)
            .truncatedTo(ChronoUnit.MINUTES)
            .plusMinutes(1) // Advance by at least 1 minute to calculate strictly next run

        // Search up to 366 days into the future
        val maxIterations = 366 * 24 * 60
        var count = 0

        while (count < maxIterations) {
            val month = current.monthValue
            if (!matchesField(monthField, month, 1, 12)) {
                current = current.plusMonths(1).withDayOfMonth(1).withHour(0).withMinute(0)
                count += 60
                continue
            }

            val dayOfMonth = current.dayOfMonth
            val dayOfWeek = current.dayOfWeek.value % 7 // 0=Sunday..6=Saturday or 1=Mon..7=Sun
            if (!matchesField(dayOfMonthField, dayOfMonth, 1, 31) || !matchesDayOfWeek(dayOfWeekField, current.dayOfWeek.value)) {
                current = current.plusDays(1).withHour(0).withMinute(0)
                count += 60
                continue
            }

            val hour = current.hour
            if (!matchesField(hourField, hour, 0, 23)) {
                current = current.plusHours(1).withMinute(0)
                count++
                continue
            }

            val minute = current.minute
            if (matchesField(minuteField, minute, 0, 59)) {
                return current.toInstant().toEpochMilli()
            }

            current = current.plusMinutes(1)
            count++
        }

        // Fallback if no matching time found within search boundary
        return afterTimestamp + 86400000L
    }

    private fun matchesDayOfWeek(field: String, dayOfWeekIso: Int): Boolean {
        if (field == "*") return true
        // ISO 1=Mon..7=Sun. Standard cron 0 or 7 = Sunday, 1 = Mon...
        val standardDay = if (dayOfWeekIso == 7) 0 else dayOfWeekIso
        val altDay = if (dayOfWeekIso == 7) 7 else dayOfWeekIso

        return matchesField(field, standardDay, 0, 7) || matchesField(field, altDay, 0, 7)
    }

    private fun matchesField(field: String, value: Int, min: Int, max: Int): Boolean {
        if (field == "*") return true

        if (field.startsWith("*/")) {
            val step = field.substring(2).toIntOrNull() ?: return false
            if (step <= 0) return false
            return (value - min) % step == 0
        }

        if (field.contains(",")) {
            val items = field.split(",")
            return items.any { it.toIntOrNull() == value }
        }

        if (field.contains("-")) {
            val bounds = field.split("-")
            if (bounds.size == 2) {
                val start = bounds[0].toIntOrNull() ?: return false
                val end = bounds[1].toIntOrNull() ?: return false
                return value in start..end
            }
        }

        return field.toIntOrNull() == value
    }
}
