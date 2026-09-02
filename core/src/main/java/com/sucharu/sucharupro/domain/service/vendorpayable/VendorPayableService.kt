package com.sucharu.sucharupro.domain.service.vendorpayable

import com.sucharu.sucharupro.data.api.model.AuthenticatedPrincipal
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.vendorpayable.*
import java.math.BigDecimal

data class CreateVendorPayableCommand(
    val vendorId: String,
    val jobId: String? = null,
    val vendorJobId: String? = null,
    val billReference: String? = null,
    val originalAmount: BigDecimal,
    val currency: String = "BDT",
    val issueDate: Long = System.currentTimeMillis(),
    val paymentTerms: VendorPayablePaymentTerms = VendorPayablePaymentTerms.NET_30,
    val customTermDays: Int? = null,
    val description: String,
    val notes: String? = null,
    val attachmentUrl: String? = null,
    val idempotencyKey: String? = null,
    val autoSubmit: Boolean = false
)

data class UpdateVendorPayableCommand(
    val vendorId: String? = null,
    val jobId: String? = null,
    val vendorJobId: String? = null,
    val billReference: String? = null,
    val originalAmount: BigDecimal? = null,
    val currency: String? = null,
    val issueDate: Long? = null,
    val paymentTerms: VendorPayablePaymentTerms? = null,
    val customTermDays: Int? = null,
    val description: String? = null,
    val notes: String? = null,
    val attachmentUrl: String? = null
)

data class AllocateVendorPayablePaymentCommand(
    val amount: BigDecimal,
    val paymentMethod: VendorPayablePaymentMethod,
    val paymentReference: String? = null,
    val paymentDate: Long = System.currentTimeMillis(),
    val notes: String? = null,
    val idempotencyKey: String? = null
)

data class VendorPayableFilter(
    val vendorId: String? = null,
    val status: VendorPayableStatus? = null,
    val jobId: String? = null,
    val isOverdueOnly: Boolean = false,
    val fromDate: Long? = null,
    val toDate: Long? = null
)

interface VendorPayableService {

    suspend fun createPayable(
        principal: AuthenticatedPrincipal,
        command: CreateVendorPayableCommand
    ): DomainResult<VendorPayable>

    suspend fun updatePayableDraft(
        principal: AuthenticatedPrincipal,
        payableId: String,
        command: UpdateVendorPayableCommand
    ): DomainResult<VendorPayable>

    suspend fun submitPayable(
        principal: AuthenticatedPrincipal,
        payableId: String
    ): DomainResult<VendorPayable>

    suspend fun approvePayable(
        principal: AuthenticatedPrincipal,
        payableId: String,
        notes: String? = null
    ): DomainResult<VendorPayable>

    suspend fun rejectPayable(
        principal: AuthenticatedPrincipal,
        payableId: String,
        reason: String
    ): DomainResult<VendorPayable>

    suspend fun cancelPayable(
        principal: AuthenticatedPrincipal,
        payableId: String,
        reason: String
    ): DomainResult<VendorPayable>

    suspend fun voidPayable(
        principal: AuthenticatedPrincipal,
        payableId: String,
        reason: String
    ): DomainResult<VendorPayable>

    suspend fun allocatePayment(
        principal: AuthenticatedPrincipal,
        payableId: String,
        command: AllocateVendorPayablePaymentCommand
    ): DomainResult<VendorPayable>

    suspend fun getPayableById(
        principal: AuthenticatedPrincipal,
        payableId: String
    ): DomainResult<VendorPayable>

    suspend fun getPayableByNumber(
        principal: AuthenticatedPrincipal,
        payableNumber: String
    ): DomainResult<VendorPayable>

    suspend fun listPayables(
        principal: AuthenticatedPrincipal,
        filter: VendorPayableFilter = VendorPayableFilter(),
        limit: Int = 50,
        offset: Int = 0
    ): DomainResult<List<VendorPayable>>

    suspend fun countPayables(
        principal: AuthenticatedPrincipal,
        filter: VendorPayableFilter = VendorPayableFilter()
    ): DomainResult<Long>

    suspend fun getVendorPayableSummary(
        principal: AuthenticatedPrincipal,
        vendorId: String
    ): DomainResult<VendorPayableSummary>

    suspend fun getVendorPayableAging(
        principal: AuthenticatedPrincipal,
        vendorId: String? = null,
        asOfDate: Long = System.currentTimeMillis()
    ): DomainResult<VendorPayableAgingReport>

    suspend fun getPayableAuditTrail(
        principal: AuthenticatedPrincipal,
        payableId: String
    ): DomainResult<List<VendorPayableAuditEvent>>

    suspend fun getPayablePaymentAllocations(
        principal: AuthenticatedPrincipal,
        payableId: String
    ): DomainResult<List<VendorPayablePaymentAllocation>>
}
