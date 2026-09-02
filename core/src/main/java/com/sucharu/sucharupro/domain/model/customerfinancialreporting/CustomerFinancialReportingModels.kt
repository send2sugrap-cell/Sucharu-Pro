package com.sucharu.sucharupro.domain.model.customerfinancialreporting

import com.sucharu.sucharupro.domain.model.customercreditcontrol.CustomerCreditRiskStatus
import com.sucharu.sucharupro.domain.model.customercreditcontrol.CustomerPaymentTermsType
import com.sucharu.sucharupro.domain.model.customerfinancial.CustomerFinancialAccountStatus
import com.sucharu.sucharupro.domain.model.customerfinancialdashboard.CustomerDueScheduleSummary
import com.sucharu.sucharupro.domain.model.customerfinancialdashboard.CustomerFinancialAction
import com.sucharu.sucharupro.domain.model.customerfinancialdashboard.CustomerFinancialWarning
import com.sucharu.sucharupro.domain.model.customerfinancialdashboard.CustomerReceivableAgingSummary
import com.sucharu.sucharupro.domain.model.customerfinancialdashboard.CustomerReconciliationStatusSummary
import com.sucharu.sucharupro.domain.model.customerledger.CustomerLedgerEntry
import com.sucharu.sucharupro.domain.model.customerledger.CustomerStatementSummary
import com.sucharu.sucharupro.domain.model.customerledger.ReconciliationDiscrepancy
import java.math.BigDecimal

/**
 * Supported financial report types for customer reporting.
 */
enum class CustomerFinancialReportType {
    CUSTOMER_STATEMENT,
    INVOICE_REPORT,
    PAYMENT_HISTORY,
    RECEIVABLE_AGING,
    SETTLEMENT_REPORT,
    CREDIT_RISK_REPORT,
    COLLECTION_REPORT,
    RECONCILIATION_REPORT,
    FINANCIAL_SUMMARY
}

/**
 * Supported report output and export formats.
 */
enum class CustomerFinancialReportFormat {
    JSON,
    CSV,
    PDF
}

/**
 * Unified request descriptor for customer financial report generation.
 */
data class CustomerFinancialReportRequest(
    val tenantId: String,
    val projectId: String,
    val customerId: String,
    val reportType: CustomerFinancialReportType,
    val format: CustomerFinancialReportFormat = CustomerFinancialReportFormat.JSON,
    val fromDate: Long? = null,
    val toDate: Long? = null,
    val invoiceId: String? = null,
    val requestedBy: String = "system",
    val correlationId: String? = null
)

/**
 * Report projection for Customer Statement.
 */
data class CustomerStatementReport(
    val customerId: String,
    val customerCode: String,
    val customerDisplayName: String,
    val accountNumber: String,
    val fromDate: Long?,
    val toDate: Long?,
    val openingBalance: BigDecimal,
    val totalInvoiced: BigDecimal,
    val totalPaid: BigDecimal,
    val totalCredits: BigDecimal,
    val totalAdjustments: BigDecimal,
    val totalRefunds: BigDecimal,
    val totalAllocated: BigDecimal,
    val currentReceivableBalance: BigDecimal,
    val availableCreditBalance: BigDecimal,
    val closingNetBalance: BigDecimal,
    val entries: List<CustomerLedgerEntry>,
    val summary: CustomerStatementSummary,
    val generatedAt: Long = System.currentTimeMillis()
)

/**
 * Report projection for Customer Invoices.
 */
data class CustomerInvoiceReportItem(
    val invoiceId: String,
    val invoiceNumber: String,
    val issueDate: Long,
    val dueDate: Long,
    val status: String,
    val grandTotal: BigDecimal,
    val paidAmount: BigDecimal,
    val dueAmount: BigDecimal,
    val isOverdue: Boolean,
    val daysOverdue: Int
)

data class CustomerInvoiceReport(
    val customerId: String,
    val customerCode: String,
    val customerDisplayName: String,
    val fromDate: Long?,
    val toDate: Long?,
    val totalInvoices: Int,
    val totalInvoicedAmount: BigDecimal,
    val totalPaidAmount: BigDecimal,
    val totalDueAmount: BigDecimal,
    val invoices: List<CustomerInvoiceReportItem>,
    val generatedAt: Long = System.currentTimeMillis()
)

/**
 * Report projection for Payment History.
 */
data class CustomerPaymentHistoryReportItem(
    val paymentId: String,
    val paymentNumber: String,
    val paymentDate: Long,
    val paymentMethod: String,
    val amount: BigDecimal,
    val status: String,
    val referenceNumber: String?
)

data class CustomerPaymentHistoryReport(
    val customerId: String,
    val customerCode: String,
    val customerDisplayName: String,
    val fromDate: Long?,
    val toDate: Long?,
    val totalPayments: Int,
    val totalPaidAmount: BigDecimal,
    val payments: List<CustomerPaymentHistoryReportItem>,
    val generatedAt: Long = System.currentTimeMillis()
)

/**
 * Report projection for Receivable Aging.
 */
data class CustomerReceivableAgingReport(
    val customerId: String,
    val customerCode: String,
    val customerDisplayName: String,
    val asOfDate: Long,
    val currentAmount: BigDecimal,
    val days1To7Amount: BigDecimal,
    val days8To30Amount: BigDecimal,
    val days31To60Amount: BigDecimal,
    val days61To90Amount: BigDecimal,
    val days90PlusAmount: BigDecimal,
    val totalOutstanding: BigDecimal,
    val maxDaysOverdue: Int,
    val riskStatus: CustomerCreditRiskStatus,
    val generatedAt: Long = System.currentTimeMillis()
)

/**
 * Report projection for Settlement and Allocations.
 */
data class CustomerSettlementReport(
    val customerId: String,
    val customerCode: String,
    val customerDisplayName: String,
    val totalInvoiced: BigDecimal,
    val totalPaid: BigDecimal,
    val totalAllocated: BigDecimal,
    val totalUnallocated: BigDecimal,
    val totalAvailableCredit: BigDecimal,
    val totalOutstanding: BigDecimal,
    val generatedAt: Long = System.currentTimeMillis()
)

/**
 * Report projection for Credit Risk & Terms.
 */
data class CustomerCreditRiskReport(
    val customerId: String,
    val customerCode: String,
    val customerDisplayName: String,
    val creditLimit: BigDecimal,
    val netReceivableExposure: BigDecimal,
    val availableCreditLimit: BigDecimal,
    val paymentTerms: CustomerPaymentTermsType,
    val creditDays: Int,
    val requiresAdvance: Boolean,
    val financialHold: Boolean,
    val holdReason: String?,
    val riskStatus: CustomerCreditRiskStatus,
    val generatedAt: Long = System.currentTimeMillis()
)

/**
 * Report projection for Collection Follow-up.
 */
data class CustomerCollectionReport(
    val customerId: String,
    val customerCode: String,
    val customerDisplayName: String,
    val totalOutstanding: BigDecimal,
    val overdueAmount: BigDecimal,
    val overdueInvoiceCount: Int,
    val priority: String,
    val pendingActionCount: Int,
    val completedActionCount: Int,
    val activePromiseCount: Int,
    val activePromisedAmount: BigDecimal,
    val nextFollowUpAt: Long?,
    val generatedAt: Long = System.currentTimeMillis()
)

/**
 * Report projection for Reconciliation Diagnostic.
 */
data class CustomerReconciliationReport(
    val customerId: String,
    val customerCode: String,
    val customerDisplayName: String,
    val reconciliationId: String,
    val reconciledAt: Long,
    val isReconciled: Boolean,
    val invoiceTotalReceivable: BigDecimal,
    val ledgerCalculatedBalance: BigDecimal,
    val availableCreditBalance: BigDecimal,
    val varianceAmount: BigDecimal,
    val discrepancyCount: Int,
    val discrepancies: List<ReconciliationDiscrepancy>,
    val generatedAt: Long = System.currentTimeMillis()
)

/**
 * Report projection for Financial Summary (Executive Brief).
 */
data class CustomerFinancialSummaryReport(
    val customerId: String,
    val customerCode: String,
    val customerDisplayName: String,
    val accountNumber: String,
    val accountStatus: CustomerFinancialAccountStatus,
    val totalInvoiced: BigDecimal,
    val totalPaid: BigDecimal,
    val totalAllocated: BigDecimal,
    val totalUnallocated: BigDecimal,
    val availableCreditBalance: BigDecimal,
    val outstandingReceivable: BigDecimal,
    val creditLimit: BigDecimal,
    val currentCreditExposure: BigDecimal,
    val availableCreditCapacity: BigDecimal,
    val riskStatus: CustomerCreditRiskStatus,
    val financialHold: Boolean,
    val holdReason: String?,
    val agingSummary: CustomerReceivableAgingSummary,
    val dueSchedule: CustomerDueScheduleSummary,
    val reconciliationSummary: CustomerReconciliationStatusSummary,
    val warnings: List<CustomerFinancialWarning>,
    val recommendedActions: List<CustomerFinancialAction>,
    val generatedAt: Long = System.currentTimeMillis()
)

/**
 * Generated report document output container.
 */
data class CustomerFinancialGeneratedDocument(
    val documentId: String,
    val reportType: CustomerFinancialReportType,
    val format: CustomerFinancialReportFormat,
    val fileName: String,
    val contentType: String,
    val contentString: String,
    val contentBytes: ByteArray = contentString.toByteArray(Charsets.UTF_8),
    val generatedAt: Long = System.currentTimeMillis(),
    val generatedBy: String = "system",
    val correlationId: String? = null,
    val metadata: Map<String, String> = emptyMap()
)

/**
 * Audit event for customer financial report generation.
 */
data class CustomerFinancialReportAuditEvent(
    val auditId: String,
    val tenantId: String,
    val projectId: String,
    val customerId: String,
    val reportType: CustomerFinancialReportType,
    val format: CustomerFinancialReportFormat,
    val requestedBy: String,
    val generatedAt: Long = System.currentTimeMillis(),
    val isSuccess: Boolean = true,
    val correlationId: String? = null,
    val errorMessage: String? = null
)
