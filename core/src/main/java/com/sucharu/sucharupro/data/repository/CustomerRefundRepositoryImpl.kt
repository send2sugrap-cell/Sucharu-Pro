package com.sucharu.sucharupro.data.repository

import com.sucharu.sucharupro.data.datasource.CustomerRefundDataSource
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.common.Money
import com.sucharu.sucharupro.domain.model.finance.CustomerRefund
import com.sucharu.sucharupro.domain.model.finance.CustomerRefundMethod
import com.sucharu.sucharupro.domain.model.finance.FinancialAdjustmentActivityEvent
import com.sucharu.sucharupro.domain.model.finance.FinancialAdjustmentActivityType
import com.sucharu.sucharupro.domain.model.finance.FinancialAdjustmentStatus
import com.sucharu.sucharupro.domain.model.finance.FinancialEntryType
import com.sucharu.sucharupro.domain.model.finance.FinancialReferenceType
import com.sucharu.sucharupro.domain.model.finance.FinancialTransactionType
import com.sucharu.sucharupro.domain.model.user.UserRole
import com.sucharu.sucharupro.domain.repository.CustomerRefundRepository
import com.sucharu.sucharupro.domain.repository.FinancialTransactionRepository
import com.sucharu.sucharupro.domain.validation.CustomerRefundAuthorizationValidator
import com.sucharu.sucharupro.domain.validation.CustomerRefundLifecycleValidator
import com.sucharu.sucharupro.domain.validation.CustomerRefundValidator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID

/**
 * Thread-safe implementation of CustomerRefundRepository with non-reentrant mutex locking (Module 09 Step 07).
 */
class CustomerRefundRepositoryImpl(
    private val refundDataSource: CustomerRefundDataSource,
    private val financialTransactionRepository: FinancialTransactionRepository
) : CustomerRefundRepository {

    private val mutex = Mutex()

    override suspend fun createRefund(
        projectId: String,
        customerId: String,
        amount: Money,
        currency: String,
        refundMethod: CustomerRefundMethod,
        refundReference: String?,
        reason: String,
        adjustmentId: String?,
        sourcePaymentId: String?,
        receivableId: String?,
        idempotencyKey: String?,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<CustomerRefund> = mutex.withLock {
        val authResult = CustomerRefundAuthorizationValidator.validateCreateDraft(callerRole)
        if (authResult is DomainResult.Error) return@withLock authResult

        // Idempotency check
        if (!idempotencyKey.isNullOrBlank()) {
            val existing = refundDataSource.getRefundByIdempotencyKey(projectId, idempotencyKey.trim())
            if (existing != null) {
                return@withLock DomainResult.Success(existing)
            }
        }

        val valResult = CustomerRefundValidator.validateCreatePayload(
            projectId = projectId,
            customerId = customerId.trim(),
            amount = amount,
            currency = currency,
            refundMethod = refundMethod,
            refundReference = refundReference?.trim()?.ifEmpty { null },
            reason = reason.trim(),
            actorId = actorId
        )
        if (valResult is DomainResult.Error) return@withLock valResult

        val refundId = UUID.randomUUID().toString()
        val refundNo = refundDataSource.generateNextRefundNo(projectId)
        val now = System.currentTimeMillis()
        val initialStatus = if (callerRole == UserRole.STAFF) FinancialAdjustmentStatus.DRAFT else FinancialAdjustmentStatus.PENDING

        val refund = CustomerRefund(
            refundId = refundId,
            refundNo = refundNo,
            projectId = projectId,
            customerId = customerId.trim(),
            adjustmentId = adjustmentId?.trim()?.ifEmpty { null },
            sourcePaymentId = sourcePaymentId?.trim()?.ifEmpty { null },
            receivableId = receivableId?.trim()?.ifEmpty { null },
            amount = amount,
            currency = currency,
            refundMethod = refundMethod,
            refundReference = refundReference?.trim()?.ifEmpty { null },
            reason = reason.trim(),
            status = initialStatus,
            financialTransactionId = null,
            idempotencyKey = idempotencyKey?.trim()?.ifEmpty { null },
            createdBy = actorId,
            createdAt = now,
            updatedAt = now
        )

        val inserted = refundDataSource.insertRefund(refund)
        if (!inserted) {
            return@withLock DomainResult.Error(message = "Failed to insert customer refund.")
        }

        refundDataSource.insertActivityEvent(
            FinancialAdjustmentActivityEvent(
                eventId = UUID.randomUUID().toString(),
                entityId = refundId,
                projectId = projectId,
                activityType = FinancialAdjustmentActivityType.REFUND_CREATED,
                actorId = actorId,
                details = "Customer refund #$refundNo created for ${amount.formatted()} $currency via ${refundMethod.defaultLabel}."
            )
        )

        DomainResult.Success(refund)
    }

    override suspend fun updateDraftRefund(
        refundId: String,
        amount: Money?,
        refundMethod: CustomerRefundMethod?,
        refundReference: String?,
        reason: String?,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<CustomerRefund> = mutex.withLock {
        val authResult = CustomerRefundAuthorizationValidator.validateUpdateDraft(callerRole)
        if (authResult is DomainResult.Error) return@withLock authResult

        val existing = refundDataSource.getRefundById(refundId)
            ?: return@withLock DomainResult.Error(message = "Customer refund '$refundId' not found.")

        if (existing.status != FinancialAdjustmentStatus.DRAFT) {
            return@withLock DomainResult.Error(
                message = "Only DRAFT refunds can be edited. Current status: ${existing.status.name}."
            )
        }

        val updatedAmount = amount ?: existing.amount
        if (!updatedAmount.isPositive()) {
            return@withLock DomainResult.Error(message = "Refund amount must be strictly greater than zero.")
        }

        val updatedMethod = refundMethod ?: existing.refundMethod
        val updatedRef = refundReference ?: existing.refundReference
        if (updatedMethod.requiresReference && updatedRef.isNullOrBlank()) {
            return@withLock DomainResult.Error(
                message = "Payment/Refund reference is required for payment method '${updatedMethod.defaultLabel}'."
            )
        }

        val updated = existing.copy(
            amount = updatedAmount,
            refundMethod = updatedMethod,
            refundReference = updatedRef?.trim()?.ifEmpty { null },
            reason = reason?.trim() ?: existing.reason,
            updatedBy = actorId,
            updatedAt = System.currentTimeMillis()
        )

        refundDataSource.updateRefund(updated)

        refundDataSource.insertActivityEvent(
            FinancialAdjustmentActivityEvent(
                eventId = UUID.randomUUID().toString(),
                entityId = refundId,
                projectId = existing.projectId,
                activityType = FinancialAdjustmentActivityType.REFUND_UPDATED,
                actorId = actorId,
                details = "Draft refund #${existing.refundNo} updated."
            )
        )

        DomainResult.Success(updated)
    }

    override suspend fun submitRefund(
        refundId: String,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<CustomerRefund> = mutex.withLock {
        val authResult = CustomerRefundAuthorizationValidator.validateSubmit(callerRole)
        if (authResult is DomainResult.Error) return@withLock authResult

        val existing = refundDataSource.getRefundById(refundId)
            ?: return@withLock DomainResult.Error(message = "Customer refund '$refundId' not found.")

        val transitionResult = CustomerRefundLifecycleValidator.validateTransition(
            existing.status,
            FinancialAdjustmentStatus.PENDING
        )
        if (transitionResult is DomainResult.Error) return@withLock transitionResult

        val now = System.currentTimeMillis()
        val updated = existing.copy(
            status = FinancialAdjustmentStatus.PENDING,
            submittedBy = actorId,
            submittedAt = now,
            updatedBy = actorId,
            updatedAt = now
        )

        refundDataSource.updateRefund(updated)

        refundDataSource.insertActivityEvent(
            FinancialAdjustmentActivityEvent(
                eventId = UUID.randomUUID().toString(),
                entityId = refundId,
                projectId = existing.projectId,
                activityType = FinancialAdjustmentActivityType.REFUND_SUBMITTED,
                actorId = actorId,
                details = "Customer refund #${existing.refundNo} submitted for approval."
            )
        )

        DomainResult.Success(updated)
    }

    override suspend fun approveRefund(
        refundId: String,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<CustomerRefund> = mutex.withLock {
        val existing = refundDataSource.getRefundById(refundId)
            ?: return@withLock DomainResult.Error(message = "Customer refund '$refundId' not found.")

        val authResult = CustomerRefundAuthorizationValidator.validateApprove(
            callerRole = callerRole,
            creatorId = existing.createdBy,
            approverId = actorId
        )
        if (authResult is DomainResult.Error) return@withLock authResult

        val transitionResult = CustomerRefundLifecycleValidator.validateTransition(
            existing.status,
            FinancialAdjustmentStatus.APPROVED
        )
        if (transitionResult is DomainResult.Error) return@withLock transitionResult

        val now = System.currentTimeMillis()
        val updated = existing.copy(
            status = FinancialAdjustmentStatus.APPROVED,
            approvedBy = actorId,
            approvedAt = now,
            updatedBy = actorId,
            updatedAt = now
        )

        refundDataSource.updateRefund(updated)

        refundDataSource.insertActivityEvent(
            FinancialAdjustmentActivityEvent(
                eventId = UUID.randomUUID().toString(),
                entityId = refundId,
                projectId = existing.projectId,
                activityType = FinancialAdjustmentActivityType.REFUND_APPROVED,
                actorId = actorId,
                details = "Customer refund #${existing.refundNo} approved."
            )
        )

        DomainResult.Success(updated)
    }

    override suspend fun rejectRefund(
        refundId: String,
        rejectionReason: String,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<CustomerRefund> = mutex.withLock {
        val authResult = CustomerRefundAuthorizationValidator.validateReject(callerRole)
        if (authResult is DomainResult.Error) return@withLock authResult

        if (rejectionReason.isBlank()) {
            return@withLock DomainResult.Error(message = "Rejection reason cannot be blank.")
        }

        val existing = refundDataSource.getRefundById(refundId)
            ?: return@withLock DomainResult.Error(message = "Customer refund '$refundId' not found.")

        val transitionResult = CustomerRefundLifecycleValidator.validateTransition(
            existing.status,
            FinancialAdjustmentStatus.REJECTED
        )
        if (transitionResult is DomainResult.Error) return@withLock transitionResult

        val now = System.currentTimeMillis()
        val updated = existing.copy(
            status = FinancialAdjustmentStatus.REJECTED,
            rejectedBy = actorId,
            rejectedAt = now,
            cancellationReason = rejectionReason.trim(),
            updatedBy = actorId,
            updatedAt = now
        )

        refundDataSource.updateRefund(updated)

        refundDataSource.insertActivityEvent(
            FinancialAdjustmentActivityEvent(
                eventId = UUID.randomUUID().toString(),
                entityId = refundId,
                projectId = existing.projectId,
                activityType = FinancialAdjustmentActivityType.REFUND_REJECTED,
                actorId = actorId,
                details = "Customer refund #${existing.refundNo} rejected. Reason: ${rejectionReason.trim()}"
            )
        )

        DomainResult.Success(updated)
    }

    override suspend fun cancelRefund(
        refundId: String,
        cancellationReason: String,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<CustomerRefund> = mutex.withLock {
        val authResult = CustomerRefundAuthorizationValidator.validateCancel(callerRole)
        if (authResult is DomainResult.Error) return@withLock authResult

        if (cancellationReason.isBlank()) {
            return@withLock DomainResult.Error(message = "Cancellation reason cannot be blank.")
        }

        val existing = refundDataSource.getRefundById(refundId)
            ?: return@withLock DomainResult.Error(message = "Customer refund '$refundId' not found.")

        val transitionResult = CustomerRefundLifecycleValidator.validateTransition(
            existing.status,
            FinancialAdjustmentStatus.CANCELLED
        )
        if (transitionResult is DomainResult.Error) return@withLock transitionResult

        val now = System.currentTimeMillis()
        val updated = existing.copy(
            status = FinancialAdjustmentStatus.CANCELLED,
            cancelledBy = actorId,
            cancelledAt = now,
            cancellationReason = cancellationReason.trim(),
            updatedBy = actorId,
            updatedAt = now
        )

        refundDataSource.updateRefund(updated)

        refundDataSource.insertActivityEvent(
            FinancialAdjustmentActivityEvent(
                eventId = UUID.randomUUID().toString(),
                entityId = refundId,
                projectId = existing.projectId,
                activityType = FinancialAdjustmentActivityType.REFUND_CANCELLED,
                actorId = actorId,
                details = "Customer refund #${existing.refundNo} cancelled. Reason: ${cancellationReason.trim()}"
            )
        )

        DomainResult.Success(updated)
    }

    override suspend fun postRefund(
        refundId: String,
        overrideAccountHead: String?,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<CustomerRefund> = mutex.withLock {
        val existing = refundDataSource.getRefundById(refundId)
            ?: return@withLock DomainResult.Error(message = "Customer refund '$refundId' not found.")

        if (existing.status.isTerminal) {
            return@withLock DomainResult.Error(
                message = "Terminal refund '${existing.refundNo}' (${existing.status.name}) cannot be posted to financial ledger."
            )
        }

        val authResult = CustomerRefundAuthorizationValidator.validatePost(
            callerRole = callerRole,
            creatorId = existing.createdBy,
            posterId = actorId
        )
        if (authResult is DomainResult.Error) return@withLock authResult

        val transitionResult = CustomerRefundLifecycleValidator.validateTransition(
            existing.status,
            FinancialAdjustmentStatus.POSTED
        )
        if (transitionResult is DomainResult.Error) return@withLock transitionResult

        val resolvedAccountHead = overrideAccountHead?.trim() ?: when (existing.refundMethod) {
            CustomerRefundMethod.CASH -> "CASH_IN_HAND"
            CustomerRefundMethod.BANK_TRANSFER,
            CustomerRefundMethod.CHEQUE -> "BANK_ACCOUNT"
            CustomerRefundMethod.MOBILE_BANKING -> "MOBILE_WALLET"
            CustomerRefundMethod.CARD -> "CARD_SETTLEMENT"
            CustomerRefundMethod.OTHER -> "CASH_IN_HAND"
        }

        // Create & Post Canonical Step 01 Financial Transaction
        val txnCreateRes = financialTransactionRepository.createTransaction(
            projectId = existing.projectId,
            transactionType = FinancialTransactionType.REFUND,
            entryType = FinancialEntryType.DEBIT,
            amount = existing.amount,
            currency = existing.currency,
            referenceType = FinancialReferenceType.REFUND,
            referenceId = existing.refundId,
            customerId = existing.customerId,
            description = "Customer refund disbursement: ${existing.reason} (${existing.refundMethod.defaultLabel})",
            notes = existing.refundReference,
            actorId = existing.createdBy,
            callerRole = callerRole
        )
        if (txnCreateRes is DomainResult.Error) return@withLock txnCreateRes
        val financialTxn = (txnCreateRes as DomainResult.Success).data

        financialTransactionRepository.submitTransaction(financialTxn.transactionId, existing.createdBy, callerRole)
        val txnPostRes = financialTransactionRepository.postTransaction(
            transactionId = financialTxn.transactionId,
            accountHead = resolvedAccountHead,
            actorId = actorId,
            callerRole = callerRole
        )
        if (txnPostRes is DomainResult.Error) return@withLock txnPostRes

        val now = System.currentTimeMillis()
        val postedRefund = existing.copy(
            status = FinancialAdjustmentStatus.POSTED,
            financialTransactionId = financialTxn.transactionId,
            approvedBy = existing.approvedBy ?: actorId,
            approvedAt = existing.approvedAt ?: now,
            postedBy = actorId,
            postedAt = now,
            updatedBy = actorId,
            updatedAt = now
        )

        refundDataSource.updateRefund(postedRefund)

        refundDataSource.insertActivityEvent(
            FinancialAdjustmentActivityEvent(
                eventId = UUID.randomUUID().toString(),
                entityId = existing.refundId,
                projectId = existing.projectId,
                activityType = FinancialAdjustmentActivityType.REFUND_POSTED,
                actorId = actorId,
                details = "Customer refund #${existing.refundNo} posted to financial ledger (#${financialTxn.transactionNo}) under account head '$resolvedAccountHead'."
            )
        )

        DomainResult.Success(postedRefund)
    }

    override suspend fun getRefundById(
        refundId: String,
        callerRole: UserRole,
        authenticatedCustomerId: String?
    ): DomainResult<CustomerRefund> = mutex.withLock {
        val refund = refundDataSource.getRefundById(refundId)
            ?: return@withLock DomainResult.Error(message = "Customer refund '$refundId' not found.")

        val authResult = CustomerRefundAuthorizationValidator.validateView(
            callerRole = callerRole,
            targetCustomerId = refund.customerId,
            authenticatedCustomerId = authenticatedCustomerId
        )
        if (authResult is DomainResult.Error) return@withLock authResult

        DomainResult.Success(refund)
    }

    override suspend fun getRefundByNumber(
        projectId: String,
        refundNo: String,
        callerRole: UserRole,
        authenticatedCustomerId: String?
    ): DomainResult<CustomerRefund> = mutex.withLock {
        val refund = refundDataSource.getRefundByNumber(projectId, refundNo)
            ?: return@withLock DomainResult.Error(message = "Refund '#$refundNo' not found in project '$projectId'.")

        val authResult = CustomerRefundAuthorizationValidator.validateView(
            callerRole = callerRole,
            targetCustomerId = refund.customerId,
            authenticatedCustomerId = authenticatedCustomerId
        )
        if (authResult is DomainResult.Error) return@withLock authResult

        DomainResult.Success(refund)
    }

    override suspend fun getRefundByIdempotencyKey(
        projectId: String,
        idempotencyKey: String,
        callerRole: UserRole
    ): DomainResult<CustomerRefund?> = mutex.withLock {
        val authResult = CustomerRefundAuthorizationValidator.validateCreateDraft(callerRole)
        if (authResult is DomainResult.Error) return@withLock authResult

        val refund = refundDataSource.getRefundByIdempotencyKey(projectId, idempotencyKey.trim())
        DomainResult.Success(refund)
    }

    override fun observeRefunds(
        projectId: String,
        callerRole: UserRole
    ): Flow<List<CustomerRefund>> {
        if (!callerRole.isInternal) return emptyFlow()
        return refundDataSource.observeRefunds(projectId)
    }

    override fun observeCustomerRefunds(
        projectId: String,
        customerId: String,
        callerRole: UserRole,
        authenticatedCustomerId: String?
    ): Flow<List<CustomerRefund>> {
        val authResult = CustomerRefundAuthorizationValidator.validateView(
            callerRole = callerRole,
            targetCustomerId = customerId,
            authenticatedCustomerId = authenticatedCustomerId
        )
        if (authResult is DomainResult.Error) return emptyFlow()
        return refundDataSource.observeCustomerRefunds(projectId, customerId)
    }

    override suspend fun getActivityEvents(
        refundId: String,
        callerRole: UserRole
    ): DomainResult<List<FinancialAdjustmentActivityEvent>> = mutex.withLock {
        val events = refundDataSource.getActivityEvents(refundId)
        DomainResult.Success(events)
    }
}
