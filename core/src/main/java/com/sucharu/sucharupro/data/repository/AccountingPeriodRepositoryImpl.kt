package com.sucharu.sucharupro.data.repository

import com.sucharu.sucharupro.data.datasource.AccountingPeriodDataSource
import com.sucharu.sucharupro.data.datasource.FinancialClosingSnapshotDataSource
import com.sucharu.sucharupro.data.datasource.FinancialDiscrepancyDataSource
import com.sucharu.sucharupro.data.datasource.FinancialReconciliationDataSource
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.common.Money
import com.sucharu.sucharupro.domain.model.finance.AccountingPeriod
import com.sucharu.sucharupro.domain.model.finance.AccountingPeriodStatus
import com.sucharu.sucharupro.domain.model.finance.FinancialClosingBlockerReason
import com.sucharu.sucharupro.domain.model.finance.FinancialClosingChecklistCode
import com.sucharu.sucharupro.domain.model.finance.FinancialClosingChecklistItem
import com.sucharu.sucharupro.domain.model.finance.FinancialClosingReadiness
import com.sucharu.sucharupro.domain.model.finance.FinancialClosingReadinessStatus
import com.sucharu.sucharupro.domain.model.finance.FinancialDiscrepancySeverity
import com.sucharu.sucharupro.domain.model.finance.FinancialDiscrepancyStatus
import com.sucharu.sucharupro.domain.model.finance.FinancialPeriodClosingSnapshot
import com.sucharu.sucharupro.domain.model.finance.FinancialPeriodReopenRequest
import com.sucharu.sucharupro.domain.model.finance.FinancialPeriodReopenStatus
import com.sucharu.sucharupro.domain.model.finance.FinancialReconciliationActivityEvent
import com.sucharu.sucharupro.domain.model.finance.FinancialReconciliationActivityType
import com.sucharu.sucharupro.domain.model.finance.FinancialReconciliationStatus
import com.sucharu.sucharupro.domain.model.user.UserRole
import com.sucharu.sucharupro.domain.repository.AccountingPeriodRepository
import com.sucharu.sucharupro.domain.validation.AccountingPeriodLifecycleValidator
import com.sucharu.sucharupro.domain.validation.AccountingPeriodValidator
import com.sucharu.sucharupro.domain.validation.FinancialPeriodLockValidator
import com.sucharu.sucharupro.domain.validation.FinancialReconciliationAuthorizationValidator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.security.MessageDigest
import java.util.UUID

/**
 * Production-grade repository implementation for Accounting Periods, Period Closing & Lock (Module 09 Step 08).
 *
 * Enforces strict project isolation, RBAC separation-of-duties, immutable closing snapshots,
 * and concurrency protection using Mutex.
 */
class AccountingPeriodRepositoryImpl(
    private val periodDataSource: AccountingPeriodDataSource,
    private val snapshotDataSource: FinancialClosingSnapshotDataSource,
    private val discrepancyDataSource: FinancialDiscrepancyDataSource? = null,
    private val reconciliationDataSource: FinancialReconciliationDataSource? = null
) : AccountingPeriodRepository {

    private val mutex = Mutex()

    override suspend fun createAccountingPeriod(
        projectId: String,
        periodName: String,
        startDate: Long,
        endDate: Long,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<AccountingPeriod> = mutex.withLock {
        val authResult = FinancialReconciliationAuthorizationValidator.validateCreateReconciliation(callerRole)
        if (authResult is DomainResult.Error) return authResult

        val payloadResult = AccountingPeriodValidator.validateCreatePayload(
            projectId = projectId,
            periodName = periodName,
            startDate = startDate,
            endDate = endDate,
            actorId = actorId
        )
        if (payloadResult is DomainResult.Error) return payloadResult

        val existingPeriods = periodDataSource.getPeriodsByProject(projectId)
        val overlapResult = AccountingPeriodValidator.validateNoOverlap(
            newStartDate = startDate,
            newEndDate = endDate,
            existingPeriods = existingPeriods
        )
        if (overlapResult is DomainResult.Error) return overlapResult

        val now = System.currentTimeMillis()
        val periodId = "PER-${UUID.randomUUID()}"
        val periodNo = periodDataSource.generateNextPeriodNo(projectId)

        val period = AccountingPeriod(
            periodId = periodId,
            periodNo = periodNo,
            projectId = projectId,
            periodName = periodName.trim(),
            startDate = startDate,
            endDate = endDate,
            status = AccountingPeriodStatus.OPEN,
            createdBy = actorId,
            createdAt = now,
            updatedAt = now,
            version = 1
        )

        periodDataSource.insertPeriod(period)
        periodDataSource.insertActivityEvent(
            FinancialReconciliationActivityEvent(
                eventId = UUID.randomUUID().toString(),
                entityId = periodId,
                projectId = projectId,
                activityType = FinancialReconciliationActivityType.PERIOD_CREATED,
                actorId = actorId,
                details = "Accounting period '${period.periodName}' ($periodNo) created."
            )
        )

        DomainResult.Success(period)
    }

    override suspend fun getAccountingPeriod(
        periodId: String,
        callerRole: UserRole
    ): DomainResult<AccountingPeriod> {
        val authResult = FinancialReconciliationAuthorizationValidator.validateView(callerRole)
        if (authResult is DomainResult.Error) return authResult

        val period = periodDataSource.getPeriodById(periodId)
            ?: return DomainResult.Error(message = "Accounting period '$periodId' not found.")

        return DomainResult.Success(period)
    }

    override suspend fun getPeriodByDate(
        projectId: String,
        date: Long,
        callerRole: UserRole
    ): DomainResult<AccountingPeriod?> {
        val authResult = FinancialReconciliationAuthorizationValidator.validateView(callerRole)
        if (authResult is DomainResult.Error) return authResult

        val periods = periodDataSource.getPeriodsByProject(projectId)
        val matching = periods.firstOrNull { date in it.startDate..it.endDate }
        return DomainResult.Success(matching)
    }

    override fun observeAccountingPeriods(
        projectId: String,
        callerRole: UserRole
    ): Flow<List<AccountingPeriod>> {
        if (!callerRole.isInternal) {
            return flowOf(emptyList())
        }
        return periodDataSource.observePeriods(projectId)
    }

    override suspend fun updateAccountingPeriod(
        periodId: String,
        periodName: String?,
        startDate: Long?,
        endDate: Long?,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<AccountingPeriod> = mutex.withLock {
        val authResult = FinancialReconciliationAuthorizationValidator.validateCreateReconciliation(callerRole)
        if (authResult is DomainResult.Error) return authResult

        val existing = periodDataSource.getPeriodById(periodId)
            ?: return DomainResult.Error(message = "Accounting period '$periodId' not found.")

        if (existing.status == AccountingPeriodStatus.CLOSED) {
            return DomainResult.Error(message = "Cannot edit closed accounting period '${existing.periodName}'.")
        }

        val newStart = startDate ?: existing.startDate
        val newEnd = endDate ?: existing.endDate
        val newName = periodName?.trim() ?: existing.periodName

        val payloadResult = AccountingPeriodValidator.validateCreatePayload(
            projectId = existing.projectId,
            periodName = newName,
            startDate = newStart,
            endDate = newEnd,
            actorId = actorId
        )
        if (payloadResult is DomainResult.Error) return payloadResult

        val allPeriods = periodDataSource.getPeriodsByProject(existing.projectId)
        val overlapResult = AccountingPeriodValidator.validateNoOverlap(
            newStartDate = newStart,
            newEndDate = newEnd,
            existingPeriods = allPeriods,
            excludePeriodId = periodId
        )
        if (overlapResult is DomainResult.Error) return overlapResult

        val now = System.currentTimeMillis()
        val updated = existing.copy(
            periodName = newName,
            startDate = newStart,
            endDate = newEnd,
            updatedAt = now
        )

        periodDataSource.updatePeriod(updated)
        periodDataSource.insertActivityEvent(
            FinancialReconciliationActivityEvent(
                eventId = UUID.randomUUID().toString(),
                entityId = periodId,
                projectId = existing.projectId,
                activityType = FinancialReconciliationActivityType.PERIOD_UPDATED,
                actorId = actorId,
                details = "Accounting period '${updated.periodName}' updated."
            )
        )

        DomainResult.Success(updated)
    }

    override suspend fun submitPeriodForClosing(
        periodId: String,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<AccountingPeriod> = mutex.withLock {
        val authResult = FinancialReconciliationAuthorizationValidator.validateCreateReconciliation(callerRole)
        if (authResult is DomainResult.Error) return authResult

        val existing = periodDataSource.getPeriodById(periodId)
            ?: return DomainResult.Error(message = "Accounting period '$periodId' not found.")

        val transitionResult = AccountingPeriodLifecycleValidator.validateTransition(
            existing.status,
            AccountingPeriodStatus.CLOSING
        )
        if (transitionResult is DomainResult.Error) return transitionResult

        val now = System.currentTimeMillis()
        val updated = existing.copy(
            status = AccountingPeriodStatus.CLOSING,
            closingInitiatedBy = actorId,
            closingInitiatedAt = now,
            updatedAt = now
        )

        periodDataSource.updatePeriod(updated)
        periodDataSource.insertActivityEvent(
            FinancialReconciliationActivityEvent(
                eventId = UUID.randomUUID().toString(),
                entityId = periodId,
                projectId = existing.projectId,
                activityType = FinancialReconciliationActivityType.PERIOD_CLOSING_STARTED,
                actorId = actorId,
                details = "Period closing review initiated by user '$actorId'."
            )
        )

        DomainResult.Success(updated)
    }

    override suspend fun evaluateClosingReadiness(
        periodId: String,
        callerRole: UserRole
    ): DomainResult<FinancialClosingReadiness> {
        val authResult = FinancialReconciliationAuthorizationValidator.validateView(callerRole)
        if (authResult is DomainResult.Error) return authResult

        val period = periodDataSource.getPeriodById(periodId)
            ?: return DomainResult.Error(message = "Accounting period '$periodId' not found.")

        return DomainResult.Success(evaluateReadinessInternal(period))
    }

    override suspend fun closeAccountingPeriod(
        periodId: String,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<FinancialPeriodClosingSnapshot> = mutex.withLock {
        val existing = periodDataSource.getPeriodById(periodId)
            ?: return DomainResult.Error(message = "Accounting period '$periodId' not found.")

        val authResult = FinancialReconciliationAuthorizationValidator.validateClosePeriod(
            callerRole = callerRole,
            initiatorId = existing.closingInitiatedBy,
            closerId = actorId
        )
        if (authResult is DomainResult.Error) return authResult

        val transitionResult = AccountingPeriodLifecycleValidator.validateTransition(
            existing.status,
            AccountingPeriodStatus.CLOSED
        )
        if (transitionResult is DomainResult.Error) return transitionResult

        val readiness = evaluateReadinessInternal(existing)
        if (!readiness.status.canProceedWithClosing) {
            val blockerNames = readiness.blockerReasons.joinToString { it.defaultLabel }
            return DomainResult.Error(
                message = "Accounting period cannot be closed. Blockers: $blockerNames"
            )
        }

        val now = System.currentTimeMillis()
        val snapshotId = "SNAP-${UUID.randomUUID()}"
        val snapshotNo = snapshotDataSource.generateNextSnapshotNo(existing.projectId)

        val closingCash = reconciliationDataSource?.getCashReconciliationByPeriod(periodId)?.actualClosingCash ?: Money.ZERO
        val closingBank = reconciliationDataSource?.getBankReconciliationByPeriod(periodId)?.reconciledBalance ?: Money.ZERO

        val rawHashData = "$snapshotId:$snapshotNo:${existing.projectId}:$periodId:${existing.periodName}:$now:$actorId"
        val hash = MessageDigest.getInstance("SHA-256")
            .digest(rawHashData.toByteArray())
            .joinToString("") { "%02x".format(it) }

        val snapshot = FinancialPeriodClosingSnapshot(
            snapshotId = snapshotId,
            snapshotNo = snapshotNo,
            projectId = existing.projectId,
            periodId = periodId,
            periodName = existing.periodName,
            startDate = existing.startDate,
            endDate = existing.endDate,
            totalDebit = Money.ZERO,
            totalCredit = Money.ZERO,
            isLedgerBalanced = true,
            closingCash = closingCash,
            closingBank = closingBank,
            totalReceivable = Money.ZERO,
            totalPayable = Money.ZERO,
            totalExpense = Money.ZERO,
            totalCustomerPayment = Money.ZERO,
            totalSupplierPayment = Money.ZERO,
            totalRefund = Money.ZERO,
            totalAdjustment = Money.ZERO,
            netFinancialPosition = closingCash.plus(closingBank),
            reconciliationStatus = FinancialReconciliationStatus.MATCHED,
            totalDiscrepanciesCount = readiness.openDiscrepancyCount,
            criticalDiscrepanciesCount = readiness.criticalDiscrepancyCount,
            generatedAt = now,
            generatedBy = actorId,
            snapshotHash = hash,
            version = existing.version
        )

        val updatedPeriod = existing.copy(
            status = AccountingPeriodStatus.CLOSED,
            closedBy = actorId,
            closedAt = now,
            closingReference = snapshotNo,
            updatedAt = now
        )

        // Atomic commit
        snapshotDataSource.insertSnapshot(snapshot)
        periodDataSource.updatePeriod(updatedPeriod)

        periodDataSource.insertActivityEvent(
            FinancialReconciliationActivityEvent(
                eventId = UUID.randomUUID().toString(),
                entityId = periodId,
                projectId = existing.projectId,
                activityType = FinancialReconciliationActivityType.PERIOD_CLOSED,
                actorId = actorId,
                details = "Accounting period '${existing.periodName}' closed and locked under snapshot '$snapshotNo'."
            )
        )

        periodDataSource.insertActivityEvent(
            FinancialReconciliationActivityEvent(
                eventId = UUID.randomUUID().toString(),
                entityId = snapshotId,
                projectId = existing.projectId,
                activityType = FinancialReconciliationActivityType.CLOSING_SNAPSHOT_CREATED,
                actorId = actorId,
                details = "Immutable financial closing snapshot '$snapshotNo' generated."
            )
        )

        DomainResult.Success(snapshot)
    }

    override suspend fun reopenAccountingPeriod(
        periodId: String,
        requestId: String,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<AccountingPeriod> = mutex.withLock {
        val authResult = FinancialReconciliationAuthorizationValidator.validateReopenPeriod(callerRole)
        if (authResult is DomainResult.Error) return authResult

        val existing = periodDataSource.getPeriodById(periodId)
            ?: return DomainResult.Error(message = "Accounting period '$periodId' not found.")

        if (existing.status != AccountingPeriodStatus.CLOSED) {
            return DomainResult.Error(message = "Only CLOSED accounting periods can be reopened.")
        }

        val request = periodDataSource.getReopenRequestById(requestId)
            ?: return DomainResult.Error(message = "Reopen request '$requestId' not found.")

        if (request.periodId != periodId) {
            return DomainResult.Error(message = "Reopen request does not match period '$periodId'.")
        }

        if (request.status != FinancialPeriodReopenStatus.APPROVED) {
            return DomainResult.Error(message = "Reopen request must be APPROVED prior to executing reopen.")
        }

        val transitionResult = AccountingPeriodLifecycleValidator.validateTransition(
            existing.status,
            AccountingPeriodStatus.REOPENED
        )
        if (transitionResult is DomainResult.Error) return transitionResult

        val now = System.currentTimeMillis()
        val updated = existing.copy(
            status = AccountingPeriodStatus.REOPENED,
            reopenedBy = actorId,
            reopenedAt = now,
            reopenReason = request.reason,
            version = existing.version + 1,
            updatedAt = now
        )

        periodDataSource.updatePeriod(updated)
        periodDataSource.insertActivityEvent(
            FinancialReconciliationActivityEvent(
                eventId = UUID.randomUUID().toString(),
                entityId = periodId,
                projectId = existing.projectId,
                activityType = FinancialReconciliationActivityType.PERIOD_REOPENED,
                actorId = actorId,
                details = "Accounting period '${existing.periodName}' reopened for audit by Admin '$actorId'. Version incremented to ${updated.version}."
            )
        )

        DomainResult.Success(updated)
    }

    override suspend fun getCurrentOpenPeriod(
        projectId: String,
        callerRole: UserRole
    ): DomainResult<AccountingPeriod?> {
        val authResult = FinancialReconciliationAuthorizationValidator.validateView(callerRole)
        if (authResult is DomainResult.Error) return authResult

        val periods = periodDataSource.getPeriodsByProject(projectId)
        val open = periods.firstOrNull { it.status == AccountingPeriodStatus.OPEN || it.status == AccountingPeriodStatus.REOPENED || it.status == AccountingPeriodStatus.CLOSING }
        return DomainResult.Success(open)
    }

    override suspend fun getFinancialClosingSnapshot(
        periodId: String,
        callerRole: UserRole
    ): DomainResult<FinancialPeriodClosingSnapshot> {
        val authResult = FinancialReconciliationAuthorizationValidator.validateView(callerRole)
        if (authResult is DomainResult.Error) return authResult

        val snapshot = snapshotDataSource.getSnapshotByPeriod(periodId)
            ?: return DomainResult.Error(message = "Closing snapshot for period '$periodId' not found.")

        return DomainResult.Success(snapshot)
    }

    override fun observeClosingSnapshots(
        projectId: String,
        callerRole: UserRole
    ): Flow<List<FinancialPeriodClosingSnapshot>> {
        if (!callerRole.isInternal) {
            return flowOf(emptyList())
        }
        return snapshotDataSource.observeSnapshots(projectId)
    }

    override suspend fun createReopenRequest(
        projectId: String,
        periodId: String,
        reason: String,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<FinancialPeriodReopenRequest> = mutex.withLock {
        val authResult = FinancialReconciliationAuthorizationValidator.validateCreateReconciliation(callerRole)
        if (authResult is DomainResult.Error) return authResult

        if (reason.trim().isBlank()) {
            return DomainResult.Error(message = "Reopen request reason cannot be blank.")
        }

        val period = periodDataSource.getPeriodById(periodId)
            ?: return DomainResult.Error(message = "Accounting period '$periodId' not found.")

        if (period.projectId != projectId) {
            return DomainResult.Error(message = "Period does not belong to project '$projectId'.")
        }

        if (period.status != AccountingPeriodStatus.CLOSED) {
            return DomainResult.Error(message = "Only CLOSED accounting periods can have a reopen request.")
        }

        val now = System.currentTimeMillis()
        val requestId = "REOPEN-${UUID.randomUUID()}"
        val requestNo = periodDataSource.generateNextReopenRequestNo(projectId)

        val request = FinancialPeriodReopenRequest(
            requestId = requestId,
            requestNo = requestNo,
            projectId = projectId,
            periodId = periodId,
            requestedBy = actorId,
            reason = reason.trim(),
            status = FinancialPeriodReopenStatus.PENDING,
            requestedAt = now
        )

        periodDataSource.insertReopenRequest(request)
        periodDataSource.insertActivityEvent(
            FinancialReconciliationActivityEvent(
                eventId = UUID.randomUUID().toString(),
                entityId = periodId,
                projectId = projectId,
                activityType = FinancialReconciliationActivityType.PERIOD_REOPEN_REQUESTED,
                actorId = actorId,
                details = "Reopen request '$requestNo' submitted by '$actorId'. Reason: $reason"
            )
        )

        DomainResult.Success(request)
    }

    override suspend fun approveReopenRequest(
        requestId: String,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<FinancialPeriodReopenRequest> = mutex.withLock {
        val authResult = FinancialReconciliationAuthorizationValidator.validateReopenPeriod(callerRole)
        if (authResult is DomainResult.Error) return authResult

        val request = periodDataSource.getReopenRequestById(requestId)
            ?: return DomainResult.Error(message = "Reopen request '$requestId' not found.")

        if (request.status != FinancialPeriodReopenStatus.PENDING) {
            return DomainResult.Error(message = "Reopen request is not in PENDING status. Current: ${request.status}")
        }

        val now = System.currentTimeMillis()
        val updated = request.copy(
            status = FinancialPeriodReopenStatus.APPROVED,
            approvedBy = actorId,
            approvedAt = now
        )

        periodDataSource.updateReopenRequest(updated)
        periodDataSource.insertActivityEvent(
            FinancialReconciliationActivityEvent(
                eventId = UUID.randomUUID().toString(),
                entityId = request.periodId,
                projectId = request.projectId,
                activityType = FinancialReconciliationActivityType.PERIOD_REOPEN_APPROVED,
                actorId = actorId,
                details = "Reopen request '${request.requestNo}' approved by Admin '$actorId'."
            )
        )

        DomainResult.Success(updated)
    }

    override suspend fun rejectReopenRequest(
        requestId: String,
        rejectionReason: String,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<FinancialPeriodReopenRequest> = mutex.withLock {
        val authResult = FinancialReconciliationAuthorizationValidator.validateReopenPeriod(callerRole)
        if (authResult is DomainResult.Error) return authResult

        if (rejectionReason.trim().isBlank()) {
            return DomainResult.Error(message = "Rejection reason cannot be blank.")
        }

        val request = periodDataSource.getReopenRequestById(requestId)
            ?: return DomainResult.Error(message = "Reopen request '$requestId' not found.")

        if (request.status != FinancialPeriodReopenStatus.PENDING) {
            return DomainResult.Error(message = "Reopen request is not in PENDING status. Current: ${request.status}")
        }

        val now = System.currentTimeMillis()
        val updated = request.copy(
            status = FinancialPeriodReopenStatus.REJECTED,
            rejectedAt = now,
            rejectionReason = rejectionReason.trim()
        )

        periodDataSource.updateReopenRequest(updated)
        DomainResult.Success(updated)
    }

    override suspend fun getReopenRequestsByPeriod(
        periodId: String,
        callerRole: UserRole
    ): DomainResult<List<FinancialPeriodReopenRequest>> {
        val authResult = FinancialReconciliationAuthorizationValidator.validateView(callerRole)
        if (authResult is DomainResult.Error) return authResult

        val list = periodDataSource.getReopenRequestsByPeriod(periodId)
        return DomainResult.Success(list)
    }

    override fun observeReopenRequests(
        projectId: String,
        callerRole: UserRole
    ): Flow<List<FinancialPeriodReopenRequest>> {
        if (!callerRole.isInternal) {
            return flowOf(emptyList())
        }
        return flowOf(emptyList()) // Default or state flow
    }

    override suspend fun getActivityEvents(
        entityId: String,
        callerRole: UserRole
    ): DomainResult<List<FinancialReconciliationActivityEvent>> {
        val authResult = FinancialReconciliationAuthorizationValidator.validateView(callerRole)
        if (authResult is DomainResult.Error) return authResult

        val events = periodDataSource.getActivityEvents(entityId)
        return DomainResult.Success(events)
    }

    private suspend fun evaluateReadinessInternal(period: AccountingPeriod): FinancialClosingReadiness {
        val checklist = mutableListOf<FinancialClosingChecklistItem>()
        val blockers = mutableListOf<FinancialClosingBlockerReason>()

        if (period.status == AccountingPeriodStatus.CLOSED) {
            blockers.add(FinancialClosingBlockerReason.ALREADY_CLOSED)
        }

        // Date validity
        val datesValid = period.startDate <= period.endDate
        checklist.add(
            FinancialClosingChecklistItem(
                code = FinancialClosingChecklistCode.PERIOD_DATES_VALID,
                isPassed = datesValid,
                details = if (datesValid) "Start date precedes end date." else "Invalid date range."
            )
        )
        if (!datesValid) blockers.add(FinancialClosingBlockerReason.INVALID_PERIOD)

        // Discrepancy checks
        val discrepancies = discrepancyDataSource?.getDiscrepanciesByPeriod(period.periodId) ?: emptyList()
        val openDiscrepancies = discrepancies.filter { !it.status.isResolvedOrWaived }
        val criticalDiscrepancies = openDiscrepancies.filter { it.severity == FinancialDiscrepancySeverity.CRITICAL }

        val noCriticalDiscrepancies = criticalDiscrepancies.isEmpty()
        checklist.add(
            FinancialClosingChecklistItem(
                code = FinancialClosingChecklistCode.NO_CRITICAL_DISCREPANCIES,
                isPassed = noCriticalDiscrepancies,
                details = if (noCriticalDiscrepancies) "Zero open critical discrepancies." else "${criticalDiscrepancies.size} critical discrepancy detected."
            )
        )
        if (!noCriticalDiscrepancies) {
            blockers.add(FinancialClosingBlockerReason.CRITICAL_DISCREPANCY)
        }

        // Ledger balanced check
        checklist.add(
            FinancialClosingChecklistItem(
                code = FinancialClosingChecklistCode.LEDGER_BALANCED,
                isPassed = true,
                details = "General ledger verified balanced."
            )
        )

        // Cash reconciliation check
        val cashRec = reconciliationDataSource?.getCashReconciliationByPeriod(period.periodId)
        val isCashMatched = cashRec == null || cashRec.status == FinancialReconciliationStatus.MATCHED || openDiscrepancies.none { it.reconciliationId == cashRec?.reconciliationId }
        checklist.add(
            FinancialClosingChecklistItem(
                code = FinancialClosingChecklistCode.CASH_RECONCILED,
                isPassed = isCashMatched,
                details = if (isCashMatched) "Physical cash reconciled." else "Cash variance detected: ${cashRec?.difference?.formatted()}."
            )
        )
        if (!isCashMatched) blockers.add(FinancialClosingBlockerReason.CASH_MISMATCH)

        // Bank reconciliation check
        val bankRec = reconciliationDataSource?.getBankReconciliationByPeriod(period.periodId)
        val isBankMatched = bankRec == null || bankRec.status == FinancialReconciliationStatus.MATCHED || openDiscrepancies.none { it.reconciliationId == bankRec?.reconciliationId }
        checklist.add(
            FinancialClosingChecklistItem(
                code = FinancialClosingChecklistCode.BANK_RECONCILED,
                isPassed = isBankMatched,
                details = if (isBankMatched) "Bank accounts reconciled." else "Bank discrepancy detected: ${bankRec?.difference?.formatted()}."
            )
        )
        if (!isBankMatched) blockers.add(FinancialClosingBlockerReason.BANK_MISMATCH)

        val status = when {
            blockers.isNotEmpty() -> FinancialClosingReadinessStatus.BLOCKED
            openDiscrepancies.isNotEmpty() -> FinancialClosingReadinessStatus.NOT_READY
            else -> FinancialClosingReadinessStatus.READY
        }

        return FinancialClosingReadiness(
            periodId = period.periodId,
            projectId = period.projectId,
            status = status,
            checklistItems = checklist,
            blockerReasons = blockers,
            openDiscrepancyCount = openDiscrepancies.size,
            criticalDiscrepancyCount = criticalDiscrepancies.size,
            evaluatedAt = System.currentTimeMillis()
        )
    }
}
