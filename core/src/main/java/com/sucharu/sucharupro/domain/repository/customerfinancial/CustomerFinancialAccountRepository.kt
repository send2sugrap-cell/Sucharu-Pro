package com.sucharu.sucharupro.domain.repository.customerfinancial

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.customerfinancial.CustomerFinancialAccount
import com.sucharu.sucharupro.domain.model.customerfinancial.CustomerFinancialAccountAuditEvent
import com.sucharu.sucharupro.domain.model.customerfinancial.CustomerFinancialAccountStatus

/**
 * Repository interface contract for Customer Financial Accounts (Module 14 Step 01).
 */
interface CustomerFinancialAccountRepository {

    /**
     * Persists a new customer financial account aggregate.
     */
    suspend fun createAccount(account: CustomerFinancialAccount): DomainResult<CustomerFinancialAccount>

    /**
     * Retrieves an account by its unique financial account ID.
     */
    suspend fun getAccountById(
        tenantId: String,
        projectId: String,
        financialAccountId: String
    ): DomainResult<CustomerFinancialAccount>

    /**
     * Retrieves the single canonical financial account for a customer within tenant/project scope.
     */
    suspend fun getAccountByCustomerId(
        tenantId: String,
        projectId: String,
        customerId: String
    ): DomainResult<CustomerFinancialAccount>

    /**
     * Lists accounts matching criteria with pagination.
     */
    suspend fun listAccounts(
        tenantId: String,
        projectId: String,
        status: CustomerFinancialAccountStatus? = null,
        limit: Int = 50,
        offset: Int = 0
    ): DomainResult<List<CustomerFinancialAccount>>

    /**
     * Updates account lifecycle status with optimistic concurrency control.
     */
    suspend fun updateAccountStatus(
        tenantId: String,
        projectId: String,
        financialAccountId: String,
        newStatus: CustomerFinancialAccountStatus,
        reason: String?,
        actorId: String,
        expectedVersion: Long
    ): DomainResult<CustomerFinancialAccount>

    /**
     * Updates account internal notes with optimistic concurrency control.
     */
    suspend fun updateAccountNotes(
        tenantId: String,
        projectId: String,
        financialAccountId: String,
        notes: String?,
        actorId: String,
        expectedVersion: Long
    ): DomainResult<CustomerFinancialAccount>

    /**
     * Records an immutable audit log entry.
     */
    suspend fun recordAuditEvent(event: CustomerFinancialAccountAuditEvent): DomainResult<CustomerFinancialAccountAuditEvent>

    /**
     * Retrieves audit history for an account in reverse chronological order.
     */
    suspend fun getAuditEvents(
        tenantId: String,
        projectId: String,
        financialAccountId: String
    ): DomainResult<List<CustomerFinancialAccountAuditEvent>>
}
