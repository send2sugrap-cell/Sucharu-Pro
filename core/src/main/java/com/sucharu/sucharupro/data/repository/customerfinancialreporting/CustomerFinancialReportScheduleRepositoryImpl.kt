package com.sucharu.sucharupro.data.repository.customerfinancialreporting

import com.sucharu.sucharupro.data.datasource.customerfinancialreporting.CustomerFinancialReportScheduleDataSource
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.customerfinancialreporting.*
import com.sucharu.sucharupro.domain.repository.customerfinancialreporting.CustomerFinancialReportScheduleRepository
import com.sucharu.sucharupro.domain.validation.customerfinancialreporting.CustomerFinancialAlertValidator

class CustomerFinancialReportScheduleRepositoryImpl(
    private val dataSource: CustomerFinancialReportScheduleDataSource
) : CustomerFinancialReportScheduleRepository {

    override suspend fun saveSchedule(schedule: CustomerFinancialReportSchedule): DomainResult<CustomerFinancialReportSchedule> {
        val valRes = CustomerFinancialAlertValidator.validateSchedule(schedule)
        if (valRes is DomainResult.Error) return valRes

        return try {
            dataSource.saveSchedule(schedule)
            DomainResult.Success(schedule)
        } catch (e: Exception) {
            DomainResult.Error(e)
        }
    }

    override suspend fun getScheduleById(
        tenantId: String,
        projectId: String,
        scheduleId: String
    ): DomainResult<CustomerFinancialReportSchedule?> {
        return try {
            val schedule = dataSource.getScheduleById(tenantId, projectId, scheduleId)
            DomainResult.Success(schedule)
        } catch (e: Exception) {
            DomainResult.Error(e)
        }
    }

    override suspend fun listSchedules(
        tenantId: String,
        projectId: String,
        customerId: String?,
        status: CustomerFinancialReportScheduleStatus?
    ): DomainResult<List<CustomerFinancialReportSchedule>> {
        return try {
            val list = dataSource.listSchedules(tenantId, projectId, customerId, status)
            DomainResult.Success(list)
        } catch (e: Exception) {
            DomainResult.Error(e)
        }
    }

    override suspend fun getDueSchedules(
        tenantId: String,
        projectId: String,
        asOfTimestamp: Long,
        limit: Int
    ): DomainResult<List<CustomerFinancialReportSchedule>> {
        return try {
            val list = dataSource.getDueSchedules(tenantId, projectId, asOfTimestamp, limit)
            DomainResult.Success(list)
        } catch (e: Exception) {
            DomainResult.Error(e)
        }
    }

    override suspend fun recordExecution(execution: CustomerFinancialScheduleExecution): DomainResult<Unit> {
        return try {
            dataSource.recordExecution(execution)
            DomainResult.Success(Unit)
        } catch (e: Exception) {
            DomainResult.Error(e)
        }
    }

    override suspend fun listExecutions(
        tenantId: String,
        projectId: String,
        scheduleId: String,
        limit: Int
    ): DomainResult<List<CustomerFinancialScheduleExecution>> {
        return try {
            val list = dataSource.listExecutions(tenantId, projectId, scheduleId, limit)
            DomainResult.Success(list)
        } catch (e: Exception) {
            DomainResult.Error(e)
        }
    }
}
