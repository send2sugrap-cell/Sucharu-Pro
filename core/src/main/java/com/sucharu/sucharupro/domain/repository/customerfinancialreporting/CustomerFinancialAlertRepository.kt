package com.sucharu.sucharupro.domain.repository.customerfinancialreporting

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.customerfinancialreporting.*

interface CustomerFinancialAlertRepository {
    suspend fun saveAlert(alert: CustomerFinancialAlert): DomainResult<CustomerFinancialAlert>
    suspend fun getAlertById(tenantId: String, projectId: String, alertId: String): DomainResult<CustomerFinancialAlert?>
    suspend fun getActiveAlertByDedupKey(tenantId: String, projectId: String, deduplicationKey: String): DomainResult<CustomerFinancialAlert?>
    suspend fun listAlerts(
        tenantId: String,
        projectId: String,
        customerId: String? = null,
        status: CustomerFinancialAlertStatus? = null,
        severity: CustomerFinancialAlertSeverity? = null,
        alertType: CustomerFinancialAlertType? = null,
        limit: Int = 100,
        offset: Int = 0
    ): DomainResult<List<CustomerFinancialAlert>>
    suspend fun countAlerts(
        tenantId: String,
        projectId: String,
        customerId: String? = null,
        status: CustomerFinancialAlertStatus? = null
    ): DomainResult<Int>
    suspend fun recordAuditEvent(event: CustomerFinancialAlertAuditEvent): DomainResult<Unit>
    suspend fun listAuditEvents(tenantId: String, projectId: String, alertId: String): DomainResult<List<CustomerFinancialAlertAuditEvent>>
}
