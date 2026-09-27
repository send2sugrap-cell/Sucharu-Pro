package com.sucharu.sucharupro.data.api.model.knowledge

import kotlinx.serialization.Serializable

@Serializable
data class KnowledgeDocumentDto(
    val knowledgeId: String,
    val title: String,
    val contentChunk: String,
    val category: String = "GENERAL_PRINTING",
    val status: String = "PUBLISHED",
    val sensitivity: String = "PUBLIC",
    val sourceReference: String? = null,
    val version: String = "1.0.0"
)

@Serializable
data class KnowledgeSearchResultDto(
    val document: KnowledgeDocumentDto,
    val relevanceScore: Double = 1.0,
    val matchSnippet: String
)

@Serializable
data class KnowledgeRAGSummaryDto(
    val totalPublishedDocumentsCount: Int,
    val categoriesSummary: Map<String, Int> = emptyMap(),
    val isCanonicalErpBoundaryPreserved: Boolean = true,
    val generatedAt: String
)
