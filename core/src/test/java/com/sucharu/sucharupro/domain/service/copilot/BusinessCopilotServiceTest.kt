package com.sucharu.sucharupro.domain.service.copilot

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class BusinessCopilotServiceTest {

    private lateinit var service: BusinessCopilotService

    @Before
    fun setUp() {
        service = BusinessCopilotService()
    }

    @Test
    fun `processQuery_generatesBanglaResponseAndRequiresHumanConfirmationForActionProposals`() {
        val response = service.processQuery("আমার এই মাসের বকেয়া কত?", customerId = "CUST-1001")

        assertNotNull(response)
        assertEquals("bn-BD", response.language)
        assertEquals("QUERY_RECEIVABLES", response.detectedIntent)
        assertTrue(response.isConfirmationPending)

        // Verify Action Proposal & Confirmation Gate
        assertEquals(1, response.toolProposals.size)
        val proposal = response.toolProposals.first()
        assertEquals("RECORD_PAYMENT", proposal.actionType)
        assertTrue("High-risk action MUST require human confirmation!", proposal.isConfirmationRequired)
        assertFalse("Action MUST NOT be executed without human confirmation!", proposal.isExecuted)

        // CRITICAL INVARIANT PROOF:
        // ERP remains system of record. Zero shadow ERP databases created!
        assertFalse("Shadow ERP database MUST NOT be created!", response.isShadowErpDatabaseCreated)
    }

    @Test
    fun `confirmAndExecuteProposal_executesActionOnlyAfterExplicitHumanConfirmation`() {
        val response = service.processQuery("আমার এই মাসের বকেয়া কত?", customerId = "CUST-1001")
        val proposal = response.toolProposals.first()

        // Execute Confirmation Gate
        val executed = service.confirmAndExecuteProposal(proposal.proposalId, actorId = "CUST-1001")

        assertNotNull(executed)
        assertTrue("Proposal MUST be marked as confirmed by human!", executed.isConfirmedByHuman)
        assertTrue("Proposal MUST be marked as executed after confirmation!", executed.isExecuted)
    }
}
