package com.sucharu.sucharupro.domain.service.customerledger

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.customerledger.CustomerLedgerEntry
import com.sucharu.sucharupro.domain.model.customerledger.CustomerLedgerEntryType
import com.sucharu.sucharupro.domain.model.customerledger.CustomerReceivableReconciliation
import com.sucharu.sucharupro.domain.model.customerledger.CustomerStatement
import com.sucharu.sucharupro.domain.model.customerledger.CustomerStatementSummary

/**
 * Service contract for Customer Ledger, Statement, and Receivable Reconciliation (Module 14 Step 05).
 */
interface CustomerLedgerService {

    suspend fun getCustomerLedger(
        tenantId: String,
        projectId: String,
        customerId: String,
        fromDate: Long? = null,
        toDate: Long? = null,
        entryType: CustomerLedgerEntryType? = null,
        limit: Int = 100,
        offset: Int = 0
    ): DomainResult<List<CustomerLedgerEntry>>

    suspend fun getCustomerStatement(
        tenantId: String,
        projectId: String,
        customerId: String,
        fromDate: Long? = null,
        toDate: Long? = null
    ): DomainResult<CustomerStatement>

    suspend fun getCustomerStatementSummary(
        tenantId: String,
        projectId: String,
        customerId: String
    ): DomainResult<CustomerStatementSummary>

    suspend fun reconcileCustomerReceivable(
        tenantId: String,
        projectId: String,
        customerId: String,
        notes: String? = null,
        actorId: String = "system",
        actorRole: String = "SYSTEM"
    ): DomainResult<CustomerReceivableReconciliation>

    suspend fun listReconciliations(
        tenantId: String,
        projectId: String,
        customerId: String?,
        limit: Int = 50,
        offset: Int = 0
    ): DomainResult<List<CustomerReceivableReconciliation>>
}
