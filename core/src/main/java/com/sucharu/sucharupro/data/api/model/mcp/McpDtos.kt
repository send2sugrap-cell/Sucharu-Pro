package com.sucharu.sucharupro.data.api.model.mcp

import kotlinx.serialization.Serializable

@Serializable
data class McpToolDefinitionDto(
    val toolId: String,
    val toolName: String,
    val description: String,
    val riskLevel: String = "READ_ONLY",
    val requiredCapability: String
)

@Serializable
data class McpToolExecutionRequestDto(
    val toolName: String,
    val parameters: Map<String, String> = emptyMap(),
    val actorUserId: String? = null
)

@Serializable
data class McpToolExecutionResultDto(
    val toolName: String,
    val status: String = "SUCCESS",
    val dataJson: String,
    val riskLevel: String = "READ_ONLY",
    val isConfirmationRequired: Boolean = false,
    val proposalId: String? = null,
    val isSuccess: Boolean = true,
    val executedAt: String
)

@Serializable
data class McpRegistrySummaryDto(
    val totalToolsCount: Int,
    val registeredTools: List<McpToolDefinitionDto> = emptyList(),
    val isInternalPricingSecretProtected: Boolean = true,
    val generatedAt: String
)
