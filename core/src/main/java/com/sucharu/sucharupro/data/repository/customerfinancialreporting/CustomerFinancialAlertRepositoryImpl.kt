package com.sucharu.sucharupro.data.repository.customerfinancialreporting

import com.sucharu.sucharupro.data.datasource.customerfinancialreporting.CustomerFinancialAlertDataSource
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.customerfinancialreporting.*
import com.sucharu.sucharupro.domain.repository.customerfinancialreporting.CustomerFinancialAlertRepository
import com.sucharu.sucharupro.domain.validation.customerfinancialreporting.CustomerFinancialAlertValidator

class CustomerFinancialAlertRepositoryImpl(
    private val dataSource: CustomerFinancialAlertDataSource
) : CustomerFinancialAlertRepository {

    override suspend fun saveAlert(alert: CustomerFinancialAlert): DomainResult<CustomerFinancialAlert> {
        val valRes = CustomerFinancialAlertValidator.validateAlert(alert)
        if (valRes is DomainResult.Error) return valRes

        return try {
            dataSource.saveAlert(alert)
            DomainResult.Success(alert)
        } catch (e: Exception) {
            DomainResult.Error(e)
        }
    }

    override suspend fun getAlertById(
        tenantId: String,
        projectId: String,
        alertId: String
    ): DomainResult<CustomerFinancialAlert?> {
        return try {
            val alert = dataSource.getAlertById(tenantId, projectId, alertId)
            DomainResult.Success(alert)
        } catch (e: Exception) {
            DomainResult.Error(e)
        }
    }

    override suspend fun getActiveAlertByDedupKey(
        tenantId: String,
        projectId: String,
        deduplicationKey: String
    ): DomainResult<CustomerFinancialAlert?> {
        return try {
            val alert = dataSource.getActiveAlertByDedupKey(tenantId, projectId, deduplicationKey)
            DomainResult.Success(alert)
        } catch (e: Exception) {
            DomainResult.Error(e)
        }
    }

    override suspend fun listAlerts(
        tenantId: String,
        projectId: String,
        customerId: String?,
        status: CustomerFinancialAlertStatus?,
        severity: CustomerFinancialAlertSeverity?,
        alertType: CustomerFinancialAlertType?,
        limit: Int,
        offset: Int
    ): DomainResult<List<CustomerFinancialAlert>> {
        return try {
            val list = dataSource.listAlerts(tenantId, projectId, customerId, status, severity, alertType, limit, offset)
            DomainResult.Success(list)
        } catch (e: Exception) {
            DomainResult.Error(e)
        }
    }

    override suspend fun countAlerts(
        tenantId: String,
        projectId: String,
        customerId: String?,
        status: CustomerFinancialAlertStatus?
    ): DomainResult<Int> {
        return try {
            val count = dataSource.countAlerts(tenantId, projectId, customerId, status)
            DomainResult.Success(count)
        } catch (e: Exception) {
            DomainResult.Error(e)
        }
    }

    override suspend fun recordAuditEvent(event: CustomerFinancialAlertAuditEvent): DomainResult<Unit> {
        return try {
            dataSource.recordAuditEvent(event)
            DomainResult.Success(Unit)
        } catch (e: Exception) {
            DomainResult.Error(e)
        }
    }

    override suspend fun listAuditEvents(
        tenantId: String,
        projectId: String,
        alertId: String
    ): DomainResult<List<CustomerFinancialAlertAuditEvent>> {
        return try {
            val events = dataSource.listAuditEvents(tenantId, projectId, alertId)
            DomainResult.Success(events)
        } catch (e: Exception) {
            DomainResult.Error(e)
        }
    }
}
