package com.sucharu.sucharupro.data.job

import com.sucharu.sucharupro.data.event.MockPostgresEventDatabase
import com.sucharu.sucharupro.data.job.postgres.PostgresJobRepository
import com.sucharu.sucharupro.data.job.postgres.PostgresJobScheduleRepository
import com.sucharu.sucharupro.data.persistence.postgres.TenantContext
import com.sucharu.sucharupro.domain.job.model.JobScheduleDefinition
import com.sucharu.sucharupro.domain.job.model.ScheduleType
import com.sucharu.sucharupro.domain.job.scheduler.JobScheduler
import com.sucharu.sucharupro.domain.job.scheduler.ScheduleCalculator
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

class JobSchedulingAndCronTest {

    private lateinit var mockDb: MockPostgresEventDatabase
    private lateinit var jobRepo: PostgresJobRepository
    private lateinit var scheduleRepo: PostgresJobScheduleRepository
    private lateinit var scheduler: JobScheduler

    @Before
    fun setUp() {
        mockDb = MockPostgresEventDatabase()
        jobRepo = PostgresJobRepository(mockDb)
        scheduleRepo = PostgresJobScheduleRepository(mockDb)
        scheduler = JobScheduler(scheduleRepo, jobRepo)
    }

    @Test
    fun testCronCalculatorEveryHour() {
        // Base time: 2026-08-24 10:15:00 UTC
        val zone = ZoneId.of("UTC")
        val baseTime = ZonedDateTime.of(2026, 8, 24, 10, 15, 0, 0, zone).toInstant().toEpochMilli()

        // Cron: "0 * * * *" -> at minute 0 of every hour
        val nextRun = ScheduleCalculator.calculateNextCronRun("0 * * * *", baseTime, "UTC")
        val nextDateTime = ZonedDateTime.ofInstant(Instant.ofEpochMilli(nextRun), zone)

        assertEquals(11, nextDateTime.hour)
        assertEquals(0, nextDateTime.minute)
    }

    @Test
    fun testCronCalculatorStepMinutes() {
        val zone = ZoneId.of("Asia/Dhaka")
        val baseTime = ZonedDateTime.of(2026, 8, 24, 14, 12, 0, 0, zone).toInstant().toEpochMilli()

        // Cron: "*/15 * * * *" -> minute 0, 15, 30, 45
        val nextRun = ScheduleCalculator.calculateNextCronRun("*/15 * * * *", baseTime, "Asia/Dhaka")
        val nextDateTime = ZonedDateTime.ofInstant(Instant.ofEpochMilli(nextRun), zone)

        assertEquals(14, nextDateTime.hour)
        assertEquals(15, nextDateTime.minute)
    }

    @Test
    fun testFixedIntervalCalculation() {
        val baseTime = 1000000L
        val intervalMs = 60000L
        val nextRun = ScheduleCalculator.calculateNextIntervalRun(baseTime, intervalMs)
        assertEquals(1060000L, nextRun)
    }

    @Test
    fun testSchedulerPollsAndEnqueuesDueRecurringJobs() {
        runBlocking {
            val tenant = TenantContext("tenant_alpha")
            val pastTime = System.currentTimeMillis() - 10000L

            val schedule = JobScheduleDefinition(
                scheduleId = "sched-hourly-summary",
                projectId = "tenant_alpha",
                jobType = "report.hourly_summary",
                scheduleType = ScheduleType.CRON,
                cronExpression = "0 * * * *",
                timezone = "Asia/Dhaka",
                nextRunAt = pastTime
            )
            scheduleRepo.saveSchedule(schedule, tenant)

            val generatedJobs = scheduler.pollAndScheduleDueJobs(tenant, 10)
            assertEquals(1, generatedJobs.size)

            val createdJob = jobRepo.getJobById(generatedJobs[0], tenant)
            assertNotNull(createdJob)
            assertEquals("report.hourly_summary", createdJob?.jobType)
            assertEquals("tenant_alpha", createdJob?.projectId)

            // Verify schedule next_run_at is moved to future
            val updatedSchedule = scheduleRepo.getScheduleById("sched-hourly-summary", tenant)
            assertTrue((updatedSchedule?.nextRunAt ?: 0L) > System.currentTimeMillis())
        }
    }
}
