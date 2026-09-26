package com.sucharu.sucharupro.domain.service.crm

import com.sucharu.sucharupro.domain.model.crm.CrmLead
import com.sucharu.sucharupro.domain.model.crm.LeadStatus
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test

class CrmLifecycleServiceTest {

    private lateinit var service: CrmLifecycleService

    @Before
    fun setUp() {
        service = CrmLifecycleService()
    }

    @Test
    fun `convertLeadToCustomer_linksLeadToCustomerWithoutDuplicates`() = runBlocking {
        val newLead = CrmLead(
            leadId = "LEAD-2026-TEST",
            leadName = "Kamrul Hasan",
            contactPhone = "01811000000",
            companyName = "Hasan Graphics",
            interestProduct = "Book & Magazine Printing",
            leadStatus = LeadStatus.QUALIFIED,
            createdAt = "2026-09-26T20:00:00Z",
            updatedAt = "2026-09-26T20:00:00Z",
            createdBy = "STAFF-001"
        )

        service.createLead(newLead)

        // Convert Lead to Canonical Customer
        val result = service.convertLeadToCustomer(
            leadId = "LEAD-2026-TEST",
            initialQuotationId = "QUOTE-2026-001",
            initialOrderId = "ORD-1001",
            actorId = "STAFF-001"
        )

        assertNotNull(result)
        assertEquals("LEAD-2026-TEST", result.leadId)
        assertNotNull(result.customerId)
        assertNotNull(result.customerCode)

        // Verify summary tracks converted leads
        val summary = service.buildCrmLifecycleSummary()
        assertEquals(1, summary.convertedCustomersCount)
    }
}
