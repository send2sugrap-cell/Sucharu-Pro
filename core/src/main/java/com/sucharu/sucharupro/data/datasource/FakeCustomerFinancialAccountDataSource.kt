package com.sucharu.sucharupro.data.datasource

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.customerfinancial.CustomerFinancialAccount
import com.sucharu.sucharupro.domain.model.customerfinancial.CustomerFinancialAccountAuditEvent
import com.sucharu.sucharupro.domain.model.customerfinancial.CustomerFinancialAccountStatus
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Thread-safe In-Memory Fake DataSource for testing Customer Financial Accounts.
 */
class FakeCustomerFinancialAccountDataSource : CustomerFinancialAccountDataSource {

    private val mutex = Mutex()
    private val accounts = mutableMapOf<String, CustomerFinancialAccount>()
    private val auditEvents = mutableListOf<CustomerFinancialAccountAuditEvent>()

    override suspend fun insertAccount(account: CustomerFinancialAccount): DomainResult<CustomerFinancialAccount> = mutex.withLock {
        // Enforce uniqueness of (tenantId, projectId, customerId)
        val duplicate = accounts.values.find {
            it.tenantId == account.tenantId && it.projectId == account.projectId && it.customerId == account.customerId
        }
        if (duplicate != null) {
            return DomainResult.Error(
                IllegalStateException("CustomerFinancialAccount already exists for customer '${account.customerId}' in project '${account.projectId}'")
            )
        }
        // Enforce uniqueness of (tenantId, accountNumber)
        val dupNumber = accounts.values.find {
            it.tenantId == account.tenantId && it.accountNumber == account.accountNumber
        }
        if (dupNumber != null) {
            return DomainResult.Error(
                IllegalStateException("CustomerFinancialAccount with account number '${account.accountNumber}' already exists")
            )
        }
        accounts[account.financialAccountId] = account
        return DomainResult.Success(account)
    }

    override suspend fun findAccountById(
        tenantId: String,
        projectId: String,
        financialAccountId: String
    ): DomainResult<CustomerFinancialAccount> = mutex.withLock {
        val account = accounts[financialAccountId]
        if (account != null && account.tenantId == tenantId && account.projectId == projectId) {
            DomainResult.Success(account)
        } else {
            DomainResult.Error(NoSuchElementException("CustomerFinancialAccount '$financialAccountId' not found"))
        }
    }

    override suspend fun findAccountByCustomerId(
        tenantId: String,
        projectId: String,
        customerId: String
    ): DomainResult<CustomerFinancialAccount> = mutex.withLock {
        val account = accounts.values.find {
            it.tenantId == tenantId && it.projectId == projectId && it.customerId == customerId
        }
        if (account != null) {
            DomainResult.Success(account)
        } else {
            DomainResult.Error(NoSuchElementException("CustomerFinancialAccount for customer '$customerId' not found"))
        }
    }

    override suspend fun listAccounts(
        tenantId: String,
        projectId: String,
        status: CustomerFinancialAccountStatus?,
        limit: Int,
        offset: Int
    ): DomainResult<List<CustomerFinancialAccount>> = mutex.withLock {
        val filtered = accounts.values.filter {
            it.tenantId == tenantId && it.projectId == projectId && (status == null || it.status == status)
        }.sortedByDescending { it.createdAt }.drop(offset).take(limit)
        DomainResult.Success(filtered)
    }

    override suspend fun updateStatus(
        tenantId: String,
        projectId: String,
        financialAccountId: String,
        newStatus: CustomerFinancialAccountStatus,
        reason: String?,
        actorId: String,
        expectedVersion: Long
    ): DomainResult<CustomerFinancialAccount> = mutex.withLock {
        val existing = accounts[financialAccountId]
            ?: return DomainResult.Error(NoSuchElementException("CustomerFinancialAccount '$financialAccountId' not found"))
        if (existing.tenantId != tenantId || existing.projectId != projectId) {
            return DomainResult.Error(NoSuchElementException("CustomerFinancialAccount '$financialAccountId' not found in scope"))
        }
        if (existing.version != expectedVersion) {
            return DomainResult.Error(
                IllegalStateException("Optimistic lock conflict for account '$financialAccountId'. Expected version $expectedVersion, found ${existing.version}")
            )
        }
        val updated = existing.copy(
            status = newStatus,
            suspensionReason = if (newStatus == CustomerFinancialAccountStatus.SUSPENDED) reason else existing.suspensionReason,
            closedReason = if (newStatus == CustomerFinancialAccountStatus.CLOSED) reason else existing.closedReason,
            updatedAt = System.currentTimeMillis(),
            updatedBy = actorId,
            version = existing.version + 1
        )
        accounts[financialAccountId] = updated
        DomainResult.Success(updated)
    }

    override suspend fun updateNotes(
        tenantId: String,
        projectId: String,
        financialAccountId: String,
        notes: String?,
        actorId: String,
        expectedVersion: Long
    ): DomainResult<CustomerFinancialAccount> = mutex.withLock {
        val existing = accounts[financialAccountId]
            ?: return DomainResult.Error(NoSuchElementException("CustomerFinancialAccount '$financialAccountId' not found"))
        if (existing.tenantId != tenantId || existing.projectId != projectId) {
            return DomainResult.Error(NoSuchElementException("CustomerFinancialAccount '$financialAccountId' not found in scope"))
        }
        if (existing.version != expectedVersion) {
            return DomainResult.Error(
                IllegalStateException("Optimistic lock conflict for account '$financialAccountId'. Expected version $expectedVersion, found ${existing.version}")
            )
        }
        val updated = existing.copy(
            notes = notes,
            updatedAt = System.currentTimeMillis(),
            updatedBy = actorId,
            version = existing.version + 1
        )
        accounts[financialAccountId] = updated
        DomainResult.Success(updated)
    }

    override suspend fun insertAuditEvent(event: CustomerFinancialAccountAuditEvent): DomainResult<CustomerFinancialAccountAuditEvent> = mutex.withLock {
        auditEvents.add(event)
        DomainResult.Success(event)
    }

    override suspend fun getAuditEvents(
        tenantId: String,
        projectId: String,
        financialAccountId: String
    ): DomainResult<List<CustomerFinancialAccountAuditEvent>> = mutex.withLock {
        val filtered = auditEvents.filter {
            it.tenantId == tenantId && it.projectId == projectId && it.financialAccountId == financialAccountId
        }.sortedByDescending { it.occurredAt }
        DomainResult.Success(filtered)
    }
}
