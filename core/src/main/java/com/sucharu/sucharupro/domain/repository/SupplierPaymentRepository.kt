package com.sucharu.sucharupro.domain.repository

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.common.Money
import com.sucharu.sucharupro.domain.model.finance.SupplierPayment
import com.sucharu.sucharupro.domain.model.finance.SupplierPaymentActivityEvent
import com.sucharu.sucharupro.domain.model.finance.SupplierPaymentMethod
import com.sucharu.sucharupro.domain.model.finance.SupplierPaymentSettlement
import com.sucharu.sucharupro.domain.model.user.UserRole
import kotlinx.coroutines.flow.Flow

/**
 * Domain repository contract for Supplier Payment & Settlement operations (Module 09 Step 05).
 */
interface SupplierPaymentRepository {

    suspend fun createPayment(
        projectId: String,
        vendorId: String,
        payableId: String,
        amount: Money,
        currency: String = "BDT",
        paymentMethod: SupplierPaymentMethod,
        paymentReference: String? = null,
        paymentDate: Long = System.currentTimeMillis(),
        idempotencyKey: String? = null,
        notes: String? = null,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<SupplierPayment>

    suspend fun getPaymentById(
        paymentId: String,
        callerRole: UserRole,
        authenticatedVendorId: String? = null
    ): DomainResult<SupplierPayment>

    suspend fun getPaymentByNumber(
        projectId: String,
        paymentNo: String,
        callerRole: UserRole,
        authenticatedVendorId: String? = null
    ): DomainResult<SupplierPayment>

    suspend fun getPaymentByIdempotencyKey(
        projectId: String,
        idempotencyKey: String,
        callerRole: UserRole
    ): DomainResult<SupplierPayment?>

    fun observePayments(
        projectId: String,
        callerRole: UserRole
    ): Flow<List<SupplierPayment>>

    fun observeVendorPayments(
        projectId: String,
        vendorId: String,
        callerRole: UserRole,
        authenticatedVendorId: String? = null
    ): Flow<List<SupplierPayment>>

    fun observePayablePayments(
        projectId: String,
        payableId: String,
        callerRole: UserRole
    ): Flow<List<SupplierPayment>>

    suspend fun updateDraftPayment(
        paymentId: String,
        amount: Money? = null,
        paymentMethod: SupplierPaymentMethod? = null,
        paymentReference: String? = null,
        paymentDate: Long? = null,
        notes: String? = null,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<SupplierPayment>

    suspend fun submitPayment(
        paymentId: String,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<SupplierPayment>

    suspend fun approvePayment(
        paymentId: String,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<SupplierPayment>

    suspend fun rejectPayment(
        paymentId: String,
        rejectionReason: String,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<SupplierPayment>

    suspend fun cancelPayment(
        paymentId: String,
        cancellationReason: String,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<SupplierPayment>

    suspend fun postPayment(
        paymentId: String,
        accountHead: String? = null,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<SupplierPayment>

    suspend fun getSettlementsByPayable(
        payableId: String,
        callerRole: UserRole
    ): DomainResult<List<SupplierPaymentSettlement>>

    suspend fun getSettlementsByPayment(
        paymentId: String,
        callerRole: UserRole
    ): DomainResult<List<SupplierPaymentSettlement>>

    fun observeSettlements(
        projectId: String,
        callerRole: UserRole
    ): Flow<List<SupplierPaymentSettlement>>

    suspend fun getActivityEvents(
        paymentId: String,
        callerRole: UserRole
    ): DomainResult<List<SupplierPaymentActivityEvent>>
}
