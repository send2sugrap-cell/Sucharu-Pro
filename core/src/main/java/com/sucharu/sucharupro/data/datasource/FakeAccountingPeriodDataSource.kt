package com.sucharu.sucharupro.data.datasource

import com.sucharu.sucharupro.domain.model.finance.AccountingPeriod
import com.sucharu.sucharupro.domain.model.finance.FinancialPeriodReopenRequest
import com.sucharu.sucharupro.domain.model.finance.FinancialReconciliationActivityEvent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * In-memory thread-safe fake data source for Accounting Period persistence (Module 09 Step 08).
 */
class FakeAccountingPeriodDataSource : AccountingPeriodDataSource {

    private val mutex = Mutex()
    private val periodsState = MutableStateFlow<Map<String, AccountingPeriod>>(emptyMap())
    private val reopenRequestsState = MutableStateFlow<Map<String, FinancialPeriodReopenRequest>>(emptyMap())
    private val activityEvents = mutableListOf<FinancialReconciliationActivityEvent>()
    private val periodCounters = mutableMapOf<String, Int>()
    private val reopenCounters = mutableMapOf<String, Int>()

    override suspend fun insertPeriod(period: AccountingPeriod): Boolean = mutex.withLock {
        if (periodsState.value.containsKey(period.periodId)) return@withLock false
        periodsState.value = periodsState.value + (period.periodId to period)
        true
    }

    override suspend fun updatePeriod(period: AccountingPeriod): Boolean = mutex.withLock {
        if (!periodsState.value.containsKey(period.periodId)) return@withLock false
        periodsState.value = periodsState.value + (period.periodId to period)
        true
    }

    override suspend fun getPeriodById(periodId: String): AccountingPeriod? = mutex.withLock {
        periodsState.value[periodId]
    }

    override suspend fun getPeriodByNumber(projectId: String, periodNo: String): AccountingPeriod? = mutex.withLock {
        periodsState.value.values.firstOrNull { it.projectId == projectId && it.periodNo == periodNo }
    }

    override suspend fun getPeriodsByProject(projectId: String): List<AccountingPeriod> = mutex.withLock {
        periodsState.value.values.filter { it.projectId == projectId }.sortedByDescending { it.startDate }
    }

    override fun observePeriods(projectId: String): Flow<List<AccountingPeriod>> {
        return periodsState.asStateFlow().map { map ->
            map.values.filter { it.projectId == projectId }.sortedByDescending { it.startDate }
        }
    }

    override suspend fun generateNextPeriodNo(projectId: String): String = mutex.withLock {
        val count = (periodCounters[projectId] ?: 0) + 1
        periodCounters[projectId] = count
        "PER-2026-${count.toString().padStart(4, '0')}"
    }

    override suspend fun insertReopenRequest(request: FinancialPeriodReopenRequest): Boolean = mutex.withLock {
        if (reopenRequestsState.value.containsKey(request.requestId)) return@withLock false
        reopenRequestsState.value = reopenRequestsState.value + (request.requestId to request)
        true
    }

    override suspend fun updateReopenRequest(request: FinancialPeriodReopenRequest): Boolean = mutex.withLock {
        if (!reopenRequestsState.value.containsKey(request.requestId)) return@withLock false
        reopenRequestsState.value = reopenRequestsState.value + (request.requestId to request)
        true
    }

    override suspend fun getReopenRequestById(requestId: String): FinancialPeriodReopenRequest? = mutex.withLock {
        reopenRequestsState.value[requestId]
    }

    override suspend fun getReopenRequestsByPeriod(periodId: String): List<FinancialPeriodReopenRequest> = mutex.withLock {
        reopenRequestsState.value.values.filter { it.periodId == periodId }.sortedByDescending { it.requestedAt }
    }

    override suspend fun generateNextReopenRequestNo(projectId: String): String = mutex.withLock {
        val count = (reopenCounters[projectId] ?: 0) + 1
        reopenCounters[projectId] = count
        "REOPEN-2026-${count.toString().padStart(4, '0')}"
    }

    override suspend fun insertActivityEvent(event: FinancialReconciliationActivityEvent): Boolean = mutex.withLock {
        activityEvents.add(event)
        true
    }

    override suspend fun getActivityEvents(entityId: String): List<FinancialReconciliationActivityEvent> = mutex.withLock {
        activityEvents.filter { it.entityId == entityId }.sortedBy { it.timestamp }
    }
}
