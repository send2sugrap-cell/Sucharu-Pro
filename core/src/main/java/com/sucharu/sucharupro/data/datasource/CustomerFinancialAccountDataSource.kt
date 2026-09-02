package com.sucharu.sucharupro.data.datasource

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.customerfinancial.CustomerFinancialAccount
import com.sucharu.sucharupro.domain.model.customerfinancial.CustomerFinancialAccountAuditEvent
import com.sucharu.sucharupro.domain.model.customerfinancial.CustomerFinancialAccountStatus

/**
 * Data source contract for Customer Financial Accounts.
 */
interface CustomerFinancialAccountDataSource {

    suspend fun insertAccount(account: CustomerFinancialAccount): DomainResult<CustomerFinancialAccount>

    suspend fun findAccountById(
        tenantId: String,
        projectId: String,
        financialAccountId: String
    ): DomainResult<CustomerFinancialAccount>

    suspend fun findAccountByCustomerId(
        tenantId: String,
        projectId: String,
        customerId: String
    ): DomainResult<CustomerFinancialAccount>

    suspend fun listAccounts(
        tenantId: String,
        projectId: String,
        status: CustomerFinancialAccountStatus?,
        limit: Int,
        offset: Int
    ): DomainResult<List<CustomerFinancialAccount>>

    suspend fun updateStatus(
        tenantId: String,
        projectId: String,
        financialAccountId: String,
        newStatus: CustomerFinancialAccountStatus,
        reason: String?,
        actorId: String,
        expectedVersion: Long
    ): DomainResult<CustomerFinancialAccount>

    suspend fun updateNotes(
        tenantId: String,
        projectId: String,
        financialAccountId: String,
        notes: String?,
        actorId: String,
        expectedVersion: Long
    ): DomainResult<CustomerFinancialAccount>

    suspend fun insertAuditEvent(event: CustomerFinancialAccountAuditEvent): DomainResult<CustomerFinancialAccountAuditEvent>

    suspend fun getAuditEvents(
        tenantId: String,
        projectId: String,
        financialAccountId: String
    ): DomainResult<List<CustomerFinancialAccountAuditEvent>>
}
