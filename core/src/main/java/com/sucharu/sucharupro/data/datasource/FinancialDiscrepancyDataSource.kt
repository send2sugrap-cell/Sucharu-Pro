package com.sucharu.sucharupro.data.datasource

import com.sucharu.sucharupro.domain.model.finance.FinancialReconciliationDiscrepancy
import kotlinx.coroutines.flow.Flow

/**
 * Data Source contract for Financial Discrepancy persistence (Module 09 Step 08).
 */
interface FinancialDiscrepancyDataSource {
    suspend fun insertDiscrepancy(discrepancy: FinancialReconciliationDiscrepancy): Boolean
    suspend fun updateDiscrepancy(discrepancy: FinancialReconciliationDiscrepancy): Boolean
    suspend fun getDiscrepancyById(discrepancyId: String): FinancialReconciliationDiscrepancy?
    suspend fun getDiscrepanciesByPeriod(periodId: String): List<FinancialReconciliationDiscrepancy>
    suspend fun getDiscrepanciesByProject(projectId: String): List<FinancialReconciliationDiscrepancy>
    fun observeDiscrepancies(projectId: String): Flow<List<FinancialReconciliationDiscrepancy>>
    fun observePeriodDiscrepancies(projectId: String, periodId: String): Flow<List<FinancialReconciliationDiscrepancy>>
    suspend fun generateNextDiscrepancyNo(projectId: String): String
}
