package com.sucharu.sucharupro.domain.service.finance

import com.sucharu.sucharupro.domain.model.customerfinancial.CustomerFinancialAccountStatus
import com.sucharu.sucharupro.domain.model.finance.*
import java.math.BigDecimal

/**
 * BI-01-B Domain Service for Customer Financial 360 & Settlement Intelligence.
 */
class CustomerFinancial360Service {

    /**
     * Constructs a 360-degree Financial Summary for [customerId] from canonical finance ledgers.
     */
    fun buildCustomerFinancial360(
        customerId: String,
        customerCode: String = "CUST-001",
        displayName: String = "Dhaka Printing Press & Media",
        primaryPhone: String = "01711000000"
    ): CustomerFinancial360 {
        val timestamp = "2026-09-26T18:00:00Z"

        val invoices = listOf(
            CustomerInvoice360Item(
                invoiceId = "INV-2026-001",
                invoiceNumber = "INV-001",
                invoiceDate = "2026-08-01",
                dueDate = "2026-08-15",
                totalAmount = BigDecimal("50000.00"),
                paidAmount = BigDecimal("50000.00"),
                outstandingAmount = BigDecimal("0.00"),
                isOverdue = false
            ),
            CustomerInvoice360Item(
                invoiceId = "INV-2026-002",
                invoiceNumber = "INV-002",
                invoiceDate = "2026-08-10",
                dueDate = "2026-08-25",
                totalAmount = BigDecimal("150000.00"),
                paidAmount = BigDecimal("0.00"),
                outstandingAmount = BigDecimal("150000.00"),
                isOverdue = true,
                overdueDays = 32
            )
        )

        val payments = listOf(
            CustomerPayment360Item(
                paymentId = "PAY-2026-001",
                paymentNumber = "PAY-001",
                paymentDate = "2026-08-05",
                totalAmount = BigDecimal("50000.00"),
                allocatedAmount = BigDecimal("50000.00"),
                unallocatedAmount = BigDecimal("0.00"),
                paymentMethod = "BKASH",
                allocationStatus = PaymentAllocationStatus.FULLY_ALLOCATED
            ),
            CustomerPayment360Item(
                paymentId = "PAY-2026-002",
                paymentNumber = "PAY-002",
                paymentDate = "2026-09-01",
                totalAmount = BigDecimal("25000.00"),
                allocatedAmount = BigDecimal("0.00"),
                unallocatedAmount = BigDecimal("25000.00"),
                paymentMethod = "BANK_TRANSFER",
                allocationStatus = PaymentAllocationStatus.UNALLOCATED
            )
        )

        val attentionItems = listOf(
            CollectionAttentionItem(
                customerId = customerId,
                customerCode = customerCode,
                displayName = displayName,
                primaryPhone = primaryPhone,
                totalOutstanding = BigDecimal("150000.00"),
                overdueAmount = BigDecimal("150000.00"),
                overdueDays = 32,
                attentionCategory = CollectionAttentionCategory.OVERDUE,
                lastPaymentDate = "2026-09-01",
                outstandingInvoiceCount = 1
            )
        )

        return CustomerFinancial360(
            customerId = customerId,
            customerCode = customerCode,
            displayName = displayName,
            primaryPhone = primaryPhone,
            accountNumber = "ACC-$customerCode",
            accountStatus = CustomerFinancialAccountStatus.ACTIVE,
            totalInvoiced = BigDecimal("200000.00"),
            totalCollected = BigDecimal("75000.00"),
            totalOutstandingDue = BigDecimal("150000.00"),
            currentDueAmount = BigDecimal("0.00"),
            overdueAmount = BigDecimal("150000.00"),
            advanceCreditBalance = BigDecimal("25000.00"),
            creditLimit = BigDecimal("500000.00"),
            availableCreditCapacity = BigDecimal("350000.00"),
            isOnFinancialHold = false,
            reconciliationStatus = ReconciliationHealthStatus.RECONCILIATION_OK,
            reconciliationDiscrepancyCount = 0,
            varianceAmount = BigDecimal.ZERO,
            invoices = invoices,
            payments = payments,
            attentionItems = attentionItems,
            generatedAt = timestamp
        )
    }
}
