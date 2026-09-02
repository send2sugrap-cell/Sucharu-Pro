package com.sucharu.sucharupro.domain.service.customerfinancialreporting

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.customercreditcontrol.ReceivableAgingBucket
import com.sucharu.sucharupro.domain.model.customerfinancialreporting.*
import com.sucharu.sucharupro.domain.repository.CustomerRepository
import com.sucharu.sucharupro.domain.repository.customercredit.CustomerCreditRepository
import com.sucharu.sucharupro.domain.repository.customerfinancial.CustomerFinancialAccountRepository
import com.sucharu.sucharupro.domain.repository.customerinvoice.CustomerInvoiceRepository
import com.sucharu.sucharupro.domain.repository.customerpayment.CustomerPaymentRepository
import com.sucharu.sucharupro.domain.service.customercollection.CustomerCollectionService
import com.sucharu.sucharupro.domain.service.customercreditcontrol.CustomerCreditControlService
import com.sucharu.sucharupro.domain.service.customerfinancialdashboard.CustomerFinancialDashboardService
import com.sucharu.sucharupro.domain.service.customerledger.CustomerLedgerService
import com.sucharu.sucharupro.domain.service.customersettlement.CustomerSettlementService
import java.math.BigDecimal
import java.util.UUID

class CustomerFinancialReportingServiceImpl(
    private val customerRepository: CustomerRepository,
    private val accountRepository: CustomerFinancialAccountRepository,
    private val invoiceRepository: CustomerInvoiceRepository,
    private val paymentRepository: CustomerPaymentRepository,
    private val creditRepository: CustomerCreditRepository,
    private val ledgerService: CustomerLedgerService,
    private val settlementService: CustomerSettlementService,
    private val creditControlService: CustomerCreditControlService,
    private val collectionService: CustomerCollectionService,
    private val dashboardService: CustomerFinancialDashboardService
) : CustomerFinancialReportingService {

    private suspend fun validateTenantCustomer(tenantId: String, projectId: String, customerId: String): DomainResult<Unit> {
        val custRes = customerRepository.findCustomerById(customerId)
        if (custRes is DomainResult.Error) {
            return DomainResult.Error(IllegalArgumentException("Customer '$customerId' not found."))
        }
        val accountRes = accountRepository.getAccountByCustomerId(tenantId, projectId, customerId)
        if (accountRes is DomainResult.Error) {
            return DomainResult.Error(IllegalArgumentException("Customer '$customerId' does not belong to tenant '$tenantId' / project '$projectId'."))
        }
        return DomainResult.Success(Unit)
    }

    override suspend fun getCustomerStatementReport(
        tenantId: String,
        projectId: String,
        customerId: String,
        fromDate: Long?,
        toDate: Long?
    ): DomainResult<CustomerStatementReport> {
        val check = validateTenantCustomer(tenantId, projectId, customerId)
        if (check is DomainResult.Error) return check

        val custRes = customerRepository.findCustomerById(customerId)
        val customer = (custRes as DomainResult.Success).data

        val accountRes = accountRepository.getAccountByCustomerId(tenantId, projectId, customerId)
        val account = (accountRes as DomainResult.Success).data

        val stmtRes = ledgerService.getCustomerStatement(tenantId, projectId, customerId, fromDate, toDate)
        if (stmtRes is DomainResult.Error) return DomainResult.Error(stmtRes.exception, stmtRes.message)
        val stmt = (stmtRes as DomainResult.Success).data

        val summaryRes = ledgerService.getCustomerStatementSummary(tenantId, projectId, customerId)
        if (summaryRes is DomainResult.Error) return DomainResult.Error(summaryRes.exception, summaryRes.message)
        val summary = (summaryRes as DomainResult.Success).data

        val report = CustomerStatementReport(
            customerId = customerId,
            customerCode = customer.customerCode,
            customerDisplayName = customer.displayName,
            accountNumber = account.accountNumber,
            fromDate = fromDate,
            toDate = toDate,
            openingBalance = stmt.openingBalance,
            totalInvoiced = summary.totalInvoiced,
            totalPaid = summary.totalPaid,
            totalCredits = summary.totalAdvances,
            totalAdjustments = summary.totalAdjustmentsCredit.add(summary.totalAdjustmentsDebit),
            totalRefunds = summary.totalRefunds,
            totalAllocated = summary.totalAllocated,
            currentReceivableBalance = summary.currentReceivableBalance,
            availableCreditBalance = summary.availableCreditBalance,
            closingNetBalance = summary.netBalance,
            entries = stmt.entries,
            summary = summary
        )
        return DomainResult.Success(report)
    }

    override suspend fun getCustomerInvoiceReport(
        tenantId: String,
        projectId: String,
        customerId: String,
        fromDate: Long?,
        toDate: Long?
    ): DomainResult<CustomerInvoiceReport> {
        val check = validateTenantCustomer(tenantId, projectId, customerId)
        if (check is DomainResult.Error) return check

        val custRes = customerRepository.findCustomerById(customerId)
        val customer = (custRes as DomainResult.Success).data

        val invRes = invoiceRepository.listInvoices(tenantId, projectId, customerId = customerId, limit = 500)
        if (invRes is DomainResult.Error) return DomainResult.Error(invRes.exception, invRes.message)
        val invoices = (invRes as DomainResult.Success).data

        val now = System.currentTimeMillis()
        val filtered = invoices.filter {
            (fromDate == null || it.createdAt >= fromDate) && (toDate == null || it.createdAt <= toDate)
        }

        val items = filtered.map { inv ->
            val isOverdue = inv.dueAmount > BigDecimal.ZERO && inv.dueDate != null && inv.dueDate < now
            val daysOverdue = if (isOverdue && inv.dueDate != null) {
                ((now - inv.dueDate) / 86400000L).toInt()
            } else 0

            CustomerInvoiceReportItem(
                invoiceId = inv.invoiceId,
                invoiceNumber = inv.invoiceNumber,
                issueDate = inv.createdAt,
                dueDate = inv.dueDate ?: inv.createdAt,
                status = inv.status.name,
                grandTotal = inv.grandTotal,
                paidAmount = inv.paidAmount,
                dueAmount = inv.dueAmount,
                isOverdue = isOverdue,
                daysOverdue = daysOverdue
            )
        }

        val totalInvoiced = items.map { it.grandTotal }.fold(BigDecimal.ZERO, BigDecimal::add)
        val totalPaid = items.map { it.paidAmount }.fold(BigDecimal.ZERO, BigDecimal::add)
        val totalDue = items.map { it.dueAmount }.fold(BigDecimal.ZERO, BigDecimal::add)

        val report = CustomerInvoiceReport(
            customerId = customerId,
            customerCode = customer.customerCode,
            customerDisplayName = customer.displayName,
            fromDate = fromDate,
            toDate = toDate,
            totalInvoices = items.size,
            totalInvoicedAmount = totalInvoiced,
            totalPaidAmount = totalPaid,
            totalDueAmount = totalDue,
            invoices = items
        )
        return DomainResult.Success(report)
    }

    override suspend fun getCustomerPaymentHistoryReport(
        tenantId: String,
        projectId: String,
        customerId: String,
        fromDate: Long?,
        toDate: Long?
    ): DomainResult<CustomerPaymentHistoryReport> {
        val check = validateTenantCustomer(tenantId, projectId, customerId)
        if (check is DomainResult.Error) return check

        val custRes = customerRepository.findCustomerById(customerId)
        val customer = (custRes as DomainResult.Success).data

        val payRes = paymentRepository.listPayments(tenantId, projectId, customerId = customerId, limit = 500)
        if (payRes is DomainResult.Error) return DomainResult.Error(payRes.exception, payRes.message)
        val payments = (payRes as DomainResult.Success).data

        val filtered = payments.filter {
            (fromDate == null || it.paymentDate >= fromDate) && (toDate == null || it.paymentDate <= toDate)
        }

        val items = filtered.map { pay ->
            CustomerPaymentHistoryReportItem(
                paymentId = pay.paymentId,
                paymentNumber = pay.paymentNumber,
                paymentDate = pay.paymentDate,
                paymentMethod = pay.paymentMethod.name,
                amount = pay.amount,
                status = pay.status.name,
                referenceNumber = pay.referenceNumber
            )
        }

        val totalPaid = items.map { it.amount }.fold(BigDecimal.ZERO, BigDecimal::add)

        val report = CustomerPaymentHistoryReport(
            customerId = customerId,
            customerCode = customer.customerCode,
            customerDisplayName = customer.displayName,
            fromDate = fromDate,
            toDate = toDate,
            totalPayments = items.size,
            totalPaidAmount = totalPaid,
            payments = items
        )
        return DomainResult.Success(report)
    }

    override suspend fun getCustomerReceivableAgingReport(
        tenantId: String,
        projectId: String,
        customerId: String,
        asOfDate: Long
    ): DomainResult<CustomerReceivableAgingReport> {
        val check = validateTenantCustomer(tenantId, projectId, customerId)
        if (check is DomainResult.Error) return check

        val custRes = customerRepository.findCustomerById(customerId)
        val customer = (custRes as DomainResult.Success).data

        val agingRes = creditControlService.getReceivableAgingReport(tenantId, projectId, customerId, asOfDate)
        if (agingRes is DomainResult.Error) return DomainResult.Error(agingRes.exception, agingRes.message)
        val aging = (agingRes as DomainResult.Success).data

        val riskRes = creditControlService.getReceivableRiskSummary(tenantId, projectId, customerId)
        val riskStatus = if (riskRes is DomainResult.Success) riskRes.data.riskStatus else com.sucharu.sucharupro.domain.model.customercreditcontrol.CustomerCreditRiskStatus.NORMAL

        val bucketMap = aging.buckets.associateBy { it.bucket }

        val report = CustomerReceivableAgingReport(
            customerId = customerId,
            customerCode = customer.customerCode,
            customerDisplayName = customer.displayName,
            asOfDate = asOfDate,
            currentAmount = bucketMap[ReceivableAgingBucket.CURRENT]?.outstandingAmount ?: BigDecimal.ZERO,
            days1To7Amount = bucketMap[ReceivableAgingBucket.DAYS_1_7]?.outstandingAmount ?: BigDecimal.ZERO,
            days8To30Amount = bucketMap[ReceivableAgingBucket.DAYS_8_30]?.outstandingAmount ?: BigDecimal.ZERO,
            days31To60Amount = bucketMap[ReceivableAgingBucket.DAYS_31_60]?.outstandingAmount ?: BigDecimal.ZERO,
            days61To90Amount = bucketMap[ReceivableAgingBucket.DAYS_61_90]?.outstandingAmount ?: BigDecimal.ZERO,
            days90PlusAmount = bucketMap[ReceivableAgingBucket.DAYS_90_PLUS]?.outstandingAmount ?: BigDecimal.ZERO,
            totalOutstanding = aging.totalOutstanding,
            maxDaysOverdue = aging.maxDaysOverdue,
            riskStatus = riskStatus
        )
        return DomainResult.Success(report)
    }

    override suspend fun getCustomerSettlementReport(
        tenantId: String,
        projectId: String,
        customerId: String
    ): DomainResult<CustomerSettlementReport> {
        val check = validateTenantCustomer(tenantId, projectId, customerId)
        if (check is DomainResult.Error) return check

        val custRes = customerRepository.findCustomerById(customerId)
        val customer = (custRes as DomainResult.Success).data

        val settleRes = settlementService.getCustomerSettlementSummary(tenantId, projectId, customerId)
        if (settleRes is DomainResult.Error) return DomainResult.Error(settleRes.exception, settleRes.message)
        val settle = (settleRes as DomainResult.Success).data

        val report = CustomerSettlementReport(
            customerId = customerId,
            customerCode = customer.customerCode,
            customerDisplayName = customer.displayName,
            totalInvoiced = settle.totalInvoiced,
            totalPaid = settle.totalPaid,
            totalAllocated = settle.totalAllocated,
            totalUnallocated = settle.totalUnallocated,
            totalAvailableCredit = settle.totalAvailableCredit,
            totalOutstanding = settle.totalOutstanding
        )
        return DomainResult.Success(report)
    }

    override suspend fun getCustomerCreditRiskReport(
        tenantId: String,
        projectId: String,
        customerId: String
    ): DomainResult<CustomerCreditRiskReport> {
        val check = validateTenantCustomer(tenantId, projectId, customerId)
        if (check is DomainResult.Error) return check

        val custRes = customerRepository.findCustomerById(customerId)
        val customer = (custRes as DomainResult.Success).data

        val riskRes = creditControlService.getReceivableRiskSummary(tenantId, projectId, customerId)
        if (riskRes is DomainResult.Error) return DomainResult.Error(riskRes.exception, riskRes.message)
        val risk = (riskRes as DomainResult.Success).data

        val report = CustomerCreditRiskReport(
            customerId = customerId,
            customerCode = customer.customerCode,
            customerDisplayName = customer.displayName,
            creditLimit = risk.creditLimit,
            netReceivableExposure = risk.netReceivableExposure,
            availableCreditLimit = risk.availableCreditLimit,
            paymentTerms = risk.paymentTermsType,
            creditDays = risk.creditDays,
            requiresAdvance = risk.requiresAdvance,
            financialHold = risk.financialHold,
            holdReason = risk.holdReason,
            riskStatus = risk.riskStatus
        )
        return DomainResult.Success(report)
    }

    override suspend fun getCustomerCollectionReport(
        tenantId: String,
        projectId: String,
        customerId: String,
        asOfDate: Long
    ): DomainResult<CustomerCollectionReport> {
        val check = validateTenantCustomer(tenantId, projectId, customerId)
        if (check is DomainResult.Error) return check

        val custRes = customerRepository.findCustomerById(customerId)
        val customer = (custRes as DomainResult.Success).data

        val collRes = collectionService.getCustomerCollectionSummary(tenantId, projectId, customerId, asOfDate)
        if (collRes is DomainResult.Error) return DomainResult.Error(collRes.exception, collRes.message)
        val coll = (collRes as DomainResult.Success).data

        val report = CustomerCollectionReport(
            customerId = customerId,
            customerCode = customer.customerCode,
            customerDisplayName = customer.displayName,
            totalOutstanding = coll.totalOutstanding,
            overdueAmount = coll.overdueAmount,
            overdueInvoiceCount = coll.overdueInvoiceCount,
            priority = coll.priority.name,
            pendingActionCount = coll.pendingActionCount,
            completedActionCount = coll.completedActionCount,
            activePromiseCount = coll.activePromiseCount,
            activePromisedAmount = coll.activePromisedAmount,
            nextFollowUpAt = coll.nextFollowUpAt
        )
        return DomainResult.Success(report)
    }

    override suspend fun getCustomerReconciliationReport(
        tenantId: String,
        projectId: String,
        customerId: String
    ): DomainResult<CustomerReconciliationReport> {
        val check = validateTenantCustomer(tenantId, projectId, customerId)
        if (check is DomainResult.Error) return check

        val custRes = customerRepository.findCustomerById(customerId)
        val customer = (custRes as DomainResult.Success).data

        val reconRes = ledgerService.reconcileCustomerReceivable(tenantId, projectId, customerId)
        if (reconRes is DomainResult.Error) return DomainResult.Error(reconRes.exception, reconRes.message)
        val recon = (reconRes as DomainResult.Success).data

        val report = CustomerReconciliationReport(
            customerId = customerId,
            customerCode = customer.customerCode,
            customerDisplayName = customer.displayName,
            reconciliationId = recon.reconciliationId,
            reconciledAt = recon.reconciledAt,
            isReconciled = recon.isConsistent,
            invoiceTotalReceivable = recon.invoiceTotalReceivable,
            ledgerCalculatedBalance = recon.ledgerCalculatedBalance,
            availableCreditBalance = recon.availableCreditBalance,
            varianceAmount = recon.difference,
            discrepancyCount = recon.discrepancyCount,
            discrepancies = recon.discrepancies
        )
        return DomainResult.Success(report)
    }

    override suspend fun getCustomerFinancialSummaryReport(
        tenantId: String,
        projectId: String,
        customerId: String,
        asOfDate: Long
    ): DomainResult<CustomerFinancialSummaryReport> {
        val check = validateTenantCustomer(tenantId, projectId, customerId)
        if (check is DomainResult.Error) return check

        val dashRes = dashboardService.getCustomerFinancialDashboard(tenantId, projectId, customerId, asOfDate)
        if (dashRes is DomainResult.Error) return DomainResult.Error(dashRes.exception, dashRes.message)
        val dash = (dashRes as DomainResult.Success).data

        val report = CustomerFinancialSummaryReport(
            customerId = customerId,
            customerCode = dash.customerCode,
            customerDisplayName = dash.customerDisplayName,
            accountNumber = dash.accountNumber,
            accountStatus = dash.accountStatus,
            totalInvoiced = dash.totalInvoiced,
            totalPaid = dash.totalPaid,
            totalAllocated = dash.totalAllocated,
            totalUnallocated = dash.totalUnallocated,
            availableCreditBalance = dash.availableCreditBalance,
            outstandingReceivable = dash.outstandingReceivable,
            creditLimit = dash.creditLimit,
            currentCreditExposure = dash.currentCreditExposure,
            availableCreditCapacity = dash.availableCreditCapacity,
            riskStatus = dash.riskStatus,
            financialHold = dash.financialHold,
            holdReason = dash.holdReason,
            agingSummary = dash.agingSummary,
            dueSchedule = dash.dueSchedule,
            reconciliationSummary = dash.reconciliationSummary,
            warnings = dash.warnings,
            recommendedActions = dash.recommendedActions
        )
        return DomainResult.Success(report)
    }

    override suspend fun exportFinancialReport(
        request: CustomerFinancialReportRequest
    ): DomainResult<CustomerFinancialGeneratedDocument> {
        val check = validateTenantCustomer(request.tenantId, request.projectId, request.customerId)
        if (check is DomainResult.Error) return check

        val docId = "DOC-${UUID.randomUUID().toString().take(12)}"
        val time = System.currentTimeMillis()

        val (contentStr, contentType, ext) = when (request.format) {
            CustomerFinancialReportFormat.CSV -> {
                when (request.reportType) {
                    CustomerFinancialReportType.CUSTOMER_STATEMENT -> {
                        val r = getCustomerStatementReport(request.tenantId, request.projectId, request.customerId, request.fromDate, request.toDate)
                        if (r is DomainResult.Error) return DomainResult.Error(r.exception, r.message)
                        Triple(CustomerFinancialReportGenerator.generateStatementCsv((r as DomainResult.Success).data), "text/csv", "csv")
                    }
                    CustomerFinancialReportType.INVOICE_REPORT -> {
                        val r = getCustomerInvoiceReport(request.tenantId, request.projectId, request.customerId, request.fromDate, request.toDate)
                        if (r is DomainResult.Error) return DomainResult.Error(r.exception, r.message)
                        Triple(CustomerFinancialReportGenerator.generateInvoicesCsv((r as DomainResult.Success).data), "text/csv", "csv")
                    }
                    CustomerFinancialReportType.PAYMENT_HISTORY -> {
                        val r = getCustomerPaymentHistoryReport(request.tenantId, request.projectId, request.customerId, request.fromDate, request.toDate)
                        if (r is DomainResult.Error) return DomainResult.Error(r.exception, r.message)
                        Triple(CustomerFinancialReportGenerator.generatePaymentsCsv((r as DomainResult.Success).data), "text/csv", "csv")
                    }
                    CustomerFinancialReportType.RECEIVABLE_AGING -> {
                        val r = getCustomerReceivableAgingReport(request.tenantId, request.projectId, request.customerId)
                        if (r is DomainResult.Error) return DomainResult.Error(r.exception, r.message)
                        Triple(CustomerFinancialReportGenerator.generateAgingCsv((r as DomainResult.Success).data), "text/csv", "csv")
                    }
                    else -> {
                        val summary = getCustomerFinancialSummaryReport(request.tenantId, request.projectId, request.customerId)
                        if (summary is DomainResult.Error) return DomainResult.Error(summary.exception, summary.message)
                        val s = (summary as DomainResult.Success).data
                        val csv = "Metric,Value\nCustomer,${s.customerDisplayName}\nTotal Invoiced,${s.totalInvoiced}\nTotal Paid,${s.totalPaid}\nOutstanding,${s.outstandingReceivable}\nCredit Limit,${s.creditLimit}\nRisk Status,${s.riskStatus.name}\n"
                        Triple(csv, "text/csv", "csv")
                    }
                }
            }
            CustomerFinancialReportFormat.PDF, CustomerFinancialReportFormat.JSON -> {
                val summary = getCustomerFinancialSummaryReport(request.tenantId, request.projectId, request.customerId)
                if (summary is DomainResult.Error) return DomainResult.Error(summary.exception, summary.message)
                val s = (summary as DomainResult.Success).data
                val lines = listOf(
                    "Account Number" to s.accountNumber,
                    "Account Status" to s.accountStatus.name,
                    "Total Invoiced" to "৳ ${s.totalInvoiced}",
                    "Total Paid" to "৳ ${s.totalPaid}",
                    "Total Allocated" to "৳ ${s.totalAllocated}",
                    "Total Unallocated" to "৳ ${s.totalUnallocated}",
                    "Available Credit Balance" to "৳ ${s.availableCreditBalance}",
                    "Outstanding Receivable" to "৳ ${s.outstandingReceivable}",
                    "Credit Limit" to "৳ ${s.creditLimit}",
                    "Net Credit Exposure" to "৳ ${s.currentCreditExposure}",
                    "Available Credit Limit" to "৳ ${s.availableCreditCapacity}",
                    "Risk Status" to s.riskStatus.name,
                    "Financial Hold" to if (s.financialHold) "YES (${s.holdReason ?: ""})" else "NO"
                )
                val textDoc = CustomerFinancialReportGenerator.generateDocumentLayout(
                    title = request.reportType.name.replace("_", " "),
                    customerCode = s.customerCode,
                    customerName = s.customerDisplayName,
                    lines = lines
                )
                val mime = if (request.format == CustomerFinancialReportFormat.PDF) "application/pdf" else "application/json"
                val extension = if (request.format == CustomerFinancialReportFormat.PDF) "pdf" else "json"
                Triple(textDoc, mime, extension)
            }
        }

        val fileName = "sucharu-${request.reportType.name.lowercase()}-${request.customerId}-$time.$ext"

        val generatedDoc = CustomerFinancialGeneratedDocument(
            documentId = docId,
            reportType = request.reportType,
            format = request.format,
            fileName = fileName,
            contentType = contentType,
            contentString = contentStr,
            generatedAt = time,
            generatedBy = request.requestedBy,
            correlationId = request.correlationId,
            metadata = mapOf("customerId" to request.customerId, "tenantId" to request.tenantId)
        )
        return DomainResult.Success(generatedDoc)
    }
}
