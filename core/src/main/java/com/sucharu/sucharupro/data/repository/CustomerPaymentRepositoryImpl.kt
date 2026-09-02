package com.sucharu.sucharupro.data.repository

import com.sucharu.sucharupro.data.datasource.CustomerPaymentDataSource
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.common.Money
import com.sucharu.sucharupro.domain.model.finance.CustomerPayment
import com.sucharu.sucharupro.domain.model.finance.CustomerPaymentActivityEvent
import com.sucharu.sucharupro.domain.model.finance.CustomerPaymentActivityType
import com.sucharu.sucharupro.domain.model.finance.CustomerPaymentMethod
import com.sucharu.sucharupro.domain.model.finance.CustomerPaymentReceipt
import com.sucharu.sucharupro.domain.model.finance.CustomerPaymentStatus
import com.sucharu.sucharupro.domain.model.finance.CustomerReceivableStatus
import com.sucharu.sucharupro.domain.model.finance.FinancialEntryType
import com.sucharu.sucharupro.domain.model.finance.FinancialReferenceType
import com.sucharu.sucharupro.domain.model.finance.FinancialTransactionType
import com.sucharu.sucharupro.domain.model.user.UserRole
import com.sucharu.sucharupro.domain.repository.CustomerPaymentRepository
import com.sucharu.sucharupro.domain.repository.CustomerReceivableRepository
import com.sucharu.sucharupro.domain.repository.FinancialTransactionRepository
import com.sucharu.sucharupro.domain.validation.CustomerPaymentAuthorizationValidator
import com.sucharu.sucharupro.domain.validation.CustomerPaymentLifecycleValidator
import com.sucharu.sucharupro.domain.validation.CustomerPaymentValidator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID

/**
 * Thread-safe implementation of CustomerPaymentRepository with non-reentrant mutex locking (Module 09 Step 03).
 */
class CustomerPaymentRepositoryImpl(
    private val dataSource: CustomerPaymentDataSource,
    private val receivableRepository: CustomerReceivableRepository,
    private val financialTransactionRepository: FinancialTransactionRepository
) : CustomerPaymentRepository {

    private val mutex = Mutex()

    override suspend fun createPayment(
        projectId: String,
        customerId: String,
        receivableId: String,
        amount: Money,
        currency: String,
        paymentMethod: CustomerPaymentMethod,
        paymentReference: String?,
        paymentDate: Long,
        idempotencyKey: String?,
        notes: String?,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<CustomerPayment> = mutex.withLock {
        val authResult = CustomerPaymentAuthorizationValidator.validateCreateDraftPayment(callerRole)
        if (authResult is DomainResult.Error) return@withLock authResult

        // Idempotency check: if key matches, return existing payment
        if (!idempotencyKey.isNullOrBlank()) {
            val existingIdempotent = dataSource.getPaymentByIdempotencyKey(projectId, idempotencyKey.trim())
            if (existingIdempotent != null) {
                return@withLock DomainResult.Success(existingIdempotent)
            }
        }

        // Validate Receivable
        val recRes = receivableRepository.getReceivableById(receivableId, callerRole)
        if (recRes is DomainResult.Error) return@withLock recRes
        val receivable = (recRes as DomainResult.Success).data

        if (receivable.projectId != projectId) {
            return@withLock DomainResult.Error(message = "Receivable '$receivableId' does not belong to project '$projectId'.")
        }
        if (receivable.customerId != customerId) {
            return@withLock DomainResult.Error(message = "Receivable '$receivableId' belongs to customer '${receivable.customerId}', not '$customerId'.")
        }
        if (receivable.status == CustomerReceivableStatus.SETTLED || receivable.status == CustomerReceivableStatus.CANCELLED) {
            return@withLock DomainResult.Error(message = "Cannot record payment against receivable in status '${receivable.status}'.")
        }
        if (amount > receivable.outstandingAmount) {
            return@withLock DomainResult.Error(message = "Payment amount '$amount' exceeds receivable outstanding amount '${receivable.outstandingAmount}'.")
        }

        // Validate Payment Reference Uniqueness
        if (!paymentReference.isNullOrBlank()) {
            val existingRef = dataSource.getActivePaymentByReference(
                projectId = projectId,
                customerId = customerId,
                paymentMethod = paymentMethod,
                paymentReference = paymentReference.trim()
            )
            if (existingRef != null && !existingRef.status.isTerminal) {
                return@withLock DomainResult.Error(
                    message = "Duplicate active customer payment found with reference '$paymentReference' for customer '$customerId' (Payment #${existingRef.paymentNo})."
                )
            }
        }

        val paymentId = "PAY-${UUID.randomUUID()}"
        val paymentNo = dataSource.generateNextPaymentNo(projectId)
        val now = System.currentTimeMillis()
        val initialStatus = if (callerRole == UserRole.STAFF) CustomerPaymentStatus.DRAFT else CustomerPaymentStatus.PENDING

        val payment = CustomerPayment(
            paymentId = paymentId,
            paymentNo = paymentNo,
            projectId = projectId,
            customerId = customerId,
            receivableId = receivableId,
            amount = amount,
            currency = currency,
            paymentMethod = paymentMethod,
            paymentReference = paymentReference?.trim()?.ifEmpty { null },
            paymentDate = paymentDate,
            idempotencyKey = idempotencyKey?.trim()?.ifEmpty { null },
            status = initialStatus,
            notes = notes?.trim()?.ifEmpty { null },
            createdBy = actorId,
            createdAt = now,
            updatedAt = now
        )

        val validationResult = CustomerPaymentValidator.validatePayment(payment, projectId)
        if (validationResult is DomainResult.Error) return@withLock validationResult

        dataSource.insertPayment(payment)
        dataSource.insertActivityEvent(
            CustomerPaymentActivityEvent(
                eventId = UUID.randomUUID().toString(),
                paymentId = paymentId,
                projectId = projectId,
                activityType = CustomerPaymentActivityType.PAYMENT_CREATED,
                actorId = actorId,
                details = "Customer payment '$paymentNo' created for amount ${amount.formatted()} ($initialStatus)."
            )
        )

        DomainResult.Success(payment)
    }

    override suspend fun updateDraftPayment(
        paymentId: String,
        amount: Money?,
        paymentMethod: CustomerPaymentMethod?,
        paymentReference: String?,
        paymentDate: Long?,
        notes: String?,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<CustomerPayment> = mutex.withLock {
        val authResult = CustomerPaymentAuthorizationValidator.validateUpdatePayment(callerRole)
        if (authResult is DomainResult.Error) return@withLock authResult

        val existing = dataSource.getPaymentById(paymentId)
            ?: return@withLock DomainResult.Error(message = "Customer payment '$paymentId' not found.")

        if (existing.status != CustomerPaymentStatus.DRAFT && existing.status != CustomerPaymentStatus.PENDING) {
            return@withLock DomainResult.Error(message = "Only DRAFT or PENDING payments may be updated. Current status: ${existing.status}")
        }

        val now = System.currentTimeMillis()
        val updated = existing.copy(
            amount = amount ?: existing.amount,
            paymentMethod = paymentMethod ?: existing.paymentMethod,
            paymentReference = paymentReference?.trim() ?: existing.paymentReference,
            paymentDate = paymentDate ?: existing.paymentDate,
            notes = notes?.trim() ?: existing.notes,
            updatedAt = now
        )

        val immutabilityResult = CustomerPaymentValidator.validateImmutabilityOnUpdate(existing, updated)
        if (immutabilityResult is DomainResult.Error) return@withLock immutabilityResult

        val valResult = CustomerPaymentValidator.validatePayment(updated, existing.projectId)
        if (valResult is DomainResult.Error) return@withLock valResult

        dataSource.updatePayment(updated)
        dataSource.insertActivityEvent(
            CustomerPaymentActivityEvent(
                eventId = UUID.randomUUID().toString(),
                paymentId = paymentId,
                projectId = existing.projectId,
                activityType = CustomerPaymentActivityType.PAYMENT_UPDATED,
                actorId = actorId,
                details = "Customer payment '${existing.paymentNo}' updated."
            )
        )

        DomainResult.Success(updated)
    }

    override suspend fun submitPayment(
        paymentId: String,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<CustomerPayment> = mutex.withLock {
        val authResult = CustomerPaymentAuthorizationValidator.validateUpdatePayment(callerRole)
        if (authResult is DomainResult.Error) return@withLock authResult

        val existing = dataSource.getPaymentById(paymentId)
            ?: return@withLock DomainResult.Error(message = "Customer payment '$paymentId' not found.")

        val transitionResult = CustomerPaymentLifecycleValidator.validateTransition(existing.status, CustomerPaymentStatus.PENDING)
        if (transitionResult is DomainResult.Error) return@withLock transitionResult

        val now = System.currentTimeMillis()
        val updated = existing.copy(
            status = CustomerPaymentStatus.PENDING,
            updatedAt = now
        )

        dataSource.updatePayment(updated)
        dataSource.insertActivityEvent(
            CustomerPaymentActivityEvent(
                eventId = UUID.randomUUID().toString(),
                paymentId = paymentId,
                projectId = existing.projectId,
                activityType = CustomerPaymentActivityType.PAYMENT_SUBMITTED,
                actorId = actorId,
                details = "Customer payment '${existing.paymentNo}' submitted for posting approval."
            )
        )

        DomainResult.Success(updated)
    }

    override suspend fun postPayment(
        paymentId: String,
        accountHead: String?,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<CustomerPayment> = mutex.withLock {
        val existing = dataSource.getPaymentById(paymentId)
            ?: return@withLock DomainResult.Error(message = "Customer payment '$paymentId' not found.")

        val authResult = CustomerPaymentAuthorizationValidator.validatePostPayment(callerRole, existing.createdBy, actorId)
        if (authResult is DomainResult.Error) return@withLock authResult

        val transitionResult = CustomerPaymentLifecycleValidator.validateTransition(existing.status, CustomerPaymentStatus.POSTED)
        if (transitionResult is DomainResult.Error) return@withLock transitionResult

        val now = System.currentTimeMillis()

        // 1. Post to Financial Transaction Ledger
        val txRes = financialTransactionRepository.createTransaction(
            projectId = existing.projectId,
            transactionType = FinancialTransactionType.RECEIPT,
            entryType = FinancialEntryType.CREDIT,
            amount = existing.amount,
            currency = existing.currency,
            referenceType = FinancialReferenceType.PAYMENT,
            referenceId = existing.paymentId,
            customerId = existing.customerId,
            description = "Customer payment receipt for #${existing.paymentNo}",
            notes = existing.notes,
            actorId = existing.createdBy,
            callerRole = UserRole.STAFF
        )
        if (txRes is DomainResult.Error) return@withLock txRes
        val financialTx = (txRes as DomainResult.Success).data
        financialTransactionRepository.submitTransaction(financialTx.transactionId, existing.createdBy, UserRole.STAFF)
        val postTxRes = financialTransactionRepository.postTransaction(financialTx.transactionId, accountHead ?: "CUSTOMER_PAYMENT", actorId, callerRole)
        if (postTxRes is DomainResult.Error) return@withLock postTxRes

        // 2. Issue Receipt
        val receiptId = "RCT-${UUID.randomUUID()}"
        val receiptNo = dataSource.generateNextReceiptNo(existing.projectId)
        val receipt = CustomerPaymentReceipt(
            receiptId = receiptId,
            receiptNo = receiptNo,
            projectId = existing.projectId,
            paymentId = existing.paymentId,
            customerId = existing.customerId,
            receivableId = existing.receivableId,
            amount = existing.amount,
            currency = existing.currency,
            paymentMethod = existing.paymentMethod,
            paymentReference = existing.paymentReference,
            paymentDate = existing.paymentDate,
            issuedBy = actorId,
            issuedAt = now,
            notes = existing.notes
        )
        dataSource.insertReceipt(receipt)

        // 3. Settle Receivable
        val settleRes = receivableRepository.recordSettlement(
            receivableId = existing.receivableId,
            settlementAmount = existing.amount,
            actorId = actorId,
            callerRole = callerRole
        )
        if (settleRes is DomainResult.Error) return@withLock settleRes

        // 4. Update Payment record to POSTED
        val updated = existing.copy(
            status = CustomerPaymentStatus.POSTED,
            postedBy = actorId,
            postedAt = now,
            receiptId = receiptId,
            financialTransactionId = financialTx.transactionId,
            updatedAt = now
        )

        dataSource.updatePayment(updated)
        dataSource.insertActivityEvent(
            CustomerPaymentActivityEvent(
                eventId = UUID.randomUUID().toString(),
                paymentId = paymentId,
                projectId = existing.projectId,
                activityType = CustomerPaymentActivityType.PAYMENT_POSTED,
                actorId = actorId,
                details = "Customer payment '${existing.paymentNo}' posted. Receipt '$receiptNo' issued."
            )
        )
        dataSource.insertActivityEvent(
            CustomerPaymentActivityEvent(
                eventId = UUID.randomUUID().toString(),
                paymentId = paymentId,
                projectId = existing.projectId,
                activityType = CustomerPaymentActivityType.RECEIPT_ISSUED,
                actorId = actorId,
                details = "Receipt '$receiptNo' issued for payment '${existing.paymentNo}'."
            )
        )

        DomainResult.Success(updated)
    }

    override suspend fun rejectPayment(
        paymentId: String,
        rejectionReason: String,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<CustomerPayment> = mutex.withLock {
        val authResult = CustomerPaymentAuthorizationValidator.validateRejectPayment(callerRole)
        if (authResult is DomainResult.Error) return@withLock authResult

        if (rejectionReason.isBlank()) {
            return@withLock DomainResult.Error(message = "Rejection reason cannot be blank.")
        }

        val existing = dataSource.getPaymentById(paymentId)
            ?: return@withLock DomainResult.Error(message = "Customer payment '$paymentId' not found.")

        val transitionResult = CustomerPaymentLifecycleValidator.validateTransition(existing.status, CustomerPaymentStatus.REJECTED)
        if (transitionResult is DomainResult.Error) return@withLock transitionResult

        val now = System.currentTimeMillis()
        val updated = existing.copy(
            status = CustomerPaymentStatus.REJECTED,
            rejectedBy = actorId,
            rejectionReason = rejectionReason.trim(),
            updatedAt = now
        )

        dataSource.updatePayment(updated)
        dataSource.insertActivityEvent(
            CustomerPaymentActivityEvent(
                eventId = UUID.randomUUID().toString(),
                paymentId = paymentId,
                projectId = existing.projectId,
                activityType = CustomerPaymentActivityType.PAYMENT_REJECTED,
                actorId = actorId,
                details = "Customer payment '${existing.paymentNo}' rejected: ${rejectionReason.trim()}"
            )
        )

        DomainResult.Success(updated)
    }

    override suspend fun cancelPayment(
        paymentId: String,
        cancellationReason: String,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<CustomerPayment> = mutex.withLock {
        val authResult = CustomerPaymentAuthorizationValidator.validateCancelPayment(callerRole)
        if (authResult is DomainResult.Error) return@withLock authResult

        if (cancellationReason.isBlank()) {
            return@withLock DomainResult.Error(message = "Cancellation reason cannot be blank.")
        }

        val existing = dataSource.getPaymentById(paymentId)
            ?: return@withLock DomainResult.Error(message = "Customer payment '$paymentId' not found.")

        val transitionResult = CustomerPaymentLifecycleValidator.validateTransition(existing.status, CustomerPaymentStatus.CANCELLED)
        if (transitionResult is DomainResult.Error) return@withLock transitionResult

        val now = System.currentTimeMillis()
        val updated = existing.copy(
            status = CustomerPaymentStatus.CANCELLED,
            cancelledBy = actorId,
            cancelledAt = now,
            cancellationReason = cancellationReason.trim(),
            updatedAt = now
        )

        dataSource.updatePayment(updated)
        dataSource.insertActivityEvent(
            CustomerPaymentActivityEvent(
                eventId = UUID.randomUUID().toString(),
                paymentId = paymentId,
                projectId = existing.projectId,
                activityType = CustomerPaymentActivityType.PAYMENT_CANCELLED,
                actorId = actorId,
                details = "Customer payment '${existing.paymentNo}' cancelled: ${cancellationReason.trim()}"
            )
        )

        DomainResult.Success(updated)
    }

    override suspend fun getPaymentById(
        paymentId: String,
        callerRole: UserRole,
        authenticatedCustomerId: String?
    ): DomainResult<CustomerPayment> = mutex.withLock {
        val payment = dataSource.getPaymentById(paymentId)
            ?: return@withLock DomainResult.Error(message = "Customer payment '$paymentId' not found.")

        val authResult = CustomerPaymentAuthorizationValidator.validateViewPayments(
            callerRole = callerRole,
            requestedCustomerId = payment.customerId,
            authenticatedCustomerId = authenticatedCustomerId
        )
        if (authResult is DomainResult.Error) return@withLock authResult

        DomainResult.Success(payment)
    }

    override suspend fun getPaymentByIdempotencyKey(
        projectId: String,
        idempotencyKey: String,
        callerRole: UserRole
    ): DomainResult<CustomerPayment?> = mutex.withLock {
        val payment = dataSource.getPaymentByIdempotencyKey(projectId, idempotencyKey)
        DomainResult.Success(payment)
    }

    override suspend fun getReceiptById(
        receiptId: String,
        callerRole: UserRole,
        authenticatedCustomerId: String?
    ): DomainResult<CustomerPaymentReceipt> = mutex.withLock {
        val receipt = dataSource.getReceiptById(receiptId)
            ?: return@withLock DomainResult.Error(message = "Customer payment receipt '$receiptId' not found.")

        val authResult = CustomerPaymentAuthorizationValidator.validateViewPayments(
            callerRole = callerRole,
            requestedCustomerId = receipt.customerId,
            authenticatedCustomerId = authenticatedCustomerId
        )
        if (authResult is DomainResult.Error) return@withLock authResult

        DomainResult.Success(receipt)
    }

    override suspend fun getReceiptByPaymentId(
        paymentId: String,
        callerRole: UserRole,
        authenticatedCustomerId: String?
    ): DomainResult<CustomerPaymentReceipt> = mutex.withLock {
        val receipt = dataSource.getReceiptByPaymentId(paymentId)
            ?: return@withLock DomainResult.Error(message = "Customer payment receipt for payment '$paymentId' not found.")

        val authResult = CustomerPaymentAuthorizationValidator.validateViewPayments(
            callerRole = callerRole,
            requestedCustomerId = receipt.customerId,
            authenticatedCustomerId = authenticatedCustomerId
        )
        if (authResult is DomainResult.Error) return@withLock authResult

        DomainResult.Success(receipt)
    }

    override fun observePayments(
        projectId: String,
        callerRole: UserRole
    ): Flow<List<CustomerPayment>> {
        return dataSource.observePayments(projectId)
    }

    override fun observeCustomerPayments(
        projectId: String,
        customerId: String,
        callerRole: UserRole,
        authenticatedCustomerId: String?
    ): Flow<List<CustomerPayment>> {
        return dataSource.observeCustomerPayments(projectId, customerId)
    }

    override fun observeCustomerReceipts(
        projectId: String,
        customerId: String,
        callerRole: UserRole,
        authenticatedCustomerId: String?
    ): Flow<List<CustomerPaymentReceipt>> {
        return dataSource.observeCustomerReceipts(projectId, customerId)
    }

    override suspend fun getActivityEvents(
        paymentId: String,
        callerRole: UserRole
    ): DomainResult<List<CustomerPaymentActivityEvent>> = mutex.withLock {
        val events = dataSource.getActivityEvents(paymentId)
        DomainResult.Success(events)
    }
}
