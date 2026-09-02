package com.sucharu.sucharupro.data.repository

import com.sucharu.sucharupro.data.datasource.CustomerFinancialAccountDataSource
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.customerfinancial.CustomerFinancialAccount
import com.sucharu.sucharupro.domain.model.customerfinancial.CustomerFinancialAccountAuditEvent
import com.sucharu.sucharupro.domain.model.customerfinancial.CustomerFinancialAccountStatus
import com.sucharu.sucharupro.domain.repository.customerfinancial.CustomerFinancialAccountRepository

/**
 * Production-grade repository implementation for Customer Financial Accounts (Module 14 Step 01).
 */
class CustomerFinancialAccountRepositoryImpl(
    private val dataSource: CustomerFinancialAccountDataSource
) : CustomerFinancialAccountRepository {

    override suspend fun createAccount(account: CustomerFinancialAccount): DomainResult<CustomerFinancialAccount> {
        return dataSource.insertAccount(account)
    }

    override suspend fun getAccountById(
        tenantId: String,
        projectId: String,
        financialAccountId: String
    ): DomainResult<CustomerFinancialAccount> {
        return dataSource.findAccountById(tenantId, projectId, financialAccountId)
    }

    override suspend fun getAccountByCustomerId(
        tenantId: String,
        projectId: String,
        customerId: String
    ): DomainResult<CustomerFinancialAccount> {
        return dataSource.findAccountByCustomerId(tenantId, projectId, customerId)
    }

    override suspend fun listAccounts(
        tenantId: String,
        projectId: String,
        status: CustomerFinancialAccountStatus?,
        limit: Int,
        offset: Int
    ): DomainResult<List<CustomerFinancialAccount>> {
        return dataSource.listAccounts(tenantId, projectId, status, limit, offset)
    }

    override suspend fun updateAccountStatus(
        tenantId: String,
        projectId: String,
        financialAccountId: String,
        newStatus: CustomerFinancialAccountStatus,
        reason: String?,
        actorId: String,
        expectedVersion: Long
    ): DomainResult<CustomerFinancialAccount> {
        return dataSource.updateStatus(tenantId, projectId, financialAccountId, newStatus, reason, actorId, expectedVersion)
    }

    override suspend fun updateAccountNotes(
        tenantId: String,
        projectId: String,
        financialAccountId: String,
        notes: String?,
        actorId: String,
        expectedVersion: Long
    ): DomainResult<CustomerFinancialAccount> {
        return dataSource.updateNotes(tenantId, projectId, financialAccountId, notes, actorId, expectedVersion)
    }

    override suspend fun recordAuditEvent(event: CustomerFinancialAccountAuditEvent): DomainResult<CustomerFinancialAccountAuditEvent> {
        return dataSource.insertAuditEvent(event)
    }

    override suspend fun getAuditEvents(
        tenantId: String,
        projectId: String,
        financialAccountId: String
    ): DomainResult<List<CustomerFinancialAccountAuditEvent>> {
        return dataSource.getAuditEvents(tenantId, projectId, financialAccountId)
    }
}
