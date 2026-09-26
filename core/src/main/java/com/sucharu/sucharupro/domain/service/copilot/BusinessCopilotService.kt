package com.sucharu.sucharupro.domain.service.copilot

import com.sucharu.sucharupro.domain.model.copilot.*
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * BI-12 Domain Service for AI + n8n Business Copilot & Tool Confirmation Gate.
 */
class BusinessCopilotService {

    private val proposalStore = ConcurrentHashMap<String, CopilotActionProposal>()
    private val memoryStore = ConcurrentHashMap<String, CopilotUserMemory>()

    /**
     * Processes a natural language query in Bangla/English and generates authorized tool proposals with confirmation gates.
     */
    fun processQuery(
        query: String,
        customerId: String = "CUST-1001",
        actorRole: String = "CUSTOMER"
    ): BusinessCopilotResponse {
        val timestamp = "2026-09-26T21:40:00Z"

        val responseText: String
        val proposals = mutableListOf<CopilotActionProposal>()
        var isPending = false

        if (query.contains("বকেয়া") || query.contains("receivable") || query.contains("due")) {
            responseText = "আপনার বর্তমান বকেয়া অ্যাকাউন্ট বিবরণী অনুযায়ী: মোট বকেয়া ৳১৫০,০০০.০০ (ইনভয়েস #INV-002, ৩২ দিন অতিবাহিত)।"
            val proposalId = "PROP-" + UUID.randomUUID().toString().take(8).uppercase()
            val prop = CopilotActionProposal(
                proposalId = proposalId,
                toolId = "TOOL-RECORD-PAYMENT",
                toolName = "record_customer_payment",
                actionType = "RECORD_PAYMENT",
                targetEntityId = "CUST-1001",
                previewDescription = "bKash / Bank মাধ্যমে ৳১৫০,০০০.০০ পেমেন্ট রেকর্ড করার অনুরোধ সাবমিট করুন।",
                isConfirmationRequired = true, // Critical Invariant: Confirmation gate active!
                isExecuted = false
            )
            proposals.add(prop)
            proposalStore[proposalId] = prop
            isPending = true
        } else if (query.contains("অর্ডার") || query.contains("order")) {
            responseText = "আপনার সাম্প্রতিক অ্যাক্টিভ অর্ডার #ORD-1001 (১,০০০ পিস মেট ফিনিশ ভিজিটিং কার্ড)। বর্তমান উৎপাদন ধাপ: PRINTING।"
        } else {
            responseText = "সুচারু প্রো বিজনেস ক Copilot আপনাকে সাহায্য করতে প্রস্তুত। আপনি বকেয়া, অর্ডার স্ট্যাটাস, অথবা অফার সম্পর্কে জানতে পারেন।"
        }

        return BusinessCopilotResponse(
            query = query,
            responseText = responseText,
            toolProposals = proposals,
            detectedIntent = if (query.contains("বকেয়া")) "QUERY_RECEIVABLES" else "GENERAL_ASSISTANCE",
            language = if (query.contains("বকেয়া") || query.contains("অর্ডার")) "bn-BD" else "en-US",
            isConfirmationPending = isPending,
            isShadowErpDatabaseCreated = false, // Critical Invariant: Always false!
            generatedAt = timestamp
        )
    }

    /**
     * Confirms and executes an action proposal via the Confirmation Gate.
     * (CRITICAL INVARIANT: High-risk/mutation actions require explicit human confirmation!)
     */
    fun confirmAndExecuteProposal(proposalId: String, actorId: String): CopilotActionProposal {
        val prop = proposalStore[proposalId] ?: throw IllegalArgumentException("Proposal not found: $proposalId")

        val executed = prop.copy(
            isConfirmedByHuman = true,
            isExecuted = true
        )

        proposalStore[proposalId] = executed
        return executed
    }
}
