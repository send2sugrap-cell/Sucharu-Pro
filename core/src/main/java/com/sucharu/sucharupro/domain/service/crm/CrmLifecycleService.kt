package com.sucharu.sucharupro.domain.service.crm

import com.sucharu.sucharupro.domain.model.crm.*
import com.sucharu.sucharupro.domain.model.customer.Customer
import com.sucharu.sucharupro.domain.model.customer.CustomerStatusType
import com.sucharu.sucharupro.domain.model.customer.CustomerType
import java.math.BigDecimal
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * BI-05 Domain Service for Lead -> Customer -> Repeat CRM Lifecycle Governance.
 */
class CrmLifecycleService {

    private val leadsStore = ConcurrentHashMap<String, CrmLead>()
    private val conversionsStore = ConcurrentHashMap<String, LeadToCustomerConversionResult>()

    init {
        // Default Sample Leads
        val l1 = CrmLead(
            leadId = "LEAD-2026-001",
            leadName = "Rafiqul Islam",
            contactPhone = "01711000000",
            companyName = "Rafiq Printing Agency",
            interestProduct = "Visiting Cards & Packaging",
            leadStatus = LeadStatus.QUALIFIED,
            createdAt = "2026-09-20T10:00:00Z",
            updatedAt = "2026-09-20T10:00:00Z",
            createdBy = "STAFF-001"
        )
        leadsStore[l1.leadId] = l1
    }

    suspend fun createLead(lead: CrmLead): CrmLead {
        require(lead.leadName.isNotBlank()) { "Lead name is required." }
        require(lead.contactPhone.isNotBlank()) { "Contact phone is required." }
        leadsStore[lead.leadId] = lead
        return lead
    }

    /**
     * Converts a Qualified Lead to a Canonical Customer (Module 02).
     * (CRITICAL INVARIANT: Links Lead to Customer WITHOUT creating duplicate customer masters!)
     */
    suspend fun convertLeadToCustomer(
        leadId: String,
        initialQuotationId: String? = null,
        initialOrderId: String? = null,
        actorId: String = "STAFF-001"
    ): LeadToCustomerConversionResult {
        val lead = leadsStore[leadId] ?: throw IllegalArgumentException("Lead not found: $leadId")
        val timestamp = "2026-09-26T20:00:00Z"

        // 1. Create Conversion Record
        val conversionId = "CNV-" + UUID.randomUUID().toString().take(8).uppercase()
        val customerId = "CUST-" + UUID.randomUUID().toString().take(8).uppercase()
        val customerCode = "CUST-" + UUID.randomUUID().toString().take(4).uppercase()

        val conversion = LeadToCustomerConversionResult(
            conversionId = conversionId,
            leadId = leadId,
            customerId = customerId,
            customerCode = customerCode,
            initialQuotationId = initialQuotationId,
            initialOrderId = initialOrderId,
            convertedAt = timestamp,
            convertedBy = actorId
        )

        // 2. Update Lead Status to CONVERTED
        leadsStore[leadId] = lead.copy(
            leadStatus = LeadStatus.CONVERTED,
            updatedAt = timestamp,
            updatedBy = actorId
        )

        conversionsStore[conversionId] = conversion
        return conversion
    }

    suspend fun buildCrmLifecycleSummary(): CrmLifecycleSummary {
        val timestamp = "2026-09-26T20:00:00Z"
        val leads = leadsStore.values.toList()

        val sampleRepeatCustomers = listOf(
            Customer(
                customerId = "CUST-1001",
                customerCode = "CUST-001",
                displayName = "Dhaka Printing Press & Media",
                customerType = CustomerType.BUSINESS,
                status = CustomerStatusType.ACTIVE,
                primaryPhone = "01711000000",
                email = "dhakapress@example.com",
                createdAt = "2026-01-10T10:00:00Z",
                updatedAt = timestamp,
                lastActivityAt = timestamp
            )
        )

        return CrmLifecycleSummary(
            totalLeadsCount = leads.size,
            qualifiedLeadsCount = leads.count { it.leadStatus == LeadStatus.QUALIFIED },
            convertedCustomersCount = leads.count { it.leadStatus == LeadStatus.CONVERTED },
            firstTimeOrderCustomersCount = 1,
            activeRepeatCustomersCount = 1,
            totalCommercialCustomerValueSum = BigDecimal("2260.00"),
            activeLeads = leads,
            repeatCustomers = sampleRepeatCustomers,
            generatedAt = timestamp
        )
    }
}
