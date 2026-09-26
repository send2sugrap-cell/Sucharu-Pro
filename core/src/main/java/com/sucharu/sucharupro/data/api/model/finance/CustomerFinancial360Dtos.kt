package com.sucharu.sucharupro.data.api.model.finance

import kotlinx.serialization.Serializable

@Serializable
data class CustomerInvoice360Dto(
    val invoiceId: String,
    val invoiceNumber: String,
    val invoiceDate: String,
    val dueDate: String,
    val totalAmount: String,
    val paidAmount: String,
    val outstandingAmount: String,
    val isOverdue: Boolean,
    val overdueDays: Int = 0
)

@Serializable
data class CustomerPayment360Dto(
    val paymentId: String,
    val paymentNumber: String,
    val paymentDate: String,
    val totalAmount: String,
    val allocatedAmount: String,
    val unallocatedAmount: String,
    val paymentMethod: String = "BKASH",
    val allocationStatus: String = "FULLY_ALLOCATED"
)

@Serializable
data class CustomerFinancial360Dto(
    val customerId: String,
    val customerCode: String,
    val displayName: String,
    val primaryPhone: String,
    val accountNumber: String,
    val accountStatus: String = "ACTIVE",

    val totalInvoiced: String,
    val totalCollected: String,
    val totalOutstandingDue: String,
    val currentDueAmount: String,
    val overdueAmount: String,
    val advanceCreditBalance: String,

    val creditLimit: String,
    val availableCreditCapacity: String,
    val isOnFinancialHold: Boolean = false,

    val reconciliationStatus: String = "RECONCILIATION_OK",
    val reconciliationDiscrepancyCount: Int = 0,
    val varianceAmount: String = "0.00",

    val invoices: List<CustomerInvoice360Dto> = emptyList(),
    val payments: List<CustomerPayment360Dto> = emptyList(),
    val generatedAt: String
)
