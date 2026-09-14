package com.sucharu.sucharupro.ui.customer.myarea

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sucharu.sucharupro.data.api.model.AuthenticatedPrincipal
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel managing presentation state for Customer My Area / Account Experience.
 */
class CustomerMyAreaViewModel : ViewModel() {

    private val _uiState = MutableStateFlow<CustomerMyAreaUiState>(CustomerMyAreaUiState.Loading)
    val uiState: StateFlow<CustomerMyAreaUiState> = _uiState.asStateFlow()

    fun loadMyArea(principal: AuthenticatedPrincipal?) {
        viewModelScope.launch {
            _uiState.value = CustomerMyAreaUiState.Loading

            try {
                val summary = buildCustomerMyAreaSummary(principal)
                _uiState.value = CustomerMyAreaUiState.Success(summary)
            } catch (e: Exception) {
                _uiState.value = CustomerMyAreaUiState.Error(
                    e.localizedMessage ?: "Failed to load Customer My Area. Please try again."
                )
            }
        }
    }

    fun refresh(principal: AuthenticatedPrincipal?) {
        viewModelScope.launch {
            val currentState = _uiState.value
            if (currentState is CustomerMyAreaUiState.Success) {
                _uiState.value = currentState.copy(isRefreshing = true)
            }

            try {
                val summary = buildCustomerMyAreaSummary(principal)
                _uiState.value = CustomerMyAreaUiState.Success(summary = summary, isRefreshing = false)
            } catch (e: Exception) {
                _uiState.value = CustomerMyAreaUiState.Error(
                    e.localizedMessage ?: "Failed to refresh Customer My Area."
                )
            }
        }
    }

    private fun buildCustomerMyAreaSummary(principal: AuthenticatedPrincipal?): CustomerMyAreaSummary {
        val custId = principal?.effectiveCustomerId ?: "CUS-001"
        val username = principal?.username ?: "Customer"

        val profile = CustomerProfileInfo(
            customerId = custId,
            customerCode = "CUS-001",
            displayName = if (username.isBlank()) "Acme Printing Corporation" else username,
            primaryPhone = "+880 1700-000001",
            email = "accounts@acmeprinting.com",
            address = "House 42, Road 11, Banani, Dhaka-1213",
            accountStatus = "ACTIVE"
        )

        val financials = MyAccountFinancialSnapshot(
            totalInvoicedFormatted = "৳150,000.00",
            totalPaidFormatted = "৳100,000.00",
            totalOutstandingDueFormatted = "৳50,000.00",
            outstandingInvoiceCount = 1
        )

        val activeOrders = listOf(
            MyOrderSummary(
                orderId = "ord-001",
                orderNumber = "ORD-000001",
                title = "Visiting Cards 1000 Pcs (300GSM Art Card)",
                statusLabel = "IN PRODUCTION",
                totalAmountFormatted = "৳150,000.00",
                createdDateFormatted = "10 Sept 2026",
                customerProgressLabel = "Printing in progress on Offset Press"
            ),
            MyOrderSummary(
                orderId = "ord-002",
                orderNumber = "ORD-000002",
                title = "Corporate A4 Brochure 2500 Pcs",
                statusLabel = "DELIVERED",
                totalAmountFormatted = "৳250,000.00",
                createdDateFormatted = "08 Sept 2026",
                customerProgressLabel = "Successfully delivered via Paperfly"
            )
        )

        val quotations = listOf(
            MyQuotationSummary(
                quotationId = "QT-001",
                quoteNumber = "QT-2026-001",
                title = "Custom Hardcover Annual Diary 500 Pcs",
                statusLabel = "PENDING APPROVAL",
                totalAmountFormatted = "৳180,000.00",
                validUntilFormatted = "20 Sept 2026"
            )
        )

        val documents = listOf(
            MyDocumentItem("DOC-001", "Tax Invoice #INV-000001", "INVOICE", "10 Sept 2026"),
            MyDocumentItem("DOC-002", "Official Payment Receipt #PAY-000001", "RECEIPT", "11 Sept 2026"),
            MyDocumentItem("DOC-003", "Approved Artwork Proof #ART-101", "PROOF", "09 Sept 2026")
        )

        return CustomerMyAreaSummary(
            profile = profile,
            financials = financials,
            activeOrders = activeOrders,
            quotations = quotations,
            documents = documents,
            unreadNotificationsCount = 0
        )
    }
}
