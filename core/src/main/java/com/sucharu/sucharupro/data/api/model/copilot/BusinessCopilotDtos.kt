package com.sucharu.sucharupro.data.api.model.copilot

import com.google.gson.annotations.SerializedName
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ProcessCopilotQueryRequestDto(
    @SerializedName("prompt")
    @SerialName("prompt")
    val prompt: String,

    @SerializedName("query")
    @SerialName("query")
    val query: String? = prompt,

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
    @SerializedName("reply")
    @SerialName("reply")
    val reply: String? = null,

    @SerializedName("answer")
    @SerialName("answer")
    val answer: String? = null,

    @SerializedName("responseText")
    @SerialName("responseText")
    val rawResponseText: String? = null,

    val query: String = "",
    val toolProposals: List<CopilotActionProposalDto> = emptyList(),
    val detectedIntent: String = "QUERY_INFORMATION",
    val language: String = "bn-BD",
    val isConfirmationPending: Boolean = false,
    val isShadowErpDatabaseCreated: Boolean = false,
    val generatedAt: String = ""
) {
    val responseText: String
        get() = reply?.takeIf { it.isNotBlank() }
            ?: answer?.takeIf { it.isNotBlank() }
            ?: rawResponseText?.takeIf { it.isNotBlank() }
            ?: ""
}

