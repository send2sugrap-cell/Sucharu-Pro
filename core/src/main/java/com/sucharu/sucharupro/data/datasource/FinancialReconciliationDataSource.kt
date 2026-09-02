package com.sucharu.sucharupro.data.datasource

import com.sucharu.sucharupro.domain.model.finance.BankReconciliation
import com.sucharu.sucharupro.domain.model.finance.CashReconciliation
import com.sucharu.sucharupro.domain.model.finance.FinancialReconciliation
import com.sucharu.sucharupro.domain.model.finance.FinancialReconciliationType
import kotlinx.coroutines.flow.Flow

/**
 * Data Source contract for Financial Reconciliation persistence (Module 09 Step 08).
 */
interface FinancialReconciliationDataSource {
    suspend fun insertReconciliation(reconciliation: FinancialReconciliation): Boolean
    suspend fun updateReconciliation(reconciliation: FinancialReconciliation): Boolean
    suspend fun getReconciliationById(reconciliationId: String): FinancialReconciliation?
    suspend fun getReconciliationByNumber(projectId: String, reconciliationNo: String): FinancialReconciliation?
    suspend fun getReconciliationByIdempotencyKey(projectId: String, idempotencyKey: String): FinancialReconciliation?
    suspend fun getReconciliationsByPeriod(periodId: String): List<FinancialReconciliation>
    suspend fun getReconciliationsByProject(projectId: String): List<FinancialReconciliation>
    fun observeReconciliations(projectId: String): Flow<List<FinancialReconciliation>>
    fun observePeriodReconciliations(projectId: String, periodId: String): Flow<List<FinancialReconciliation>>
    suspend fun generateNextReconciliationNo(projectId: String): String

    // Cash reconciliations
    suspend fun insertCashReconciliation(cashReconciliation: CashReconciliation): Boolean
    suspend fun getCashReconciliationByPeriod(periodId: String): CashReconciliation?

    // Bank reconciliations
    suspend fun insertBankReconciliation(bankReconciliation: BankReconciliation): Boolean
    suspend fun getBankReconciliationByPeriod(periodId: String): BankReconciliation?
}
