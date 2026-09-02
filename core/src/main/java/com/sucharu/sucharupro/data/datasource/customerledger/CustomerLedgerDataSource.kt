package com.sucharu.sucharupro.data.datasource.customerledger

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.customerledger.CustomerReceivableReconciliation

/**
 * Data source interface for Customer Ledger reconciliations and audits (Module 14 Step 05).
 */
interface CustomerLedgerDataSource {

    suspend fun insertReconciliation(
        reconciliation: CustomerReceivableReconciliation
    ): DomainResult<CustomerReceivableReconciliation>

    suspend fun findReconciliationById(
        tenantId: String,
        projectId: String,
        reconciliationId: String
    ): DomainResult<CustomerReceivableReconciliation>

    suspend fun listReconciliations(
        tenantId: String,
        projectId: String,
        customerId: String?,
        limit: Int,
        offset: Int
    ): DomainResult<List<CustomerReceivableReconciliation>>
}
