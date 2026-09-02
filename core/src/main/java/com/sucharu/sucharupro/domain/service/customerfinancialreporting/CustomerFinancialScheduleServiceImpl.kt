package com.sucharu.sucharupro.domain.service.customerfinancialreporting

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.customerfinancialreporting.*
import com.sucharu.sucharupro.domain.repository.CustomerRepository
import com.sucharu.sucharupro.domain.repository.customerfinancialreporting.CustomerFinancialAlertRepository
import com.sucharu.sucharupro.domain.repository.customerfinancialreporting.CustomerFinancialReportScheduleRepository
import com.sucharu.sucharupro.domain.validation.customerfinancialreporting.CustomerFinancialAlertValidator
import java.time.Instant
import java.time.ZoneId
import java.util.UUID

class CustomerFinancialScheduleServiceImpl(
    private val scheduleRepository: CustomerFinancialReportScheduleRepository,
    private val customerRepository: CustomerRepository,
    private val documentDeliveryService: CustomerFinancialDocumentDeliveryService,
    private val alertRepository: CustomerFinancialAlertRepository? = null
) : CustomerFinancialScheduleService {

    private fun calculateNextRun(fromTimestamp: Long, frequency: CustomerFinancialScheduleFrequency, timezone: String): Long {
        val zone = try { ZoneId.of(timezone) } catch (_: Exception) { ZoneId.of("UTC") }
        val zdt = Instant.ofEpochMilli(fromTimestamp).atZone(zone)
        val nextZdt = when (frequency) {
            CustomerFinancialScheduleFrequency.DAILY -> zdt.plusDays(1)
            CustomerFinancialScheduleFrequency.WEEKLY -> zdt.plusWeeks(1)
            CustomerFinancialScheduleFrequency.MONTHLY -> zdt.plusMonths(1)
        }
        return nextZdt.toInstant().toEpochMilli()
    }

    override suspend fun createSchedule(
        tenantId: String,
        projectId: String,
        customerId: String,
        reportType: CustomerFinancialReportType,
        format: CustomerFinancialReportFormat,
        frequency: CustomerFinancialScheduleFrequency,
        timezone: String,
        firstRunAt: Long?,
        actorId: String,
        actorRole: String
    ): DomainResult<CustomerFinancialReportSchedule> {
        val cust = customerRepository.getCustomerById(customerId)
            ?: return DomainResult.Error(IllegalArgumentException("Customer '$customerId' does not exist."))

        val now = System.currentTimeMillis()
        val nextRun = firstRunAt ?: calculateNextRun(now, frequency, timezone)

        val schedule = CustomerFinancialReportSchedule(
            scheduleId = UUID.randomUUID().toString(),
            tenantId = tenantId,
            projectId = projectId,
            customerId = customerId,
            reportType = reportType,
            format = format,
            frequency = frequency,
            timezone = timezone,
            status = CustomerFinancialReportScheduleStatus.ACTIVE,
            nextRunAt = nextRun,
            createdBy = actorId,
            createdAt = now,
            updatedAt = now
        )

        val saveRes = scheduleRepository.saveSchedule(schedule)
        if (saveRes is DomainResult.Error) return saveRes

        alertRepository?.recordAuditEvent(
            CustomerFinancialAlertAuditEvent(
                tenantId = tenantId,
                projectId = projectId,
                alertId = schedule.scheduleId,
                eventType = CustomerFinancialAlertEventType.SCHEDULE_CREATED,
                actorId = actorId,
                actorRole = actorRole,
                detailsJson = "{\"reportType\":\"${reportType.name}\",\"frequency\":\"${frequency.name}\"}"
            )
        )

        return DomainResult.Success(schedule)
    }

    override suspend fun updateSchedule(
        tenantId: String,
        projectId: String,
        scheduleId: String,
        format: CustomerFinancialReportFormat?,
        frequency: CustomerFinancialScheduleFrequency?,
        timezone: String?,
        nextRunAt: Long?,
        actorId: String,
        actorRole: String
    ): DomainResult<CustomerFinancialReportSchedule> {
        val schedule = when (val res = getSchedule(tenantId, projectId, scheduleId)) {
            is DomainResult.Success -> res.data
            is DomainResult.Error -> return res
            DomainResult.Loading -> return DomainResult.Error(IllegalStateException("Loading state"))
        }

        if (schedule.status == CustomerFinancialReportScheduleStatus.CANCELLED) {
            return DomainResult.Error(IllegalStateException("Cannot update a CANCELLED schedule."))
        }

        val updated = schedule.copy(
            format = format ?: schedule.format,
            frequency = frequency ?: schedule.frequency,
            timezone = timezone ?: schedule.timezone,
            nextRunAt = nextRunAt ?: schedule.nextRunAt,
            updatedAt = System.currentTimeMillis(),
            version = schedule.version + 1
        )

        val saveRes = scheduleRepository.saveSchedule(updated)
        if (saveRes is DomainResult.Error) return saveRes

        alertRepository?.recordAuditEvent(
            CustomerFinancialAlertAuditEvent(
                tenantId = tenantId,
                projectId = projectId,
                alertId = scheduleId,
                eventType = CustomerFinancialAlertEventType.SCHEDULE_UPDATED,
                actorId = actorId,
                actorRole = actorRole
            )
        )

        return DomainResult.Success(updated)
    }

    override suspend fun pauseSchedule(
        tenantId: String,
        projectId: String,
        scheduleId: String,
        actorId: String,
        actorRole: String
    ): DomainResult<CustomerFinancialReportSchedule> {
        val schedule = when (val res = getSchedule(tenantId, projectId, scheduleId)) {
            is DomainResult.Success -> res.data
            is DomainResult.Error -> return res
            DomainResult.Loading -> return DomainResult.Error(IllegalStateException("Loading state"))
        }

        val valRes = CustomerFinancialAlertValidator.validateScheduleStatusTransition(
            schedule.status, CustomerFinancialReportScheduleStatus.PAUSED
        )
        if (valRes is DomainResult.Error) return valRes

        val updated = schedule.copy(
            status = CustomerFinancialReportScheduleStatus.PAUSED,
            updatedAt = System.currentTimeMillis(),
            version = schedule.version + 1
        )

        val saveRes = scheduleRepository.saveSchedule(updated)
        if (saveRes is DomainResult.Error) return saveRes

        alertRepository?.recordAuditEvent(
            CustomerFinancialAlertAuditEvent(
                tenantId = tenantId,
                projectId = projectId,
                alertId = scheduleId,
                eventType = CustomerFinancialAlertEventType.SCHEDULE_PAUSED,
                actorId = actorId,
                actorRole = actorRole
            )
        )

        return DomainResult.Success(updated)
    }

    override suspend fun resumeSchedule(
        tenantId: String,
        projectId: String,
        scheduleId: String,
        actorId: String,
        actorRole: String
    ): DomainResult<CustomerFinancialReportSchedule> {
        val schedule = when (val res = getSchedule(tenantId, projectId, scheduleId)) {
            is DomainResult.Success -> res.data
            is DomainResult.Error -> return res
            DomainResult.Loading -> return DomainResult.Error(IllegalStateException("Loading state"))
        }

        val valRes = CustomerFinancialAlertValidator.validateScheduleStatusTransition(
            schedule.status, CustomerFinancialReportScheduleStatus.ACTIVE
        )
        if (valRes is DomainResult.Error) return valRes

        val now = System.currentTimeMillis()
        val nextRun = if (schedule.nextRunAt < now) {
            calculateNextRun(now, schedule.frequency, schedule.timezone)
        } else schedule.nextRunAt

        val updated = schedule.copy(
            status = CustomerFinancialReportScheduleStatus.ACTIVE,
            nextRunAt = nextRun,
            updatedAt = now,
            version = schedule.version + 1
        )

        val saveRes = scheduleRepository.saveSchedule(updated)
        if (saveRes is DomainResult.Error) return saveRes

        alertRepository?.recordAuditEvent(
            CustomerFinancialAlertAuditEvent(
                tenantId = tenantId,
                projectId = projectId,
                alertId = scheduleId,
                eventType = CustomerFinancialAlertEventType.SCHEDULE_RESUMED,
                actorId = actorId,
                actorRole = actorRole
            )
        )

        return DomainResult.Success(updated)
    }

    override suspend fun cancelSchedule(
        tenantId: String,
        projectId: String,
        scheduleId: String,
        actorId: String,
        actorRole: String
    ): DomainResult<CustomerFinancialReportSchedule> {
        val schedule = when (val res = getSchedule(tenantId, projectId, scheduleId)) {
            is DomainResult.Success -> res.data
            is DomainResult.Error -> return res
            DomainResult.Loading -> return DomainResult.Error(IllegalStateException("Loading state"))
        }

        val valRes = CustomerFinancialAlertValidator.validateScheduleStatusTransition(
            schedule.status, CustomerFinancialReportScheduleStatus.CANCELLED
        )
        if (valRes is DomainResult.Error) return valRes

        val updated = schedule.copy(
            status = CustomerFinancialReportScheduleStatus.CANCELLED,
            updatedAt = System.currentTimeMillis(),
            version = schedule.version + 1
        )

        val saveRes = scheduleRepository.saveSchedule(updated)
        if (saveRes is DomainResult.Error) return saveRes

        alertRepository?.recordAuditEvent(
            CustomerFinancialAlertAuditEvent(
                tenantId = tenantId,
                projectId = projectId,
                alertId = scheduleId,
                eventType = CustomerFinancialAlertEventType.SCHEDULE_CANCELLED,
                actorId = actorId,
                actorRole = actorRole
            )
        )

        return DomainResult.Success(updated)
    }

    override suspend fun listSchedules(
        tenantId: String,
        projectId: String,
        customerId: String?,
        status: CustomerFinancialReportScheduleStatus?
    ): DomainResult<List<CustomerFinancialReportSchedule>> {
        return scheduleRepository.listSchedules(tenantId, projectId, customerId, status)
    }

    override suspend fun getSchedule(
        tenantId: String,
        projectId: String,
        scheduleId: String
    ): DomainResult<CustomerFinancialReportSchedule> {
        val res = scheduleRepository.getScheduleById(tenantId, projectId, scheduleId)
        if (res is DomainResult.Error) return res
        val schedule = (res as DomainResult.Success).data
            ?: return DomainResult.Error(IllegalArgumentException("Schedule '$scheduleId' not found."))
        return DomainResult.Success(schedule)
    }

    override suspend fun executeDueSchedules(
        tenantId: String,
        projectId: String,
        asOfTimestamp: Long,
        actorId: String,
        actorRole: String
    ): DomainResult<List<CustomerFinancialScheduleExecution>> {
        val dueRes = scheduleRepository.getDueSchedules(tenantId, projectId, asOfTimestamp, limit = 50)
        if (dueRes is DomainResult.Error) return dueRes
        val dueSchedules = (dueRes as DomainResult.Success).data

        val executions = mutableListOf<CustomerFinancialScheduleExecution>()

        for (schedule in dueSchedules) {
            val executionId = UUID.randomUUID().toString()
            val now = System.currentTimeMillis()

            alertRepository?.recordAuditEvent(
                CustomerFinancialAlertAuditEvent(
                    tenantId = tenantId,
                    projectId = projectId,
                    alertId = schedule.scheduleId,
                    eventType = CustomerFinancialAlertEventType.SCHEDULE_EXECUTION_STARTED,
                    actorId = actorId,
                    actorRole = actorRole,
                    detailsJson = "{\"executionId\":\"$executionId\"}"
                )
            )

            val deliveryRes = documentDeliveryService.generateAndRegisterDelivery(
                tenantId = tenantId,
                projectId = projectId,
                customerId = schedule.customerId,
                reportType = schedule.reportType,
                format = schedule.format,
                actorId = actorId,
                actorRole = actorRole
            )

            if (deliveryRes is DomainResult.Success) {
                val delivery = deliveryRes.data
                val exec = CustomerFinancialScheduleExecution(
                    executionId = executionId,
                    tenantId = tenantId,
                    projectId = projectId,
                    scheduleId = schedule.scheduleId,
                    customerId = schedule.customerId,
                    reportType = schedule.reportType,
                    format = schedule.format,
                    executedAt = now,
                    status = CustomerFinancialScheduleExecutionStatus.SUCCESS,
                    documentDeliveryId = delivery.deliveryId
                )
                scheduleRepository.recordExecution(exec)
                executions.add(exec)

                // Advance schedule
                val nextRun = calculateNextRun(now, schedule.frequency, schedule.timezone)
                val updatedSchedule = schedule.copy(
                    lastRunAt = now,
                    lastRunStatus = "SUCCESS",
                    consecutiveFailures = 0,
                    nextRunAt = nextRun,
                    updatedAt = now,
                    version = schedule.version + 1
                )
                scheduleRepository.saveSchedule(updatedSchedule)

                alertRepository?.recordAuditEvent(
                    CustomerFinancialAlertAuditEvent(
                        tenantId = tenantId,
                        projectId = projectId,
                        alertId = schedule.scheduleId,
                        eventType = CustomerFinancialAlertEventType.SCHEDULE_EXECUTION_SUCCEEDED,
                        actorId = actorId,
                        actorRole = actorRole,
                        detailsJson = "{\"deliveryId\":\"${delivery.deliveryId}\",\"nextRunAt\":$nextRun}"
                    )
                )
            } else {
                val err = (deliveryRes as DomainResult.Error).message
                val exec = CustomerFinancialScheduleExecution(
                    executionId = executionId,
                    tenantId = tenantId,
                    projectId = projectId,
                    scheduleId = schedule.scheduleId,
                    customerId = schedule.customerId,
                    reportType = schedule.reportType,
                    format = schedule.format,
                    executedAt = now,
                    status = CustomerFinancialScheduleExecutionStatus.FAILED,
                    errorMessage = err
                )
                scheduleRepository.recordExecution(exec)
                executions.add(exec)

                val nextRun = calculateNextRun(now, schedule.frequency, schedule.timezone)
                val updatedSchedule = schedule.copy(
                    lastRunAt = now,
                    lastRunStatus = "FAILED",
                    consecutiveFailures = schedule.consecutiveFailures + 1,
                    nextRunAt = nextRun,
                    updatedAt = now,
                    version = schedule.version + 1
                )
                scheduleRepository.saveSchedule(updatedSchedule)

                alertRepository?.recordAuditEvent(
                    CustomerFinancialAlertAuditEvent(
                        tenantId = tenantId,
                        projectId = projectId,
                        alertId = schedule.scheduleId,
                        eventType = CustomerFinancialAlertEventType.SCHEDULE_EXECUTION_FAILED,
                        actorId = actorId,
                        actorRole = actorRole,
                        detailsJson = "{\"error\":\"$err\"}"
                    )
                )
            }
        }

        return DomainResult.Success(executions)
    }

    override suspend fun listExecutions(
        tenantId: String,
        projectId: String,
        scheduleId: String,
        limit: Int
    ): DomainResult<List<CustomerFinancialScheduleExecution>> {
        return scheduleRepository.listExecutions(tenantId, projectId, scheduleId, limit)
    }
}
