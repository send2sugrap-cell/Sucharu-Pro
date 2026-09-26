package com.sucharu.sucharupro.data.api.model.copilot

import kotlinx.serialization.Serializable

@Serializable
data class ProcessCopilotQueryRequestDto(
    val query: String,
    val customerId: String? = null,
    val language: String = "bn-BD"
)

@Serializable
data class CopilotActionProposalDto(
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

@Serializable
data class BusinessCopilotResponseDto(
    val query: String,
    val responseText: String,
    val toolProposals: List<CopilotActionProposalDto> = emptyList(),
    val detectedIntent: String = "QUERY_INFORMATION",
    val language: String = "bn-BD",
    val isConfirmationPending: Boolean = false,
    val isShadowErpDatabaseCreated: Boolean = false,
    val generatedAt: String
)
