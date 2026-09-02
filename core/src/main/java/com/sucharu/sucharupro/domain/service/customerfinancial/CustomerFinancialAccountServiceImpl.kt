package com.sucharu.sucharupro.domain.service.customerfinancial

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.customerfinancial.CustomerFinancialAccount
import com.sucharu.sucharupro.domain.model.customerfinancial.CustomerFinancialAccountAuditEvent
import com.sucharu.sucharupro.domain.model.customerfinancial.CustomerFinancialAccountStatus
import com.sucharu.sucharupro.domain.repository.CustomerRepository
import com.sucharu.sucharupro.domain.repository.customerfinancial.CustomerFinancialAccountRepository
import com.sucharu.sucharupro.domain.validation.customerfinancial.CustomerFinancialAccountValidator
import java.util.UUID

/**
 * Production implementation of [CustomerFinancialAccountService] (Module 14 Step 01).
 */
class CustomerFinancialAccountServiceImpl(
    private val repository: CustomerFinancialAccountRepository,
    private val customerRepository: CustomerRepository
) : CustomerFinancialAccountService {

    override suspend fun createFinancialAccount(
        tenantId: String,
        projectId: String,
        customerId: String,
        currency: String,
        notes: String?,
        actorId: String,
        actorRole: String
    ): DomainResult<CustomerFinancialAccount> {
        val validation = CustomerFinancialAccountValidator.validateCreation(tenantId, projectId, customerId, currency)
        if (validation is DomainResult.Error) return validation

        // 1. Verify that customer exists in canonical customer repository
        val customerRes = customerRepository.findCustomerById(customerId)
        if (customerRes is DomainResult.Error) {
            return DomainResult.Error(
                IllegalArgumentException("Customer '$customerId' does not exist: ${customerRes.message}")
            )
        }

        // 2. Check for duplicate account for customer
        val existingRes = repository.getAccountByCustomerId(tenantId, projectId, customerId)
        if (existingRes is DomainResult.Success) {
            return DomainResult.Error(
                IllegalStateException("CustomerFinancialAccount already exists for customer '$customerId'")
            )
        }

        val financialAccountId = "CFA-${UUID.randomUUID().toString().take(8).uppercase()}"
        val accountNumber = "ACC-${customerId.takeLast(6).uppercase()}-${System.currentTimeMillis().toString().takeLast(4)}"

        val now = System.currentTimeMillis()
        val account = CustomerFinancialAccount(
            financialAccountId = financialAccountId,
            tenantId = tenantId,
            projectId = projectId,
            customerId = customerId,
            accountNumber = accountNumber,
            currency = currency.uppercase(),
            status = CustomerFinancialAccountStatus.ACTIVE,
            notes = notes,
            createdAt = now,
            createdBy = actorId,
            updatedAt = now,
            updatedBy = actorId,
            version = 1L
        )

        val createdRes = repository.createAccount(account)
        if (createdRes is DomainResult.Success) {
            // Record audit event
            repository.recordAuditEvent(
                CustomerFinancialAccountAuditEvent(
                    auditId = "AUD-${UUID.randomUUID().toString().take(8).uppercase()}",
                    financialAccountId = financialAccountId,
                    customerId = customerId,
                    tenantId = tenantId,
                    projectId = projectId,
                    actorId = actorId,
                    actorRole = actorRole,
                    action = "ACCOUNT_CREATED",
                    previousStatus = null,
                    newStatus = CustomerFinancialAccountStatus.ACTIVE,
                    reason = "Initial creation of customer financial account",
                    occurredAt = now,
                    metadataJson = """{"accountNumber":"$accountNumber","currency":"$currency"}"""
                )
            )
        }
        return createdRes
    }

    override suspend fun getFinancialAccountById(
        tenantId: String,
        projectId: String,
        financialAccountId: String
    ): DomainResult<CustomerFinancialAccount> {
        return repository.getAccountById(tenantId, projectId, financialAccountId)
    }

    override suspend fun getFinancialAccountByCustomerId(
        tenantId: String,
        projectId: String,
        customerId: String
    ): DomainResult<CustomerFinancialAccount> {
        return repository.getAccountByCustomerId(tenantId, projectId, customerId)
    }

    override suspend fun listFinancialAccounts(
        tenantId: String,
        projectId: String,
        status: CustomerFinancialAccountStatus?,
        limit: Int,
        offset: Int
    ): DomainResult<List<CustomerFinancialAccount>> {
        return repository.listAccounts(tenantId, projectId, status, limit, offset)
    }

    override suspend fun suspendAccount(
        tenantId: String,
        projectId: String,
        financialAccountId: String,
        reason: String,
        actorId: String,
        actorRole: String,
        expectedVersion: Long
    ): DomainResult<CustomerFinancialAccount> {
        val existingRes = repository.getAccountById(tenantId, projectId, financialAccountId)
        if (existingRes is DomainResult.Error) return existingRes
        val existing = (existingRes as DomainResult.Success).data

        val valRes = CustomerFinancialAccountValidator.validateStatusTransition(
            existing,
            CustomerFinancialAccountStatus.SUSPENDED,
            reason
        )
        if (valRes is DomainResult.Error) return valRes

        val updatedRes = repository.updateAccountStatus(
            tenantId, projectId, financialAccountId,
            CustomerFinancialAccountStatus.SUSPENDED, reason, actorId, expectedVersion
        )
        if (updatedRes is DomainResult.Success) {
            repository.recordAuditEvent(
                CustomerFinancialAccountAuditEvent(
                    auditId = "AUD-${UUID.randomUUID().toString().take(8).uppercase()}",
                    financialAccountId = financialAccountId,
                    customerId = existing.customerId,
                    tenantId = tenantId,
                    projectId = projectId,
                    actorId = actorId,
                    actorRole = actorRole,
                    action = "ACCOUNT_SUSPENDED",
                    previousStatus = existing.status,
                    newStatus = CustomerFinancialAccountStatus.SUSPENDED,
                    reason = reason,
                    occurredAt = System.currentTimeMillis()
                )
            )
        }
        return updatedRes
    }

    override suspend fun reactivateAccount(
        tenantId: String,
        projectId: String,
        financialAccountId: String,
        reason: String?,
        actorId: String,
        actorRole: String,
        expectedVersion: Long
    ): DomainResult<CustomerFinancialAccount> {
        val existingRes = repository.getAccountById(tenantId, projectId, financialAccountId)
        if (existingRes is DomainResult.Error) return existingRes
        val existing = (existingRes as DomainResult.Success).data

        val valRes = CustomerFinancialAccountValidator.validateStatusTransition(
            existing,
            CustomerFinancialAccountStatus.ACTIVE,
            reason
        )
        if (valRes is DomainResult.Error) return valRes

        val updatedRes = repository.updateAccountStatus(
            tenantId, projectId, financialAccountId,
            CustomerFinancialAccountStatus.ACTIVE, reason, actorId, expectedVersion
        )
        if (updatedRes is DomainResult.Success) {
            repository.recordAuditEvent(
                CustomerFinancialAccountAuditEvent(
                    auditId = "AUD-${UUID.randomUUID().toString().take(8).uppercase()}",
                    financialAccountId = financialAccountId,
                    customerId = existing.customerId,
                    tenantId = tenantId,
                    projectId = projectId,
                    actorId = actorId,
                    actorRole = actorRole,
                    action = "ACCOUNT_REACTIVATED",
                    previousStatus = existing.status,
                    newStatus = CustomerFinancialAccountStatus.ACTIVE,
                    reason = reason,
                    occurredAt = System.currentTimeMillis()
                )
            )
        }
        return updatedRes
    }

    override suspend fun closeAccount(
        tenantId: String,
        projectId: String,
        financialAccountId: String,
        reason: String,
        actorId: String,
        actorRole: String,
        expectedVersion: Long
    ): DomainResult<CustomerFinancialAccount> {
        val existingRes = repository.getAccountById(tenantId, projectId, financialAccountId)
        if (existingRes is DomainResult.Error) return existingRes
        val existing = (existingRes as DomainResult.Success).data

        val valRes = CustomerFinancialAccountValidator.validateStatusTransition(
            existing,
            CustomerFinancialAccountStatus.CLOSED,
            reason
        )
        if (valRes is DomainResult.Error) return valRes

        val updatedRes = repository.updateAccountStatus(
            tenantId, projectId, financialAccountId,
            CustomerFinancialAccountStatus.CLOSED, reason, actorId, expectedVersion
        )
        if (updatedRes is DomainResult.Success) {
            repository.recordAuditEvent(
                CustomerFinancialAccountAuditEvent(
                    auditId = "AUD-${UUID.randomUUID().toString().take(8).uppercase()}",
                    financialAccountId = financialAccountId,
                    customerId = existing.customerId,
                    tenantId = tenantId,
                    projectId = projectId,
                    actorId = actorId,
                    actorRole = actorRole,
                    action = "ACCOUNT_CLOSED",
                    previousStatus = existing.status,
                    newStatus = CustomerFinancialAccountStatus.CLOSED,
                    reason = reason,
                    occurredAt = System.currentTimeMillis()
                )
            )
        }
        return updatedRes
    }

    override suspend fun updateAccountNotes(
        tenantId: String,
        projectId: String,
        financialAccountId: String,
        notes: String?,
        actorId: String,
        actorRole: String,
        expectedVersion: Long
    ): DomainResult<CustomerFinancialAccount> {
        val existingRes = repository.getAccountById(tenantId, projectId, financialAccountId)
        if (existingRes is DomainResult.Error) return existingRes
        val existing = (existingRes as DomainResult.Success).data

        val updatedRes = repository.updateAccountNotes(
            tenantId, projectId, financialAccountId,
            notes, actorId, expectedVersion
        )
        if (updatedRes is DomainResult.Success) {
            repository.recordAuditEvent(
                CustomerFinancialAccountAuditEvent(
                    auditId = "AUD-${UUID.randomUUID().toString().take(8).uppercase()}",
                    financialAccountId = financialAccountId,
                    customerId = existing.customerId,
                    tenantId = tenantId,
                    projectId = projectId,
                    actorId = actorId,
                    actorRole = actorRole,
                    action = "NOTES_UPDATED",
                    previousStatus = existing.status,
                    newStatus = existing.status,
                    reason = "Account notes updated",
                    occurredAt = System.currentTimeMillis()
                )
            )
        }
        return updatedRes
    }

    override suspend fun getAuditHistory(
        tenantId: String,
        projectId: String,
        financialAccountId: String
    ): DomainResult<List<CustomerFinancialAccountAuditEvent>> {
        return repository.getAuditEvents(tenantId, projectId, financialAccountId)
    }
}
