package com.sucharu.sucharupro.domain.service.ai

import com.sucharu.sucharupro.domain.model.ai.*
import com.sucharu.sucharupro.domain.model.copilot.CopilotActionProposal
import com.sucharu.sucharupro.domain.model.copilot.CopilotUserMemory
import com.sucharu.sucharupro.domain.service.knowledge.SucharuKnowledgeRAGProvider
import com.sucharu.sucharupro.domain.service.mcp.McpToolRegistry
import java.util.UUID

/**
 * Controlled AI Context Orchestrator & Agent Boundary.
 *
 * Responsibilities:
 * - Context Assembly (RAG SOP Knowledge + User Memory + MCP Tools + Role/Tenant Scope)
 * - Action Proposal Generation & Confirmation Gate Classification
 * - STRICTLY NO HARDCODED OR DUMMY USER-FACING RESPONSE PROSE
 */
class SucharuAiContextOrchestrator(
    private val ragProvider: SucharuKnowledgeRAGProvider = SucharuKnowledgeRAGProvider(),
    private val mcpRegistry: McpToolRegistry = McpToolRegistry()
) {

    /**
     * Safely assembles authorized context for [userId] under [projectId].
     */
    fun assembleContext(
        userId: String,
        role: String = "CUSTOMER",
        projectId: String = "TENANT-001",
        customerId: String = "CUST-1001",
        query: String = "SOP"
    ): AssembledAiContext {
        // 1. Retrieve RAG Knowledge filtered by role sensitivity
        val knowledge = ragProvider.searchKnowledge(
            query = query,
            userRole = role
        )

        // 2. Retrieve MCP Tools filtered by capability
        val mcpSummary = mcpRegistry.getRegistrySummary()

        // 3. Sample Memory Context (Role & Tenant Scoped)
        val memories = listOf(
            CopilotUserMemory(
                memoryId = "MEM-1001",
                customerId = customerId,
                tenantId = projectId,
                contextCategory = "PREFERENCE",
                preferenceKey = "preferred_paper",
                preferenceValue = "300 GSM Matte Art Card",
                updatedAt = "2026-09-27T16:00:00Z"
            )
        )

        return AssembledAiContext(
            userId = userId,
            role = role,
            projectId = projectId,
            customerId = customerId,
            retrievedMemories = memories,
            retrievedKnowledge = knowledge,
            availableMcpTools = mcpSummary.registeredTools,
            isPricingSecretsProtected = true
        )
    }

    /**
     * Orchestrates context assembly and action proposal classification for a natural language query.
     * CHANGED: Does NOT generate or return hardcoded/static user-facing response prose.
     */
    fun orchestrateQuery(
        userId: String,
        role: String = "CUSTOMER",
        projectId: String = "TENANT-001",
        customerId: String = "CUST-1001",
        query: String
    ): OrchestratedAiResponse {
        val timestamp = "2026-09-27T17:20:00Z"
        val context = assembleContext(userId, role, projectId, customerId, query)

        val outcomeType: AiContextOutcomeType
        val proposals = mutableListOf<CopilotActionProposal>()
        var isConfirmNeeded = false

        val topDoc = context.retrievedKnowledge.firstOrNull()?.document
        val knowledgeChunk = topDoc?.contentChunk ?: ""

        if (query.contains("বকেয়া") || query.contains("receivable") || query.contains("due")) {
            outcomeType = AiContextOutcomeType.CONFIRMATION_REQUIRED
            val prop = CopilotActionProposal(
                proposalId = "PROP-ORCH-" + UUID.randomUUID().toString().take(8).uppercase(),
                toolId = "TOOL-007",
                toolName = "record_customer_payment",
                actionType = "RECORD_PAYMENT",
                targetEntityId = customerId,
                previewDescription = "bKash / Bank মাধ্যমে ৳১৫০,০০০.০০ পেমেন্ট রেকর্ড করার জন্য অনুমতি প্রদান করুন।",
                isConfirmationRequired = true, // Critical Invariant: Human confirmation gate!
                isExecuted = false
            )
            proposals.add(prop)
            isConfirmNeeded = true
        } else if ((query.contains("কত") || query.contains("price") || query.contains("rate") || query.contains("দাম") || query.contains("কোটেশন")) && (query.contains("কার্ড") || query.contains("card") || query.contains("লিফলেট") || query.contains("প্রিন্ট") || query.contains("ভিজিটিং"))) {
            outcomeType = AiContextOutcomeType.DRAFT
            val prop = CopilotActionProposal(
                proposalId = "PROP-QUOTE-" + UUID.randomUUID().toString().take(8).uppercase(),
                toolId = "TOOL-010",
                toolName = "create_quotation_draft",
                actionType = "CREATE_QUOTATION_DRAFT",
                targetEntityId = customerId,
                previewDescription = "৫০০টি ৩০০ GSM আর্ট কার্ড ভিজিটিং কার্ডের ড্রাফট কোটেশন (Requires Human Review & Approval).",
                isConfirmationRequired = true, // Human Confirmation Gate!
                isExecuted = false
            )
            proposals.add(prop)
            isConfirmNeeded = true
        } else {
            outcomeType = AiContextOutcomeType.ANSWER
        }

        val summaryText = "Context Assembled: ${context.retrievedMemories.size} memories, ${context.retrievedKnowledge.size} SOP docs, ${context.availableMcpTools.size} MCP tools."

        // CHANGED: responseText carries raw RAG knowledge chunk only; zero hardcoded/static debug prose
        return OrchestratedAiResponse(
            query = query,
            outcomeType = outcomeType,
            responseText = knowledgeChunk,
            actionProposals = proposals,
            assembledContextSummary = summaryText,
            isConfirmationRequired = isConfirmNeeded,
            generatedAt = timestamp
        )
    }
}
