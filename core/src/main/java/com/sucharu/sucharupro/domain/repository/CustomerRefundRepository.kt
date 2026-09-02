package com.sucharu.sucharupro.domain.repository

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.common.Money
import com.sucharu.sucharupro.domain.model.finance.CustomerRefund
import com.sucharu.sucharupro.domain.model.finance.CustomerRefundMethod
import com.sucharu.sucharupro.domain.model.finance.FinancialAdjustmentActivityEvent
import com.sucharu.sucharupro.domain.model.user.UserRole
import kotlinx.coroutines.flow.Flow

/**
 * Domain repository contract for Customer Refund management (Module 09 Step 07).
 */
interface CustomerRefundRepository {

    suspend fun createRefund(
        projectId: String,
        customerId: String,
        amount: Money,
        currency: String = "BDT",
        refundMethod: CustomerRefundMethod,
        refundReference: String? = null,
        reason: String,
        adjustmentId: String? = null,
        sourcePaymentId: String? = null,
        receivableId: String? = null,
        idempotencyKey: String? = null,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<CustomerRefund>

    suspend fun updateDraftRefund(
        refundId: String,
        amount: Money? = null,
        refundMethod: CustomerRefundMethod? = null,
        refundReference: String? = null,
        reason: String? = null,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<CustomerRefund>

    suspend fun submitRefund(
        refundId: String,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<CustomerRefund>

    suspend fun approveRefund(
        refundId: String,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<CustomerRefund>

    suspend fun rejectRefund(
        refundId: String,
        rejectionReason: String,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<CustomerRefund>

    suspend fun cancelRefund(
        refundId: String,
        cancellationReason: String,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<CustomerRefund>

    suspend fun postRefund(
        refundId: String,
        overrideAccountHead: String? = null,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<CustomerRefund>

    suspend fun getRefundById(
        refundId: String,
        callerRole: UserRole,
        authenticatedCustomerId: String? = null
    ): DomainResult<CustomerRefund>

    suspend fun getRefundByNumber(
        projectId: String,
        refundNo: String,
        callerRole: UserRole,
        authenticatedCustomerId: String? = null
    ): DomainResult<CustomerRefund>

    suspend fun getRefundByIdempotencyKey(
        projectId: String,
        idempotencyKey: String,
        callerRole: UserRole
    ): DomainResult<CustomerRefund?>

    fun observeRefunds(
        projectId: String,
        callerRole: UserRole
    ): Flow<List<CustomerRefund>>

    fun observeCustomerRefunds(
        projectId: String,
        customerId: String,
        callerRole: UserRole,
        authenticatedCustomerId: String? = null
    ): Flow<List<CustomerRefund>>

    suspend fun getActivityEvents(
        refundId: String,
        callerRole: UserRole
    ): DomainResult<List<FinancialAdjustmentActivityEvent>>
}
