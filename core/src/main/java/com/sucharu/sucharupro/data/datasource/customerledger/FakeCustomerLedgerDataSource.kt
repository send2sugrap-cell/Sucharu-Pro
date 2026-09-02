package com.sucharu.sucharupro.data.datasource.customerledger

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.customerledger.CustomerReceivableReconciliation
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Thread-safe In-Memory DataSource for Customer Ledger and Reconciliations (Module 14 Step 05).
 */
class FakeCustomerLedgerDataSource : CustomerLedgerDataSource {

    private val mutex = Mutex()
    private val reconciliations = mutableMapOf<String, CustomerReceivableReconciliation>()

    override suspend fun insertReconciliation(
        reconciliation: CustomerReceivableReconciliation
    ): DomainResult<CustomerReceivableReconciliation> = mutex.withLock {
        reconciliations[reconciliation.reconciliationId] = reconciliation
        DomainResult.Success(reconciliation)
    }

    override suspend fun findReconciliationById(
        tenantId: String,
        projectId: String,
        reconciliationId: String
    ): DomainResult<CustomerReceivableReconciliation> = mutex.withLock {
        val rec = reconciliations[reconciliationId]
        if (rec != null && rec.tenantId == tenantId && rec.projectId == projectId) {
            DomainResult.Success(rec)
        } else {
            DomainResult.Error(IllegalArgumentException("Reconciliation '$reconciliationId' not found"))
        }
    }

    override suspend fun listReconciliations(
        tenantId: String,
        projectId: String,
        customerId: String?,
        limit: Int,
        offset: Int
    ): DomainResult<List<CustomerReceivableReconciliation>> = mutex.withLock {
        val filtered = reconciliations.values
            .filter { it.tenantId == tenantId && it.projectId == projectId }
            .filter { customerId == null || it.customerId == customerId }
            .sortedByDescending { it.reconciledAt }
            .drop(offset)
            .take(limit)
        DomainResult.Success(filtered)
    }
}
