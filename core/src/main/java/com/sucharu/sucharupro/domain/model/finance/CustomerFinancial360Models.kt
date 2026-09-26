package com.sucharu.sucharupro.domain.model.finance

import com.sucharu.sucharupro.domain.model.customerfinancial.CustomerFinancialAccountStatus
import java.math.BigDecimal

/**
 * BI-01-B Payment & Settlement Allocation Status.
 */
enum class PaymentAllocationStatus {
    FULLY_ALLOCATED,
    PARTIALLY_ALLOCATED,
    UNALLOCATED,
    ADVANCE
}

/**
 * BI-01-B Reconciliation Health Status.
 */
enum class ReconciliationHealthStatus {
    RECONCILIATION_OK,
    RECONCILIATION_EXCEPTION,
    UNRESOLVED_DISCREPANCY
}

/**
 * Customer Invoice 360 Item.
 */
data class CustomerInvoice360Item(
    val invoiceId: String,
    val invoiceNumber: String,
    val invoiceDate: String,
    val dueDate: String,
    val totalAmount: BigDecimal,
    val paidAmount: BigDecimal,
    val outstandingAmount: BigDecimal,
    val isOverdue: Boolean,
    val overdueDays: Int = 0
)

/**
 * Customer Payment 360 Item.
 */
data class CustomerPayment360Item(
    val paymentId: String,
    val paymentNumber: String,
    val paymentDate: String,
    val totalAmount: BigDecimal,
    val allocatedAmount: BigDecimal,
    val unallocatedAmount: BigDecimal,
    val paymentMethod: String = "BKASH",
    val allocationStatus: PaymentAllocationStatus
)

/**
 * BI-01-B Customer Financial 360 Master Read Model.
 */
data class CustomerFinancial360(
    val customerId: String,
    val customerCode: String,
    val displayName: String,
    val primaryPhone: String,
    val accountNumber: String,
    val accountStatus: CustomerFinancialAccountStatus = CustomerFinancialAccountStatus.ACTIVE,

    val totalInvoiced: BigDecimal,
    val totalCollected: BigDecimal,
    val totalOutstandingDue: BigDecimal,
    val currentDueAmount: BigDecimal,
    val overdueAmount: BigDecimal,
    val advanceCreditBalance: BigDecimal,

    val creditLimit: BigDecimal,
    val availableCreditCapacity: BigDecimal,
    val isOnFinancialHold: Boolean = false,

    val reconciliationStatus: ReconciliationHealthStatus = ReconciliationHealthStatus.RECONCILIATION_OK,
    val reconciliationDiscrepancyCount: Int = 0,
    val varianceAmount: BigDecimal = BigDecimal.ZERO,

    val invoices: List<CustomerInvoice360Item> = emptyList(),
    val payments: List<CustomerPayment360Item> = emptyList(),
    val attentionItems: List<CollectionAttentionItem> = emptyList(),

    val generatedAt: String
)
