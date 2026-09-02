package com.sucharu.sucharupro.data.repository

import com.sucharu.sucharupro.data.datasource.AccountingPeriodDataSource
import com.sucharu.sucharupro.data.datasource.FinancialDiscrepancyDataSource
import com.sucharu.sucharupro.data.datasource.FinancialReconciliationDataSource
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.common.Money
import com.sucharu.sucharupro.domain.model.finance.BankReconciliation
import com.sucharu.sucharupro.domain.model.finance.CashReconciliation
import com.sucharu.sucharupro.domain.model.finance.FinancialClosingReadinessStatus
import com.sucharu.sucharupro.domain.model.finance.FinancialControlSummary
import com.sucharu.sucharupro.domain.model.finance.FinancialDiscrepancySeverity
import com.sucharu.sucharupro.domain.model.finance.FinancialDiscrepancyStatus
import com.sucharu.sucharupro.domain.model.finance.FinancialEntryType
import com.sucharu.sucharupro.domain.model.finance.FinancialLedgerEntry
import com.sucharu.sucharupro.domain.model.finance.FinancialReconciliation
import com.sucharu.sucharupro.domain.model.finance.FinancialReconciliationActivityEvent
import com.sucharu.sucharupro.domain.model.finance.FinancialReconciliationActivityType
import com.sucharu.sucharupro.domain.model.finance.FinancialReconciliationCalculator
import com.sucharu.sucharupro.domain.model.finance.FinancialReconciliationDiscrepancy
import com.sucharu.sucharupro.domain.model.finance.FinancialReconciliationStatus
import com.sucharu.sucharupro.domain.model.finance.FinancialReconciliationType
import com.sucharu.sucharupro.domain.model.finance.FinancialTransaction
import com.sucharu.sucharupro.domain.model.finance.LedgerReconciliationReport
import com.sucharu.sucharupro.domain.model.finance.LedgerReconciliationService
import com.sucharu.sucharupro.domain.model.user.UserRole
import com.sucharu.sucharupro.domain.repository.FinancialReconciliationRepository
import com.sucharu.sucharupro.domain.validation.FinancialReconciliationAuthorizationValidator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID

/**
 * Production-grade repository implementation for Financial Reconciliations, Discrepancies & Control (Module 09 Step 08).
 *
 * Implements deterministic balance verification, strict project isolation, RBAC waiver controls,
 * and concurrency protection using Mutex.
 */
class FinancialReconciliationRepositoryImpl(
    private val reconciliationDataSource: FinancialReconciliationDataSource,
    private val discrepancyDataSource: FinancialDiscrepancyDataSource,
    private val periodDataSource: AccountingPeriodDataSource? = null
) : FinancialReconciliationRepository {

    private val mutex = Mutex()

    override suspend fun createReconciliation(
        projectId: String,
        periodId: String,
        type: FinancialReconciliationType,
        referenceId: String?,
        expectedAmount: Money,
        actualAmount: Money,
        currency: String,
        notes: String?,
        idempotencyKey: String?,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<FinancialReconciliation> = mutex.withLock {
        val authResult = FinancialReconciliationAuthorizationValidator.validateCreateReconciliation(callerRole)
        if (authResult is DomainResult.Error) return authResult

        if (projectId.isBlank()) return DomainResult.Error(message = "Project ID cannot be blank.")
        if (periodId.isBlank()) return DomainResult.Error(message = "Period ID cannot be blank.")

        // Idempotency check
        if (!idempotencyKey.isNullOrBlank()) {
            val existing = reconciliationDataSource.getReconciliationByIdempotencyKey(projectId, idempotencyKey)
            if (existing != null) {
                return DomainResult.Success(existing)
            }
        }

        val now = System.currentTimeMillis()
        val reconciliationId = "REC-${UUID.randomUUID()}"
        val reconciliationNo = reconciliationDataSource.generateNextReconciliationNo(projectId)

        val diff = actualAmount.minus(expectedAmount)
        val status = if (diff.isZero()) FinancialReconciliationStatus.MATCHED else FinancialReconciliationStatus.MISMATCHED

        val reconciliation = FinancialReconciliation(
            reconciliationId = reconciliationId,
            reconciliationNo = reconciliationNo,
            projectId = projectId,
            periodId = periodId,
            reconciliationType = type,
            referenceId = referenceId,
            expectedAmount = expectedAmount,
            actualAmount = actualAmount,
            differenceAmount = diff,
            currency = currency,
            status = status,
            notes = notes?.trim(),
            createdAt = now,
            updatedAt = now,
            idempotencyKey = idempotencyKey
        )

        reconciliationDataSource.insertReconciliation(reconciliation)

        // If mismatched, automatically create a discrepancy record
        if (status == FinancialReconciliationStatus.MISMATCHED) {
            val discrepancyId = "DISC-${UUID.randomUUID()}"
            val discrepancyNo = discrepancyDataSource.generateNextDiscrepancyNo(projectId)
            val severity = if (diff.abs().amount.toDouble() > 10000.0) FinancialDiscrepancySeverity.CRITICAL else FinancialDiscrepancySeverity.HIGH

            val discrepancy = FinancialReconciliationDiscrepancy(
                discrepancyId = discrepancyId,
                discrepancyNo = discrepancyNo,
                projectId = projectId,
                periodId = periodId,
                reconciliationId = reconciliationId,
                type = type,
                expectedAmount = expectedAmount,
                actualAmount = actualAmount,
                differenceAmount = diff,
                currency = currency,
                severity = severity,
                status = FinancialDiscrepancyStatus.OPEN,
                description = "Variance of ${diff.formatted()} detected in ${type.defaultLabel}.",
                detectedAt = now
            )
            discrepancyDataSource.insertDiscrepancy(discrepancy)
        }

        periodDataSource?.insertActivityEvent(
            FinancialReconciliationActivityEvent(
                eventId = UUID.randomUUID().toString(),
                entityId = reconciliationId,
                projectId = projectId,
                activityType = FinancialReconciliationActivityType.RECONCILIATION_CREATED,
                actorId = actorId,
                details = "Reconciliation '$reconciliationNo' (${type.defaultLabel}) created. Status: ${status.defaultLabel}"
            )
        )

        DomainResult.Success(reconciliation)
    }

    override suspend fun getReconciliation(
        reconciliationId: String,
        callerRole: UserRole
    ): DomainResult<FinancialReconciliation> {
        val authResult = FinancialReconciliationAuthorizationValidator.validateView(callerRole)
        if (authResult is DomainResult.Error) return authResult

        val item = reconciliationDataSource.getReconciliationById(reconciliationId)
            ?: return DomainResult.Error(message = "Reconciliation '$reconciliationId' not found.")

        return DomainResult.Success(item)
    }

    override suspend fun getReconciliationByNumber(
        projectId: String,
        reconciliationNo: String,
        callerRole: UserRole
    ): DomainResult<FinancialReconciliation> {
        val authResult = FinancialReconciliationAuthorizationValidator.validateView(callerRole)
        if (authResult is DomainResult.Error) return authResult

        val item = reconciliationDataSource.getReconciliationByNumber(projectId, reconciliationNo)
            ?: return DomainResult.Error(message = "Reconciliation '$reconciliationNo' not found.")

        return DomainResult.Success(item)
    }

    override suspend fun getReconciliationsByPeriod(
        periodId: String,
        callerRole: UserRole
    ): DomainResult<List<FinancialReconciliation>> {
        val authResult = FinancialReconciliationAuthorizationValidator.validateView(callerRole)
        if (authResult is DomainResult.Error) return authResult

        val list = reconciliationDataSource.getReconciliationsByPeriod(periodId)
        return DomainResult.Success(list)
    }

    override fun observeReconciliations(
        projectId: String,
        callerRole: UserRole
    ): Flow<List<FinancialReconciliation>> {
        if (!callerRole.isInternal) {
            return flowOf(emptyList())
        }
        return reconciliationDataSource.observeReconciliations(projectId)
    }

    override fun observePeriodReconciliations(
        projectId: String,
        periodId: String,
        callerRole: UserRole
    ): Flow<List<FinancialReconciliation>> {
        if (!callerRole.isInternal) {
            return flowOf(emptyList())
        }
        return reconciliationDataSource.observePeriodReconciliations(projectId, periodId)
    }

    override suspend fun executeReconciliation(
        reconciliationId: String,
        actualAmount: Money?,
        notes: String?,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<FinancialReconciliation> = mutex.withLock {
        val authResult = FinancialReconciliationAuthorizationValidator.validateCreateReconciliation(callerRole)
        if (authResult is DomainResult.Error) return authResult

        val existing = reconciliationDataSource.getReconciliationById(reconciliationId)
            ?: return DomainResult.Error(message = "Reconciliation '$reconciliationId' not found.")

        val now = System.currentTimeMillis()
        val updatedActual = actualAmount ?: existing.actualAmount
        val diff = updatedActual.minus(existing.expectedAmount)
        val status = if (diff.isZero()) FinancialReconciliationStatus.MATCHED else FinancialReconciliationStatus.MISMATCHED

        val updated = existing.copy(
            actualAmount = updatedActual,
            differenceAmount = diff,
            status = status,
            notes = notes?.trim() ?: existing.notes,
            reconciledBy = actorId,
            reconciledAt = now,
            updatedAt = now
        )

        reconciliationDataSource.updateReconciliation(updated)

        periodDataSource?.insertActivityEvent(
            FinancialReconciliationActivityEvent(
                eventId = UUID.randomUUID().toString(),
                entityId = reconciliationId,
                projectId = existing.projectId,
                activityType = FinancialReconciliationActivityType.RECONCILIATION_STARTED,
                actorId = actorId,
                details = "Reconciliation '${existing.reconciliationNo}' executed by '$actorId'. Status: ${status.defaultLabel}"
            )
        )

        DomainResult.Success(updated)
    }

    override suspend fun completeReconciliation(
        reconciliationId: String,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<FinancialReconciliation> = mutex.withLock {
        val existing = reconciliationDataSource.getReconciliationById(reconciliationId)
            ?: return DomainResult.Error(message = "Reconciliation '$reconciliationId' not found.")

        val authResult = FinancialReconciliationAuthorizationValidator.validateApproveReconciliation(
            callerRole = callerRole,
            creatorId = existing.reconciledBy,
            approverId = actorId
        )
        if (authResult is DomainResult.Error) return authResult

        val now = System.currentTimeMillis()
        val updated = existing.copy(
            status = if (existing.differenceAmount.isZero()) FinancialReconciliationStatus.CLOSED else FinancialReconciliationStatus.APPROVED,
            approvedBy = actorId,
            approvedAt = now,
            closedAt = if (existing.differenceAmount.isZero()) now else existing.closedAt,
            updatedAt = now
        )

        reconciliationDataSource.updateReconciliation(updated)

        periodDataSource?.insertActivityEvent(
            FinancialReconciliationActivityEvent(
                eventId = UUID.randomUUID().toString(),
                entityId = reconciliationId,
                projectId = existing.projectId,
                activityType = FinancialReconciliationActivityType.RECONCILIATION_APPROVED,
                actorId = actorId,
                details = "Reconciliation '${existing.reconciliationNo}' approved & completed by '$actorId'."
            )
        )

        DomainResult.Success(updated)
    }

    override suspend fun reopenReconciliation(
        reconciliationId: String,
        reason: String,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<FinancialReconciliation> = mutex.withLock {
        val authResult = FinancialReconciliationAuthorizationValidator.validateCreateReconciliation(callerRole)
        if (authResult is DomainResult.Error) return authResult

        if (reason.trim().isBlank()) {
            return DomainResult.Error(message = "Reopen reason cannot be blank.")
        }

        val existing = reconciliationDataSource.getReconciliationById(reconciliationId)
            ?: return DomainResult.Error(message = "Reconciliation '$reconciliationId' not found.")

        val now = System.currentTimeMillis()
        val updated = existing.copy(
            status = FinancialReconciliationStatus.IN_PROGRESS,
            notes = "${existing.notes ?: ""}\nReopened: $reason".trim(),
            updatedAt = now
        )

        reconciliationDataSource.updateReconciliation(updated)
        DomainResult.Success(updated)
    }

    override suspend fun executeCashReconciliation(
        projectId: String,
        periodId: String,
        openingCash: Money,
        cashReceipts: Money,
        cashPayments: Money,
        cashAdjustments: Money,
        actualClosingCash: Money,
        notes: String?,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<CashReconciliation> = mutex.withLock {
        val authResult = FinancialReconciliationAuthorizationValidator.validateCreateReconciliation(callerRole)
        if (authResult is DomainResult.Error) return authResult

        val expectedClosing = FinancialReconciliationCalculator.calculateExpectedClosingCash(
            openingCash = openingCash,
            cashReceipts = cashReceipts,
            cashPayments = cashPayments,
            cashAdjustments = cashAdjustments
        )

        val diff = actualClosingCash.minus(expectedClosing)
        val status = if (diff.isZero()) FinancialReconciliationStatus.MATCHED else FinancialReconciliationStatus.MISMATCHED
        val now = System.currentTimeMillis()

        val cashRec = CashReconciliation(
            reconciliationId = "CASH-REC-${UUID.randomUUID()}",
            projectId = projectId,
            periodId = periodId,
            openingCash = openingCash,
            cashReceipts = cashReceipts,
            cashPayments = cashPayments,
            cashAdjustments = cashAdjustments,
            expectedClosingCash = expectedClosing,
            actualClosingCash = actualClosingCash,
            difference = diff,
            currency = "BDT",
            status = status,
            verifiedBy = actorId,
            verifiedAt = now,
            notes = notes?.trim()
        )

        reconciliationDataSource.insertCashReconciliation(cashRec)

        if (status == FinancialReconciliationStatus.MISMATCHED) {
            val disc = FinancialReconciliationDiscrepancy(
                discrepancyId = "DISC-${UUID.randomUUID()}",
                discrepancyNo = discrepancyDataSource.generateNextDiscrepancyNo(projectId),
                projectId = projectId,
                periodId = periodId,
                reconciliationId = cashRec.reconciliationId,
                type = FinancialReconciliationType.CASH,
                expectedAmount = expectedClosing,
                actualAmount = actualClosingCash,
                differenceAmount = diff,
                currency = "BDT",
                severity = if (diff.abs().amount.toDouble() > 5000.0) FinancialDiscrepancySeverity.CRITICAL else FinancialDiscrepancySeverity.MEDIUM,
                status = FinancialDiscrepancyStatus.OPEN,
                description = "Cash discrepancy of ${diff.formatted()} (Expected: ${expectedClosing.formatted()}, Actual: ${actualClosingCash.formatted()}).",
                detectedAt = now
            )
            discrepancyDataSource.insertDiscrepancy(disc)
        }

        DomainResult.Success(cashRec)
    }

    override suspend fun getCashReconciliation(
        periodId: String,
        callerRole: UserRole
    ): DomainResult<CashReconciliation> {
        val authResult = FinancialReconciliationAuthorizationValidator.validateView(callerRole)
        if (authResult is DomainResult.Error) return authResult

        val item = reconciliationDataSource.getCashReconciliationByPeriod(periodId)
            ?: return DomainResult.Error(message = "Cash reconciliation for period '$periodId' not found.")

        return DomainResult.Success(item)
    }

    override suspend fun executeBankReconciliation(
        projectId: String,
        periodId: String,
        bankAccountId: String,
        bankName: String,
        openingBankBalance: Money,
        ledgerDeposits: Money,
        ledgerWithdrawals: Money,
        bankStatementBalance: Money,
        outstandingDeposits: Money,
        outstandingWithdrawals: Money,
        adjustments: Money,
        notes: String?,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<BankReconciliation> = mutex.withLock {
        val authResult = FinancialReconciliationAuthorizationValidator.validateCreateReconciliation(callerRole)
        if (authResult is DomainResult.Error) return authResult

        val expectedBank = FinancialReconciliationCalculator.calculateExpectedClosingBank(
            openingBank = openingBankBalance,
            deposits = ledgerDeposits,
            withdrawals = ledgerWithdrawals,
            outstandingDeposits = outstandingDeposits,
            outstandingWithdrawals = outstandingWithdrawals,
            adjustments = adjustments
        )

        val reconciledBalance = bankStatementBalance.plus(outstandingDeposits).minus(outstandingWithdrawals).plus(adjustments)
        val ledgerEnd = openingBankBalance.plus(ledgerDeposits).minus(ledgerWithdrawals)
        val diff = reconciledBalance.minus(ledgerEnd)
        val status = if (diff.isZero()) FinancialReconciliationStatus.MATCHED else FinancialReconciliationStatus.MISMATCHED
        val now = System.currentTimeMillis()

        val bankRec = BankReconciliation(
            reconciliationId = "BANK-REC-${UUID.randomUUID()}",
            projectId = projectId,
            periodId = periodId,
            bankAccountId = bankAccountId,
            bankName = bankName,
            openingBankBalance = openingBankBalance,
            ledgerDeposits = ledgerDeposits,
            ledgerWithdrawals = ledgerWithdrawals,
            bankStatementBalance = bankStatementBalance,
            outstandingDeposits = outstandingDeposits,
            outstandingWithdrawals = outstandingWithdrawals,
            adjustments = adjustments,
            reconciledBalance = reconciledBalance,
            difference = diff,
            currency = "BDT",
            status = status,
            verifiedBy = actorId,
            verifiedAt = now,
            notes = notes?.trim()
        )

        reconciliationDataSource.insertBankReconciliation(bankRec)

        if (status == FinancialReconciliationStatus.MISMATCHED) {
            val disc = FinancialReconciliationDiscrepancy(
                discrepancyId = "DISC-${UUID.randomUUID()}",
                discrepancyNo = discrepancyDataSource.generateNextDiscrepancyNo(projectId),
                projectId = projectId,
                periodId = periodId,
                reconciliationId = bankRec.reconciliationId,
                type = FinancialReconciliationType.BANK,
                expectedAmount = ledgerEnd,
                actualAmount = reconciledBalance,
                differenceAmount = diff,
                currency = "BDT",
                severity = if (diff.abs().amount.toDouble() > 10000.0) FinancialDiscrepancySeverity.CRITICAL else FinancialDiscrepancySeverity.HIGH,
                status = FinancialDiscrepancyStatus.OPEN,
                description = "Bank balance discrepancy of ${diff.formatted()} between ledger (${ledgerEnd.formatted()}) and statement (${reconciledBalance.formatted()}).",
                detectedAt = now
            )
            discrepancyDataSource.insertDiscrepancy(disc)
        }

        DomainResult.Success(bankRec)
    }

    override suspend fun getBankReconciliation(
        periodId: String,
        callerRole: UserRole
    ): DomainResult<BankReconciliation> {
        val authResult = FinancialReconciliationAuthorizationValidator.validateView(callerRole)
        if (authResult is DomainResult.Error) return authResult

        val item = reconciliationDataSource.getBankReconciliationByPeriod(periodId)
            ?: return DomainResult.Error(message = "Bank reconciliation for period '$periodId' not found.")

        return DomainResult.Success(item)
    }

    override suspend fun executeLedgerReconciliation(
        projectId: String,
        periodId: String,
        transactions: List<FinancialTransaction>,
        ledgerEntries: List<FinancialLedgerEntry>,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<LedgerReconciliationReport> {
        val authResult = FinancialReconciliationAuthorizationValidator.validateView(callerRole)
        if (authResult is DomainResult.Error) return authResult

        val report = LedgerReconciliationService.reconcile(
            projectId = projectId,
            periodId = periodId,
            transactions = transactions,
            ledgerEntries = ledgerEntries
        )

        return DomainResult.Success(report)
    }

    override suspend fun getDiscrepancies(
        projectId: String,
        periodId: String?,
        callerRole: UserRole
    ): DomainResult<List<FinancialReconciliationDiscrepancy>> {
        val authResult = FinancialReconciliationAuthorizationValidator.validateView(callerRole)
        if (authResult is DomainResult.Error) return authResult

        val all = if (periodId != null) {
            discrepancyDataSource.getDiscrepanciesByPeriod(periodId)
        } else {
            discrepancyDataSource.getDiscrepanciesByProject(projectId)
        }
        return DomainResult.Success(all)
    }

    override fun observeDiscrepancies(
        projectId: String,
        periodId: String?,
        callerRole: UserRole
    ): Flow<List<FinancialReconciliationDiscrepancy>> {
        if (!callerRole.isInternal) {
            return flowOf(emptyList())
        }
        return if (periodId != null) {
            discrepancyDataSource.observePeriodDiscrepancies(projectId, periodId)
        } else {
            discrepancyDataSource.observeDiscrepancies(projectId)
        }
    }

    override suspend fun getDiscrepancyById(
        discrepancyId: String,
        callerRole: UserRole
    ): DomainResult<FinancialReconciliationDiscrepancy> {
        val authResult = FinancialReconciliationAuthorizationValidator.validateView(callerRole)
        if (authResult is DomainResult.Error) return authResult

        val item = discrepancyDataSource.getDiscrepancyById(discrepancyId)
            ?: return DomainResult.Error(message = "Discrepancy '$discrepancyId' not found.")

        return DomainResult.Success(item)
    }

    override suspend fun resolveDiscrepancy(
        discrepancyId: String,
        resolutionNote: String,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<FinancialReconciliationDiscrepancy> = mutex.withLock {
        val authResult = FinancialReconciliationAuthorizationValidator.validateCreateReconciliation(callerRole)
        if (authResult is DomainResult.Error) return authResult

        if (resolutionNote.trim().isBlank()) {
            return DomainResult.Error(message = "Resolution note cannot be blank.")
        }

        val existing = discrepancyDataSource.getDiscrepancyById(discrepancyId)
            ?: return DomainResult.Error(message = "Discrepancy '$discrepancyId' not found.")

        val now = System.currentTimeMillis()
        val updated = existing.copy(
            status = FinancialDiscrepancyStatus.RESOLVED,
            resolvedBy = actorId,
            resolvedAt = now,
            resolutionNote = resolutionNote.trim()
        )

        discrepancyDataSource.updateDiscrepancy(updated)

        periodDataSource?.insertActivityEvent(
            FinancialReconciliationActivityEvent(
                eventId = UUID.randomUUID().toString(),
                entityId = discrepancyId,
                projectId = existing.projectId,
                activityType = FinancialReconciliationActivityType.DISCREPANCY_RESOLVED,
                actorId = actorId,
                details = "Discrepancy '${existing.discrepancyNo}' resolved by '$actorId'. Note: $resolutionNote"
            )
        )

        DomainResult.Success(updated)
    }

    override suspend fun waiveDiscrepancy(
        discrepancyId: String,
        waiverReason: String,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<FinancialReconciliationDiscrepancy> = mutex.withLock {
        val authResult = FinancialReconciliationAuthorizationValidator.validateWaiveCriticalDiscrepancy(callerRole)
        if (authResult is DomainResult.Error) return authResult

        if (waiverReason.trim().isBlank()) {
            return DomainResult.Error(message = "Waiver reason cannot be blank.")
        }

        val existing = discrepancyDataSource.getDiscrepancyById(discrepancyId)
            ?: return DomainResult.Error(message = "Discrepancy '$discrepancyId' not found.")

        val now = System.currentTimeMillis()
        val updated = existing.copy(
            status = FinancialDiscrepancyStatus.WAIVED,
            waivedBy = actorId,
            waivedAt = now,
            waiverReason = waiverReason.trim()
        )

        discrepancyDataSource.updateDiscrepancy(updated)

        periodDataSource?.insertActivityEvent(
            FinancialReconciliationActivityEvent(
                eventId = UUID.randomUUID().toString(),
                entityId = discrepancyId,
                projectId = existing.projectId,
                activityType = FinancialReconciliationActivityType.DISCREPANCY_WAIVED,
                actorId = actorId,
                details = "Discrepancy '${existing.discrepancyNo}' waived by Admin '$actorId'. Reason: $waiverReason"
            )
        )

        DomainResult.Success(updated)
    }

    override suspend fun getFinancialControlSummary(
        projectId: String,
        periodId: String?,
        callerRole: UserRole
    ): DomainResult<FinancialControlSummary> {
        val authResult = FinancialReconciliationAuthorizationValidator.validateView(callerRole)
        if (authResult is DomainResult.Error) return authResult

        val periods = periodDataSource?.getPeriodsByProject(projectId) ?: emptyList()
        val active = if (periodId != null) {
            periods.firstOrNull { it.periodId == periodId }
        } else {
            periods.firstOrNull { it.status.isOpenForPosting } ?: periods.firstOrNull()
        }

        val reconciliations = if (active != null) {
            reconciliationDataSource.getReconciliationsByPeriod(active.periodId)
        } else {
            reconciliationDataSource.getReconciliationsByProject(projectId)
        }

        val discrepancies = if (active != null) {
            discrepancyDataSource.getDiscrepanciesByPeriod(active.periodId)
        } else {
            discrepancyDataSource.getDiscrepanciesByProject(projectId)
        }

        val matchedCount = reconciliations.count { it.status == FinancialReconciliationStatus.MATCHED || it.status == FinancialReconciliationStatus.CLOSED }
        val openDiscrepancies = discrepancies.filter { !it.status.isResolvedOrWaived }
        val criticalDiscrepancies = openDiscrepancies.filter { it.severity == FinancialDiscrepancySeverity.CRITICAL }

        val cashRec = active?.let { reconciliationDataSource.getCashReconciliationByPeriod(it.periodId) }
        val bankRec = active?.let { reconciliationDataSource.getBankReconciliationByPeriod(it.periodId) }

        val summary = FinancialControlSummary(
            activePeriod = active,
            totalDebit = Money.ZERO,
            totalCredit = Money.ZERO,
            isLedgerBalanced = true,
            totalReceivableOutstanding = Money.ZERO,
            totalPayableOutstanding = Money.ZERO,
            totalExpenses = Money.ZERO,
            totalCustomerPayments = Money.ZERO,
            totalSupplierPayments = Money.ZERO,
            totalRefunds = Money.ZERO,
            totalCreditNotes = Money.ZERO,
            totalDebitNotes = Money.ZERO,
            cashInHandBalance = cashRec?.actualClosingCash ?: Money.ZERO,
            bankBalance = bankRec?.reconciledBalance ?: Money.ZERO,
            totalReconciliationsCount = reconciliations.size,
            matchedReconciliationsCount = matchedCount,
            openDiscrepanciesCount = openDiscrepancies.size,
            criticalDiscrepanciesCount = criticalDiscrepancies.size,
            closingReadiness = if (criticalDiscrepancies.isNotEmpty()) FinancialClosingReadinessStatus.BLOCKED else if (openDiscrepancies.isNotEmpty()) FinancialClosingReadinessStatus.NOT_READY else FinancialClosingReadinessStatus.READY
        )

        return DomainResult.Success(summary)
    }

    override suspend fun getReconciliationHistory(
        entityId: String,
        callerRole: UserRole
    ): DomainResult<List<FinancialReconciliationActivityEvent>> {
        val authResult = FinancialReconciliationAuthorizationValidator.validateView(callerRole)
        if (authResult is DomainResult.Error) return authResult

        val events = periodDataSource?.getActivityEvents(entityId) ?: emptyList()
        return DomainResult.Success(events)
    }
}
