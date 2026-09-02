package com.sucharu.sucharupro.domain.service.businessfinancialadjustment

import com.sucharu.sucharupro.data.api.model.AuthenticatedPrincipal
import com.sucharu.sucharupro.data.datasource.businessfinancialadjustment.AdjustmentFilter
import com.sucharu.sucharupro.data.datasource.businessfinancialadjustment.RefundFilter
import com.sucharu.sucharupro.data.datasource.businessfinancialadjustment.WriteOffFilter
import com.sucharu.sucharupro.domain.model.businessfinancialadjustment.*
import com.sucharu.sucharupro.domain.model.common.DomainResult
import java.math.BigDecimal

data class CreateAdjustmentCommand(
    val adjustmentNumber: String? = null,
    val adjustmentType: BusinessFinancialAdjustmentType,
    val sourceType: AdjustmentSourceType,
    val sourceId: String,
    val originalTransactionId: String? = null,
    val originalAmount: BigDecimal = BigDecimal.ZERO,
    val adjustmentAmount: BigDecimal,
    val currency: String = "BDT",
    val reason: String,
    val justification: String,
    val periodId: String,
    val costCenterId: String? = null,
    val jobId: String? = null,
    val customerId: String? = null,
    val vendorId: String? = null,
    val idempotencyKey: String? = null
)

data class SubmitAdjustmentCommand(
    val adjustmentId: String,
    val notes: String? = null,
    val correlationId: String? = null
)

data class ReviewAdjustmentCommand(
    val adjustmentId: String,
    val notes: String? = null,
    val correlationId: String? = null
)

data class ApproveAdjustmentCommand(
    val adjustmentId: String,
    val notes: String? = null,
    val correlationId: String? = null
)

data class RejectAdjustmentCommand(
    val adjustmentId: String,
    val reason: String,
    val correlationId: String? = null
)

data class CancelAdjustmentCommand(
    val adjustmentId: String,
    val reason: String,
    val correlationId: String? = null
)

data class PostAdjustmentCommand(
    val adjustmentId: String,
    val debitAccount: String? = null,
    val creditAccount: String? = null,
    val correlationId: String? = null,
    val idempotencyKey: String? = null
)

data class ReverseAdjustmentCommand(
    val adjustmentId: String,
    val reason: String,
    val correlationId: String? = null,
    val idempotencyKey: String? = null
)

data class CreateRefundCommand(
    val refundNumber: String? = null,
    val sourceType: AdjustmentSourceType,
    val sourceId: String,
    val customerId: String? = null,
    val vendorId: String? = null,
    val originalTransactionId: String? = null,
    val eligibleBalance: BigDecimal = BigDecimal.ZERO,
    val requestedAmount: BigDecimal,
    val currency: String = "BDT",
    val refundReason: String,
    val paymentMethod: String = "BANK_TRANSFER",
    val periodId: String,
    val idempotencyKey: String? = null
)

data class ApproveRefundCommand(
    val refundId: String,
    val approvedAmount: BigDecimal? = null,
    val notes: String? = null,
    val correlationId: String? = null
)

data class PostRefundCommand(
    val refundId: String,
    val correlationId: String? = null,
    val idempotencyKey: String? = null
)

data class CreateWriteOffCommand(
    val writeOffNumber: String? = null,
    val sourceType: AdjustmentSourceType,
    val sourceId: String,
    val writeOffType: BusinessFinancialWriteOffType,
    val eligibleBalance: BigDecimal = BigDecimal.ZERO,
    val amount: BigDecimal,
    val currency: String = "BDT",
    val reason: String,
    val justification: String,
    val periodId: String,
    val customerId: String? = null,
    val vendorId: String? = null,
    val idempotencyKey: String? = null
)

data class ApproveWriteOffCommand(
    val writeOffId: String,
    val notes: String? = null,
    val correlationId: String? = null
)

data class PostWriteOffCommand(
    val writeOffId: String,
    val correlationId: String? = null,
    val idempotencyKey: String? = null
)

interface BusinessFinancialAdjustmentService {

    // --- Adjustments ---
    suspend fun createAdjustment(principal: AuthenticatedPrincipal, command: CreateAdjustmentCommand): DomainResult<BusinessFinancialAdjustment>
    suspend fun submitAdjustment(principal: AuthenticatedPrincipal, command: SubmitAdjustmentCommand): DomainResult<BusinessFinancialAdjustment>
    suspend fun reviewAdjustment(principal: AuthenticatedPrincipal, command: ReviewAdjustmentCommand): DomainResult<BusinessFinancialAdjustment>
    suspend fun approveAdjustment(principal: AuthenticatedPrincipal, command: ApproveAdjustmentCommand): DomainResult<BusinessFinancialAdjustment>
    suspend fun rejectAdjustment(principal: AuthenticatedPrincipal, command: RejectAdjustmentCommand): DomainResult<BusinessFinancialAdjustment>
    suspend fun cancelAdjustment(principal: AuthenticatedPrincipal, command: CancelAdjustmentCommand): DomainResult<BusinessFinancialAdjustment>
    suspend fun postAdjustment(principal: AuthenticatedPrincipal, command: PostAdjustmentCommand): DomainResult<BusinessFinancialAdjustment>
    suspend fun reverseAdjustment(principal: AuthenticatedPrincipal, command: ReverseAdjustmentCommand): DomainResult<BusinessFinancialAdjustment>
    suspend fun getAdjustmentById(principal: AuthenticatedPrincipal, id: String): DomainResult<BusinessFinancialAdjustment>
    suspend fun listAdjustments(principal: AuthenticatedPrincipal, filter: AdjustmentFilter = AdjustmentFilter()): DomainResult<List<BusinessFinancialAdjustment>>

    // --- Refunds ---
    suspend fun createRefund(principal: AuthenticatedPrincipal, command: CreateRefundCommand): DomainResult<BusinessFinancialRefund>
    suspend fun approveRefund(principal: AuthenticatedPrincipal, command: ApproveRefundCommand): DomainResult<BusinessFinancialRefund>
    suspend fun postRefund(principal: AuthenticatedPrincipal, command: PostRefundCommand): DomainResult<BusinessFinancialRefund>
    suspend fun getRefundById(principal: AuthenticatedPrincipal, id: String): DomainResult<BusinessFinancialRefund>
    suspend fun listRefunds(principal: AuthenticatedPrincipal, filter: RefundFilter = RefundFilter()): DomainResult<List<BusinessFinancialRefund>>

    // --- Write-Offs ---
    suspend fun createWriteOff(principal: AuthenticatedPrincipal, command: CreateWriteOffCommand): DomainResult<BusinessFinancialWriteOff>
    suspend fun approveWriteOff(principal: AuthenticatedPrincipal, command: ApproveWriteOffCommand): DomainResult<BusinessFinancialWriteOff>
    suspend fun postWriteOff(principal: AuthenticatedPrincipal, command: PostWriteOffCommand): DomainResult<BusinessFinancialWriteOff>
    suspend fun getWriteOffById(principal: AuthenticatedPrincipal, id: String): DomainResult<BusinessFinancialWriteOff>
    suspend fun listWriteOffs(principal: AuthenticatedPrincipal, filter: WriteOffFilter = WriteOffFilter()): DomainResult<List<BusinessFinancialWriteOff>>

    // --- Analytics, Exceptions & Audit ---
    suspend fun getSummary(principal: AuthenticatedPrincipal, periodId: String? = null): DomainResult<BusinessFinancialAdjustmentSummary>
    suspend fun listExceptions(principal: AuthenticatedPrincipal): DomainResult<List<BusinessFinancialException>>
    suspend fun listAuditEvents(principal: AuthenticatedPrincipal, entityId: String? = null, entityType: String? = null): DomainResult<List<BusinessFinancialAdjustmentAuditEvent>>
}
