package com.sucharu.sucharupro.data.datasource.customerfinancialreporting

import com.sucharu.sucharupro.domain.model.customerfinancialreporting.CustomerFinancialAlert
import com.sucharu.sucharupro.domain.model.customerfinancialreporting.CustomerFinancialAlertAuditEvent
import com.sucharu.sucharupro.domain.model.customerfinancialreporting.CustomerFinancialAlertSeverity
import com.sucharu.sucharupro.domain.model.customerfinancialreporting.CustomerFinancialAlertStatus
import com.sucharu.sucharupro.domain.model.customerfinancialreporting.CustomerFinancialAlertType

interface CustomerFinancialAlertDataSource {
    suspend fun saveAlert(alert: CustomerFinancialAlert)
    suspend fun getAlertById(tenantId: String, projectId: String, alertId: String): CustomerFinancialAlert?
    suspend fun getActiveAlertByDedupKey(tenantId: String, projectId: String, deduplicationKey: String): CustomerFinancialAlert?
    suspend fun listAlerts(
        tenantId: String,
        projectId: String,
        customerId: String? = null,
        status: CustomerFinancialAlertStatus? = null,
        severity: CustomerFinancialAlertSeverity? = null,
        alertType: CustomerFinancialAlertType? = null,
        limit: Int = 100,
        offset: Int = 0
    ): List<CustomerFinancialAlert>
    suspend fun countAlerts(
        tenantId: String,
        projectId: String,
        customerId: String? = null,
        status: CustomerFinancialAlertStatus? = null
    ): Int
    suspend fun recordAuditEvent(event: CustomerFinancialAlertAuditEvent)
    suspend fun listAuditEvents(tenantId: String, projectId: String, alertId: String): List<CustomerFinancialAlertAuditEvent>
}
