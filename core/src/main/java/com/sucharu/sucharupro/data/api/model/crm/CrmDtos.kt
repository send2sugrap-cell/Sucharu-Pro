package com.sucharu.sucharupro.data.api.model.crm

import kotlinx.serialization.Serializable

@Serializable
data class CrmLeadDto(
    val leadId: String,
    val leadName: String,
    val contactPhone: String,
    val email: String? = null,
    val companyName: String? = null,
    val interestProduct: String? = null,
    val leadStatus: String = "NEW",
    val assignedStaffId: String? = null,
    val notes: String? = null,
    val createdAt: String = "",
    val updatedAt: String = ""
)

@Serializable
data class ConvertLeadToCustomerRequestDto(
    val leadId: String,
    val initialQuotationId: String? = null,
    val initialOrderId: String? = null
)

@Serializable
data class LeadToCustomerConversionResultDto(
    val conversionId: String,
    val leadId: String,
    val customerId: String,
    val customerCode: String,
    val initialQuotationId: String? = null,
    val initialOrderId: String? = null,
    val convertedAt: String,
    val convertedBy: String
)

@Serializable
data class CrmLifecycleSummaryDto(
    val totalLeadsCount: Int,
    val qualifiedLeadsCount: Int,
    val convertedCustomersCount: Int,
    val firstTimeOrderCustomersCount: Int,
    val activeRepeatCustomersCount: Int,
    val totalCommercialCustomerValueSum: String,
    val activeLeads: List<CrmLeadDto> = emptyList(),
    val generatedAt: String
)
