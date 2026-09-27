package com.sucharu.sucharupro.domain.service.ai

import com.sucharu.sucharupro.domain.model.ai.AiContextOutcomeType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SucharuAiContextOrchestratorTest {

    private lateinit var orchestrator: SucharuAiContextOrchestrator

    @Before
    fun setUp() {
        orchestrator = SucharuAiContextOrchestrator()
    }

    @Test
    fun `assembleContext_assemblesAuthorizedMemoriesKnowledgeAndMcpTools`() {
        val context = orchestrator.assembleContext(
            userId = "USR-1001",
            role = "CUSTOMER",
            projectId = "TENANT-001",
            customerId = "CUST-1001",
            query = "SOP"
        )

        assertNotNull(context)
        assertEquals("USR-1001", context.userId)
        assertEquals("CUSTOMER", context.role)
        assertEquals("TENANT-001", context.projectId)

        // Verify Assembled Components
        assertTrue(context.retrievedMemories.isNotEmpty())
        assertTrue(context.retrievedKnowledge.isNotEmpty())
        assertTrue(context.availableMcpTools.isNotEmpty())

        // CRITICAL INVARIANT PROOF:
        // Internal pricing secrets MUST be protected!
        assertTrue("Pricing secrets MUST be protected!", context.isPricingSecretsProtected)
    }

    @Test
    fun `orchestrateQuery_enforcesHumanConfirmationGatesForFinancialActions`() {
        val response = orchestrator.orchestrateQuery(
            userId = "USR-1001",
            role = "CUSTOMER",
            projectId = "TENANT-001",
            customerId = "CUST-1001",
            query = "আমার কত টাকা বকেয়া আছে?"
        )

        assertNotNull(response)
        assertEquals(AiContextOutcomeType.CONFIRMATION_REQUIRED, response.outcomeType)
        assertTrue("Financial mutations MUST require human confirmation!", response.isConfirmationRequired)

        assertEquals(1, response.actionProposals.size)
        val proposal = response.actionProposals.first()
        assertEquals("RECORD_PAYMENT", proposal.actionType)
        assertTrue(proposal.isConfirmationRequired)
        assertFalse(proposal.isConfirmedByHuman)
    }
}
