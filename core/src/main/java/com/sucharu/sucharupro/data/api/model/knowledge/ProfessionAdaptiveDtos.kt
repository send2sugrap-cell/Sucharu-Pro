package com.sucharu.sucharupro.data.api.model.knowledge

import kotlinx.serialization.Serializable

@Serializable
data class ProfessionContextDto(
    val professionType: String = "OTHER",
    val industry: String? = null,
    val primaryGoal: String? = null,
    val experienceLevel: String = "MID_LEVEL"
)

@Serializable
data class ProfessionAdaptiveQueryRequestDto(
    val context: ProfessionContextDto,
    val userQuery: String
)

@Serializable
data class ProfessionProfileDto(
    val professionType: String,
    val displayName: String,
    val primaryWorkflow: String,
    val relevantDomains: List<String> = emptyList()
)

@Serializable
data class ProfessionAdaptiveGuidanceDto(
    val professionType: String,
    val displayName: String,
    val primaryWorkflow: String,
    val query: String,
    val guidanceText: String,
    val mappedDomains: List<String> = emptyList(),
    val recommendedActionSteps: List<String> = emptyList(),
    val isHealthcareSafetyBoundActive: Boolean = false,
    val generatedAt: String
)
