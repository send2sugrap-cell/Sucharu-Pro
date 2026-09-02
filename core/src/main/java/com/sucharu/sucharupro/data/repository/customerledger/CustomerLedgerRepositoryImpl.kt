package com.sucharu.sucharupro.data.repository.customerledger

import com.sucharu.sucharupro.data.datasource.customerledger.CustomerLedgerDataSource
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.customerledger.CustomerReceivableReconciliation
import com.sucharu.sucharupro.domain.repository.customerledger.CustomerLedgerRepository

/**
 * Concrete implementation of [CustomerLedgerRepository].
 */
class CustomerLedgerRepositoryImpl(
    private val dataSource: CustomerLedgerDataSource
) : CustomerLedgerRepository {

    override suspend fun saveReconciliation(
        reconciliation: CustomerReceivableReconciliation
    ): DomainResult<CustomerReceivableReconciliation> {
        return dataSource.insertReconciliation(reconciliation)
    }

    override suspend fun getReconciliationById(
        tenantId: String,
        projectId: String,
        reconciliationId: String
    ): DomainResult<CustomerReceivableReconciliation> {
        return dataSource.findReconciliationById(tenantId, projectId, reconciliationId)
    }

    override suspend fun listReconciliations(
        tenantId: String,
        projectId: String,
        customerId: String?,
        limit: Int,
        offset: Int
    ): DomainResult<List<CustomerReceivableReconciliation>> {
        return dataSource.listReconciliations(tenantId, projectId, customerId, limit, offset)
    }
}
