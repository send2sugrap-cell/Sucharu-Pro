package com.sucharu.sucharupro.data.datasource

import com.sucharu.sucharupro.domain.model.finance.FinancialReconciliationDiscrepancy
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * In-memory thread-safe fake data source for Financial Discrepancy persistence (Module 09 Step 08).
 */
class FakeFinancialDiscrepancyDataSource : FinancialDiscrepancyDataSource {

    private val mutex = Mutex()
    private val discrepanciesState = MutableStateFlow<Map<String, FinancialReconciliationDiscrepancy>>(emptyMap())
    private val counters = mutableMapOf<String, Int>()

    override suspend fun insertDiscrepancy(discrepancy: FinancialReconciliationDiscrepancy): Boolean = mutex.withLock {
        if (discrepanciesState.value.containsKey(discrepancy.discrepancyId)) return@withLock false
        discrepanciesState.value = discrepanciesState.value + (discrepancy.discrepancyId to discrepancy)
        true
    }

    override suspend fun updateDiscrepancy(discrepancy: FinancialReconciliationDiscrepancy): Boolean = mutex.withLock {
        if (!discrepanciesState.value.containsKey(discrepancy.discrepancyId)) return@withLock false
        discrepanciesState.value = discrepanciesState.value + (discrepancy.discrepancyId to discrepancy)
        true
    }

    override suspend fun getDiscrepancyById(discrepancyId: String): FinancialReconciliationDiscrepancy? = mutex.withLock {
        discrepanciesState.value[discrepancyId]
    }

    override suspend fun getDiscrepanciesByPeriod(periodId: String): List<FinancialReconciliationDiscrepancy> = mutex.withLock {
        discrepanciesState.value.values.filter { it.periodId == periodId }.sortedByDescending { it.detectedAt }
    }

    override suspend fun getDiscrepanciesByProject(projectId: String): List<FinancialReconciliationDiscrepancy> = mutex.withLock {
        discrepanciesState.value.values.filter { it.projectId == projectId }.sortedByDescending { it.detectedAt }
    }

    override fun observeDiscrepancies(projectId: String): Flow<List<FinancialReconciliationDiscrepancy>> {
        return discrepanciesState.asStateFlow().map { map ->
            map.values.filter { it.projectId == projectId }.sortedByDescending { it.detectedAt }
        }
    }

    override fun observePeriodDiscrepancies(projectId: String, periodId: String): Flow<List<FinancialReconciliationDiscrepancy>> {
        return discrepanciesState.asStateFlow().map { map ->
            map.values.filter { it.projectId == projectId && it.periodId == periodId }.sortedByDescending { it.detectedAt }
        }
    }

    override suspend fun generateNextDiscrepancyNo(projectId: String): String = mutex.withLock {
        val count = (counters[projectId] ?: 0) + 1
        counters[projectId] = count
        "DISC-2026-${count.toString().padStart(4, '0')}"
    }
}
