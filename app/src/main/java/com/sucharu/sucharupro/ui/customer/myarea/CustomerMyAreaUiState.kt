package com.sucharu.sucharupro.ui.customer.myarea

import com.sucharu.sucharupro.domain.model.common.Money

/**
 * Customer Profile Info Model for My Area.
 */
data class CustomerProfileInfo(
    val customerId: String,
    val customerCode: String,
    val displayName: String,
    val primaryPhone: String,
    val email: String,
    val address: String,
    val accountStatus: String = "ACTIVE"
)

/**
 * Customer Order Summary Model for My Area.
 */
data class MyOrderSummary(
    val orderId: String,
    val orderNumber: String,
    val title: String,
    val statusLabel: String,
    val totalAmountFormatted: String,
    val createdDateFormatted: String,
    val customerProgressLabel: String
)

/**
 * Customer Quotation Summary Model for My Area.
 */
data class MyQuotationSummary(
    val quotationId: String,
    val quoteNumber: String,
    val title: String,
    val statusLabel: String,
    val totalAmountFormatted: String,
    val validUntilFormatted: String
)

/**
 * Customer Payment & Invoice Snapshot Model for My Area.
 */
data class MyAccountFinancialSnapshot(
    val totalInvoicedFormatted: String,
    val totalPaidFormatted: String,
    val totalOutstandingDueFormatted: String,
    val outstandingInvoiceCount: Int = 0
)

/**
 * Customer Authorized Document Model for My Area.
 */
data class MyDocumentItem(
    val documentId: String,
    val title: String,
    val documentType: String,
    val dateFormatted: String,
    val isDownloadable: Boolean = true
)

/**
 * Top-Level Aggregate Presentation Container for Customer My Area.
 */
data class CustomerMyAreaSummary(
    val profile: CustomerProfileInfo,
    val financials: MyAccountFinancialSnapshot,
    val activeOrders: List<MyOrderSummary>,
    val quotations: List<MyQuotationSummary>,
    val documents: List<MyDocumentItem>,
    val unreadNotificationsCount: Int = 0
)

/**
 * Reactive Presentation UI State for Customer My Area.
 */
sealed interface CustomerMyAreaUiState {
    object Loading : CustomerMyAreaUiState
    data class Success(
        val summary: CustomerMyAreaSummary,
        val isRefreshing: Boolean = false
    ) : CustomerMyAreaUiState
    data class Error(val errorMessage: String) : CustomerMyAreaUiState
    data class Empty(val message: String = "No Customer Account Data Available") : CustomerMyAreaUiState
}
