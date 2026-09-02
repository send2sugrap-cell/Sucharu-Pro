package com.sucharu.sucharupro.domain.job.model

import java.util.UUID

/**
 * Schedule type classification.
 */
enum class ScheduleType {
    CRON,
    FIXED_INTERVAL,
    ONE_TIME_DELAY
}

/**
 * Server-authoritative recurring job schedule definition (INFRA-04 Step 04).
 */
data class JobScheduleDefinition(
    val scheduleId: String = UUID.randomUUID().toString(),
    val projectId: String,
    val jobType: String,
    val scheduleType: ScheduleType = ScheduleType.CRON,
    val cronExpression: String? = null,
    val fixedIntervalMs: Long? = null,
    val timezone: String = "Asia/Dhaka",
    val isEnabled: Boolean = true,
    val payloadJson: String = "{}",
    val lastRunAt: Long? = null,
    val nextRunAt: Long,
    val scheduleVersion: String = "v1",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    init {
        require(scheduleId.isNotBlank()) { "scheduleId cannot be blank" }
        require(projectId.isNotBlank()) { "projectId cannot be blank" }
        require(jobType.isNotBlank()) { "jobType cannot be blank" }
        require(timezone.isNotBlank()) { "timezone cannot be blank" }
        if (scheduleType == ScheduleType.CRON) {
            require(!cronExpression.isNullOrBlank()) { "cronExpression is required for CRON schedules" }
        } else if (scheduleType == ScheduleType.FIXED_INTERVAL) {
            require(fixedIntervalMs != null && fixedIntervalMs > 0) { "fixedIntervalMs must be positive for FIXED_INTERVAL schedules" }
        }
    }
}
