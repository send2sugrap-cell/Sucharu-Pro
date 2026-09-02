package com.sucharu.sucharupro.domain.repository

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.common.Money
import com.sucharu.sucharupro.domain.model.finance.CustomerCreditNote
import com.sucharu.sucharupro.domain.model.finance.FinancialAdjustment
import com.sucharu.sucharupro.domain.model.finance.FinancialAdjustmentActivityEvent
import com.sucharu.sucharupro.domain.model.finance.FinancialAdjustmentDirection
import com.sucharu.sucharupro.domain.model.finance.FinancialAdjustmentSummary
import com.sucharu.sucharupro.domain.model.finance.FinancialAdjustmentType
import com.sucharu.sucharupro.domain.model.finance.FinancialReferenceType
import com.sucharu.sucharupro.domain.model.finance.VendorDebitNote
import com.sucharu.sucharupro.domain.model.user.UserRole
import kotlinx.coroutines.flow.Flow

/**
 * Domain repository contract for Financial Adjustments, Credit Notes, and Debit Notes (Module 09 Step 07).
 */
interface FinancialAdjustmentRepository {

    suspend fun createAdjustment(
        projectId: String,
        adjustmentType: FinancialAdjustmentType,
        direction: FinancialAdjustmentDirection = adjustmentType.defaultDirection,
        amount: Money,
        currency: String = "BDT",
        customerId: String? = null,
        vendorId: String? = null,
        referenceType: FinancialReferenceType,
        referenceId: String,
        reasonCode: String,
        reason: String,
        description: String,
        notes: String? = null,
        relatedReceivableId: String? = null,
        relatedPayableId: String? = null,
        relatedPaymentId: String? = null,
        relatedSupplierPaymentId: String? = null,
        idempotencyKey: String? = null,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<FinancialAdjustment>

    suspend fun updateDraftAdjustment(
        adjustmentId: String,
        amount: Money? = null,
        reasonCode: String? = null,
        reason: String? = null,
        description: String? = null,
        notes: String? = null,
        relatedReceivableId: String? = null,
        relatedPayableId: String? = null,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<FinancialAdjustment>

    suspend fun submitAdjustment(
        adjustmentId: String,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<FinancialAdjustment>

    suspend fun approveAdjustment(
        adjustmentId: String,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<FinancialAdjustment>

    suspend fun rejectAdjustment(
        adjustmentId: String,
        rejectionReason: String,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<FinancialAdjustment>

    suspend fun cancelAdjustment(
        adjustmentId: String,
        cancellationReason: String,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<FinancialAdjustment>

    suspend fun postAdjustment(
        adjustmentId: String,
        overrideAccountHead: String? = null,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<FinancialAdjustment>

    suspend fun getAdjustmentById(
        adjustmentId: String,
        callerRole: UserRole,
        authenticatedCustomerId: String? = null,
        authenticatedVendorId: String? = null
    ): DomainResult<FinancialAdjustment>

    suspend fun getAdjustmentByNumber(
        projectId: String,
        adjustmentNo: String,
        callerRole: UserRole,
        authenticatedCustomerId: String? = null,
        authenticatedVendorId: String? = null
    ): DomainResult<FinancialAdjustment>

    suspend fun getAdjustmentByIdempotencyKey(
        projectId: String,
        idempotencyKey: String,
        callerRole: UserRole
    ): DomainResult<FinancialAdjustment?>

    fun observeAdjustments(
        projectId: String,
        callerRole: UserRole
    ): Flow<List<FinancialAdjustment>>

    fun observeCustomerAdjustments(
        projectId: String,
        customerId: String,
        callerRole: UserRole,
        authenticatedCustomerId: String? = null
    ): Flow<List<FinancialAdjustment>>

    fun observeVendorAdjustments(
        projectId: String,
        vendorId: String,
        callerRole: UserRole,
        authenticatedVendorId: String? = null
    ): Flow<List<FinancialAdjustment>>

    suspend fun getCreditNoteById(
        creditNoteId: String,
        callerRole: UserRole,
        authenticatedCustomerId: String? = null
    ): DomainResult<CustomerCreditNote>

    fun observeCreditNotes(
        projectId: String,
        callerRole: UserRole
    ): Flow<List<CustomerCreditNote>>

    suspend fun getDebitNoteById(
        debitNoteId: String,
        callerRole: UserRole,
        authenticatedVendorId: String? = null
    ): DomainResult<VendorDebitNote>

    fun observeDebitNotes(
        projectId: String,
        callerRole: UserRole
    ): Flow<List<VendorDebitNote>>

    suspend fun getAdjustmentSummary(
        projectId: String,
        callerRole: UserRole,
        authenticatedCustomerId: String? = null,
        authenticatedVendorId: String? = null
    ): DomainResult<FinancialAdjustmentSummary>

    suspend fun getActivityEvents(
        entityId: String,
        callerRole: UserRole
    ): DomainResult<List<FinancialAdjustmentActivityEvent>>
}
