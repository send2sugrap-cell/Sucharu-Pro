package com.sucharu.sucharupro.domain.repository

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.common.Money
import com.sucharu.sucharupro.domain.model.finance.CustomerDueSummary
import com.sucharu.sucharupro.domain.model.finance.CustomerReceivable
import com.sucharu.sucharupro.domain.model.finance.CustomerReceivableActivityEvent
import com.sucharu.sucharupro.domain.model.finance.FinancialReferenceType
import com.sucharu.sucharupro.domain.model.user.UserRole
import kotlinx.coroutines.flow.Flow

/**
 * Domain repository contract for Customer Receivable & Due Management (Module 09 Step 02).
 */
interface CustomerReceivableRepository {

    suspend fun createReceivable(
        projectId: String,
        customerId: String,
        referenceType: FinancialReferenceType,
        referenceId: String,
        financialTransactionId: String? = null,
        originalAmount: Money,
        currency: String = "BDT",
        dueDate: Long,
        description: String,
        notes: String? = null,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<CustomerReceivable>

    suspend fun updateReceivable(
        receivableId: String,
        dueDate: Long? = null,
        description: String? = null,
        notes: String? = null,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<CustomerReceivable>

    suspend fun recordSettlement(
        receivableId: String,
        settlementAmount: Money,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<CustomerReceivable>

    suspend fun cancelReceivable(
        receivableId: String,
        cancellationReason: String,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<CustomerReceivable>

    suspend fun getReceivableById(
        receivableId: String,
        callerRole: UserRole,
        authenticatedCustomerId: String? = null
    ): DomainResult<CustomerReceivable>

    suspend fun getReceivablesByReference(
        projectId: String,
        referenceId: String,
        callerRole: UserRole
    ): DomainResult<List<CustomerReceivable>>

    fun observeReceivables(
        projectId: String,
        callerRole: UserRole
    ): Flow<List<CustomerReceivable>>

    fun observeCustomerReceivables(
        projectId: String,
        customerId: String,
        callerRole: UserRole,
        authenticatedCustomerId: String? = null
    ): Flow<List<CustomerReceivable>>

    suspend fun getCustomerDueSummary(
        projectId: String,
        customerId: String? = null,
        callerRole: UserRole,
        authenticatedCustomerId: String? = null
    ): DomainResult<CustomerDueSummary>

    suspend fun getActivityEvents(
        receivableId: String,
        callerRole: UserRole
    ): DomainResult<List<CustomerReceivableActivityEvent>>
}
