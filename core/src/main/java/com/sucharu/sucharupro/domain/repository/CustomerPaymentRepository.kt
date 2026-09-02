package com.sucharu.sucharupro.domain.repository

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.common.Money
import com.sucharu.sucharupro.domain.model.finance.CustomerPayment
import com.sucharu.sucharupro.domain.model.finance.CustomerPaymentActivityEvent
import com.sucharu.sucharupro.domain.model.finance.CustomerPaymentMethod
import com.sucharu.sucharupro.domain.model.finance.CustomerPaymentReceipt
import com.sucharu.sucharupro.domain.model.user.UserRole
import kotlinx.coroutines.flow.Flow

/**
 * Domain repository contract for Customer Payment & Receipt Management (Module 09 Step 03).
 */
interface CustomerPaymentRepository {

    suspend fun createPayment(
        projectId: String,
        customerId: String,
        receivableId: String,
        amount: Money,
        currency: String = "BDT",
        paymentMethod: CustomerPaymentMethod,
        paymentReference: String? = null,
        paymentDate: Long = System.currentTimeMillis(),
        idempotencyKey: String? = null,
        notes: String? = null,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<CustomerPayment>

    suspend fun updateDraftPayment(
        paymentId: String,
        amount: Money? = null,
        paymentMethod: CustomerPaymentMethod? = null,
        paymentReference: String? = null,
        paymentDate: Long? = null,
        notes: String? = null,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<CustomerPayment>

    suspend fun submitPayment(
        paymentId: String,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<CustomerPayment>

    suspend fun postPayment(
        paymentId: String,
        accountHead: String? = null,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<CustomerPayment>

    suspend fun rejectPayment(
        paymentId: String,
        rejectionReason: String,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<CustomerPayment>

    suspend fun cancelPayment(
        paymentId: String,
        cancellationReason: String,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<CustomerPayment>

    suspend fun getPaymentById(
        paymentId: String,
        callerRole: UserRole,
        authenticatedCustomerId: String? = null
    ): DomainResult<CustomerPayment>

    suspend fun getPaymentByIdempotencyKey(
        projectId: String,
        idempotencyKey: String,
        callerRole: UserRole
    ): DomainResult<CustomerPayment?>

    suspend fun getReceiptById(
        receiptId: String,
        callerRole: UserRole,
        authenticatedCustomerId: String? = null
    ): DomainResult<CustomerPaymentReceipt>

    suspend fun getReceiptByPaymentId(
        paymentId: String,
        callerRole: UserRole,
        authenticatedCustomerId: String? = null
    ): DomainResult<CustomerPaymentReceipt>

    fun observePayments(
        projectId: String,
        callerRole: UserRole
    ): Flow<List<CustomerPayment>>

    fun observeCustomerPayments(
        projectId: String,
        customerId: String,
        callerRole: UserRole,
        authenticatedCustomerId: String? = null
    ): Flow<List<CustomerPayment>>

    fun observeCustomerReceipts(
        projectId: String,
        customerId: String,
        callerRole: UserRole,
        authenticatedCustomerId: String? = null
    ): Flow<List<CustomerPaymentReceipt>>

    suspend fun getActivityEvents(
        paymentId: String,
        callerRole: UserRole
    ): DomainResult<List<CustomerPaymentActivityEvent>>
}
