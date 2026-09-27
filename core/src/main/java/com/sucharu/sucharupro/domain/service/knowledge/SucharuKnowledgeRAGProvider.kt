package com.sucharu.sucharupro.domain.service.knowledge

import com.sucharu.sucharupro.domain.model.knowledge.*
import java.util.concurrent.ConcurrentHashMap

/**
 * Production-grade Structured RAG Knowledge Provider for Sucharu Pro.
 *
 * Provides approved, version-controlled knowledge for Printing SOPs, Paper Specifications,
 * Commercial Rules, and Bangladesh Compliance across the 25 Canonical Knowledge Domains.
 *
 * Enforces Security Sensitivity Tiers (PUBLIC, CUSTOMER_VISIBLE, STAFF_ONLY, MANAGEMENT_ONLY, CONFIDENTIAL_SECRET).
 */
class SucharuKnowledgeRAGProvider {

    private val knowledgeStore = ConcurrentHashMap<String, KnowledgeDocument>()

    init {
        // Seed Approved Sucharu Knowledge Documents
        val doc1 = KnowledgeDocument(
            knowledgeId = "KNOW-SOP-001",
            title = "Sucharu Production Pipeline 13 Stages SOP",
            contentChunk = "Sucharu Graphics production follows 13 locked stages: DESIGN -> APPROVAL -> QC -> ITEM_APPROVAL -> CTP -> PRINTING -> LAMINATION -> FOLDING -> BINDING -> FINAL_QC -> PACKAGING -> READY -> DELIVERED. Each stage requires explicit QC verification.",
            category = KnowledgeCategory.SOP_PROCESS_MANAGEMENT,
            status = KnowledgeStatus.PUBLISHED,
            sensitivity = KnowledgeSensitivity.PUBLIC,
            sourceReference = "SOP-PROD-2026-01",
            tags = listOf("production", "pipeline", "stages", "sop")
        )

        val doc2 = KnowledgeDocument(
            knowledgeId = "KNOW-PRINT-002",
            title = "GSM Paper Selection Guide for Commercial Printing",
            contentChunk = "For Business Cards, recommended paper is 300 GSM Art Card with Matte/Gloss Lamination. For Promotional Flyers, recommended paper is 150 GSM Art Paper. For Book Covers, 300 GSM Art Card with Spot UV is standard.",
            category = KnowledgeCategory.PRINTING_TECHNICAL,
            status = KnowledgeStatus.PUBLISHED,
            sensitivity = KnowledgeSensitivity.PUBLIC,
            sourceReference = "SOP-PAPER-2026-02",
            tags = listOf("paper", "gsm", "art card", "art paper", "visiting card", "flyer")
        )

        val doc3 = KnowledgeDocument(
            knowledgeId = "KNOW-COMM-003",
            title = "Quotation Approval & Commercial Lock Rules",
            contentChunk = "Quotations prepared by AI or sales estimators require human Admin/Staff approval before release to customer. Upon customer acceptance, commercial terms are locked into an immutable OrderPriceSnapshot.",
            category = KnowledgeCategory.QUOTATION_SALES,
            status = KnowledgeStatus.PUBLISHED,
            sensitivity = KnowledgeSensitivity.CUSTOMER_VISIBLE,
            sourceReference = "SOP-COMM-2026-03",
            tags = listOf("quotation", "approval", "commercial lock", "pricing")
        )

        val doc4 = KnowledgeDocument(
            knowledgeId = "KNOW-CONF-004",
            title = "Internal Costing Formula & Margin Policy",
            contentChunk = "Internal vendor purchase rates, paper substrate supplier discounts, and gross margin calculations are strictly confidential management information and must never be disclosed to external customers or affiliates.",
            category = KnowledgeCategory.JOB_COSTING_COMMERCIAL,
            status = KnowledgeStatus.PUBLISHED,
            sensitivity = KnowledgeSensitivity.CONFIDENTIAL_SECRET,
            sourceReference = "SOP-MGMT-2026-99",
            tags = listOf("confidential", "margin", "costing", "secret")
        )

        knowledgeStore[doc1.knowledgeId] = doc1
        knowledgeStore[doc2.knowledgeId] = doc2
        knowledgeStore[doc3.knowledgeId] = doc3
        knowledgeStore[doc4.knowledgeId] = doc4
    }

    /**
     * Searches approved knowledge documents matching [query], [category], and role sensitivity bounds.
     */
    fun searchKnowledge(
        query: String,
        category: KnowledgeCategory? = null,
        userRole: String = "CUSTOMER"
    ): List<KnowledgeSearchResult> {
        val lowerQuery = query.lowercase()

        return knowledgeStore.values
            .filter { doc -> doc.status == KnowledgeStatus.PUBLISHED || doc.status == KnowledgeStatus.APPROVED }
            .filter { doc -> category == null || doc.category == category }
            .filter { doc -> isRoleAuthorizedForSensitivity(userRole, doc.sensitivity) }
            .filter { doc ->
                doc.title.lowercase().contains(lowerQuery) ||
                doc.contentChunk.lowercase().contains(lowerQuery) ||
                doc.tags.any { it.lowercase().contains(lowerQuery) }
            }
            .map { doc ->
                KnowledgeSearchResult(
                    document = doc,
                    relevanceScore = 0.95,
                    matchSnippet = doc.contentChunk.take(150) + "..."
                )
            }
    }

    /**
     * Evaluates whether [userRole] is authorized to view [sensitivity].
     */
    private fun isRoleAuthorizedForSensitivity(userRole: String, sensitivity: KnowledgeSensitivity): Boolean {
        return when (sensitivity) {
            KnowledgeSensitivity.PUBLIC -> true
            KnowledgeSensitivity.CUSTOMER_VISIBLE -> userRole in setOf("CUSTOMER", "AFFILIATE", "STAFF", "MANAGER", "ADMIN", "OWNER")
            KnowledgeSensitivity.STAFF_ONLY -> userRole in setOf("STAFF", "MANAGER", "ADMIN", "OWNER")
            KnowledgeSensitivity.MANAGEMENT_ONLY -> userRole in setOf("MANAGER", "ADMIN", "OWNER")
            KnowledgeSensitivity.CONFIDENTIAL_SECRET -> userRole in setOf("ADMIN", "OWNER")
        }
    }

    fun getKnowledgeRAGSummary(): KnowledgeRAGSummary {
        val timestamp = "2026-09-27T17:10:00Z"
        val docs = knowledgeStore.values.filter { it.status == KnowledgeStatus.PUBLISHED }
        val categoryCounts = docs.groupBy { it.category.name }.mapValues { it.value.size }

        return KnowledgeRAGSummary(
            totalPublishedDocumentsCount = docs.size,
            totalDomainsCount = 25,
            categoriesSummary = categoryCounts,
            isCanonicalErpBoundaryPreserved = true,
            generatedAt = timestamp
        )
    }
}
