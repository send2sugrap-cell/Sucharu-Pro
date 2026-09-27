package com.sucharu.sucharupro.data.api.model.ai

import kotlinx.serialization.Serializable

@Serializable
data class OrchestrateQueryRequestDto(
    val query: String,
    val userId: String? = null,
    val customerId: String? = null,
    val projectId: String? = null
)

@Serializable
data class AssembledAiContextDto(
    val userId: String,
    val role: String,
    val projectId: String,
    val customerId: String,
    val memoriesCount: Int = 0,
    val knowledgeCount: Int = 0,
    val toolsCount: Int = 0,
    val isPricingSecretsProtected: Boolean = true
)

@Serializable
data class OrchestratedAiResponseDto(
    val query: String,
    val outcomeType: String = "ANSWER",
    val responseText: String,
    val assembledContextSummary: String,
    val isConfirmationRequired: Boolean = false,
    val generatedAt: String
)
