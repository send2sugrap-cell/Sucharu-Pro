package com.sucharu.sucharupro.domain.model.ai.sales

import java.math.BigDecimal

/**
 * Structured Printing Requirement Specification Model extracted from Natural Language.
 */
data class PrintingRequirementSpec(
    val productType: String = "Visiting Card", // Visiting Card, Flyer, Brochure, Book, Packaging, etc.
    val quantity: Long = 1000,
    val paperGsm: String = "300 GSM Art Card",
    val sizeDimensions: String = "3.5x2 inches",
    val pageCount: Int = 2,
    val colorConfig: String = "4/4 CMYK Color",
    val finishingLamination: String = "Matte Lamination",
    val bindingType: String = "Cut-to-size",
    val targetDeliveryDate: String? = null,
    val isSpecificationComplete: Boolean = true
)

/**
 * Non-Binding R1 Quotation Draft Model requiring Human Approval (R2).
 */
data class QuotationDraft(
    val draftId: String,
    val customerId: String,
    val customerName: String = "Customer",
    val spec: PrintingRequirementSpec,
    val calculatedTotalCost: BigDecimal,
    val calculatedSellingPrice: BigDecimal,
    val isApprovedByHuman: Boolean = false, // Critical Invariant: Requires human approval!
    val approvedByStaffId: String? = null,
    val status: String = "DRAFT", // DRAFT -> APPROVED -> ACCEPTED_COMMERCIAL_LOCK
    val generatedAt: String
)

/**
 * Sales Consultant Response Model.
 */
data class SalesConsultantResponse(
    val query: String,
    val requirementSpec: PrintingRequirementSpec,
    val adviceText: String,
    val quotationDraft: QuotationDraft? = null,
    val isHumanApprovalRequired: Boolean = true,
    val isPricingSecretsProtected: Boolean = true, // Critical Invariant: Always true!
    val generatedAt: String
)
