package com.sucharu.sucharupro.domain.repository.customerledger

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.customerledger.CustomerReceivableReconciliation

/**
 * Repository contract for Customer Ledger and Reconciliations (Module 14 Step 05).
 */
interface CustomerLedgerRepository {

    suspend fun saveReconciliation(
        reconciliation: CustomerReceivableReconciliation
    ): DomainResult<CustomerReceivableReconciliation>

    suspend fun getReconciliationById(
        tenantId: String,
        projectId: String,
        reconciliationId: String
    ): DomainResult<CustomerReceivableReconciliation>

    suspend fun listReconciliations(
        tenantId: String,
        projectId: String,
        customerId: String?,
        limit: Int = 50,
        offset: Int = 0
    ): DomainResult<List<CustomerReceivableReconciliation>>
}
