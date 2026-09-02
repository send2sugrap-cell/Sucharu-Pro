package com.sucharu.sucharupro.domain.repository

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.common.Money
import com.sucharu.sucharupro.domain.model.finance.FinancialReferenceType
import com.sucharu.sucharupro.domain.model.finance.VendorPayable
import com.sucharu.sucharupro.domain.model.finance.VendorPayableActivityEvent
import com.sucharu.sucharupro.domain.model.finance.VendorPayableSummary
import com.sucharu.sucharupro.domain.model.user.UserRole
import kotlinx.coroutines.flow.Flow

/**
 * Domain repository contract for Supplier/Vendor Payable operations (Module 09 Step 04).
 */
interface VendorPayableRepository {

    suspend fun createPayable(
        projectId: String,
        vendorId: String,
        referenceType: FinancialReferenceType,
        referenceId: String,
        financialTransactionId: String? = null,
        supplierInvoiceNo: String? = null,
        originalAmount: Money,
        currency: String = "BDT",
        dueDate: Long,
        payableDate: Long = System.currentTimeMillis(),
        description: String,
        notes: String? = null,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<VendorPayable>

    suspend fun getPayableById(
        payableId: String,
        callerRole: UserRole,
        authenticatedVendorId: String? = null
    ): DomainResult<VendorPayable>

    suspend fun getPayableByNumber(
        projectId: String,
        payableNo: String,
        callerRole: UserRole,
        authenticatedVendorId: String? = null
    ): DomainResult<VendorPayable>

    suspend fun getPayableByReference(
        projectId: String,
        vendorId: String,
        referenceId: String,
        callerRole: UserRole
    ): DomainResult<VendorPayable>

    suspend fun getPayableByInvoice(
        projectId: String,
        vendorId: String,
        supplierInvoiceNo: String,
        callerRole: UserRole
    ): DomainResult<VendorPayable>

    fun observePayables(
        projectId: String,
        callerRole: UserRole
    ): Flow<List<VendorPayable>>

    fun observeVendorPayables(
        projectId: String,
        vendorId: String,
        callerRole: UserRole,
        authenticatedVendorId: String? = null
    ): Flow<List<VendorPayable>>

    suspend fun getVendorPayableSummary(
        projectId: String,
        vendorId: String? = null,
        callerRole: UserRole,
        authenticatedVendorId: String? = null,
        asOfTimestamp: Long = System.currentTimeMillis()
    ): DomainResult<VendorPayableSummary>

    suspend fun updateDraftPayable(
        payableId: String,
        originalAmount: Money? = null,
        dueDate: Long? = null,
        supplierInvoiceNo: String? = null,
        description: String? = null,
        notes: String? = null,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<VendorPayable>

    suspend fun submitPayable(
        payableId: String,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<VendorPayable>

    suspend fun approvePayable(
        payableId: String,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<VendorPayable>

    suspend fun cancelPayable(
        payableId: String,
        cancellationReason: String,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<VendorPayable>

    suspend fun recordSettlement(
        payableId: String,
        settlementAmount: Money,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<VendorPayable>

    suspend fun getActivityEvents(
        payableId: String,
        callerRole: UserRole
    ): DomainResult<List<VendorPayableActivityEvent>>
}
