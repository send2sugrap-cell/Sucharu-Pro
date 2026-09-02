package com.sucharu.sucharupro.domain.service.customerfinancialdashboard

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.customerfinancialdashboard.*

interface CustomerFinancialDashboardService {

    suspend fun getCustomerFinancialDashboard(
        tenantId: String,
        projectId: String,
        customerId: String,
        asOfDate: Long = System.currentTimeMillis()
    ): DomainResult<CustomerFinancialDashboard>

    suspend fun getFinancialWarnings(
        tenantId: String,
        projectId: String,
        customerId: String,
        asOfDate: Long = System.currentTimeMillis()
    ): DomainResult<List<CustomerFinancialWarning>>

    suspend fun getRecommendedFinancialActions(
        tenantId: String,
        projectId: String,
        customerId: String,
        asOfDate: Long = System.currentTimeMillis()
    ): DomainResult<List<CustomerFinancialAction>>

    suspend fun getReceivableAgingSummary(
        tenantId: String,
        projectId: String,
        customerId: String,
        asOfDate: Long = System.currentTimeMillis()
    ): DomainResult<CustomerReceivableAgingSummary>

    suspend fun getRecentFinancialActivity(
        tenantId: String,
        projectId: String,
        customerId: String,
        limit: Int = 50
    ): DomainResult<List<CustomerFinancialActivityItem>>
}
