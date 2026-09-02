package com.sucharu.sucharupro.domain.repository

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.common.Money
import com.sucharu.sucharupro.domain.model.finance.BankReconciliation
import com.sucharu.sucharupro.domain.model.finance.CashReconciliation
import com.sucharu.sucharupro.domain.model.finance.FinancialControlSummary
import com.sucharu.sucharupro.domain.model.finance.FinancialEntryType
import com.sucharu.sucharupro.domain.model.finance.FinancialLedgerEntry
import com.sucharu.sucharupro.domain.model.finance.FinancialReconciliation
import com.sucharu.sucharupro.domain.model.finance.FinancialReconciliationActivityEvent
import com.sucharu.sucharupro.domain.model.finance.FinancialReconciliationDiscrepancy
import com.sucharu.sucharupro.domain.model.finance.FinancialReconciliationType
import com.sucharu.sucharupro.domain.model.finance.FinancialTransaction
import com.sucharu.sucharupro.domain.model.finance.LedgerReconciliationReport
import com.sucharu.sucharupro.domain.model.user.UserRole
import kotlinx.coroutines.flow.Flow

/**
 * Domain repository contract for Financial Reconciliations, Discrepancies & Control (Module 09 Step 08).
 */
interface FinancialReconciliationRepository {

    suspend fun createReconciliation(
        projectId: String,
        periodId: String,
        type: FinancialReconciliationType,
        referenceId: String? = null,
        expectedAmount: Money,
        actualAmount: Money,
        currency: String = "BDT",
        notes: String? = null,
        idempotencyKey: String? = null,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<FinancialReconciliation>

    suspend fun getReconciliation(
        reconciliationId: String,
        callerRole: UserRole
    ): DomainResult<FinancialReconciliation>

    suspend fun getReconciliationByNumber(
        projectId: String,
        reconciliationNo: String,
        callerRole: UserRole
    ): DomainResult<FinancialReconciliation>

    suspend fun getReconciliationsByPeriod(
        periodId: String,
        callerRole: UserRole
    ): DomainResult<List<FinancialReconciliation>>

    fun observeReconciliations(
        projectId: String,
        callerRole: UserRole
    ): Flow<List<FinancialReconciliation>>

    fun observePeriodReconciliations(
        projectId: String,
        periodId: String,
        callerRole: UserRole
    ): Flow<List<FinancialReconciliation>>

    suspend fun executeReconciliation(
        reconciliationId: String,
        actualAmount: Money? = null,
        notes: String? = null,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<FinancialReconciliation>

    suspend fun completeReconciliation(
        reconciliationId: String,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<FinancialReconciliation>

    suspend fun reopenReconciliation(
        reconciliationId: String,
        reason: String,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<FinancialReconciliation>

    suspend fun executeCashReconciliation(
        projectId: String,
        periodId: String,
        openingCash: Money,
        cashReceipts: Money,
        cashPayments: Money,
        cashAdjustments: Money = Money.ZERO,
        actualClosingCash: Money,
        notes: String? = null,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<CashReconciliation>

    suspend fun getCashReconciliation(
        periodId: String,
        callerRole: UserRole
    ): DomainResult<CashReconciliation>

    suspend fun executeBankReconciliation(
        projectId: String,
        periodId: String,
        bankAccountId: String = "PRIMARY_BANK",
        bankName: String = "Standard Commercial Bank",
        openingBankBalance: Money,
        ledgerDeposits: Money,
        ledgerWithdrawals: Money,
        bankStatementBalance: Money,
        outstandingDeposits: Money = Money.ZERO,
        outstandingWithdrawals: Money = Money.ZERO,
        adjustments: Money = Money.ZERO,
        notes: String? = null,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<BankReconciliation>

    suspend fun getBankReconciliation(
        periodId: String,
        callerRole: UserRole
    ): DomainResult<BankReconciliation>

    suspend fun executeLedgerReconciliation(
        projectId: String,
        periodId: String,
        transactions: List<FinancialTransaction>,
        ledgerEntries: List<FinancialLedgerEntry>,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<LedgerReconciliationReport>

    suspend fun getDiscrepancies(
        projectId: String,
        periodId: String? = null,
        callerRole: UserRole
    ): DomainResult<List<FinancialReconciliationDiscrepancy>>

    fun observeDiscrepancies(
        projectId: String,
        periodId: String? = null,
        callerRole: UserRole
    ): Flow<List<FinancialReconciliationDiscrepancy>>

    suspend fun getDiscrepancyById(
        discrepancyId: String,
        callerRole: UserRole
    ): DomainResult<FinancialReconciliationDiscrepancy>

    suspend fun resolveDiscrepancy(
        discrepancyId: String,
        resolutionNote: String,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<FinancialReconciliationDiscrepancy>

    suspend fun waiveDiscrepancy(
        discrepancyId: String,
        waiverReason: String,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<FinancialReconciliationDiscrepancy>

    suspend fun getFinancialControlSummary(
        projectId: String,
        periodId: String? = null,
        callerRole: UserRole
    ): DomainResult<FinancialControlSummary>

    suspend fun getReconciliationHistory(
        entityId: String,
        callerRole: UserRole
    ): DomainResult<List<FinancialReconciliationActivityEvent>>
}
