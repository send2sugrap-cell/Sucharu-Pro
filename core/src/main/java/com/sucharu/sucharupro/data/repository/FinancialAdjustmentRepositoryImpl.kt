package com.sucharu.sucharupro.data.repository

import com.sucharu.sucharupro.data.datasource.FinancialAdjustmentDataSource
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.common.Money
import com.sucharu.sucharupro.domain.model.finance.CustomerCreditNote
import com.sucharu.sucharupro.domain.model.finance.FinancialAdjustment
import com.sucharu.sucharupro.domain.model.finance.FinancialAdjustmentActivityEvent
import com.sucharu.sucharupro.domain.model.finance.FinancialAdjustmentActivityType
import com.sucharu.sucharupro.domain.model.finance.FinancialAdjustmentDirection
import com.sucharu.sucharupro.domain.model.finance.FinancialAdjustmentStatus
import com.sucharu.sucharupro.domain.model.finance.FinancialAdjustmentSummary
import com.sucharu.sucharupro.domain.model.finance.FinancialAdjustmentType
import com.sucharu.sucharupro.domain.model.finance.FinancialEntryType
import com.sucharu.sucharupro.domain.model.finance.FinancialReferenceType
import com.sucharu.sucharupro.domain.model.finance.FinancialTransactionType
import com.sucharu.sucharupro.domain.model.finance.VendorDebitNote
import com.sucharu.sucharupro.domain.model.user.UserRole
import com.sucharu.sucharupro.domain.repository.CustomerReceivableRepository
import com.sucharu.sucharupro.domain.repository.FinancialAdjustmentRepository
import com.sucharu.sucharupro.domain.repository.FinancialTransactionRepository
import com.sucharu.sucharupro.domain.repository.VendorPayableRepository
import com.sucharu.sucharupro.domain.validation.FinancialAdjustmentAuthorizationValidator
import com.sucharu.sucharupro.domain.validation.FinancialAdjustmentLifecycleValidator
import com.sucharu.sucharupro.domain.validation.FinancialAdjustmentValidator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID

/**
 * Thread-safe implementation of FinancialAdjustmentRepository with non-reentrant mutex locking (Module 09 Step 07).
 */
class FinancialAdjustmentRepositoryImpl(
    private val adjustmentDataSource: FinancialAdjustmentDataSource,
    private val financialTransactionRepository: FinancialTransactionRepository,
    private val customerReceivableRepository: CustomerReceivableRepository,
    private val vendorPayableRepository: VendorPayableRepository
) : FinancialAdjustmentRepository {

    private val mutex = Mutex()

    override suspend fun createAdjustment(
        projectId: String,
        adjustmentType: FinancialAdjustmentType,
        direction: FinancialAdjustmentDirection,
        amount: Money,
        currency: String,
        customerId: String?,
        vendorId: String?,
        referenceType: FinancialReferenceType,
        referenceId: String,
        reasonCode: String,
        reason: String,
        description: String,
        notes: String?,
        relatedReceivableId: String?,
        relatedPayableId: String?,
        relatedPaymentId: String?,
        relatedSupplierPaymentId: String?,
        idempotencyKey: String?,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<FinancialAdjustment> = mutex.withLock {
        val authResult = FinancialAdjustmentAuthorizationValidator.validateCreateDraft(callerRole)
        if (authResult is DomainResult.Error) return@withLock authResult

        // Idempotency check
        if (!idempotencyKey.isNullOrBlank()) {
            val existing = adjustmentDataSource.getAdjustmentByIdempotencyKey(projectId, idempotencyKey.trim())
            if (existing != null) {
                return@withLock DomainResult.Success(existing)
            }
        }

        val valResult = FinancialAdjustmentValidator.validateCreatePayload(
            projectId = projectId,
            adjustmentType = adjustmentType,
            direction = direction,
            amount = amount,
            currency = currency,
            customerId = customerId?.trim()?.ifEmpty { null },
            vendorId = vendorId?.trim()?.ifEmpty { null },
            referenceType = referenceType,
            referenceId = referenceId.trim(),
            reasonCode = reasonCode.trim(),
            reason = reason.trim(),
            description = description.trim(),
            actorId = actorId
        )
        if (valResult is DomainResult.Error) return@withLock valResult

        // Validate limits if attached to receivable
        if (!relatedReceivableId.isNullOrBlank()) {
            val recRes = customerReceivableRepository.getReceivableById(relatedReceivableId, callerRole)
            if (recRes is DomainResult.Success) {
                val limitCheck = FinancialAdjustmentValidator.validateReceivableAdjustmentLimit(
                    adjustmentAmount = amount,
                    receivableOutstandingAmount = recRes.data.outstandingAmount
                )
                if (limitCheck is DomainResult.Error) return@withLock limitCheck
            }
        }

        // Validate limits if attached to payable
        if (!relatedPayableId.isNullOrBlank()) {
            val payRes = vendorPayableRepository.getPayableById(relatedPayableId, callerRole)
            if (payRes is DomainResult.Success) {
                val limitCheck = FinancialAdjustmentValidator.validatePayableAdjustmentLimit(
                    adjustmentAmount = amount,
                    payableOutstandingAmount = payRes.data.outstandingAmount
                )
                if (limitCheck is DomainResult.Error) return@withLock limitCheck
            }
        }

        val adjustmentId = UUID.randomUUID().toString()
        val adjustmentNo = adjustmentDataSource.generateNextAdjustmentNo(projectId)
        val now = System.currentTimeMillis()
        val initialStatus = if (callerRole == UserRole.STAFF) FinancialAdjustmentStatus.DRAFT else FinancialAdjustmentStatus.PENDING

        val adjustment = FinancialAdjustment(
            adjustmentId = adjustmentId,
            adjustmentNo = adjustmentNo,
            projectId = projectId,
            adjustmentType = adjustmentType,
            direction = direction,
            status = initialStatus,
            amount = amount,
            currency = currency,
            customerId = customerId?.trim()?.ifEmpty { null },
            vendorId = vendorId?.trim()?.ifEmpty { null },
            referenceType = referenceType,
            referenceId = referenceId.trim(),
            reasonCode = reasonCode.trim(),
            reason = reason.trim(),
            description = description.trim(),
            notes = notes?.trim()?.ifEmpty { null },
            relatedReceivableId = relatedReceivableId?.trim()?.ifEmpty { null },
            relatedPayableId = relatedPayableId?.trim()?.ifEmpty { null },
            relatedPaymentId = relatedPaymentId?.trim()?.ifEmpty { null },
            relatedSupplierPaymentId = relatedSupplierPaymentId?.trim()?.ifEmpty { null },
            idempotencyKey = idempotencyKey?.trim()?.ifEmpty { null },
            createdBy = actorId,
            createdAt = now,
            updatedAt = now
        )

        val inserted = adjustmentDataSource.insertAdjustment(adjustment)
        if (!inserted) {
            return@withLock DomainResult.Error(message = "Failed to insert financial adjustment.")
        }

        adjustmentDataSource.insertActivityEvent(
            FinancialAdjustmentActivityEvent(
                eventId = UUID.randomUUID().toString(),
                entityId = adjustmentId,
                projectId = projectId,
                activityType = FinancialAdjustmentActivityType.ADJUSTMENT_CREATED,
                actorId = actorId,
                details = "Financial adjustment #$adjustmentNo (${adjustmentType.defaultLabel}) created for ${amount.formatted()} $currency."
            )
        )

        DomainResult.Success(adjustment)
    }

    override suspend fun updateDraftAdjustment(
        adjustmentId: String,
        amount: Money?,
        reasonCode: String?,
        reason: String?,
        description: String?,
        notes: String?,
        relatedReceivableId: String?,
        relatedPayableId: String?,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<FinancialAdjustment> = mutex.withLock {
        val authResult = FinancialAdjustmentAuthorizationValidator.validateUpdateDraft(callerRole)
        if (authResult is DomainResult.Error) return@withLock authResult

        val existing = adjustmentDataSource.getAdjustmentById(adjustmentId)
            ?: return@withLock DomainResult.Error(message = "Adjustment '$adjustmentId' not found.")

        if (existing.status != FinancialAdjustmentStatus.DRAFT) {
            return@withLock DomainResult.Error(
                message = "Only DRAFT adjustments can be edited. Current status: ${existing.status.name}."
            )
        }

        val updatedAmount = amount ?: existing.amount
        if (!updatedAmount.isPositive()) {
            return@withLock DomainResult.Error(message = "Adjustment amount must be strictly greater than zero.")
        }

        val updated = existing.copy(
            amount = updatedAmount,
            reasonCode = reasonCode?.trim() ?: existing.reasonCode,
            reason = reason?.trim() ?: existing.reason,
            description = description?.trim() ?: existing.description,
            notes = notes?.trim() ?: existing.notes,
            relatedReceivableId = relatedReceivableId?.trim() ?: existing.relatedReceivableId,
            relatedPayableId = relatedPayableId?.trim() ?: existing.relatedPayableId,
            updatedBy = actorId,
            updatedAt = System.currentTimeMillis()
        )

        adjustmentDataSource.updateAdjustment(updated)

        adjustmentDataSource.insertActivityEvent(
            FinancialAdjustmentActivityEvent(
                eventId = UUID.randomUUID().toString(),
                entityId = adjustmentId,
                projectId = existing.projectId,
                activityType = FinancialAdjustmentActivityType.ADJUSTMENT_UPDATED,
                actorId = actorId,
                details = "Draft adjustment #${existing.adjustmentNo} updated."
            )
        )

        DomainResult.Success(updated)
    }

    override suspend fun submitAdjustment(
        adjustmentId: String,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<FinancialAdjustment> = mutex.withLock {
        val authResult = FinancialAdjustmentAuthorizationValidator.validateSubmit(callerRole)
        if (authResult is DomainResult.Error) return@withLock authResult

        val existing = adjustmentDataSource.getAdjustmentById(adjustmentId)
            ?: return@withLock DomainResult.Error(message = "Adjustment '$adjustmentId' not found.")

        val transitionResult = FinancialAdjustmentLifecycleValidator.validateTransition(
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

        adjustmentDataSource.updateAdjustment(updated)

        adjustmentDataSource.insertActivityEvent(
            FinancialAdjustmentActivityEvent(
                eventId = UUID.randomUUID().toString(),
                entityId = adjustmentId,
                projectId = existing.projectId,
                activityType = FinancialAdjustmentActivityType.ADJUSTMENT_SUBMITTED,
                actorId = actorId,
                details = "Adjustment #${existing.adjustmentNo} submitted for approval."
            )
        )

        DomainResult.Success(updated)
    }

    override suspend fun approveAdjustment(
        adjustmentId: String,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<FinancialAdjustment> = mutex.withLock {
        val existing = adjustmentDataSource.getAdjustmentById(adjustmentId)
            ?: return@withLock DomainResult.Error(message = "Adjustment '$adjustmentId' not found.")

        val authResult = FinancialAdjustmentAuthorizationValidator.validateApprove(
            callerRole = callerRole,
            creatorId = existing.createdBy,
            approverId = actorId
        )
        if (authResult is DomainResult.Error) return@withLock authResult

        val transitionResult = FinancialAdjustmentLifecycleValidator.validateTransition(
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

        adjustmentDataSource.updateAdjustment(updated)

        adjustmentDataSource.insertActivityEvent(
            FinancialAdjustmentActivityEvent(
                eventId = UUID.randomUUID().toString(),
                entityId = adjustmentId,
                projectId = existing.projectId,
                activityType = FinancialAdjustmentActivityType.ADJUSTMENT_APPROVED,
                actorId = actorId,
                details = "Adjustment #${existing.adjustmentNo} approved."
            )
        )

        DomainResult.Success(updated)
    }

    override suspend fun rejectAdjustment(
        adjustmentId: String,
        rejectionReason: String,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<FinancialAdjustment> = mutex.withLock {
        val authResult = FinancialAdjustmentAuthorizationValidator.validateReject(callerRole)
        if (authResult is DomainResult.Error) return@withLock authResult

        if (rejectionReason.isBlank()) {
            return@withLock DomainResult.Error(message = "Rejection reason cannot be blank.")
        }

        val existing = adjustmentDataSource.getAdjustmentById(adjustmentId)
            ?: return@withLock DomainResult.Error(message = "Adjustment '$adjustmentId' not found.")

        val transitionResult = FinancialAdjustmentLifecycleValidator.validateTransition(
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

        adjustmentDataSource.updateAdjustment(updated)

        adjustmentDataSource.insertActivityEvent(
            FinancialAdjustmentActivityEvent(
                eventId = UUID.randomUUID().toString(),
                entityId = adjustmentId,
                projectId = existing.projectId,
                activityType = FinancialAdjustmentActivityType.ADJUSTMENT_REJECTED,
                actorId = actorId,
                details = "Adjustment #${existing.adjustmentNo} rejected. Reason: ${rejectionReason.trim()}"
            )
        )

        DomainResult.Success(updated)
    }

    override suspend fun cancelAdjustment(
        adjustmentId: String,
        cancellationReason: String,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<FinancialAdjustment> = mutex.withLock {
        val authResult = FinancialAdjustmentAuthorizationValidator.validateCancel(callerRole)
        if (authResult is DomainResult.Error) return@withLock authResult

        if (cancellationReason.isBlank()) {
            return@withLock DomainResult.Error(message = "Cancellation reason cannot be blank.")
        }

        val existing = adjustmentDataSource.getAdjustmentById(adjustmentId)
            ?: return@withLock DomainResult.Error(message = "Adjustment '$adjustmentId' not found.")

        val transitionResult = FinancialAdjustmentLifecycleValidator.validateTransition(
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

        adjustmentDataSource.updateAdjustment(updated)

        adjustmentDataSource.insertActivityEvent(
            FinancialAdjustmentActivityEvent(
                eventId = UUID.randomUUID().toString(),
                entityId = adjustmentId,
                projectId = existing.projectId,
                activityType = FinancialAdjustmentActivityType.ADJUSTMENT_CANCELLED,
                actorId = actorId,
                details = "Adjustment #${existing.adjustmentNo} cancelled. Reason: ${cancellationReason.trim()}"
            )
        )

        DomainResult.Success(updated)
    }

    override suspend fun postAdjustment(
        adjustmentId: String,
        overrideAccountHead: String?,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<FinancialAdjustment> = mutex.withLock {
        val existing = adjustmentDataSource.getAdjustmentById(adjustmentId)
            ?: return@withLock DomainResult.Error(message = "Adjustment '$adjustmentId' not found.")

        if (existing.status.isTerminal) {
            return@withLock DomainResult.Error(
                message = "Terminal adjustment '${existing.adjustmentNo}' (${existing.status.name}) cannot be posted to financial ledger."
            )
        }

        val authResult = FinancialAdjustmentAuthorizationValidator.validatePost(
            callerRole = callerRole,
            creatorId = existing.createdBy,
            posterId = actorId
        )
        if (authResult is DomainResult.Error) return@withLock authResult

        val transitionResult = FinancialAdjustmentLifecycleValidator.validateTransition(
            existing.status,
            FinancialAdjustmentStatus.POSTED
        )
        if (transitionResult is DomainResult.Error) return@withLock transitionResult

        // Determine transaction type and entry type
        val entryType = if (existing.direction == FinancialAdjustmentDirection.DEBIT) {
            FinancialEntryType.DEBIT
        } else {
            FinancialEntryType.CREDIT
        }

        val transactionType = when (existing.adjustmentType) {
            FinancialAdjustmentType.CUSTOMER_CREDIT_NOTE -> FinancialTransactionType.CREDIT
            FinancialAdjustmentType.VENDOR_DEBIT_NOTE -> FinancialTransactionType.DEBIT
            FinancialAdjustmentType.CUSTOMER_REFUND -> FinancialTransactionType.REFUND
            else -> FinancialTransactionType.ADJUSTMENT
        }

        val resolvedAccountHead = overrideAccountHead?.trim() ?: when (existing.adjustmentType) {
            FinancialAdjustmentType.CUSTOMER_CREDIT_NOTE,
            FinancialAdjustmentType.CUSTOMER_BALANCE_ADJUSTMENT,
            FinancialAdjustmentType.CUSTOMER_DUE_ADJUSTMENT -> "SALES_RETURN"
            FinancialAdjustmentType.VENDOR_DEBIT_NOTE,
            FinancialAdjustmentType.VENDOR_BALANCE_ADJUSTMENT,
            FinancialAdjustmentType.VENDOR_PAYABLE_ADJUSTMENT -> "PURCHASE_RETURN"
            FinancialAdjustmentType.CUSTOMER_REFUND -> "CASH_IN_HAND"
            FinancialAdjustmentType.GENERAL_ADJUSTMENT -> "ADJUSTMENT_ACCOUNT"
        }

        // Create & Post Canonical Step 01 Financial Transaction
        val txnCreateRes = financialTransactionRepository.createTransaction(
            projectId = existing.projectId,
            transactionType = transactionType,
            entryType = entryType,
            amount = existing.amount,
            currency = existing.currency,
            referenceType = FinancialReferenceType.ADJUSTMENT,
            referenceId = existing.adjustmentId,
            customerId = existing.customerId,
            vendorId = existing.vendorId,
            description = "Financial adjustment posting: ${existing.description} (${existing.adjustmentType.defaultLabel})",
            notes = existing.notes,
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
        var createdCreditNoteId: String? = null
        var createdDebitNoteId: String? = null

        // Generate Customer Credit Note document if applicable
        if (existing.adjustmentType == FinancialAdjustmentType.CUSTOMER_CREDIT_NOTE) {
            val cnId = UUID.randomUUID().toString()
            val cnNo = adjustmentDataSource.generateNextCreditNoteNo(existing.projectId)
            val creditNote = CustomerCreditNote(
                creditNoteId = cnId,
                creditNoteNo = cnNo,
                projectId = existing.projectId,
                adjustmentId = existing.adjustmentId,
                customerId = existing.customerId!!,
                referenceType = existing.referenceType,
                referenceId = existing.referenceId,
                amount = existing.amount,
                currency = existing.currency,
                reason = existing.reason,
                issuedBy = actorId,
                issuedAt = now,
                financialTransactionId = financialTxn.transactionId,
                notes = existing.notes
            )
            adjustmentDataSource.insertCreditNote(creditNote)
            createdCreditNoteId = cnId

            adjustmentDataSource.insertActivityEvent(
                FinancialAdjustmentActivityEvent(
                    eventId = UUID.randomUUID().toString(),
                    entityId = existing.adjustmentId,
                    projectId = existing.projectId,
                    activityType = FinancialAdjustmentActivityType.CREDIT_NOTE_ISSUED,
                    actorId = actorId,
                    details = "Credit Note #$cnNo issued for ${existing.amount.formatted()} ${existing.currency}."
                )
            )
        }

        // Generate Vendor Debit Note document if applicable
        if (existing.adjustmentType == FinancialAdjustmentType.VENDOR_DEBIT_NOTE) {
            val dnId = UUID.randomUUID().toString()
            val dnNo = adjustmentDataSource.generateNextDebitNoteNo(existing.projectId)
            val debitNote = VendorDebitNote(
                debitNoteId = dnId,
                debitNoteNo = dnNo,
                projectId = existing.projectId,
                adjustmentId = existing.adjustmentId,
                vendorId = existing.vendorId!!,
                referenceType = existing.referenceType,
                referenceId = existing.referenceId,
                amount = existing.amount,
                currency = existing.currency,
                reason = existing.reason,
                issuedBy = actorId,
                issuedAt = now,
                financialTransactionId = financialTxn.transactionId,
                notes = existing.notes
            )
            adjustmentDataSource.insertDebitNote(debitNote)
            createdDebitNoteId = dnId

            adjustmentDataSource.insertActivityEvent(
                FinancialAdjustmentActivityEvent(
                    eventId = UUID.randomUUID().toString(),
                    entityId = existing.adjustmentId,
                    projectId = existing.projectId,
                    activityType = FinancialAdjustmentActivityType.DEBIT_NOTE_ISSUED,
                    actorId = actorId,
                    details = "Debit Note #$dnNo issued for ${existing.amount.formatted()} ${existing.currency}."
                )
            )
        }

        // Settle attached receivable if specified
        if (!existing.relatedReceivableId.isNullOrBlank()) {
            customerReceivableRepository.recordSettlement(
                receivableId = existing.relatedReceivableId,
                settlementAmount = existing.amount,
                actorId = actorId,
                callerRole = callerRole
            )
            adjustmentDataSource.insertActivityEvent(
                FinancialAdjustmentActivityEvent(
                    eventId = UUID.randomUUID().toString(),
                    entityId = existing.adjustmentId,
                    projectId = existing.projectId,
                    activityType = FinancialAdjustmentActivityType.RECEIVABLE_ADJUSTED,
                    actorId = actorId,
                    details = "Customer receivable #${existing.relatedReceivableId} adjusted by ${existing.amount.formatted()} ${existing.currency}."
                )
            )
        }

        // Settle attached payable if specified
        if (!existing.relatedPayableId.isNullOrBlank()) {
            vendorPayableRepository.recordSettlement(
                payableId = existing.relatedPayableId,
                settlementAmount = existing.amount,
                actorId = actorId,
                callerRole = callerRole
            )
            adjustmentDataSource.insertActivityEvent(
                FinancialAdjustmentActivityEvent(
                    eventId = UUID.randomUUID().toString(),
                    entityId = existing.adjustmentId,
                    projectId = existing.projectId,
                    activityType = FinancialAdjustmentActivityType.PAYABLE_ADJUSTED,
                    actorId = actorId,
                    details = "Vendor payable #${existing.relatedPayableId} adjusted by ${existing.amount.formatted()} ${existing.currency}."
                )
            )
        }

        val postedAdjustment = existing.copy(
            status = FinancialAdjustmentStatus.POSTED,
            financialTransactionId = financialTxn.transactionId,
            creditNoteId = createdCreditNoteId ?: existing.creditNoteId,
            debitNoteId = createdDebitNoteId ?: existing.debitNoteId,
            approvedBy = existing.approvedBy ?: actorId,
            approvedAt = existing.approvedAt ?: now,
            postedBy = actorId,
            postedAt = now,
            updatedBy = actorId,
            updatedAt = now
        )

        adjustmentDataSource.updateAdjustment(postedAdjustment)

        adjustmentDataSource.insertActivityEvent(
            FinancialAdjustmentActivityEvent(
                eventId = UUID.randomUUID().toString(),
                entityId = existing.adjustmentId,
                projectId = existing.projectId,
                activityType = FinancialAdjustmentActivityType.ADJUSTMENT_POSTED,
                actorId = actorId,
                details = "Adjustment #${existing.adjustmentNo} posted to financial ledger (#${financialTxn.transactionNo}) under account head '$resolvedAccountHead'."
            )
        )

        DomainResult.Success(postedAdjustment)
    }

    override suspend fun getAdjustmentById(
        adjustmentId: String,
        callerRole: UserRole,
        authenticatedCustomerId: String?,
        authenticatedVendorId: String?
    ): DomainResult<FinancialAdjustment> = mutex.withLock {
        val adjustment = adjustmentDataSource.getAdjustmentById(adjustmentId)
            ?: return@withLock DomainResult.Error(message = "Financial adjustment '$adjustmentId' not found.")

        val authResult = FinancialAdjustmentAuthorizationValidator.validateView(
            callerRole = callerRole,
            targetCustomerId = adjustment.customerId,
            authenticatedCustomerId = authenticatedCustomerId,
            targetVendorId = adjustment.vendorId,
            authenticatedVendorId = authenticatedVendorId
        )
        if (authResult is DomainResult.Error) return@withLock authResult

        DomainResult.Success(adjustment)
    }

    override suspend fun getAdjustmentByNumber(
        projectId: String,
        adjustmentNo: String,
        callerRole: UserRole,
        authenticatedCustomerId: String?,
        authenticatedVendorId: String?
    ): DomainResult<FinancialAdjustment> = mutex.withLock {
        val adjustment = adjustmentDataSource.getAdjustmentByNumber(projectId, adjustmentNo)
            ?: return@withLock DomainResult.Error(message = "Adjustment '#$adjustmentNo' not found in project '$projectId'.")

        val authResult = FinancialAdjustmentAuthorizationValidator.validateView(
            callerRole = callerRole,
            targetCustomerId = adjustment.customerId,
            authenticatedCustomerId = authenticatedCustomerId,
            targetVendorId = adjustment.vendorId,
            authenticatedVendorId = authenticatedVendorId
        )
        if (authResult is DomainResult.Error) return@withLock authResult

        DomainResult.Success(adjustment)
    }

    override suspend fun getAdjustmentByIdempotencyKey(
        projectId: String,
        idempotencyKey: String,
        callerRole: UserRole
    ): DomainResult<FinancialAdjustment?> = mutex.withLock {
        val authResult = FinancialAdjustmentAuthorizationValidator.validateCreateDraft(callerRole)
        if (authResult is DomainResult.Error) return@withLock authResult

        val adjustment = adjustmentDataSource.getAdjustmentByIdempotencyKey(projectId, idempotencyKey.trim())
        DomainResult.Success(adjustment)
    }

    override fun observeAdjustments(
        projectId: String,
        callerRole: UserRole
    ): Flow<List<FinancialAdjustment>> {
        if (!callerRole.isInternal) return emptyFlow()
        return adjustmentDataSource.observeAdjustments(projectId)
    }

    override fun observeCustomerAdjustments(
        projectId: String,
        customerId: String,
        callerRole: UserRole,
        authenticatedCustomerId: String?
    ): Flow<List<FinancialAdjustment>> {
        val authResult = FinancialAdjustmentAuthorizationValidator.validateView(
            callerRole = callerRole,
            targetCustomerId = customerId,
            authenticatedCustomerId = authenticatedCustomerId
        )
        if (authResult is DomainResult.Error) return emptyFlow()
        return adjustmentDataSource.observeCustomerAdjustments(projectId, customerId)
    }

    override fun observeVendorAdjustments(
        projectId: String,
        vendorId: String,
        callerRole: UserRole,
        authenticatedVendorId: String?
    ): Flow<List<FinancialAdjustment>> {
        val authResult = FinancialAdjustmentAuthorizationValidator.validateView(
            callerRole = callerRole,
            targetVendorId = vendorId,
            authenticatedVendorId = authenticatedVendorId
        )
        if (authResult is DomainResult.Error) return emptyFlow()
        return adjustmentDataSource.observeVendorAdjustments(projectId, vendorId)
    }

    override suspend fun getCreditNoteById(
        creditNoteId: String,
        callerRole: UserRole,
        authenticatedCustomerId: String?
    ): DomainResult<CustomerCreditNote> = mutex.withLock {
        val note = adjustmentDataSource.getCreditNoteById(creditNoteId)
            ?: return@withLock DomainResult.Error(message = "Credit Note '$creditNoteId' not found.")

        val authResult = FinancialAdjustmentAuthorizationValidator.validateView(
            callerRole = callerRole,
            targetCustomerId = note.customerId,
            authenticatedCustomerId = authenticatedCustomerId
        )
        if (authResult is DomainResult.Error) return@withLock authResult

        DomainResult.Success(note)
    }

    override fun observeCreditNotes(
        projectId: String,
        callerRole: UserRole
    ): Flow<List<CustomerCreditNote>> {
        if (!callerRole.isInternal) return emptyFlow()
        return adjustmentDataSource.observeCreditNotes(projectId)
    }

    override suspend fun getDebitNoteById(
        debitNoteId: String,
        callerRole: UserRole,
        authenticatedVendorId: String?
    ): DomainResult<VendorDebitNote> = mutex.withLock {
        val note = adjustmentDataSource.getDebitNoteById(debitNoteId)
            ?: return@withLock DomainResult.Error(message = "Debit Note '$debitNoteId' not found.")

        val authResult = FinancialAdjustmentAuthorizationValidator.validateView(
            callerRole = callerRole,
            targetVendorId = note.vendorId,
            authenticatedVendorId = authenticatedVendorId
        )
        if (authResult is DomainResult.Error) return@withLock authResult

        DomainResult.Success(note)
    }

    override fun observeDebitNotes(
        projectId: String,
        callerRole: UserRole
    ): Flow<List<VendorDebitNote>> {
        if (!callerRole.isInternal) return emptyFlow()
        return adjustmentDataSource.observeDebitNotes(projectId)
    }

    override suspend fun getAdjustmentSummary(
        projectId: String,
        callerRole: UserRole,
        authenticatedCustomerId: String?,
        authenticatedVendorId: String?
    ): DomainResult<FinancialAdjustmentSummary> = mutex.withLock {
        val adjustments = adjustmentDataSource.observeAdjustments(projectId).first()
        val creditNotes = adjustmentDataSource.observeCreditNotes(projectId).first()
        val debitNotes = adjustmentDataSource.observeDebitNotes(projectId).first()

        var postedAmount = Money.ZERO
        var pendingAmount = Money.ZERO
        var cancelledAmount = Money.ZERO

        adjustments.forEach { adj ->
            when (adj.status) {
                FinancialAdjustmentStatus.POSTED -> postedAmount += adj.amount
                FinancialAdjustmentStatus.PENDING -> pendingAmount += adj.amount
                FinancialAdjustmentStatus.CANCELLED,
                FinancialAdjustmentStatus.REJECTED -> cancelledAmount += adj.amount
                FinancialAdjustmentStatus.DRAFT,
                FinancialAdjustmentStatus.APPROVED -> Unit
            }
        }

        var totalCNAmount = Money.ZERO
        creditNotes.forEach { totalCNAmount += it.amount }

        var totalDNAmount = Money.ZERO
        debitNotes.forEach { totalDNAmount += it.amount }

        val customerCredit = if (!authenticatedCustomerId.isNullOrBlank()) {
            var sum = Money.ZERO
            creditNotes.filter { it.customerId == authenticatedCustomerId }.forEach { sum += it.amount }
            sum
        } else Money.ZERO

        val vendorDebit = if (!authenticatedVendorId.isNullOrBlank()) {
            var sum = Money.ZERO
            debitNotes.filter { it.vendorId == authenticatedVendorId }.forEach { sum += it.amount }
            sum
        } else Money.ZERO

        val summary = FinancialAdjustmentSummary(
            projectId = projectId,
            totalAdjustmentsCount = adjustments.size,
            totalCreditNotesCount = creditNotes.size,
            totalDebitNotesCount = debitNotes.size,
            totalRefundsCount = 0,
            totalPostedAdjustmentAmount = postedAmount,
            totalPendingAdjustmentAmount = pendingAmount,
            totalCancelledAdjustmentAmount = cancelledAmount,
            totalCreditNotesAmount = totalCNAmount,
            totalDebitNotesAmount = totalDNAmount,
            totalRefundsAmount = Money.ZERO,
            customerTotalCredit = customerCredit,
            vendorTotalDebit = vendorDebit
        )

        DomainResult.Success(summary)
    }

    override suspend fun getActivityEvents(
        entityId: String,
        callerRole: UserRole
    ): DomainResult<List<FinancialAdjustmentActivityEvent>> = mutex.withLock {
        val events = adjustmentDataSource.getActivityEvents(entityId)
        DomainResult.Success(events)
    }
}
