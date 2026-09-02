package com.sucharu.sucharupro.data.datasource.customerfinancialreporting

import com.sucharu.sucharupro.domain.model.customerfinancialreporting.CustomerFinancialAlert
import com.sucharu.sucharupro.domain.model.customerfinancialreporting.CustomerFinancialAlertAuditEvent
import com.sucharu.sucharupro.domain.model.customerfinancialreporting.CustomerFinancialAlertSeverity
import com.sucharu.sucharupro.domain.model.customerfinancialreporting.CustomerFinancialAlertStatus
import com.sucharu.sucharupro.domain.model.customerfinancialreporting.CustomerFinancialAlertType
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

class FakeCustomerFinancialAlertDataSource : CustomerFinancialAlertDataSource {

    private val alerts = ConcurrentHashMap<String, CustomerFinancialAlert>()
    private val auditEvents = ConcurrentHashMap<String, CopyOnWriteArrayList<CustomerFinancialAlertAuditEvent>>()

    override suspend fun saveAlert(alert: CustomerFinancialAlert) {
        alerts[alert.alertId] = alert
    }

    override suspend fun getAlertById(tenantId: String, projectId: String, alertId: String): CustomerFinancialAlert? {
        val alert = alerts[alertId] ?: return null
        return if (alert.tenantId == tenantId && alert.projectId == projectId) alert else null
    }

    override suspend fun getActiveAlertByDedupKey(
        tenantId: String,
        projectId: String,
        deduplicationKey: String
    ): CustomerFinancialAlert? {
        return alerts.values.firstOrNull {
            it.tenantId == tenantId &&
            it.projectId == projectId &&
            it.deduplicationKey == deduplicationKey &&
            it.status in setOf(CustomerFinancialAlertStatus.OPEN, CustomerFinancialAlertStatus.ACKNOWLEDGED)
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
    ): List<CustomerFinancialAlert> {
        return alerts.values
            .filter { it.tenantId == tenantId && it.projectId == projectId }
            .filter { customerId == null || it.customerId == customerId }
            .filter { status == null || it.status == status }
            .filter { severity == null || it.severity == severity }
            .filter { alertType == null || it.alertType == alertType }
            .sortedByDescending { it.detectedAt }
            .drop(offset)
            .take(limit)
    }

    override suspend fun countAlerts(
        tenantId: String,
        projectId: String,
        customerId: String?,
        status: CustomerFinancialAlertStatus?
    ): Int {
        return alerts.values
            .filter { it.tenantId == tenantId && it.projectId == projectId }
            .filter { customerId == null || it.customerId == customerId }
            .count { status == null || it.status == status }
    }

    override suspend fun recordAuditEvent(event: CustomerFinancialAlertAuditEvent) {
        auditEvents.computeIfAbsent(event.alertId) { CopyOnWriteArrayList() }.add(event)
    }

    override suspend fun listAuditEvents(
        tenantId: String,
        projectId: String,
        alertId: String
    ): List<CustomerFinancialAlertAuditEvent> {
        return auditEvents[alertId]?.filter {
            it.tenantId == tenantId && it.projectId == projectId
        }?.sortedBy { it.timestamp } ?: emptyList()
    }
}
