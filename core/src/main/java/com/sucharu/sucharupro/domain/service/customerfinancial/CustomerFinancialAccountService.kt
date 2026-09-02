package com.sucharu.sucharupro.domain.service.customerfinancial

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.customerfinancial.CustomerFinancialAccount
import com.sucharu.sucharupro.domain.model.customerfinancial.CustomerFinancialAccountAuditEvent
import com.sucharu.sucharupro.domain.model.customerfinancial.CustomerFinancialAccountStatus

/**
 * Domain Service for Customer Financial Account operations (Module 14 Step 01).
 */
interface CustomerFinancialAccountService {

    /**
     * Creates a single canonical financial account for a customer.
     * Validates customer existence, tenant/project boundary, and absence of existing accounts.
     */
    suspend fun createFinancialAccount(
        tenantId: String,
        projectId: String,
        customerId: String,
        currency: String = "BDT",
        notes: String? = null,
        actorId: String,
        actorRole: String
    ): DomainResult<CustomerFinancialAccount>

    /**
     * Looks up an account by its unique financial account ID.
     */
    suspend fun getFinancialAccountById(
        tenantId: String,
        projectId: String,
        financialAccountId: String
    ): DomainResult<CustomerFinancialAccount>

    /**
     * Looks up the account associated with a specific customer.
     */
    suspend fun getFinancialAccountByCustomerId(
        tenantId: String,
        projectId: String,
        customerId: String
    ): DomainResult<CustomerFinancialAccount>

    /**
     * Lists accounts matching criteria with pagination.
     */
    suspend fun listFinancialAccounts(
        tenantId: String,
        projectId: String,
        status: CustomerFinancialAccountStatus? = null,
        limit: Int = 50,
        offset: Int = 0
    ): DomainResult<List<CustomerFinancialAccount>>

    /**
     * Suspends an active financial account with mandatory rationale.
     */
    suspend fun suspendAccount(
        tenantId: String,
        projectId: String,
        financialAccountId: String,
        reason: String,
        actorId: String,
        actorRole: String,
        expectedVersion: Long
    ): DomainResult<CustomerFinancialAccount>

    /**
     * Reactivates a suspended financial account.
     */
    suspend fun reactivateAccount(
        tenantId: String,
        projectId: String,
        financialAccountId: String,
        reason: String? = null,
        actorId: String,
        actorRole: String,
        expectedVersion: Long
    ): DomainResult<CustomerFinancialAccount>

    /**
     * Closes an account permanently with mandatory rationale.
     */
    suspend fun closeAccount(
        tenantId: String,
        projectId: String,
        financialAccountId: String,
        reason: String,
        actorId: String,
        actorRole: String,
        expectedVersion: Long
    ): DomainResult<CustomerFinancialAccount>

    /**
     * Updates account internal notes.
     */
    suspend fun updateAccountNotes(
        tenantId: String,
        projectId: String,
        financialAccountId: String,
        notes: String?,
        actorId: String,
        actorRole: String,
        expectedVersion: Long
    ): DomainResult<CustomerFinancialAccount>

    /**
     * Retrieves audit history for an account.
     */
    suspend fun getAuditHistory(
        tenantId: String,
        projectId: String,
        financialAccountId: String
    ): DomainResult<List<CustomerFinancialAccountAuditEvent>>
}
