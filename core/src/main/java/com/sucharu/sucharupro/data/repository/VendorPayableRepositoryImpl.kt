package com.sucharu.sucharupro.data.repository

import com.sucharu.sucharupro.data.datasource.VendorPayableDataSource
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.common.Money
import com.sucharu.sucharupro.domain.model.finance.FinancialReferenceType
import com.sucharu.sucharupro.domain.model.finance.VendorPayable
import com.sucharu.sucharupro.domain.model.finance.VendorPayableActivityEvent
import com.sucharu.sucharupro.domain.model.finance.VendorPayableActivityType
import com.sucharu.sucharupro.domain.model.finance.VendorPayableAgingBucket
import com.sucharu.sucharupro.domain.model.finance.VendorPayableStatus
import com.sucharu.sucharupro.domain.model.finance.VendorPayableSummary
import com.sucharu.sucharupro.domain.model.user.UserRole
import com.sucharu.sucharupro.domain.repository.VendorPayableRepository
import com.sucharu.sucharupro.domain.service.finance.VendorPayableAgingCalculator
import com.sucharu.sucharupro.domain.validation.VendorPayableAuthorizationValidator
import com.sucharu.sucharupro.domain.validation.VendorPayableLifecycleValidator
import com.sucharu.sucharupro.domain.validation.VendorPayableValidator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID

/**
 * Thread-safe implementation of VendorPayableRepository with non-reentrant mutex locking (Module 09 Step 04).
 */
class VendorPayableRepositoryImpl(
    private val dataSource: VendorPayableDataSource
) : VendorPayableRepository {

    private val mutex = Mutex()

    override suspend fun createPayable(
        projectId: String,
        vendorId: String,
        referenceType: FinancialReferenceType,
        referenceId: String,
        financialTransactionId: String?,
        supplierInvoiceNo: String?,
        originalAmount: Money,
        currency: String,
        dueDate: Long,
        payableDate: Long,
        description: String,
        notes: String?,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<VendorPayable> = mutex.withLock {
        val authResult = VendorPayableAuthorizationValidator.validateCreatePayable(callerRole)
        if (authResult is DomainResult.Error) return@withLock authResult

        val valResult = VendorPayableValidator.validateCreatePayload(
            projectId = projectId,
            vendorId = vendorId,
            referenceType = referenceType,
            referenceId = referenceId,
            originalAmount = originalAmount,
            currency = currency,
            dueDate = dueDate,
            description = description,
            actorId = actorId
        )
        if (valResult is DomainResult.Error) return@withLock valResult

        // Duplicate active payable protection for same reference
        val existingRef = dataSource.getPayableByReference(projectId, vendorId, referenceId)
        if (existingRef != null && !existingRef.status.isTerminal) {
            return@withLock DomainResult.Error(
                message = "Duplicate active payable obligation found for reference '$referenceId' (Payable #${existingRef.payableNo})."
            )
        }

        // Duplicate active supplier invoice protection for same vendor
        if (!supplierInvoiceNo.isNullOrBlank()) {
            val existingInv = dataSource.getPayableByInvoice(projectId, vendorId, supplierInvoiceNo.trim())
            if (existingInv != null && !existingInv.status.isTerminal) {
                return@withLock DomainResult.Error(
                    message = "Duplicate active supplier invoice '${supplierInvoiceNo.trim()}' already registered for vendor '$vendorId' (Payable #${existingInv.payableNo})."
                )
            }
        }

        val payableId = UUID.randomUUID().toString()
        val payableNo = dataSource.generateNextPayableNo(projectId)
        val now = System.currentTimeMillis()
        val initialStatus = if (callerRole == UserRole.STAFF) VendorPayableStatus.DRAFT else VendorPayableStatus.APPROVED
        val initialAging = VendorPayableAgingCalculator.calculateAgingBucket(dueDate, now)

        val payable = VendorPayable(
            payableId = payableId,
            payableNo = payableNo,
            projectId = projectId,
            vendorId = vendorId,
            referenceType = referenceType,
            referenceId = referenceId,
            supplierInvoiceNo = supplierInvoiceNo?.trim(),
            financialTransactionId = financialTransactionId,
            originalAmount = originalAmount,
            settledAmount = Money.ZERO,
            currency = currency,
            dueDate = dueDate,
            payableDate = payableDate,
            status = initialStatus,
            agingBucket = initialAging,
            description = description,
            notes = notes,
            createdBy = actorId,
            createdAt = now,
            updatedAt = now,
            approvedBy = if (initialStatus == VendorPayableStatus.APPROVED) actorId else null,
            approvedAt = if (initialStatus == VendorPayableStatus.APPROVED) now else null
        )

        val inserted = dataSource.insertPayable(payable)
        if (!inserted) {
            return@withLock DomainResult.Error(message = "Failed to insert supplier payable record.")
        }

        dataSource.insertActivityEvent(
            VendorPayableActivityEvent(
                eventId = UUID.randomUUID().toString(),
                payableId = payableId,
                projectId = projectId,
                activityType = VendorPayableActivityType.PAYABLE_CREATED,
                actorId = actorId,
                details = "Payable #$payableNo created for vendor '$vendorId' with amount ${originalAmount.formatted()} $currency."
            )
        )

        DomainResult.Success(payable)
    }

    override suspend fun getPayableById(
        payableId: String,
        callerRole: UserRole,
        authenticatedVendorId: String?
    ): DomainResult<VendorPayable> = mutex.withLock {
        val payable = dataSource.getPayableById(payableId)
            ?: return@withLock DomainResult.Error(message = "Supplier payable '$payableId' not found.")

        val authResult = VendorPayableAuthorizationValidator.validateViewPayables(
            callerRole = callerRole,
            requestedVendorId = payable.vendorId,
            authenticatedVendorId = authenticatedVendorId
        )
        if (authResult is DomainResult.Error) return@withLock authResult

        DomainResult.Success(payable)
    }

    override suspend fun getPayableByNumber(
        projectId: String,
        payableNo: String,
        callerRole: UserRole,
        authenticatedVendorId: String?
    ): DomainResult<VendorPayable> = mutex.withLock {
        val payable = dataSource.getPayableByNumber(projectId, payableNo)
            ?: return@withLock DomainResult.Error(message = "Supplier payable '#$payableNo' not found in project '$projectId'.")

        val authResult = VendorPayableAuthorizationValidator.validateViewPayables(
            callerRole = callerRole,
            requestedVendorId = payable.vendorId,
            authenticatedVendorId = authenticatedVendorId
        )
        if (authResult is DomainResult.Error) return@withLock authResult

        DomainResult.Success(payable)
    }

    override suspend fun getPayableByReference(
        projectId: String,
        vendorId: String,
        referenceId: String,
        callerRole: UserRole
    ): DomainResult<VendorPayable> = mutex.withLock {
        val authResult = VendorPayableAuthorizationValidator.validateViewPayables(callerRole)
        if (authResult is DomainResult.Error) return@withLock authResult

        val payable = dataSource.getPayableByReference(projectId, vendorId, referenceId)
            ?: return@withLock DomainResult.Error(
                message = "Supplier payable not found for reference '$referenceId' in project '$projectId'."
            )

        DomainResult.Success(payable)
    }

    override suspend fun getPayableByInvoice(
        projectId: String,
        vendorId: String,
        supplierInvoiceNo: String,
        callerRole: UserRole
    ): DomainResult<VendorPayable> = mutex.withLock {
        val authResult = VendorPayableAuthorizationValidator.validateViewPayables(callerRole)
        if (authResult is DomainResult.Error) return@withLock authResult

        val payable = dataSource.getPayableByInvoice(projectId, vendorId, supplierInvoiceNo)
            ?: return@withLock DomainResult.Error(
                message = "Supplier payable not found for invoice '$supplierInvoiceNo' in project '$projectId'."
            )

        DomainResult.Success(payable)
    }

    override fun observePayables(
        projectId: String,
        callerRole: UserRole
    ): Flow<List<VendorPayable>> {
        val authResult = VendorPayableAuthorizationValidator.validateViewPayables(callerRole)
        if (authResult is DomainResult.Error) return emptyFlow()
        return dataSource.observePayables(projectId)
    }

    override fun observeVendorPayables(
        projectId: String,
        vendorId: String,
        callerRole: UserRole,
        authenticatedVendorId: String?
    ): Flow<List<VendorPayable>> {
        val authResult = VendorPayableAuthorizationValidator.validateViewPayables(
            callerRole = callerRole,
            requestedVendorId = vendorId,
            authenticatedVendorId = authenticatedVendorId
        )
        if (authResult is DomainResult.Error) return emptyFlow()
        return dataSource.observeVendorPayables(projectId, vendorId)
    }

    override suspend fun getVendorPayableSummary(
        projectId: String,
        vendorId: String?,
        callerRole: UserRole,
        authenticatedVendorId: String?,
        asOfTimestamp: Long
    ): DomainResult<VendorPayableSummary> = mutex.withLock {
        val authResult = VendorPayableAuthorizationValidator.validateViewPayables(
            callerRole = callerRole,
            requestedVendorId = vendorId,
            authenticatedVendorId = authenticatedVendorId
        )
        if (authResult is DomainResult.Error) return@withLock authResult

        val payables = if (vendorId != null) {
            dataSource.observeVendorPayables(projectId, vendorId).first()
        } else {
            dataSource.observePayables(projectId).first()
        }

        var totalOriginal = Money.ZERO
        var totalSettled = Money.ZERO
        var totalOutstanding = Money.ZERO
        var totalOverdue = Money.ZERO

        var agingCurrent = Money.ZERO
        var aging1To30 = Money.ZERO
        var aging31To60 = Money.ZERO
        var aging61To90 = Money.ZERO
        var agingOver90 = Money.ZERO

        var openCount = 0
        var overdueCount = 0

        for (p in payables) {
            if (p.status == VendorPayableStatus.CANCELLED) continue

            totalOriginal += p.originalAmount
            totalSettled += p.settledAmount
            val out = p.outstandingAmount
            totalOutstanding += out

            if (out.isPositive()) {
                openCount++
                val bucket = VendorPayableAgingCalculator.calculateAgingBucket(p.dueDate, asOfTimestamp)
                when (bucket) {
                    VendorPayableAgingBucket.CURRENT -> agingCurrent += out
                    VendorPayableAgingBucket.DAYS_1_TO_30 -> {
                        aging1To30 += out
                        totalOverdue += out
                        overdueCount++
                    }
                    VendorPayableAgingBucket.DAYS_31_TO_60 -> {
                        aging31To60 += out
                        totalOverdue += out
                        overdueCount++
                    }
                    VendorPayableAgingBucket.DAYS_61_TO_90 -> {
                        aging61To90 += out
                        totalOverdue += out
                        overdueCount++
                    }
                    VendorPayableAgingBucket.DAYS_OVER_90 -> {
                        agingOver90 += out
                        totalOverdue += out
                        overdueCount++
                    }
                }
            }
        }

        DomainResult.Success(
            VendorPayableSummary(
                projectId = projectId,
                vendorId = vendorId,
                totalPayablesCount = payables.size,
                openPayablesCount = openCount,
                overduePayablesCount = overdueCount,
                totalOriginalAmount = totalOriginal,
                totalSettledAmount = totalSettled,
                totalOutstandingPayable = totalOutstanding,
                totalOverdueAmount = totalOverdue,
                agingCurrent = agingCurrent,
                aging1To30Days = aging1To30,
                aging31To60Days = aging31To60,
                aging61To90Days = aging61To90,
                agingOver90Days = agingOver90
            )
        )
    }

    override suspend fun updateDraftPayable(
        payableId: String,
        originalAmount: Money?,
        dueDate: Long?,
        supplierInvoiceNo: String?,
        description: String?,
        notes: String?,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<VendorPayable> = mutex.withLock {
        val authResult = VendorPayableAuthorizationValidator.validateUpdateDraft(callerRole)
        if (authResult is DomainResult.Error) return@withLock authResult

        val existing = dataSource.getPayableById(payableId)
            ?: return@withLock DomainResult.Error(message = "Supplier payable '$payableId' not found.")

        if (existing.status != VendorPayableStatus.DRAFT) {
            return@withLock DomainResult.Error(
                message = "Only DRAFT payables can be edited. Current status: ${existing.status.name}."
            )
        }

        val updated = existing.copy(
            originalAmount = originalAmount ?: existing.originalAmount,
            dueDate = dueDate ?: existing.dueDate,
            supplierInvoiceNo = supplierInvoiceNo?.trim() ?: existing.supplierInvoiceNo,
            description = description ?: existing.description,
            notes = notes ?: existing.notes,
            updatedBy = actorId,
            updatedAt = System.currentTimeMillis()
        )

        val updatedSuccess = dataSource.updatePayable(updated)
        if (!updatedSuccess) {
            return@withLock DomainResult.Error(message = "Failed to update draft payable '$payableId'.")
        }

        dataSource.insertActivityEvent(
            VendorPayableActivityEvent(
                eventId = UUID.randomUUID().toString(),
                payableId = payableId,
                projectId = existing.projectId,
                activityType = VendorPayableActivityType.PAYABLE_UPDATED,
                actorId = actorId,
                details = "Draft payable #${existing.payableNo} updated."
            )
        )

        DomainResult.Success(updated)
    }

    override suspend fun submitPayable(
        payableId: String,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<VendorPayable> = mutex.withLock {
        val authResult = VendorPayableAuthorizationValidator.validateSubmitPayable(callerRole)
        if (authResult is DomainResult.Error) return@withLock authResult

        val existing = dataSource.getPayableById(payableId)
            ?: return@withLock DomainResult.Error(message = "Supplier payable '$payableId' not found.")

        val transitionResult = VendorPayableLifecycleValidator.validateTransition(
            existing.status,
            VendorPayableStatus.PENDING
        )
        if (transitionResult is DomainResult.Error) return@withLock transitionResult

        val updated = existing.copy(
            status = VendorPayableStatus.PENDING,
            updatedBy = actorId,
            updatedAt = System.currentTimeMillis()
        )

        dataSource.updatePayable(updated)

        dataSource.insertActivityEvent(
            VendorPayableActivityEvent(
                eventId = UUID.randomUUID().toString(),
                payableId = payableId,
                projectId = existing.projectId,
                activityType = VendorPayableActivityType.PAYABLE_SUBMITTED,
                actorId = actorId,
                details = "Payable #${existing.payableNo} submitted for approval."
            )
        )

        DomainResult.Success(updated)
    }

    override suspend fun approvePayable(
        payableId: String,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<VendorPayable> = mutex.withLock {
        val existing = dataSource.getPayableById(payableId)
            ?: return@withLock DomainResult.Error(message = "Supplier payable '$payableId' not found.")

        val authResult = VendorPayableAuthorizationValidator.validateApprovePayable(
            callerRole = callerRole,
            creatorId = existing.createdBy,
            approverId = actorId
        )
        if (authResult is DomainResult.Error) return@withLock authResult

        val transitionResult = VendorPayableLifecycleValidator.validateTransition(
            existing.status,
            VendorPayableStatus.APPROVED
        )
        if (transitionResult is DomainResult.Error) return@withLock transitionResult

        val now = System.currentTimeMillis()
        val updated = existing.copy(
            status = VendorPayableStatus.APPROVED,
            approvedBy = actorId,
            approvedAt = now,
            updatedBy = actorId,
            updatedAt = now
        )

        dataSource.updatePayable(updated)

        dataSource.insertActivityEvent(
            VendorPayableActivityEvent(
                eventId = UUID.randomUUID().toString(),
                payableId = payableId,
                projectId = existing.projectId,
                activityType = VendorPayableActivityType.PAYABLE_APPROVED,
                actorId = actorId,
                details = "Payable #${existing.payableNo} approved."
            )
        )

        DomainResult.Success(updated)
    }

    override suspend fun cancelPayable(
        payableId: String,
        cancellationReason: String,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<VendorPayable> = mutex.withLock {
        val authResult = VendorPayableAuthorizationValidator.validateCancelPayable(callerRole)
        if (authResult is DomainResult.Error) return@withLock authResult

        if (cancellationReason.isBlank()) {
            return@withLock DomainResult.Error(message = "Cancellation reason cannot be blank.")
        }

        val existing = dataSource.getPayableById(payableId)
            ?: return@withLock DomainResult.Error(message = "Supplier payable '$payableId' not found.")

        val transitionResult = VendorPayableLifecycleValidator.validateTransition(
            existing.status,
            VendorPayableStatus.CANCELLED
        )
        if (transitionResult is DomainResult.Error) return@withLock transitionResult

        val now = System.currentTimeMillis()
        val updated = existing.copy(
            status = VendorPayableStatus.CANCELLED,
            cancelledBy = actorId,
            cancelledAt = now,
            cancellationReason = cancellationReason.trim(),
            updatedBy = actorId,
            updatedAt = now
        )

        dataSource.updatePayable(updated)

        dataSource.insertActivityEvent(
            VendorPayableActivityEvent(
                eventId = UUID.randomUUID().toString(),
                payableId = payableId,
                projectId = existing.projectId,
                activityType = VendorPayableActivityType.PAYABLE_CANCELLED,
                actorId = actorId,
                details = "Payable #${existing.payableNo} cancelled. Reason: ${cancellationReason.trim()}"
            )
        )

        DomainResult.Success(updated)
    }

    override suspend fun recordSettlement(
        payableId: String,
        settlementAmount: Money,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<VendorPayable> = mutex.withLock {
        val authResult = VendorPayableAuthorizationValidator.validateRecordSettlement(callerRole)
        if (authResult is DomainResult.Error) return@withLock authResult

        val existing = dataSource.getPayableById(payableId)
            ?: return@withLock DomainResult.Error(message = "Supplier payable '$payableId' not found.")

        val valResult = VendorPayableValidator.validateSettlement(existing, settlementAmount)
        if (valResult is DomainResult.Error) return@withLock valResult

        val newSettledAmount = existing.settledAmount + settlementAmount
        val isFullySettled = newSettledAmount >= existing.originalAmount
        val targetStatus = if (isFullySettled) VendorPayableStatus.SETTLED else VendorPayableStatus.PARTIALLY_SETTLED

        val now = System.currentTimeMillis()
        val updated = existing.copy(
            settledAmount = newSettledAmount,
            status = targetStatus,
            settledAt = if (isFullySettled) now else existing.settledAt,
            updatedBy = actorId,
            updatedAt = now
        )

        dataSource.updatePayable(updated)

        dataSource.insertActivityEvent(
            VendorPayableActivityEvent(
                eventId = UUID.randomUUID().toString(),
                payableId = payableId,
                projectId = existing.projectId,
                activityType = VendorPayableActivityType.PAYABLE_SETTLEMENT_RECORDED,
                actorId = actorId,
                details = "Recorded settlement of ${settlementAmount.formatted()} on payable #${existing.payableNo}. Remaining balance: ${updated.outstandingAmount.formatted()}."
            )
        )

        DomainResult.Success(updated)
    }

    override suspend fun getActivityEvents(
        payableId: String,
        callerRole: UserRole
    ): DomainResult<List<VendorPayableActivityEvent>> = mutex.withLock {
        val authResult = VendorPayableAuthorizationValidator.validateViewPayables(callerRole)
        if (authResult is DomainResult.Error) return@withLock authResult

        val events = dataSource.getActivityEvents(payableId)
        DomainResult.Success(events)
    }
}
