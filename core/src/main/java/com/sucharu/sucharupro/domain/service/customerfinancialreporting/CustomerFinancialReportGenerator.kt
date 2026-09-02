package com.sucharu.sucharupro.domain.service.customerfinancialreporting

import com.sucharu.sucharupro.domain.model.customerfinancialreporting.*
import java.text.SimpleDateFormat
import java.util.*

object CustomerFinancialReportGenerator {

    private val isoDateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }

    private fun escapeCsv(value: Any?): String {
        if (value == null) return ""
        val str = value.toString()
        return if (str.contains(",") || str.contains("\"") || str.contains("\n") || str.contains("\r")) {
            "\"" + str.replace("\"", "\"\"") + "\""
        } else {
            str
        }
    }

    fun generateStatementCsv(report: CustomerStatementReport): String {
        val sb = StringBuilder()
        sb.appendLine("Customer Statement - Sucharu Pro ERP")
        sb.appendLine("Customer: ${report.customerDisplayName} (${report.customerCode})")
        sb.appendLine("Account: ${report.accountNumber}")
        sb.appendLine("Opening Balance: ${report.openingBalance}, Closing Balance: ${report.closingNetBalance}")
        sb.appendLine("Generated At (UTC): ${isoDateFormat.format(Date(report.generatedAt))}")
        sb.appendLine()
        sb.appendLine("Entry ID,Effective Date (UTC),Entry Type,Reference Type,Reference ID,Debit,Credit,Balance After,Description")

        for (e in report.entries) {
            val dateStr = isoDateFormat.format(Date(e.effectiveAt))
            sb.appendLine(
                listOf(
                    escapeCsv(e.entryId),
                    escapeCsv(dateStr),
                    escapeCsv(e.entryType.name),
                    escapeCsv(e.referenceType),
                    escapeCsv(e.referenceId),
                    escapeCsv(e.debitAmount),
                    escapeCsv(e.creditAmount),
                    escapeCsv(e.balanceAfter),
                    escapeCsv(e.description)
                ).joinToString(",")
            )
        }
        return sb.toString()
    }

    fun generateInvoicesCsv(report: CustomerInvoiceReport): String {
        val sb = StringBuilder()
        sb.appendLine("Customer Invoices Report - Sucharu Pro ERP")
        sb.appendLine("Customer: ${report.customerDisplayName} (${report.customerCode})")
        sb.appendLine("Total Invoiced: ${report.totalInvoicedAmount}, Total Due: ${report.totalDueAmount}")
        sb.appendLine()
        sb.appendLine("Invoice ID,Invoice Number,Issue Date,Due Date,Status,Grand Total,Paid Amount,Due Amount,Is Overdue,Days Overdue")

        for (inv in report.invoices) {
            sb.appendLine(
                listOf(
                    escapeCsv(inv.invoiceId),
                    escapeCsv(inv.invoiceNumber),
                    escapeCsv(isoDateFormat.format(Date(inv.issueDate))),
                    escapeCsv(isoDateFormat.format(Date(inv.dueDate))),
                    escapeCsv(inv.status),
                    escapeCsv(inv.grandTotal),
                    escapeCsv(inv.paidAmount),
                    escapeCsv(inv.dueAmount),
                    escapeCsv(inv.isOverdue),
                    escapeCsv(inv.daysOverdue)
                ).joinToString(",")
            )
        }
        return sb.toString()
    }

    fun generatePaymentsCsv(report: CustomerPaymentHistoryReport): String {
        val sb = StringBuilder()
        sb.appendLine("Customer Payment History - Sucharu Pro ERP")
        sb.appendLine("Customer: ${report.customerDisplayName} (${report.customerCode})")
        sb.appendLine("Total Payments: ${report.totalPayments}, Total Paid: ${report.totalPaidAmount}")
        sb.appendLine()
        sb.appendLine("Payment ID,Payment Number,Payment Date,Method,Amount,Status,Reference Number")

        for (pay in report.payments) {
            sb.appendLine(
                listOf(
                    escapeCsv(pay.paymentId),
                    escapeCsv(pay.paymentNumber),
                    escapeCsv(isoDateFormat.format(Date(pay.paymentDate))),
                    escapeCsv(pay.paymentMethod),
                    escapeCsv(pay.amount),
                    escapeCsv(pay.status),
                    escapeCsv(pay.referenceNumber)
                ).joinToString(",")
            )
        }
        return sb.toString()
    }

    fun generateAgingCsv(report: CustomerReceivableAgingReport): String {
        val sb = StringBuilder()
        sb.appendLine("Customer Receivable Aging Report - Sucharu Pro ERP")
        sb.appendLine("Customer: ${report.customerDisplayName} (${report.customerCode})")
        sb.appendLine("Risk Status: ${report.riskStatus.name}, Total Outstanding: ${report.totalOutstanding}")
        sb.appendLine()
        sb.appendLine("Bucket,Amount")
        sb.appendLine("Current,${report.currentAmount}")
        sb.appendLine("1–7 Days,${report.days1To7Amount}")
        sb.appendLine("8–30 Days,${report.days8To30Amount}")
        sb.appendLine("31–60 Days,${report.days31To60Amount}")
        sb.appendLine("61–90 Days,${report.days61To90Amount}")
        sb.appendLine("90+ Days,${report.days90PlusAmount}")
        sb.appendLine("Total Outstanding,${report.totalOutstanding}")
        return sb.toString()
    }

    fun generateDocumentLayout(title: String, customerCode: String, customerName: String, lines: List<Pair<String, String>>): String {
        val sb = StringBuilder()
        sb.appendLine("================================================================================")
        sb.appendLine("                     SUCHARU GRAPHICS / SUCHARU PRO ERP                         ")
        sb.appendLine("                            FINANCIAL REPORT                                    ")
        sb.appendLine("================================================================================")
        sb.appendLine("Report Title: $title")
        sb.appendLine("Customer:     $customerName ($customerCode)")
        sb.appendLine("Generated At: ${isoDateFormat.format(Date())} UTC")
        sb.appendLine("--------------------------------------------------------------------------------")
        for ((k, v) in lines) {
            sb.appendLine(String.format("%-35s : %s", k, v))
        }
        sb.appendLine("================================================================================")
        sb.appendLine("CONFIDENTIAL & PROPRIETARY — SUCHARU PRO FINANCIAL MANAGEMENT SUBSYSTEM")
        return sb.toString()
    }
}
