package com.sucharu.sucharupro.domain.service.customerfinancialreporting

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.customerfinancialreporting.*

interface CustomerFinancialReportingService {

    suspend fun getCustomerStatementReport(
        tenantId: String,
        projectId: String,
        customerId: String,
        fromDate: Long? = null,
        toDate: Long? = null
    ): DomainResult<CustomerStatementReport>

    suspend fun getCustomerInvoiceReport(
        tenantId: String,
        projectId: String,
        customerId: String,
        fromDate: Long? = null,
        toDate: Long? = null
    ): DomainResult<CustomerInvoiceReport>

    suspend fun getCustomerPaymentHistoryReport(
        tenantId: String,
        projectId: String,
        customerId: String,
        fromDate: Long? = null,
        toDate: Long? = null
    ): DomainResult<CustomerPaymentHistoryReport>

    suspend fun getCustomerReceivableAgingReport(
        tenantId: String,
        projectId: String,
        customerId: String,
        asOfDate: Long = System.currentTimeMillis()
    ): DomainResult<CustomerReceivableAgingReport>

    suspend fun getCustomerSettlementReport(
        tenantId: String,
        projectId: String,
        customerId: String
    ): DomainResult<CustomerSettlementReport>

    suspend fun getCustomerCreditRiskReport(
        tenantId: String,
        projectId: String,
        customerId: String
    ): DomainResult<CustomerCreditRiskReport>

    suspend fun getCustomerCollectionReport(
        tenantId: String,
        projectId: String,
        customerId: String,
        asOfDate: Long = System.currentTimeMillis()
    ): DomainResult<CustomerCollectionReport>

    suspend fun getCustomerReconciliationReport(
        tenantId: String,
        projectId: String,
        customerId: String
    ): DomainResult<CustomerReconciliationReport>

    suspend fun getCustomerFinancialSummaryReport(
        tenantId: String,
        projectId: String,
        customerId: String,
        asOfDate: Long = System.currentTimeMillis()
    ): DomainResult<CustomerFinancialSummaryReport>

    suspend fun exportFinancialReport(
        request: CustomerFinancialReportRequest
    ): DomainResult<CustomerFinancialGeneratedDocument>
}
