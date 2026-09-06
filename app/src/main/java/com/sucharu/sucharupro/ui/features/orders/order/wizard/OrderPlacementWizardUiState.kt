package com.sucharu.sucharupro.ui.features.orders.order.wizard

import com.sucharu.sucharupro.domain.model.order.Order
import java.math.BigDecimal

enum class WizardStep(val stepNumber: Int, val title: String) {
    CUSTOMER(1, "Customer Context"),
    PRODUCT(2, "Product / Service"),
    SPECIFICATIONS(3, "Specifications"),
    QUANTITY(4, "Quantity"),
    ESTIMATION(5, "Commercial Estimate"),
    REVIEW(6, "Quotation Review")
}

data class OrderPlacementWizardUiState(
    val currentStep: WizardStep = WizardStep.CUSTOMER,
    
    // Step 1: Customer Context
    val customerId: String = "CUST-DEFAULT-001",
    val customerName: String = "Standard Commercial Client",
    val customerEmail: String = "client@sucharu.pro",
    
    // Step 2: Product Selection
    val selectedProductCategory: String = "Flyers / Brochures",
    val selectedProductDescription: String = "Standard Commercial Printing Job",
    
    // Step 3: Specifications
    val itemDescription: String = "A4 Promotional Flyer - 150 GSM Gloss",
    val widthMm: Int = 210,
    val heightMm: Int = 297,
    val paperGsm: Int = 150,
    val paperType: String = "Art Paper (Gloss / Matt)",
    val printingSides: String = "Double-Sided 4/4",
    val colorMode: String = "CMYK 4-Color",
    val lamination: String = "Matte Lamination",
    val binding: String = "None",
    
    // Step 4: Quantity
    val quantity: Int = 1000,
    val quantityError: String? = null,
    
    // Step 5: Commercial Estimation
    val isCalculating: Boolean = false,
    val unitPrice: BigDecimal = BigDecimal("15.50"),
    val subtotalAmount: BigDecimal = BigDecimal("15500.00"),
    val discountAmount: BigDecimal = BigDecimal("0.00"),
    val taxAmount: BigDecimal = BigDecimal("0.00"),
    val totalAmount: BigDecimal = BigDecimal("15500.00"),
    val estimateDiagnostics: List<String> = emptyList(),
    
    // Step 6: Review & Confirmation
    val orderNotes: String = "Standard delivery within 3 business days.",
    val isSubmitting: Boolean = false,
    val createdOrder: Order? = null,
    val submissionError: String? = null
)
