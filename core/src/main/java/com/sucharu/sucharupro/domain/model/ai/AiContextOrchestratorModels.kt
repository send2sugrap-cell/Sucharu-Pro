package com.sucharu.sucharupro.domain.model.ai

import com.sucharu.sucharupro.domain.model.copilot.CopilotActionProposal
import com.sucharu.sucharupro.domain.model.copilot.CopilotToolDefinition
import com.sucharu.sucharupro.domain.model.copilot.CopilotUserMemory
import com.sucharu.sucharupro.domain.model.knowledge.KnowledgeSearchResult

/**
 * Typed AI Response Outcome Categories.
 */
enum class AiContextOutcomeType {
    ANSWER,
    DRAFT,
    ACTION_PROPOSAL,
    CONFIRMATION_REQUIRED,
    DENIED,
    INSUFFICIENT_AUTHORITY,
    INSUFFICIENT_DATA
}

/**
 * Safely Assembled Context Model for AI Agent Reasoning.
 */
data class AssembledAiContext(
    val userId: String,
    val role: String,
    val projectId: String,
    val customerId: String,
    val retrievedMemories: List<CopilotUserMemory> = emptyList(),
    val retrievedKnowledge: List<KnowledgeSearchResult> = emptyList(),
    val availableMcpTools: List<CopilotToolDefinition> = emptyList(),
    val isPricingSecretsProtected: Boolean = true // Critical Invariant: Always true!
) {
    init {
        require(userId.isNotBlank()) { "User ID cannot be blank." }
        require(role.isNotBlank()) { "Role cannot be blank." }
        require(projectId.isNotBlank()) { "Project ID cannot be blank." }
    }
}

/**
 * Orchestrated AI Response Model.
 */
data class OrchestratedAiResponse(
    val query: String,
    val outcomeType: AiContextOutcomeType = AiContextOutcomeType.ANSWER,
    val responseText: String,
    val actionProposals: List<CopilotActionProposal> = emptyList(),
    val assembledContextSummary: String,
    val isConfirmationRequired: Boolean = false,
    val generatedAt: String
)
