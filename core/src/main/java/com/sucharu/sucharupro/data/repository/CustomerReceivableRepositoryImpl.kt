package com.sucharu.sucharupro.data.repository

import com.sucharu.sucharupro.data.datasource.CustomerReceivableDataSource
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.common.Money
import com.sucharu.sucharupro.domain.model.finance.CustomerDueSummary
import com.sucharu.sucharupro.domain.model.finance.CustomerReceivable
import com.sucharu.sucharupro.domain.model.finance.CustomerReceivableActivityEvent
import com.sucharu.sucharupro.domain.model.finance.CustomerReceivableActivityType
import com.sucharu.sucharupro.domain.model.finance.CustomerReceivableStatus
import com.sucharu.sucharupro.domain.model.finance.FinancialReferenceType
import com.sucharu.sucharupro.domain.model.finance.ReceivableAgingBucket
import com.sucharu.sucharupro.domain.model.user.UserRole
import com.sucharu.sucharupro.domain.repository.CustomerReceivableRepository
import com.sucharu.sucharupro.domain.service.finance.CustomerReceivableAgingCalculator
import com.sucharu.sucharupro.domain.validation.CustomerReceivableAuthorizationValidator
import com.sucharu.sucharupro.domain.validation.CustomerReceivableLifecycleValidator
import com.sucharu.sucharupro.domain.validation.CustomerReceivableValidator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID

/**
 * Production-grade repository implementation for Customer Receivable & Due Management (Module 09 Step 02).
 */
class CustomerReceivableRepositoryImpl(
    private val dataSource: CustomerReceivableDataSource
) : CustomerReceivableRepository {

    private val mutex = Mutex()

    override suspend fun createReceivable(
        projectId: String,
        customerId: String,
        referenceType: FinancialReferenceType,
        referenceId: String,
        financialTransactionId: String?,
        originalAmount: Money,
        currency: String,
        dueDate: Long,
        description: String,
        notes: String?,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<CustomerReceivable> = mutex.withLock {
        val authResult = CustomerReceivableAuthorizationValidator.validateCreateReceivable(callerRole)
        if (authResult is DomainResult.Error) return authResult

        if (projectId.isBlank()) return DomainResult.Error(message = "Project ID cannot be blank.")
        if (customerId.isBlank()) return DomainResult.Error(message = "Customer ID cannot be blank.")
        if (referenceId.isBlank()) return DomainResult.Error(message = "Reference ID cannot be blank.")
        if (!originalAmount.isPositive()) return DomainResult.Error(message = "Original amount must be strictly positive (> 0).")

        // Duplicate prevention for same obligation reference
        val existingWithRef = dataSource.getReceivablesByReference(projectId, referenceId)
            .firstOrNull { it.referenceType == referenceType && it.status != CustomerReceivableStatus.CANCELLED }
        if (existingWithRef != null) {
            return DomainResult.Error(
                message = "A receivable obligation already exists for reference '$referenceType #$referenceId' (Receivable No: ${existingWithRef.receivableNo})."
            )
        }

        val now = System.currentTimeMillis()
        val receivableId = "REC-${UUID.randomUUID()}"
        val receivableNo = "RCV-${now.toString().takeLast(6)}-${(100..999).random()}"

        val agingBucket = CustomerReceivableAgingCalculator.calculateAgingBucket(dueDate, now)
        val initialStatus = if (now > dueDate) CustomerReceivableStatus.OVERDUE else CustomerReceivableStatus.OPEN

        val receivable = CustomerReceivable(
            receivableId = receivableId,
            receivableNo = receivableNo,
            projectId = projectId,
            customerId = customerId,
            referenceType = referenceType,
            referenceId = referenceId,
            financialTransactionId = financialTransactionId,
            originalAmount = originalAmount,
            settledAmount = Money.ZERO,
            currency = currency,
            dueDate = dueDate,
            status = initialStatus,
            agingBucket = agingBucket,
            description = description.trim(),
            notes = notes?.trim(),
            createdBy = actorId,
            createdAt = now,
            updatedAt = now
        )

        val validationResult = CustomerReceivableValidator.validateReceivable(receivable, projectId)
        if (validationResult is DomainResult.Error) return validationResult

        dataSource.insertReceivable(receivable)
        dataSource.insertActivityEvent(
            CustomerReceivableActivityEvent(
                eventId = UUID.randomUUID().toString(),
                receivableId = receivableId,
                projectId = projectId,
                activityType = CustomerReceivableActivityType.RECEIVABLE_CREATED,
                actorId = actorId,
                details = "Customer receivable obligation '$receivableNo' created for amount ${originalAmount.formatted()}."
            )
        )

        DomainResult.Success(receivable)
    }

    override suspend fun updateReceivable(
        receivableId: String,
        dueDate: Long?,
        description: String?,
        notes: String?,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<CustomerReceivable> = mutex.withLock {
        val authResult = CustomerReceivableAuthorizationValidator.validateUpdateReceivable(callerRole)
        if (authResult is DomainResult.Error) return authResult

        val existing = dataSource.getReceivableById(receivableId)
            ?: return DomainResult.Error(message = "Customer receivable '$receivableId' not found.")

        if (existing.status.isTerminal) {
            return DomainResult.Error(
                message = "Terminal customer receivable '${existing.receivableNo}' (${existing.status}) cannot be updated."
            )
        }

        val now = System.currentTimeMillis()
        val newDueDate = dueDate ?: existing.dueDate
        val newAgingBucket = CustomerReceivableAgingCalculator.calculateAgingBucket(newDueDate, now)
        val effectiveStatus = if (existing.status == CustomerReceivableStatus.OPEN || existing.status == CustomerReceivableStatus.OVERDUE) {
            if (now > newDueDate) CustomerReceivableStatus.OVERDUE else CustomerReceivableStatus.OPEN
        } else {
            existing.status
        }

        val updated = existing.copy(
            dueDate = newDueDate,
            status = effectiveStatus,
            agingBucket = newAgingBucket,
            description = description?.trim() ?: existing.description,
            notes = notes?.trim() ?: existing.notes,
            updatedAt = now
        )

        val immutabilityResult = CustomerReceivableValidator.validateImmutabilityOnUpdate(existing, updated)
        if (immutabilityResult is DomainResult.Error) return immutabilityResult

        val validationResult = CustomerReceivableValidator.validateReceivable(updated, existing.projectId)
        if (validationResult is DomainResult.Error) return validationResult

        dataSource.updateReceivable(updated)
        dataSource.insertActivityEvent(
            CustomerReceivableActivityEvent(
                eventId = UUID.randomUUID().toString(),
                receivableId = receivableId,
                projectId = existing.projectId,
                activityType = CustomerReceivableActivityType.RECEIVABLE_UPDATED,
                actorId = actorId,
                details = "Customer receivable '${existing.receivableNo}' details updated."
            )
        )

        DomainResult.Success(updated)
    }

    override suspend fun recordSettlement(
        receivableId: String,
        settlementAmount: Money,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<CustomerReceivable> = mutex.withLock {
        val authResult = CustomerReceivableAuthorizationValidator.validateRecordSettlement(callerRole)
        if (authResult is DomainResult.Error) return authResult

        if (!settlementAmount.isPositive()) {
            return DomainResult.Error(message = "Settlement amount must be strictly positive (> 0).")
        }

        val existing = dataSource.getReceivableById(receivableId)
            ?: return DomainResult.Error(message = "Customer receivable '$receivableId' not found.")

        if (existing.status.isTerminal) {
            return DomainResult.Error(
                message = "Cannot record settlement on terminal receivable '${existing.receivableNo}' (${existing.status})."
            )
        }

        val newSettledAmount = existing.settledAmount + settlementAmount
        if (newSettledAmount > existing.originalAmount) {
            return DomainResult.Error(
                message = "Settlement of ${settlementAmount.formatted()} exceeds remaining outstanding balance of ${existing.outstandingAmount.formatted()}."
            )
        }

        val now = System.currentTimeMillis()
        val isFullySettled = newSettledAmount == existing.originalAmount
        val targetStatus = if (isFullySettled) CustomerReceivableStatus.SETTLED else CustomerReceivableStatus.PARTIALLY_SETTLED

        val transitionResult = CustomerReceivableLifecycleValidator.validateTransition(existing.status, targetStatus)
        if (transitionResult is DomainResult.Error) return transitionResult

        val updated = existing.copy(
            settledAmount = newSettledAmount,
            status = targetStatus,
            settledAt = if (isFullySettled) now else null,
            updatedAt = now
        )

        val validationResult = CustomerReceivableValidator.validateReceivable(updated, existing.projectId)
        if (validationResult is DomainResult.Error) return validationResult

        dataSource.updateReceivable(updated)
        dataSource.insertActivityEvent(
            CustomerReceivableActivityEvent(
                eventId = UUID.randomUUID().toString(),
                receivableId = receivableId,
                projectId = existing.projectId,
                activityType = CustomerReceivableActivityType.RECEIVABLE_SETTLEMENT_RECORDED,
                actorId = actorId,
                details = "Settlement of ${settlementAmount.formatted()} recorded. Outstanding due: ${updated.outstandingAmount.formatted()}."
            )
        )

        DomainResult.Success(updated)
    }

    override suspend fun cancelReceivable(
        receivableId: String,
        cancellationReason: String,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<CustomerReceivable> = mutex.withLock {
        val authResult = CustomerReceivableAuthorizationValidator.validateCancelReceivable(callerRole)
        if (authResult is DomainResult.Error) return authResult

        if (cancellationReason.trim().isEmpty()) {
            return DomainResult.Error(message = "Cancellation reason cannot be blank.")
        }

        val existing = dataSource.getReceivableById(receivableId)
            ?: return DomainResult.Error(message = "Customer receivable '$receivableId' not found.")

        if (existing.settledAmount.isPositive()) {
            return DomainResult.Error(
                message = "Cannot cancel receivable '${existing.receivableNo}' because a settlement of ${existing.settledAmount.formatted()} has already been applied."
            )
        }

        val transitionResult = CustomerReceivableLifecycleValidator.validateTransition(
            existing.status,
            CustomerReceivableStatus.CANCELLED
        )
        if (transitionResult is DomainResult.Error) return transitionResult

        val now = System.currentTimeMillis()
        val updated = existing.copy(
            status = CustomerReceivableStatus.CANCELLED,
            cancelledAt = now,
            cancellationReason = cancellationReason.trim(),
            updatedAt = now
        )

        val validationResult = CustomerReceivableValidator.validateReceivable(updated, existing.projectId)
        if (validationResult is DomainResult.Error) return validationResult

        dataSource.updateReceivable(updated)
        dataSource.insertActivityEvent(
            CustomerReceivableActivityEvent(
                eventId = UUID.randomUUID().toString(),
                receivableId = receivableId,
                projectId = existing.projectId,
                activityType = CustomerReceivableActivityType.RECEIVABLE_CANCELLED,
                actorId = actorId,
                details = "Customer receivable cancelled by '$actorId': $cancellationReason"
            )
        )

        DomainResult.Success(updated)
    }

    override suspend fun getReceivableById(
        receivableId: String,
        callerRole: UserRole,
        authenticatedCustomerId: String?
    ): DomainResult<CustomerReceivable> {
        val receivable = dataSource.getReceivableById(receivableId)
            ?: return DomainResult.Error(message = "Customer receivable '$receivableId' not found.")

        val authResult = CustomerReceivableAuthorizationValidator.validateViewReceivables(
            callerRole = callerRole,
            requestedCustomerId = receivable.customerId,
            authenticatedCustomerId = authenticatedCustomerId
        )
        if (authResult is DomainResult.Error) return authResult

        // Evaluate dynamic aging and effective overdue status on read
        val now = System.currentTimeMillis()
        val dynamicAging = CustomerReceivableAgingCalculator.calculateAgingBucket(receivable.dueDate, now)
        val dynamicStatus = CustomerReceivableAgingCalculator.evaluateEffectiveStatus(receivable, now)

        val evaluated = receivable.copy(
            agingBucket = dynamicAging,
            status = dynamicStatus
        )

        return DomainResult.Success(evaluated)
    }

    override suspend fun getReceivablesByReference(
        projectId: String,
        referenceId: String,
        callerRole: UserRole
    ): DomainResult<List<CustomerReceivable>> {
        val authResult = CustomerReceivableAuthorizationValidator.validateViewReceivables(callerRole)
        if (authResult is DomainResult.Error) return authResult

        val list = dataSource.getReceivablesByReference(projectId, referenceId)
        return DomainResult.Success(list)
    }

    override fun observeReceivables(
        projectId: String,
        callerRole: UserRole
    ): Flow<List<CustomerReceivable>> {
        if (!callerRole.isInternal) {
            return flowOf(emptyList())
        }
        val now = System.currentTimeMillis()
        return dataSource.observeReceivables(projectId).map { list ->
            list.map { item ->
                item.copy(
                    agingBucket = CustomerReceivableAgingCalculator.calculateAgingBucket(item.dueDate, now),
                    status = CustomerReceivableAgingCalculator.evaluateEffectiveStatus(item, now)
                )
            }
        }
    }

    override fun observeCustomerReceivables(
        projectId: String,
        customerId: String,
        callerRole: UserRole,
        authenticatedCustomerId: String?
    ): Flow<List<CustomerReceivable>> {
        val authResult = CustomerReceivableAuthorizationValidator.validateViewReceivables(
            callerRole = callerRole,
            requestedCustomerId = customerId,
            authenticatedCustomerId = authenticatedCustomerId
        )
        if (authResult is DomainResult.Error) {
            return flowOf(emptyList())
        }

        val now = System.currentTimeMillis()
        return dataSource.observeReceivablesByCustomer(projectId, customerId).map { list ->
            list.map { item ->
                item.copy(
                    agingBucket = CustomerReceivableAgingCalculator.calculateAgingBucket(item.dueDate, now),
                    status = CustomerReceivableAgingCalculator.evaluateEffectiveStatus(item, now)
                )
            }
        }
    }

    override suspend fun getCustomerDueSummary(
        projectId: String,
        customerId: String?,
        callerRole: UserRole,
        authenticatedCustomerId: String?
    ): DomainResult<CustomerDueSummary> {
        val authResult = CustomerReceivableAuthorizationValidator.validateViewReceivables(
            callerRole = callerRole,
            requestedCustomerId = customerId,
            authenticatedCustomerId = authenticatedCustomerId
        )
        if (authResult is DomainResult.Error) return authResult

        val allList = dataSource.observeReceivables(projectId).first()
        val filtered = if (customerId != null) allList.filter { it.customerId == customerId } else allList
        val active = filtered.filter { it.status != CustomerReceivableStatus.CANCELLED }

        val now = System.currentTimeMillis()
        var totalOriginal = Money.ZERO
        var totalSettled = Money.ZERO
        var totalOutstanding = Money.ZERO
        var totalOverdue = Money.ZERO
        var openCount = 0
        var overdueCount = 0

        var agingCurrent = Money.ZERO
        var aging1to30 = Money.ZERO
        var aging31to60 = Money.ZERO
        var aging61to90 = Money.ZERO
        var agingOver90 = Money.ZERO

        active.forEach { item ->
            totalOriginal += item.originalAmount
            totalSettled += item.settledAmount
            val outstanding = item.outstandingAmount
            totalOutstanding += outstanding

            val effStatus = CustomerReceivableAgingCalculator.evaluateEffectiveStatus(item, now)
            val aging = CustomerReceivableAgingCalculator.calculateAgingBucket(item.dueDate, now)

            if (effStatus == CustomerReceivableStatus.OVERDUE) {
                totalOverdue += outstanding
                overdueCount++
            } else if (effStatus == CustomerReceivableStatus.OPEN || effStatus == CustomerReceivableStatus.PARTIALLY_SETTLED) {
                openCount++
            }

            if (outstanding.isPositive()) {
                when (aging) {
                    ReceivableAgingBucket.CURRENT -> agingCurrent += outstanding
                    ReceivableAgingBucket.DAYS_1_TO_30 -> aging1to30 += outstanding
                    ReceivableAgingBucket.DAYS_31_TO_60 -> aging31to60 += outstanding
                    ReceivableAgingBucket.DAYS_61_TO_90 -> aging61to90 += outstanding
                    ReceivableAgingBucket.DAYS_OVER_90 -> agingOver90 += outstanding
                }
            }
        }

        val summary = CustomerDueSummary(
            projectId = projectId,
            customerId = customerId,
            totalReceivablesCount = active.size,
            openReceivablesCount = openCount,
            overdueReceivablesCount = overdueCount,
            totalOriginalAmount = totalOriginal,
            totalSettledAmount = totalSettled,
            totalOutstandingDue = totalOutstanding,
            totalOverdueAmount = totalOverdue,
            agingCurrent = agingCurrent,
            aging1To30Days = aging1to30,
            aging31To60Days = aging31to60,
            aging61To90Days = aging61to90,
            agingOver90Days = agingOver90
        )

        return DomainResult.Success(summary)
    }

    override suspend fun getActivityEvents(
        receivableId: String,
        callerRole: UserRole
    ): DomainResult<List<CustomerReceivableActivityEvent>> {
        val authResult = CustomerReceivableAuthorizationValidator.validateViewReceivables(callerRole)
        if (authResult is DomainResult.Error) return authResult

        val events = dataSource.getActivityEvents(receivableId)
        return DomainResult.Success(events)
    }
}
