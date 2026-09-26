package com.sucharu.sucharupro.domain.model.copilot

/**
 * BI-12 Tool Risk & Confirmation Hierarchy.
 */
enum class CopilotToolRiskLevel {
    READ_ONLY,
    PREPARE_ACTION,
    CONFIRM_REQUIRED,
    HIGH_RISK
}

/**
 * Registered Copilot Business Tool Definition.
 */
data class CopilotToolDefinition(
    val toolId: String,
    val toolName: String,
    val description: String,
    val riskLevel: CopilotToolRiskLevel = CopilotToolRiskLevel.READ_ONLY,
    val requiredCapability: String
)

/**
 * Actionable Tool Execution Proposal with Confirmation Gate.
 */
data class CopilotActionProposal(
    val proposalId: String,
    val toolId: String,
    val toolName: String,
    val actionType: String,
    val targetEntityId: String? = null,
    val previewDescription: String,
    val isConfirmationRequired: Boolean = true,
    val isExecuted: Boolean = false,
    val isConfirmedByHuman: Boolean = false
)

/**
 * Contextual Customer & Staff Preference Memory Record.
 */
data class CopilotUserMemory(
    val memoryId: String,
    val customerId: String,
    val tenantId: String,
    val contextCategory: String = "PREFERENCE",
    val preferenceKey: String,
    val preferenceValue: String,
    val updatedAt: String
)

/**
 * BI-12 Master Business Copilot Response Model.
 */
data class BusinessCopilotResponse(
    val query: String,
    val responseText: String,
    val toolProposals: List<CopilotActionProposal> = emptyList(),
    val detectedIntent: String = "QUERY_INFORMATION",
    val language: String = "bn-BD",
    val isConfirmationPending: Boolean = false,
    val isShadowErpDatabaseCreated: Boolean = false, // Critical Invariant: Always false!
    val generatedAt: String
)
