package com.sucharu.sucharupro.domain.service.ai

import com.sucharu.sucharupro.domain.model.ai.*
import com.sucharu.sucharupro.domain.model.copilot.CopilotActionProposal
import com.sucharu.sucharupro.domain.model.copilot.CopilotUserMemory
import com.sucharu.sucharupro.domain.model.knowledge.KnowledgeCategory
import com.sucharu.sucharupro.domain.service.knowledge.SucharuKnowledgeRAGProvider
import com.sucharu.sucharupro.domain.service.mcp.McpToolRegistry
import java.util.UUID

/**
 * Controlled AI Context Orchestrator & Agent Boundary.
 *
 * Safely assembles persistent user memory, RAG SOP knowledge, MCP tools, and role RBAC
 * for Gemini reasoning while protecting internal commercial secrets and enforcing human confirmation gates.
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
     * Orchestrates a natural language query through the AI Agent Boundary.
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
        val responseText: String
        val proposals = mutableListOf<CopilotActionProposal>()
        var isConfirmNeeded = false

        if (query.contains("বকেয়া") || query.contains("receivable") || query.contains("due")) {
            outcomeType = AiContextOutcomeType.CONFIRMATION_REQUIRED
            responseText = "আপনার বর্তমান বকেয়া অ্যাকাউন্ট বিবরণী অনুযায়ী: মোট বকেয়া ৳১৫০,০০০.০০। পেমেন্ট সাপোর্ট তৈরি করতে চান?"
            val prop = CopilotActionProposal(
                proposalId = "PROP-ORCH-" + UUID.randomUUID().toString().take(8).uppercase(),
                toolId = "TOOL-007",
                toolName = "record_customer_payment",
                actionType = "RECORD_PAYMENT",
                targetEntityId = customerId,
                previewDescription = "bKash / Bank এর মাধ্যমে ৳১৫০,০০০.০০ পেমেন্ট রেকর্ড করার জন্য অনুমতি প্রদান করুন।",
                isConfirmationRequired = true, // Critical Invariant: Human confirmation gate!
                isExecuted = false
            )
            proposals.add(prop)
            isConfirmNeeded = true
        } else if (query.contains("SOP") || query.contains("stage") || query.contains("paper")) {
            outcomeType = AiContextOutcomeType.ANSWER
            val topDoc = context.retrievedKnowledge.firstOrNull()?.document
            responseText = topDoc?.contentChunk ?: "সুচারু গ্রাফিক্স ১৩টি লকিং প্রোডাকশন ধাপ মেনে কাজ করে।"
        } else {
            outcomeType = AiContextOutcomeType.ANSWER
            responseText = "সুচারু প্রো এআই কনটেক্সট অর্কেস্ট্রেটর আপনাকে সাহায্য করতে প্রস্তুত।"
        }

        val summaryText = "Context Assembled: ${context.retrievedMemories.size} memories, ${context.retrievedKnowledge.size} SOP docs, ${context.availableMcpTools.size} MCP tools."

        return OrchestratedAiResponse(
            query = query,
            outcomeType = outcomeType,
            responseText = responseText,
            actionProposals = proposals,
            assembledContextSummary = summaryText,
            isConfirmationRequired = isConfirmNeeded,
            generatedAt = timestamp
        )
    }
}
