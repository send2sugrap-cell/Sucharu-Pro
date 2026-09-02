package com.sucharu.sucharupro.data.datasource

import com.sucharu.sucharupro.domain.model.finance.BankReconciliation
import com.sucharu.sucharupro.domain.model.finance.CashReconciliation
import com.sucharu.sucharupro.domain.model.finance.FinancialReconciliation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * In-memory thread-safe fake data source for Financial Reconciliation persistence (Module 09 Step 08).
 */
class FakeFinancialReconciliationDataSource : FinancialReconciliationDataSource {

    private val mutex = Mutex()
    private val reconciliationsState = MutableStateFlow<Map<String, FinancialReconciliation>>(emptyMap())
    private val cashReconciliations = mutableMapOf<String, CashReconciliation>()
    private val bankReconciliations = mutableMapOf<String, BankReconciliation>()
    private val counters = mutableMapOf<String, Int>()

    override suspend fun insertReconciliation(reconciliation: FinancialReconciliation): Boolean = mutex.withLock {
        if (reconciliationsState.value.containsKey(reconciliation.reconciliationId)) return@withLock false
        reconciliationsState.value = reconciliationsState.value + (reconciliation.reconciliationId to reconciliation)
        true
    }

    override suspend fun updateReconciliation(reconciliation: FinancialReconciliation): Boolean = mutex.withLock {
        if (!reconciliationsState.value.containsKey(reconciliation.reconciliationId)) return@withLock false
        reconciliationsState.value = reconciliationsState.value + (reconciliation.reconciliationId to reconciliation)
        true
    }

    override suspend fun getReconciliationById(reconciliationId: String): FinancialReconciliation? = mutex.withLock {
        reconciliationsState.value[reconciliationId]
    }

    override suspend fun getReconciliationByNumber(projectId: String, reconciliationNo: String): FinancialReconciliation? = mutex.withLock {
        reconciliationsState.value.values.firstOrNull { it.projectId == projectId && it.reconciliationNo == reconciliationNo }
    }

    override suspend fun getReconciliationByIdempotencyKey(projectId: String, idempotencyKey: String): FinancialReconciliation? = mutex.withLock {
        reconciliationsState.value.values.firstOrNull { it.projectId == projectId && it.idempotencyKey == idempotencyKey }
    }

    override suspend fun getReconciliationsByPeriod(periodId: String): List<FinancialReconciliation> = mutex.withLock {
        reconciliationsState.value.values.filter { it.periodId == periodId }.sortedByDescending { it.createdAt }
    }

    override suspend fun getReconciliationsByProject(projectId: String): List<FinancialReconciliation> = mutex.withLock {
        reconciliationsState.value.values.filter { it.projectId == projectId }.sortedByDescending { it.createdAt }
    }

    override fun observeReconciliations(projectId: String): Flow<List<FinancialReconciliation>> {
        return reconciliationsState.asStateFlow().map { map ->
            map.values.filter { it.projectId == projectId }.sortedByDescending { it.createdAt }
        }
    }

    override fun observePeriodReconciliations(projectId: String, periodId: String): Flow<List<FinancialReconciliation>> {
        return reconciliationsState.asStateFlow().map { map ->
            map.values.filter { it.projectId == projectId && it.periodId == periodId }.sortedByDescending { it.createdAt }
        }
    }

    override suspend fun generateNextReconciliationNo(projectId: String): String = mutex.withLock {
        val count = (counters[projectId] ?: 0) + 1
        counters[projectId] = count
        "REC-2026-${count.toString().padStart(4, '0')}"
    }

    override suspend fun insertCashReconciliation(cashReconciliation: CashReconciliation): Boolean = mutex.withLock {
        cashReconciliations[cashReconciliation.periodId] = cashReconciliation
        true
    }

    override suspend fun getCashReconciliationByPeriod(periodId: String): CashReconciliation? = mutex.withLock {
        cashReconciliations[periodId]
    }

    override suspend fun insertBankReconciliation(bankReconciliation: BankReconciliation): Boolean = mutex.withLock {
        bankReconciliations[bankReconciliation.periodId] = bankReconciliation
        true
    }

    override suspend fun getBankReconciliationByPeriod(periodId: String): BankReconciliation? = mutex.withLock {
        bankReconciliations[periodId]
    }
}
