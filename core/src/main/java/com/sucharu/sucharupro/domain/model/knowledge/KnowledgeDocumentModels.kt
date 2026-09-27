package com.sucharu.sucharupro.domain.model.knowledge

/**
 * Knowledge Domain Categories.
 */
enum class KnowledgeCategory {
    PRINTING_SPECIFICATIONS,
    PRINTING_BUSINESS_OPERATIONS,
    OFFICE_SOP,
    COMMERCIAL_RULES,
    COMPLIANCE_VAT_RULES,
    MARKETING_STRATEGY,
    GENERAL_PRINTING
}

/**
 * Knowledge Document Lifecycle Status.
 */
enum class KnowledgeStatus {
    DRAFT,
    APPROVED,
    PUBLISHED,
    ARCHIVED
}

/**
 * Knowledge Document Security Sensitivity Tiers.
 */
enum class KnowledgeSensitivity {
    PUBLIC,
    CUSTOMER_VISIBLE,
    STAFF_ONLY,
    MANAGEMENT_ONLY,
    CONFIDENTIAL_SECRET
}

/**
 * Structured Knowledge Document Entity.
 */
data class KnowledgeDocument(
    val knowledgeId: String,
    val title: String,
    val contentChunk: String,
    val category: KnowledgeCategory = KnowledgeCategory.GENERAL_PRINTING,
    val status: KnowledgeStatus = KnowledgeStatus.PUBLISHED,
    val sensitivity: KnowledgeSensitivity = KnowledgeSensitivity.PUBLIC,
    val sourceReference: String? = null,
    val version: String = "1.0.0",
    val tags: List<String> = emptyList(),
    val effectiveFrom: String? = null,
    val effectiveUntil: String? = null
) {
    init {
        require(knowledgeId.isNotBlank()) { "Knowledge ID cannot be blank." }
        require(title.isNotBlank()) { "Title cannot be blank." }
        require(contentChunk.isNotBlank()) { "Content chunk cannot be blank." }
    }
}

/**
 * Knowledge Search Result item with relevance score.
 */
data class KnowledgeSearchResult(
    val document: KnowledgeDocument,
    val relevanceScore: Double = 1.0,
    val matchSnippet: String
)

/**
 * Master RAG Knowledge Summary Model.
 */
data class KnowledgeRAGSummary(
    val totalPublishedDocumentsCount: Int,
    val categoriesSummary: Map<String, Int> = emptyMap(),
    val isCanonicalErpBoundaryPreserved: Boolean = true, // Critical Invariant: Always true!
    val generatedAt: String
)
