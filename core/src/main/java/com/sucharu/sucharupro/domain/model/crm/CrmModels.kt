package com.sucharu.sucharupro.domain.model.crm

import com.sucharu.sucharupro.domain.model.customer.Customer
import java.math.BigDecimal

/**
 * BI-05 Lead Status Lifecycle.
 */
enum class LeadStatus {
    NEW,
    CONTACTED,
    QUALIFIED,
    QUOTED,
    CONVERTED,
    LOST
}

/**
 * BI-05 Customer Relationship Lifecycle Stage.
 */
enum class CustomerLifecycleStage {
    NEW_LEAD,
    QUALIFIED_LEAD,
    FIRST_TIME_CUSTOMER,
    ACTIVE_REPEAT_CUSTOMER,
    INACTIVE_CUSTOMER
}

/**
 * CRM Lead Entity.
 */
data class CrmLead(
    val leadId: String,
    val leadName: String,
    val contactPhone: String,
    val email: String? = null,
    val companyName: String? = null,
    val interestProduct: String? = null,
    val leadStatus: LeadStatus = LeadStatus.NEW,
    val assignedStaffId: String? = null,
    val notes: String? = null,
    val createdAt: String,
    val updatedAt: String,
    val createdBy: String,
    val updatedBy: String? = null
) {
    init {
        require(leadId.isNotBlank()) { "Lead ID cannot be blank." }
        require(leadName.isNotBlank()) { "Lead Name cannot be blank." }
        require(contactPhone.isNotBlank()) { "Contact Phone cannot be blank." }
    }
}

/**
 * Lead to Canonical Customer Conversion Result.
 */
data class LeadToCustomerConversionResult(
    val conversionId: String,
    val leadId: String,
    val customerId: String,
    val customerCode: String,
    val initialQuotationId: String? = null,
    val initialOrderId: String? = null,
    val convertedAt: String,
    val convertedBy: String
)

/**
 * BI-05 Master CRM & Customer Retention Summary Model.
 */
data class CrmLifecycleSummary(
    val totalLeadsCount: Int,
    val qualifiedLeadsCount: Int,
    val convertedCustomersCount: Int,
    val firstTimeOrderCustomersCount: Int,
    val activeRepeatCustomersCount: Int,
    val totalCommercialCustomerValueSum: BigDecimal,
    val activeLeads: List<CrmLead> = emptyList(),
    val repeatCustomers: List<Customer> = emptyList(),
    val generatedAt: String
)
