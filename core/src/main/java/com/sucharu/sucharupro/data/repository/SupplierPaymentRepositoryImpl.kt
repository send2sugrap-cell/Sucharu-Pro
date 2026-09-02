package com.sucharu.sucharupro.data.repository

import com.sucharu.sucharupro.data.datasource.SupplierPaymentDataSource
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.common.Money
import com.sucharu.sucharupro.domain.model.finance.FinancialEntryType
import com.sucharu.sucharupro.domain.model.finance.FinancialReferenceType
import com.sucharu.sucharupro.domain.model.finance.FinancialTransactionType
import com.sucharu.sucharupro.domain.model.finance.SupplierPayment
import com.sucharu.sucharupro.domain.model.finance.SupplierPaymentActivityEvent
import com.sucharu.sucharupro.domain.model.finance.SupplierPaymentActivityType
import com.sucharu.sucharupro.domain.model.finance.SupplierPaymentMethod
import com.sucharu.sucharupro.domain.model.finance.SupplierPaymentSettlement
import com.sucharu.sucharupro.domain.model.finance.SupplierPaymentStatus
import com.sucharu.sucharupro.domain.model.user.UserRole
import com.sucharu.sucharupro.domain.repository.FinancialTransactionRepository
import com.sucharu.sucharupro.domain.repository.SupplierPaymentRepository
import com.sucharu.sucharupro.domain.repository.VendorPayableRepository
import com.sucharu.sucharupro.domain.validation.SupplierPaymentAuthorizationValidator
import com.sucharu.sucharupro.domain.validation.SupplierPaymentLifecycleValidator
import com.sucharu.sucharupro.domain.validation.SupplierPaymentValidator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID

/**
 * Thread-safe implementation of SupplierPaymentRepository with non-reentrant mutex locking (Module 09 Step 05).
 */
class SupplierPaymentRepositoryImpl(
    private val dataSource: SupplierPaymentDataSource,
    private val vendorPayableRepository: VendorPayableRepository,
    private val financialTransactionRepository: FinancialTransactionRepository
) : SupplierPaymentRepository {

    private val mutex = Mutex()

    override suspend fun createPayment(
        projectId: String,
        vendorId: String,
        payableId: String,
        amount: Money,
        currency: String,
        paymentMethod: SupplierPaymentMethod,
        paymentReference: String?,
        paymentDate: Long,
        idempotencyKey: String?,
        notes: String?,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<SupplierPayment> = mutex.withLock {
        val authResult = SupplierPaymentAuthorizationValidator.validateCreateDraftPayment(callerRole)
        if (authResult is DomainResult.Error) return@withLock authResult

        // Idempotency check: if key matches, return existing payment
        if (!idempotencyKey.isNullOrBlank()) {
            val existingIdempotent = dataSource.getPaymentByIdempotencyKey(projectId, idempotencyKey.trim())
            if (existingIdempotent != null) {
                return@withLock DomainResult.Success(existingIdempotent)
            }
        }

        val valResult = SupplierPaymentValidator.validateCreatePayload(
            projectId = projectId,
            vendorId = vendorId,
            payableId = payableId,
            amount = amount,
            currency = currency,
            paymentMethod = paymentMethod,
            paymentReference = paymentReference,
            paymentDate = paymentDate,
            actorId = actorId
        )
        if (valResult is DomainResult.Error) return@withLock valResult

        // Duplicate active payment reference check
        if (!paymentReference.isNullOrBlank()) {
            val existingRef = dataSource.getActivePaymentByReference(
                projectId = projectId,
                vendorId = vendorId,
                paymentMethod = paymentMethod,
                paymentReference = paymentReference.trim()
            )
            if (existingRef != null && !existingRef.status.isTerminal) {
                return@withLock DomainResult.Error(
                    message = "Duplicate active supplier payment found with reference '$paymentReference' for vendor '$vendorId' (Payment #${existingRef.paymentNo})."
                )
            }
        }

        // Validate Payable compatibility
        val payableRes = vendorPayableRepository.getPayableById(payableId, callerRole)
        if (payableRes is DomainResult.Error) return@withLock payableRes
        val payable = (payableRes as DomainResult.Success).data

        val payableCheck = SupplierPaymentValidator.validatePayableCompatibility(
            payable = payable,
            projectId = projectId,
            vendorId = vendorId,
            paymentAmount = amount
        )
        if (payableCheck is DomainResult.Error) return@withLock payableCheck

        val paymentId = UUID.randomUUID().toString()
        val paymentNo = dataSource.generateNextPaymentNo(projectId)
        val now = System.currentTimeMillis()
        val initialStatus = if (callerRole == UserRole.STAFF) SupplierPaymentStatus.DRAFT else SupplierPaymentStatus.PENDING

        val payment = SupplierPayment(
            paymentId = paymentId,
            paymentNo = paymentNo,
            projectId = projectId,
            vendorId = vendorId,
            payableId = payableId,
            financialTransactionId = null,
            amount = amount,
            currency = currency,
            paymentMethod = paymentMethod,
            paymentReference = paymentReference?.trim(),
            paymentDate = paymentDate,
            status = initialStatus,
            notes = notes,
            idempotencyKey = idempotencyKey?.trim(),
            createdBy = actorId,
            createdAt = now,
            updatedAt = now
        )

        val inserted = dataSource.insertPayment(payment)
        if (!inserted) {
            return@withLock DomainResult.Error(message = "Failed to insert supplier payment record.")
        }

        dataSource.insertActivityEvent(
            SupplierPaymentActivityEvent(
                eventId = UUID.randomUUID().toString(),
                paymentId = paymentId,
                projectId = projectId,
                activityType = SupplierPaymentActivityType.PAYMENT_CREATED,
                actorId = actorId,
                details = "Supplier payment #$paymentNo created for payable #${payable.payableNo} with amount ${amount.formatted()} $currency via ${paymentMethod.defaultLabel}."
            )
        )

        DomainResult.Success(payment)
    }

    override suspend fun getPaymentById(
        paymentId: String,
        callerRole: UserRole,
        authenticatedVendorId: String?
    ): DomainResult<SupplierPayment> = mutex.withLock {
        val payment = dataSource.getPaymentById(paymentId)
            ?: return@withLock DomainResult.Error(message = "Supplier payment '$paymentId' not found.")

        val authResult = SupplierPaymentAuthorizationValidator.validateViewPayments(
            callerRole = callerRole,
            requestedVendorId = payment.vendorId,
            authenticatedVendorId = authenticatedVendorId
        )
        if (authResult is DomainResult.Error) return@withLock authResult

        DomainResult.Success(payment)
    }

    override suspend fun getPaymentByNumber(
        projectId: String,
        paymentNo: String,
        callerRole: UserRole,
        authenticatedVendorId: String?
    ): DomainResult<SupplierPayment> = mutex.withLock {
        val payment = dataSource.getPaymentByNumber(projectId, paymentNo)
            ?: return@withLock DomainResult.Error(message = "Supplier payment '#$paymentNo' not found in project '$projectId'.")

        val authResult = SupplierPaymentAuthorizationValidator.validateViewPayments(
            callerRole = callerRole,
            requestedVendorId = payment.vendorId,
            authenticatedVendorId = authenticatedVendorId
        )
        if (authResult is DomainResult.Error) return@withLock authResult

        DomainResult.Success(payment)
    }

    override suspend fun getPaymentByIdempotencyKey(
        projectId: String,
        idempotencyKey: String,
        callerRole: UserRole
    ): DomainResult<SupplierPayment?> = mutex.withLock {
        val authResult = SupplierPaymentAuthorizationValidator.validateViewPayments(callerRole)
        if (authResult is DomainResult.Error) return@withLock authResult

        val payment = dataSource.getPaymentByIdempotencyKey(projectId, idempotencyKey.trim())
        DomainResult.Success(payment)
    }

    override fun observePayments(
        projectId: String,
        callerRole: UserRole
    ): Flow<List<SupplierPayment>> {
        val authResult = SupplierPaymentAuthorizationValidator.validateViewPayments(callerRole)
        if (authResult is DomainResult.Error) return emptyFlow()
        return dataSource.observePayments(projectId)
    }

    override fun observeVendorPayments(
        projectId: String,
        vendorId: String,
        callerRole: UserRole,
        authenticatedVendorId: String?
    ): Flow<List<SupplierPayment>> {
        val authResult = SupplierPaymentAuthorizationValidator.validateViewPayments(
            callerRole = callerRole,
            requestedVendorId = vendorId,
            authenticatedVendorId = authenticatedVendorId
        )
        if (authResult is DomainResult.Error) return emptyFlow()
        return dataSource.observeVendorPayments(projectId, vendorId)
    }

    override fun observePayablePayments(
        projectId: String,
        payableId: String,
        callerRole: UserRole
    ): Flow<List<SupplierPayment>> {
        val authResult = SupplierPaymentAuthorizationValidator.validateViewPayments(callerRole)
        if (authResult is DomainResult.Error) return emptyFlow()
        return dataSource.observePayablePayments(projectId, payableId)
    }

    override suspend fun updateDraftPayment(
        paymentId: String,
        amount: Money?,
        paymentMethod: SupplierPaymentMethod?,
        paymentReference: String?,
        paymentDate: Long?,
        notes: String?,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<SupplierPayment> = mutex.withLock {
        val authResult = SupplierPaymentAuthorizationValidator.validateUpdateDraft(callerRole)
        if (authResult is DomainResult.Error) return@withLock authResult

        val existing = dataSource.getPaymentById(paymentId)
            ?: return@withLock DomainResult.Error(message = "Supplier payment '$paymentId' not found.")

        if (existing.status != SupplierPaymentStatus.DRAFT) {
            return@withLock DomainResult.Error(
                message = "Only DRAFT supplier payments can be updated. Current status: ${existing.status.name}."
            )
        }

        val updatedAmount = amount ?: existing.amount
        val updatedMethod = paymentMethod ?: existing.paymentMethod
        val updatedRef = paymentReference ?: existing.paymentReference

        if (updatedMethod.requiresReference && updatedRef.isNullOrBlank()) {
            return@withLock DomainResult.Error(
                message = "Payment reference is required for payment method '${updatedMethod.defaultLabel}'."
            )
        }

        val updated = existing.copy(
            amount = updatedAmount,
            paymentMethod = updatedMethod,
            paymentReference = updatedRef?.trim(),
            paymentDate = paymentDate ?: existing.paymentDate,
            notes = notes ?: existing.notes,
            updatedBy = actorId,
            updatedAt = System.currentTimeMillis()
        )

        dataSource.updatePayment(updated)

        dataSource.insertActivityEvent(
            SupplierPaymentActivityEvent(
                eventId = UUID.randomUUID().toString(),
                paymentId = paymentId,
                projectId = existing.projectId,
                activityType = SupplierPaymentActivityType.PAYMENT_UPDATED,
                actorId = actorId,
                details = "Draft supplier payment #${existing.paymentNo} updated."
            )
        )

        DomainResult.Success(updated)
    }

    override suspend fun submitPayment(
        paymentId: String,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<SupplierPayment> = mutex.withLock {
        val authResult = SupplierPaymentAuthorizationValidator.validateSubmitPayment(callerRole)
        if (authResult is DomainResult.Error) return@withLock authResult

        val existing = dataSource.getPaymentById(paymentId)
            ?: return@withLock DomainResult.Error(message = "Supplier payment '$paymentId' not found.")

        val transitionResult = SupplierPaymentLifecycleValidator.validateTransition(
            existing.status,
            SupplierPaymentStatus.PENDING
        )
        if (transitionResult is DomainResult.Error) return@withLock transitionResult

        val now = System.currentTimeMillis()
        val updated = existing.copy(
            status = SupplierPaymentStatus.PENDING,
            submittedBy = actorId,
            submittedAt = now,
            updatedBy = actorId,
            updatedAt = now
        )

        dataSource.updatePayment(updated)

        dataSource.insertActivityEvent(
            SupplierPaymentActivityEvent(
                eventId = UUID.randomUUID().toString(),
                paymentId = paymentId,
                projectId = existing.projectId,
                activityType = SupplierPaymentActivityType.PAYMENT_SUBMITTED,
                actorId = actorId,
                details = "Supplier payment #${existing.paymentNo} submitted for approval."
            )
        )

        DomainResult.Success(updated)
    }

    override suspend fun approvePayment(
        paymentId: String,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<SupplierPayment> = mutex.withLock {
        val existing = dataSource.getPaymentById(paymentId)
            ?: return@withLock DomainResult.Error(message = "Supplier payment '$paymentId' not found.")

        val authResult = SupplierPaymentAuthorizationValidator.validateApprovePayment(
            callerRole = callerRole,
            creatorId = existing.createdBy,
            approverId = actorId
        )
        if (authResult is DomainResult.Error) return@withLock authResult

        val transitionResult = SupplierPaymentLifecycleValidator.validateTransition(
            existing.status,
            SupplierPaymentStatus.APPROVED
        )
        if (transitionResult is DomainResult.Error) return@withLock transitionResult

        val now = System.currentTimeMillis()
        val updated = existing.copy(
            status = SupplierPaymentStatus.APPROVED,
            approvedBy = actorId,
            approvedAt = now,
            updatedBy = actorId,
            updatedAt = now
        )

        dataSource.updatePayment(updated)

        dataSource.insertActivityEvent(
            SupplierPaymentActivityEvent(
                eventId = UUID.randomUUID().toString(),
                paymentId = paymentId,
                projectId = existing.projectId,
                activityType = SupplierPaymentActivityType.PAYMENT_APPROVED,
                actorId = actorId,
                details = "Supplier payment #${existing.paymentNo} approved."
            )
        )

        DomainResult.Success(updated)
    }

    override suspend fun rejectPayment(
        paymentId: String,
        rejectionReason: String,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<SupplierPayment> = mutex.withLock {
        val authResult = SupplierPaymentAuthorizationValidator.validateRejectPayment(callerRole)
        if (authResult is DomainResult.Error) return@withLock authResult

        if (rejectionReason.isBlank()) {
            return@withLock DomainResult.Error(message = "Rejection reason cannot be blank.")
        }

        val existing = dataSource.getPaymentById(paymentId)
            ?: return@withLock DomainResult.Error(message = "Supplier payment '$paymentId' not found.")

        val transitionResult = SupplierPaymentLifecycleValidator.validateTransition(
            existing.status,
            SupplierPaymentStatus.REJECTED
        )
        if (transitionResult is DomainResult.Error) return@withLock transitionResult

        val now = System.currentTimeMillis()
        val updated = existing.copy(
            status = SupplierPaymentStatus.REJECTED,
            rejectedBy = actorId,
            rejectedAt = now,
            cancellationReason = rejectionReason.trim(),
            updatedBy = actorId,
            updatedAt = now
        )

        dataSource.updatePayment(updated)

        dataSource.insertActivityEvent(
            SupplierPaymentActivityEvent(
                eventId = UUID.randomUUID().toString(),
                paymentId = paymentId,
                projectId = existing.projectId,
                activityType = SupplierPaymentActivityType.PAYMENT_REJECTED,
                actorId = actorId,
                details = "Supplier payment #${existing.paymentNo} rejected. Reason: ${rejectionReason.trim()}"
            )
        )

        DomainResult.Success(updated)
    }

    override suspend fun cancelPayment(
        paymentId: String,
        cancellationReason: String,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<SupplierPayment> = mutex.withLock {
        val authResult = SupplierPaymentAuthorizationValidator.validateCancelPayment(callerRole)
        if (authResult is DomainResult.Error) return@withLock authResult

        if (cancellationReason.isBlank()) {
            return@withLock DomainResult.Error(message = "Cancellation reason cannot be blank.")
        }

        val existing = dataSource.getPaymentById(paymentId)
            ?: return@withLock DomainResult.Error(message = "Supplier payment '$paymentId' not found.")

        val transitionResult = SupplierPaymentLifecycleValidator.validateTransition(
            existing.status,
            SupplierPaymentStatus.CANCELLED
        )
        if (transitionResult is DomainResult.Error) return@withLock transitionResult

        val now = System.currentTimeMillis()
        val updated = existing.copy(
            status = SupplierPaymentStatus.CANCELLED,
            cancelledBy = actorId,
            cancelledAt = now,
            cancellationReason = cancellationReason.trim(),
            updatedBy = actorId,
            updatedAt = now
        )

        dataSource.updatePayment(updated)

        dataSource.insertActivityEvent(
            SupplierPaymentActivityEvent(
                eventId = UUID.randomUUID().toString(),
                paymentId = paymentId,
                projectId = existing.projectId,
                activityType = SupplierPaymentActivityType.PAYMENT_CANCELLED,
                actorId = actorId,
                details = "Supplier payment #${existing.paymentNo} cancelled. Reason: ${cancellationReason.trim()}"
            )
        )

        DomainResult.Success(updated)
    }

    override suspend fun postPayment(
        paymentId: String,
        accountHead: String?,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<SupplierPayment> = mutex.withLock {
        val existing = dataSource.getPaymentById(paymentId)
            ?: return@withLock DomainResult.Error(message = "Supplier payment '$paymentId' not found.")

        if (existing.status.isTerminal) {
            return@withLock DomainResult.Error(
                message = "Terminal supplier payment '${existing.paymentNo}' (${existing.status.name}) cannot be posted."
            )
        }

        // Separation of duties check
        val authResult = SupplierPaymentAuthorizationValidator.validatePostPayment(
            callerRole = callerRole,
            creatorId = existing.createdBy,
            posterId = actorId
        )
        if (authResult is DomainResult.Error) return@withLock authResult

        val transitionResult = SupplierPaymentLifecycleValidator.validateTransition(
            existing.status,
            SupplierPaymentStatus.POSTED
        )
        if (transitionResult is DomainResult.Error) return@withLock transitionResult

        // Re-read current Vendor Payable
        val payableRes = vendorPayableRepository.getPayableById(existing.payableId, callerRole)
        if (payableRes is DomainResult.Error) return@withLock payableRes
        val payable = (payableRes as DomainResult.Success).data

        if (existing.amount > payable.outstandingAmount) {
            return@withLock DomainResult.Error(
                message = "Cannot post supplier payment: amount (${existing.amount.formatted()}) exceeds current outstanding liability of ${payable.outstandingAmount.formatted()} on payable #${payable.payableNo}."
            )
        }

        val previousOutstanding = payable.outstandingAmount
        val newOutstanding = previousOutstanding - existing.amount
        val now = System.currentTimeMillis()

        // 1. Record Settlement on Step 04 Vendor Payable
        val settleResult = vendorPayableRepository.recordSettlement(
            payableId = existing.payableId,
            settlementAmount = existing.amount,
            actorId = actorId,
            callerRole = callerRole
        )
        if (settleResult is DomainResult.Error) return@withLock settleResult

        // 2. Create and Post Financial Transaction to Canonical Step 01 Ledger
        val resolvedAccountHead = accountHead ?: when (existing.paymentMethod) {
            SupplierPaymentMethod.CASH -> "CASH_IN_HAND"
            SupplierPaymentMethod.BANK_TRANSFER,
            SupplierPaymentMethod.CHEQUE -> "BANK_ACCOUNT"
            SupplierPaymentMethod.MOBILE_BANKING -> "MOBILE_WALLET"
            SupplierPaymentMethod.CARD -> "CARD_SETTLEMENT"
            SupplierPaymentMethod.OTHER -> "CASH_AND_BANK"
        }

        val txnCreateRes = financialTransactionRepository.createTransaction(
            projectId = existing.projectId,
            transactionType = FinancialTransactionType.PAYMENT,
            entryType = FinancialEntryType.DEBIT,
            amount = existing.amount,
            currency = existing.currency,
            referenceType = FinancialReferenceType.PAYMENT,
            referenceId = existing.paymentId,
            vendorId = existing.vendorId,
            description = "Supplier payment disbursement for payable #${payable.payableNo} (${existing.paymentMethod.defaultLabel})",
            notes = existing.notes,
            actorId = existing.createdBy,
            callerRole = callerRole
        )
        if (txnCreateRes is DomainResult.Error) return@withLock txnCreateRes
        val financialTxn = (txnCreateRes as DomainResult.Success).data

        // Post financial transaction
        financialTransactionRepository.submitTransaction(financialTxn.transactionId, existing.createdBy, callerRole)
        val txnPostRes = financialTransactionRepository.postTransaction(
            transactionId = financialTxn.transactionId,
            accountHead = resolvedAccountHead,
            actorId = actorId,
            callerRole = callerRole
        )
        if (txnPostRes is DomainResult.Error) return@withLock txnPostRes

        // 3. Create Immutable Settlement Record
        val settlement = SupplierPaymentSettlement(
            settlementId = UUID.randomUUID().toString(),
            projectId = existing.projectId,
            paymentId = existing.paymentId,
            payableId = existing.payableId,
            vendorId = existing.vendorId,
            settledAmount = existing.amount,
            previousOutstanding = previousOutstanding,
            newOutstanding = newOutstanding,
            settlementDate = now,
            financialTransactionId = financialTxn.transactionId,
            createdBy = actorId,
            createdAt = now
        )
        dataSource.insertSettlement(settlement)

        // 4. Update Payment to POSTED
        val postedPayment = existing.copy(
            status = SupplierPaymentStatus.POSTED,
            financialTransactionId = financialTxn.transactionId,
            approvedBy = existing.approvedBy ?: actorId,
            approvedAt = existing.approvedAt ?: now,
            postedAt = now,
            updatedBy = actorId,
            updatedAt = now
        )
        dataSource.updatePayment(postedPayment)

        // 5. Emit Audit Events
        dataSource.insertActivityEvent(
            SupplierPaymentActivityEvent(
                eventId = UUID.randomUUID().toString(),
                paymentId = existing.paymentId,
                projectId = existing.projectId,
                activityType = SupplierPaymentActivityType.PAYMENT_POSTED,
                actorId = actorId,
                details = "Supplier payment #${existing.paymentNo} posted to financial ledger (#${financialTxn.transactionNo}) under account head '$resolvedAccountHead'."
            )
        )

        dataSource.insertActivityEvent(
            SupplierPaymentActivityEvent(
                eventId = UUID.randomUUID().toString(),
                paymentId = existing.paymentId,
                projectId = existing.projectId,
                activityType = SupplierPaymentActivityType.PAYMENT_SETTLEMENT_RECORDED,
                actorId = actorId,
                details = "Settled ${existing.amount.formatted()} on payable #${payable.payableNo}. Remaining balance: ${newOutstanding.formatted()}."
            )
        )

        DomainResult.Success(postedPayment)
    }

    override suspend fun getSettlementsByPayable(
        payableId: String,
        callerRole: UserRole
    ): DomainResult<List<SupplierPaymentSettlement>> = mutex.withLock {
        val authResult = SupplierPaymentAuthorizationValidator.validateViewPayments(callerRole)
        if (authResult is DomainResult.Error) return@withLock authResult

        val list = dataSource.getSettlementsByPayable(payableId)
        DomainResult.Success(list)
    }

    override suspend fun getSettlementsByPayment(
        paymentId: String,
        callerRole: UserRole
    ): DomainResult<List<SupplierPaymentSettlement>> = mutex.withLock {
        val authResult = SupplierPaymentAuthorizationValidator.validateViewPayments(callerRole)
        if (authResult is DomainResult.Error) return@withLock authResult

        val list = dataSource.getSettlementsByPayment(paymentId)
        DomainResult.Success(list)
    }

    override fun observeSettlements(
        projectId: String,
        callerRole: UserRole
    ): Flow<List<SupplierPaymentSettlement>> {
        val authResult = SupplierPaymentAuthorizationValidator.validateViewPayments(callerRole)
        if (authResult is DomainResult.Error) return emptyFlow()
        return dataSource.observeSettlements(projectId)
    }

    override suspend fun getActivityEvents(
        paymentId: String,
        callerRole: UserRole
    ): DomainResult<List<SupplierPaymentActivityEvent>> = mutex.withLock {
        val authResult = SupplierPaymentAuthorizationValidator.validateViewPayments(callerRole)
        if (authResult is DomainResult.Error) return@withLock authResult

        val list = dataSource.getActivityEvents(paymentId)
        DomainResult.Success(list)
    }
}
