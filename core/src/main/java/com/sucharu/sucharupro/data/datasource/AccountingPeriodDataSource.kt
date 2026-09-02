package com.sucharu.sucharupro.data.datasource

import com.sucharu.sucharupro.domain.model.finance.AccountingPeriod
import com.sucharu.sucharupro.domain.model.finance.FinancialPeriodReopenRequest
import com.sucharu.sucharupro.domain.model.finance.FinancialReconciliationActivityEvent
import kotlinx.coroutines.flow.Flow

/**
 * Data Source contract for Accounting Period persistence and lifecycle management (Module 09 Step 08).
 */
interface AccountingPeriodDataSource {
    suspend fun insertPeriod(period: AccountingPeriod): Boolean
    suspend fun updatePeriod(period: AccountingPeriod): Boolean
    suspend fun getPeriodById(periodId: String): AccountingPeriod?
    suspend fun getPeriodByNumber(projectId: String, periodNo: String): AccountingPeriod?
    suspend fun getPeriodsByProject(projectId: String): List<AccountingPeriod>
    fun observePeriods(projectId: String): Flow<List<AccountingPeriod>>
    suspend fun generateNextPeriodNo(projectId: String): String

    // Reopen requests
    suspend fun insertReopenRequest(request: FinancialPeriodReopenRequest): Boolean
    suspend fun updateReopenRequest(request: FinancialPeriodReopenRequest): Boolean
    suspend fun getReopenRequestById(requestId: String): FinancialPeriodReopenRequest?
    suspend fun getReopenRequestsByPeriod(periodId: String): List<FinancialPeriodReopenRequest>
    suspend fun generateNextReopenRequestNo(projectId: String): String

    // Activity Events
    suspend fun insertActivityEvent(event: FinancialReconciliationActivityEvent): Boolean
    suspend fun getActivityEvents(entityId: String): List<FinancialReconciliationActivityEvent>
}
