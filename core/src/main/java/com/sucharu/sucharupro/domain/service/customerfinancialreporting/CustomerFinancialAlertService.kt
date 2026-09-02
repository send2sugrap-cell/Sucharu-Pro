package com.sucharu.sucharupro.domain.service.customerfinancialreporting

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.customerfinancialreporting.*

interface CustomerFinancialAlertService {

    suspend fun evaluateCustomerFinancialAlerts(
        tenantId: String,
        projectId: String,
        customerId: String,
        actorId: String,
        actorRole: String,
        asOfDate: Long = System.currentTimeMillis()
    ): DomainResult<List<CustomerFinancialAlert>>

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

    suspend fun getAlertSummary(
        tenantId: String,
        projectId: String,
        customerId: String? = null
    ): DomainResult<CustomerFinancialAlertSummary>

    suspend fun getAlertById(
        tenantId: String,
        projectId: String,
        alertId: String
    ): DomainResult<CustomerFinancialAlert>

    suspend fun acknowledgeAlert(
        tenantId: String,
        projectId: String,
        alertId: String,
        actorId: String,
        actorRole: String
    ): DomainResult<CustomerFinancialAlert>

    suspend fun resolveAlert(
        tenantId: String,
        projectId: String,
        alertId: String,
        reason: String,
        actorId: String,
        actorRole: String
    ): DomainResult<CustomerFinancialAlert>

    suspend fun dismissAlert(
        tenantId: String,
        projectId: String,
        alertId: String,
        reason: String,
        actorId: String,
        actorRole: String
    ): DomainResult<CustomerFinancialAlert>

    suspend fun getAlertAuditHistory(
        tenantId: String,
        projectId: String,
        alertId: String
    ): DomainResult<List<CustomerFinancialAlertAuditEvent>>
}
