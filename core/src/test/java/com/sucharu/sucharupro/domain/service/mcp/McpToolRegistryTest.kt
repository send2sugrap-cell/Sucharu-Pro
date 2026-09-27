package com.sucharu.sucharupro.domain.service.mcp

import com.sucharu.sucharupro.domain.model.copilot.CopilotToolDefinition
import com.sucharu.sucharupro.domain.model.copilot.CopilotToolRiskLevel
import com.sucharu.sucharupro.domain.model.mcp.McpToolExecutionRequest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class McpToolRegistryTest {

    private lateinit var registry: McpToolRegistry

    @Before
    fun setUp() {
        registry = McpToolRegistry()
    }

    @Test
    fun `getRegistrySummary_returnsTypedToolsAndProtectsInternalPricingSecrets`() {
        val summary = registry.getRegistrySummary()

        assertNotNull(summary)
        assertTrue(summary.totalToolsCount >= 9)
        assertTrue("Pricing secrets MUST be protected!", summary.isInternalPricingSecretProtected)

        val toolNames = summary.registeredTools.map { it.toolName }
        assertTrue(toolNames.contains("get_customer_360"))
        assertTrue(toolNames.contains("record_customer_payment"))
        assertTrue(toolNames.contains("accept_and_lock_quotation"))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `register_prohibitsToolsExposingInternalVendorRatesOrMargins`() {
        val invalidTool = CopilotToolDefinition(
            toolId = "TOOL-INVALID",
            toolName = "get_internal_vendor_purchase_rate",
            description = "Unsafe tool exposing vendor purchase rates",
            riskLevel = CopilotToolRiskLevel.READ_ONLY,
            requiredCapability = "ADMIN_ONLY"
        )

        registry.register(invalidTool)
    }

    @Test
    fun `invokeTool_enforcesR0ReadOnlyAndR2HumanConfirmationGate`() {
        // 1. Invoke R0 Read-Only Tool (get_customer_360)
        val r0Req = McpToolExecutionRequest(
            toolName = "get_customer_360",
            actorUserId = "CUST-1001",
            projectId = "TENANT-001"
        )
        val r0Result = registry.invokeTool(r0Req)

        assertNotNull(r0Result)
        assertEquals("SUCCESS", r0Result.status)
        assertEquals(CopilotToolRiskLevel.READ_ONLY, r0Result.riskLevel)
        assertTrue(!r0Result.isConfirmationRequired)

        // 2. Invoke R2 Confirm-Required Tool (record_customer_payment)
        val r2Req = McpToolExecutionRequest(
            toolName = "record_customer_payment",
            actorUserId = "CUST-1001",
            projectId = "TENANT-001"
        )
        val r2Result = registry.invokeTool(r2Req)

        assertNotNull(r2Result)
        assertEquals("CONFIRMATION_REQUIRED", r2Result.status)
        assertEquals(CopilotToolRiskLevel.CONFIRM_REQUIRED, r2Result.riskLevel)
        assertTrue("R2 tool MUST require human confirmation proposal!", r2Result.isConfirmationRequired)
        assertNotNull(r2Result.proposalId)
    }
}
