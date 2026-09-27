package com.sucharu.sucharupro.data.api.model.ai.sales

import kotlinx.serialization.Serializable

@Serializable
data class PrintingRequirementSpecDto(
    val productType: String = "Visiting Card",
    val quantity: Long = 1000,
    val paperGsm: String = "300 GSM Art Card",
    val sizeDimensions: String = "3.5x2 inches",
    val pageCount: Int = 2,
    val colorConfig: String = "4/4 CMYK Color",
    val finishingLamination: String = "Matte Lamination",
    val bindingType: String = "Cut-to-size",
    val isSpecificationComplete: Boolean = true
)

@Serializable
data class QuotationDraftDto(
    val draftId: String,
    val customerId: String,
    val customerName: String,
    val spec: PrintingRequirementSpecDto,
    val calculatedSellingPrice: String,
    val isApprovedByHuman: Boolean = false,
    val approvedByStaffId: String? = null,
    val status: String = "DRAFT",
    val generatedAt: String
)

@Serializable
data class ConsultSalesQueryRequestDto(
    val query: String,
    val customerId: String? = null
)

@Serializable
data class SalesConsultantResponseDto(
    val query: String,
    val requirementSpec: PrintingRequirementSpecDto,
    val adviceText: String,
    val quotationDraft: QuotationDraftDto? = null,
    val isHumanApprovalRequired: Boolean = true,
    val isPricingSecretsProtected: Boolean = true,
    val generatedAt: String
)
