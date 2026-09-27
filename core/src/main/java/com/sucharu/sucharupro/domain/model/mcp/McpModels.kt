package com.sucharu.sucharupro.domain.model.mcp

import com.sucharu.sucharupro.domain.model.copilot.CopilotToolDefinition
import com.sucharu.sucharupro.domain.model.copilot.CopilotToolRiskLevel

/**
 * MCP Tool Execution Request.
 */
data class McpToolExecutionRequest(
    val toolName: String,
    val parameters: Map<String, String> = emptyMap(),
    val actorUserId: String,
    val actorRole: String = "CUSTOMER",
    val projectId: String = "TENANT-001"
) {
    init {
        require(toolName.isNotBlank()) { "Tool name cannot be blank." }
        require(actorUserId.isNotBlank()) { "Actor User ID cannot be blank." }
        require(projectId.isNotBlank()) { "Project ID cannot be blank." }
    }
}

/**
 * MCP Tool Execution Result with Risk Policy & Confirmation Metadata.
 */
data class McpToolExecutionResult(
    val toolName: String,
    val status: String = "SUCCESS",
    val dataJson: String,
    val riskLevel: CopilotToolRiskLevel = CopilotToolRiskLevel.READ_ONLY,
    val isConfirmationRequired: Boolean = false,
    val proposalId: String? = null,
    val isSuccess: Boolean = true,
    val errorMessage: String? = null,
    val executedAt: String
)

/**
 * MCP Tool Registry Summary.
 */
data class McpRegistrySummary(
    val totalToolsCount: Int,
    val registeredTools: List<CopilotToolDefinition> = emptyList(),
    val isInternalPricingSecretProtected: Boolean = true, // Critical Invariant: Always true!
    val generatedAt: String
)
