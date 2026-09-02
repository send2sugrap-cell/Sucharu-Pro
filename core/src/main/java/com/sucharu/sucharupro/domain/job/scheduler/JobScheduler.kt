package com.sucharu.sucharupro.domain.job.scheduler

import com.sucharu.sucharupro.data.api.model.PrincipalType
import com.sucharu.sucharupro.data.job.postgres.JobRepository
import com.sucharu.sucharupro.data.job.postgres.JobScheduleRepository
import com.sucharu.sucharupro.data.persistence.postgres.TenantContext
import com.sucharu.sucharupro.domain.job.model.JobDefinition
import com.sucharu.sucharupro.domain.job.model.JobPriority
import com.sucharu.sucharupro.domain.job.model.JobScheduleDefinition
import com.sucharu.sucharupro.domain.job.model.JobStatus
import com.sucharu.sucharupro.domain.job.model.JobTriggerType
import com.sucharu.sucharupro.domain.job.model.ScheduleType
import java.util.UUID

/**
 * Production-grade Server-Authoritative Job Scheduler (INFRA-04 Step 04).
 */
class JobScheduler(
    private val scheduleRepository: JobScheduleRepository,
    private val jobRepository: JobRepository
) {
    /**
     * Polls due schedules for a tenant, generates background jobs, and advances next run time.
     * @return List of newly scheduled job IDs.
     */
    suspend fun pollAndScheduleDueJobs(tenantContext: TenantContext, batchSize: Int = 10): List<String> {
        val dueSchedules = scheduleRepository.getDueSchedules(batchSize, tenantContext)
        val generatedJobIds = mutableListOf<String>()

        val now = System.currentTimeMillis()
        for (schedule in dueSchedules) {
            // 1. Calculate next run
            val nextRunAt = when (schedule.scheduleType) {
                ScheduleType.CRON -> {
                    val cron = schedule.cronExpression ?: continue
                    ScheduleCalculator.calculateNextCronRun(cron, now, schedule.timezone)
                }
                ScheduleType.FIXED_INTERVAL -> {
                    val interval = schedule.fixedIntervalMs ?: continue
                    ScheduleCalculator.calculateNextIntervalRun(now, interval)
                }
                ScheduleType.ONE_TIME_DELAY -> {
                    // Disabled after one run
                    now + 864000000L
                }
            }

            // 2. Enqueue background job
            val jobId = UUID.randomUUID().toString()
            val job = JobDefinition(
                jobId = jobId,
                projectId = tenantContext.projectId,
                jobType = schedule.jobType,
                jobVersion = schedule.scheduleVersion,
                triggerType = JobTriggerType.SCHEDULE,
                priority = JobPriority.NORMAL,
                status = JobStatus.QUEUED,
                payloadJson = schedule.payloadJson,
                correlationId = UUID.randomUUID().toString(),
                actorType = PrincipalType.SYSTEM,
                actorId = "SCHEDULER",
                principalType = PrincipalType.SYSTEM,
                source = "scheduler:${schedule.scheduleId}"
            )

            jobRepository.enqueueJob(job, tenantContext)
            generatedJobIds.add(jobId)

            // 3. Advance schedule run times
            if (schedule.scheduleType == ScheduleType.ONE_TIME_DELAY) {
                scheduleRepository.setScheduleEnabled(schedule.scheduleId, false, tenantContext)
            } else {
                scheduleRepository.updateScheduleRunTimes(
                    scheduleId = schedule.scheduleId,
                    lastRunAt = now,
                    nextRunAt = nextRunAt,
                    tenantContext = tenantContext
                )
            }
        }

        return generatedJobIds
    }
}
