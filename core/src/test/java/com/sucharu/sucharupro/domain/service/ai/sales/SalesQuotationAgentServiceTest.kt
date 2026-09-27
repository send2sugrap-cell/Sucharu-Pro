package com.sucharu.sucharupro.domain.service.ai.sales

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

class SalesQuotationAgentServiceTest {

    private lateinit var service: SalesQuotationAgentService

    @Before
    fun setUp() {
        service = SalesQuotationAgentService()
    }

    @Test
    fun `extractRequirementSpec_extractsPrintingSpecificationsFromBanglaQuery`() {
        val spec = service.extractRequirementSpec("আমার ৫০০০ কপি বই ছাপাতে হবে")

        assertNotNull(spec)
        assertEquals("Book Printing", spec.productType)
        assertEquals(5000L, spec.quantity)
        assertEquals(100, spec.pageCount)
        assertEquals("Perfect Binding", spec.bindingType)
        assertTrue(spec.isSpecificationComplete)
    }

    @Test
    fun `consultAndGenerateQuotationDraft_generatesNonBindingDraftRequiringHumanApproval`() {
        val response = service.consultAndGenerateQuotationDraft(
            query = "১০০০ কপি ভিজিটিং কার্ড",
            customerId = "CUST-1001",
            userRole = "CUSTOMER"
        )

        assertNotNull(response)
        assertTrue("Commercial secrecy MUST be protected!", response.isPricingSecretsProtected)
        assertTrue("Human approval MUST be required for quotation drafts!", response.isHumanApprovalRequired)

        val draft = response.quotationDraft
        assertNotNull(draft)
        assertEquals("DRAFT", draft!!.status)
        assertFalse("Quotation draft MUST NOT be auto-approved!", draft.isApprovedByHuman)
        assertEquals(BigDecimal("410.00"), draft.calculatedSellingPrice)
    }

    @Test
    fun `approveQuotationDraft_approvesDraftViaHumanApprovalGate`() {
        val response = service.consultAndGenerateQuotationDraft("১০০০ কপি কার্ড", customerId = "CUST-1001")
        val draftId = response.quotationDraft!!.draftId

        // Execute Human Staff/Admin Approval Gate (R2)
        val approvedDraft = service.approveQuotationDraft(draftId = draftId, approverStaffId = "STAFF-001")

        assertNotNull(approvedDraft)
        assertEquals("APPROVED", approvedDraft.status)
        assertTrue("Quotation draft MUST be marked as approved by human!", approvedDraft.isApprovedByHuman)
        assertEquals("STAFF-001", approvedDraft.approvedByStaffId)
    }
}
