package com.sucharu.sucharupro.data.api.model

import com.sucharu.sucharupro.domain.model.customerfinancialreporting.*
import java.math.BigDecimal

data class ExportCustomerFinancialReportRequest(
    val reportType: String,
    val format: String = "JSON",
    val fromDate: Long? = null,
    val toDate: Long? = null,
    val invoiceId: String? = null
)

data class CustomerStatementReportDto(
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
    val entries: List<CustomerLedgerEntryDto>,
    val summary: CustomerStatementSummaryDto,
    val generatedAt: Long
)

data class CustomerInvoiceReportItemDto(
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

data class CustomerInvoiceReportDto(
    val customerId: String,
    val customerCode: String,
    val customerDisplayName: String,
    val fromDate: Long?,
    val toDate: Long?,
    val totalInvoices: Int,
    val totalInvoicedAmount: BigDecimal,
    val totalPaidAmount: BigDecimal,
    val totalDueAmount: BigDecimal,
    val invoices: List<CustomerInvoiceReportItemDto>,
    val generatedAt: Long
)

data class CustomerPaymentHistoryReportItemDto(
    val paymentId: String,
    val paymentNumber: String,
    val paymentDate: Long,
    val paymentMethod: String,
    val amount: BigDecimal,
    val status: String,
    val referenceNumber: String?
)

data class CustomerPaymentHistoryReportDto(
    val customerId: String,
    val customerCode: String,
    val customerDisplayName: String,
    val fromDate: Long?,
    val toDate: Long?,
    val totalPayments: Int,
    val totalPaidAmount: BigDecimal,
    val payments: List<CustomerPaymentHistoryReportItemDto>,
    val generatedAt: Long
)

data class CustomerAgingStatementReportDto(
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
    val riskStatus: String,
    val generatedAt: Long
)

data class CustomerSettlementReportDto(
    val customerId: String,
    val customerCode: String,
    val customerDisplayName: String,
    val totalInvoiced: BigDecimal,
    val totalPaid: BigDecimal,
    val totalAllocated: BigDecimal,
    val totalUnallocated: BigDecimal,
    val totalAvailableCredit: BigDecimal,
    val totalOutstanding: BigDecimal,
    val generatedAt: Long
)

data class CustomerCreditRiskReportDto(
    val customerId: String,
    val customerCode: String,
    val customerDisplayName: String,
    val creditLimit: BigDecimal,
    val netReceivableExposure: BigDecimal,
    val availableCreditLimit: BigDecimal,
    val paymentTerms: String,
    val creditDays: Int,
    val requiresAdvance: Boolean,
    val financialHold: Boolean,
    val holdReason: String?,
    val riskStatus: String,
    val generatedAt: Long
)

data class CustomerCollectionReportDto(
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
    val generatedAt: Long
)

data class CustomerReconciliationReportDto(
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
    val discrepancies: List<ReconciliationDiscrepancyDto>,
    val generatedAt: Long
)

data class CustomerFinancialSummaryReportDto(
    val customerId: String,
    val customerCode: String,
    val customerDisplayName: String,
    val accountNumber: String,
    val accountStatus: String,
    val totalInvoiced: BigDecimal,
    val totalPaid: BigDecimal,
    val totalAllocated: BigDecimal,
    val totalUnallocated: BigDecimal,
    val availableCreditBalance: BigDecimal,
    val outstandingReceivable: BigDecimal,
    val creditLimit: BigDecimal,
    val currentCreditExposure: BigDecimal,
    val availableCreditCapacity: BigDecimal,
    val riskStatus: String,
    val financialHold: Boolean,
    val holdReason: String?,
    val agingSummary: CustomerReceivableAgingSummaryDto,
    val dueSchedule: CustomerDueScheduleSummaryDto,
    val reconciliationSummary: CustomerReconciliationStatusSummaryDto,
    val warnings: List<CustomerFinancialWarningDto>,
    val recommendedActions: List<CustomerFinancialActionDto>,
    val generatedAt: Long
)

data class CustomerFinancialGeneratedDocumentDto(
    val documentId: String,
    val reportType: String,
    val format: String,
    val fileName: String,
    val contentType: String,
    val contentString: String,
    val generatedAt: Long,
    val generatedBy: String,
    val correlationId: String? = null,
    val metadata: Map<String, String> = emptyMap()
)

fun CustomerStatementReport.toDto(): CustomerStatementReportDto {
    return CustomerStatementReportDto(
        customerId = customerId,
        customerCode = customerCode,
        customerDisplayName = customerDisplayName,
        accountNumber = accountNumber,
        fromDate = fromDate,
        toDate = toDate,
        openingBalance = openingBalance,
        totalInvoiced = totalInvoiced,
        totalPaid = totalPaid,
        totalCredits = totalCredits,
        totalAdjustments = totalAdjustments,
        totalRefunds = totalRefunds,
        totalAllocated = totalAllocated,
        currentReceivableBalance = currentReceivableBalance,
        availableCreditBalance = availableCreditBalance,
        closingNetBalance = closingNetBalance,
        entries = entries.map { it.toDto() },
        summary = summary.toDto(),
        generatedAt = generatedAt
    )
}

fun CustomerInvoiceReport.toDto(): CustomerInvoiceReportDto {
    return CustomerInvoiceReportDto(
        customerId = customerId,
        customerCode = customerCode,
        customerDisplayName = customerDisplayName,
        fromDate = fromDate,
        toDate = toDate,
        totalInvoices = totalInvoices,
        totalInvoicedAmount = totalInvoicedAmount,
        totalPaidAmount = totalPaidAmount,
        totalDueAmount = totalDueAmount,
        invoices = invoices.map {
            CustomerInvoiceReportItemDto(
                invoiceId = it.invoiceId,
                invoiceNumber = it.invoiceNumber,
                issueDate = it.issueDate,
                dueDate = it.dueDate,
                status = it.status,
                grandTotal = it.grandTotal,
                paidAmount = it.paidAmount,
                dueAmount = it.dueAmount,
                isOverdue = it.isOverdue,
                daysOverdue = it.daysOverdue
            )
        },
        generatedAt = generatedAt
    )
}

fun CustomerPaymentHistoryReport.toDto(): CustomerPaymentHistoryReportDto {
    return CustomerPaymentHistoryReportDto(
        customerId = customerId,
        customerCode = customerCode,
        customerDisplayName = customerDisplayName,
        fromDate = fromDate,
        toDate = toDate,
        totalPayments = totalPayments,
        totalPaidAmount = totalPaidAmount,
        payments = payments.map {
            CustomerPaymentHistoryReportItemDto(
                paymentId = it.paymentId,
                paymentNumber = it.paymentNumber,
                paymentDate = it.paymentDate,
                paymentMethod = it.paymentMethod,
                amount = it.amount,
                status = it.status,
                referenceNumber = it.referenceNumber
            )
        },
        generatedAt = generatedAt
    )
}

fun CustomerReceivableAgingReport.toDto(): CustomerAgingStatementReportDto {
    return CustomerAgingStatementReportDto(
        customerId = customerId,
        customerCode = customerCode,
        customerDisplayName = customerDisplayName,
        asOfDate = asOfDate,
        currentAmount = currentAmount,
        days1To7Amount = days1To7Amount,
        days8To30Amount = days8To30Amount,
        days31To60Amount = days31To60Amount,
        days61To90Amount = days61To90Amount,
        days90PlusAmount = days90PlusAmount,
        totalOutstanding = totalOutstanding,
        maxDaysOverdue = maxDaysOverdue,
        riskStatus = riskStatus.name,
        generatedAt = generatedAt
    )
}

fun CustomerSettlementReport.toDto(): CustomerSettlementReportDto {
    return CustomerSettlementReportDto(
        customerId = customerId,
        customerCode = customerCode,
        customerDisplayName = customerDisplayName,
        totalInvoiced = totalInvoiced,
        totalPaid = totalPaid,
        totalAllocated = totalAllocated,
        totalUnallocated = totalUnallocated,
        totalAvailableCredit = totalAvailableCredit,
        totalOutstanding = totalOutstanding,
        generatedAt = generatedAt
    )
}

fun CustomerCreditRiskReport.toDto(): CustomerCreditRiskReportDto {
    return CustomerCreditRiskReportDto(
        customerId = customerId,
        customerCode = customerCode,
        customerDisplayName = customerDisplayName,
        creditLimit = creditLimit,
        netReceivableExposure = netReceivableExposure,
        availableCreditLimit = availableCreditLimit,
        paymentTerms = paymentTerms.name,
        creditDays = creditDays,
        requiresAdvance = requiresAdvance,
        financialHold = financialHold,
        holdReason = holdReason,
        riskStatus = riskStatus.name,
        generatedAt = generatedAt
    )
}

fun CustomerCollectionReport.toDto(): CustomerCollectionReportDto {
    return CustomerCollectionReportDto(
        customerId = customerId,
        customerCode = customerCode,
        customerDisplayName = customerDisplayName,
        totalOutstanding = totalOutstanding,
        overdueAmount = overdueAmount,
        overdueInvoiceCount = overdueInvoiceCount,
        priority = priority,
        pendingActionCount = pendingActionCount,
        completedActionCount = completedActionCount,
        activePromiseCount = activePromiseCount,
        activePromisedAmount = activePromisedAmount,
        nextFollowUpAt = nextFollowUpAt,
        generatedAt = generatedAt
    )
}

fun CustomerReconciliationReport.toDto(): CustomerReconciliationReportDto {
    return CustomerReconciliationReportDto(
        customerId = customerId,
        customerCode = customerCode,
        customerDisplayName = customerDisplayName,
        reconciliationId = reconciliationId,
        reconciledAt = reconciledAt,
        isReconciled = isReconciled,
        invoiceTotalReceivable = invoiceTotalReceivable,
        ledgerCalculatedBalance = ledgerCalculatedBalance,
        availableCreditBalance = availableCreditBalance,
        varianceAmount = varianceAmount,
        discrepancyCount = discrepancyCount,
        discrepancies = discrepancies.map {
            ReconciliationDiscrepancyDto(
                discrepancyType = it.discrepancyType,
                referenceType = it.referenceType,
                referenceId = it.referenceId,
                expectedAmount = it.expectedAmount,
                actualAmount = it.actualAmount,
                difference = it.difference,
                description = it.description
            )
        },
        generatedAt = generatedAt
    )
}

fun CustomerFinancialSummaryReport.toDto(): CustomerFinancialSummaryReportDto {
    return CustomerFinancialSummaryReportDto(
        customerId = customerId,
        customerCode = customerCode,
        customerDisplayName = customerDisplayName,
        accountNumber = accountNumber,
        accountStatus = accountStatus.name,
        totalInvoiced = totalInvoiced,
        totalPaid = totalPaid,
        totalAllocated = totalAllocated,
        totalUnallocated = totalUnallocated,
        availableCreditBalance = availableCreditBalance,
        outstandingReceivable = outstandingReceivable,
        creditLimit = creditLimit,
        currentCreditExposure = currentCreditExposure,
        availableCreditCapacity = availableCreditCapacity,
        riskStatus = riskStatus.name,
        financialHold = financialHold,
        holdReason = holdReason,
        agingSummary = agingSummary.toDto(),
        dueSchedule = dueSchedule.toDto(),
        reconciliationSummary = reconciliationSummary.toDto(),
        warnings = warnings.map { it.toDto() },
        recommendedActions = recommendedActions.map { it.toDto() },
        generatedAt = generatedAt
    )
}

fun CustomerFinancialGeneratedDocument.toDto(): CustomerFinancialGeneratedDocumentDto {
    return CustomerFinancialGeneratedDocumentDto(
        documentId = documentId,
        reportType = reportType.name,
        format = format.name,
        fileName = fileName,
        contentType = contentType,
        contentString = contentString,
        generatedAt = generatedAt,
        generatedBy = generatedBy,
        correlationId = correlationId,
        metadata = metadata
    )
}
