package com.sucharu.sucharupro.domain.repository

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.finance.AccountingPeriod
import com.sucharu.sucharupro.domain.model.finance.FinancialClosingReadiness
import com.sucharu.sucharupro.domain.model.finance.FinancialPeriodClosingSnapshot
import com.sucharu.sucharupro.domain.model.finance.FinancialPeriodReopenRequest
import com.sucharu.sucharupro.domain.model.finance.FinancialReconciliationActivityEvent
import com.sucharu.sucharupro.domain.model.user.UserRole
import kotlinx.coroutines.flow.Flow

/**
 * Domain repository contract for Accounting Periods, Period Closing & Lock enforcement (Module 09 Step 08).
 */
interface AccountingPeriodRepository {

    suspend fun createAccountingPeriod(
        projectId: String,
        periodName: String,
        startDate: Long,
        endDate: Long,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<AccountingPeriod>

    suspend fun getAccountingPeriod(
        periodId: String,
        callerRole: UserRole
    ): DomainResult<AccountingPeriod>

    suspend fun getPeriodByDate(
        projectId: String,
        date: Long,
        callerRole: UserRole
    ): DomainResult<AccountingPeriod?>

    fun observeAccountingPeriods(
        projectId: String,
        callerRole: UserRole
    ): Flow<List<AccountingPeriod>>

    suspend fun updateAccountingPeriod(
        periodId: String,
        periodName: String? = null,
        startDate: Long? = null,
        endDate: Long? = null,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<AccountingPeriod>

    suspend fun submitPeriodForClosing(
        periodId: String,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<AccountingPeriod>

    suspend fun evaluateClosingReadiness(
        periodId: String,
        callerRole: UserRole
    ): DomainResult<FinancialClosingReadiness>

    suspend fun closeAccountingPeriod(
        periodId: String,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<FinancialPeriodClosingSnapshot>

    suspend fun reopenAccountingPeriod(
        periodId: String,
        requestId: String,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<AccountingPeriod>

    suspend fun getCurrentOpenPeriod(
        projectId: String,
        callerRole: UserRole
    ): DomainResult<AccountingPeriod?>

    suspend fun getFinancialClosingSnapshot(
        periodId: String,
        callerRole: UserRole
    ): DomainResult<FinancialPeriodClosingSnapshot>

    fun observeClosingSnapshots(
        projectId: String,
        callerRole: UserRole
    ): Flow<List<FinancialPeriodClosingSnapshot>>

    suspend fun createReopenRequest(
        projectId: String,
        periodId: String,
        reason: String,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<FinancialPeriodReopenRequest>

    suspend fun approveReopenRequest(
        requestId: String,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<FinancialPeriodReopenRequest>

    suspend fun rejectReopenRequest(
        requestId: String,
        rejectionReason: String,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<FinancialPeriodReopenRequest>

    suspend fun getReopenRequestsByPeriod(
        periodId: String,
        callerRole: UserRole
    ): DomainResult<List<FinancialPeriodReopenRequest>>

    fun observeReopenRequests(
        projectId: String,
        callerRole: UserRole
    ): Flow<List<FinancialPeriodReopenRequest>>

    suspend fun getActivityEvents(
        entityId: String,
        callerRole: UserRole
    ): DomainResult<List<FinancialReconciliationActivityEvent>>
}
