package com.sucharu.sucharupro.domain.model.knowledge

/**
 * Locked Canonical 25-Domain Sucharu AI Knowledge Taxonomy (Prompt 06-A).
 */
enum class KnowledgeCategory {
    PRINTING_TECHNICAL,
    PRINTING_BUSINESS,
    JOB_COSTING_COMMERCIAL,
    QUOTATION_SALES,
    CUSTOMER_CRM,
    MARKETING_CUSTOMER_ACQUISITION,
    DIGITAL_MARKETING_CONTENT,
    B2B_B2C_SALES,
    RETENTION_UPSELL_CROSSSELL,
    PRODUCTION_PLANNING,
    QUALITY_REWORK,
    INVENTORY_DISTRIBUTION,
    VENDOR_PROCUREMENT,
    FINANCE_CASHFLOW,
    OFFICE_MANAGEMENT,
    SOP_PROCESS_MANAGEMENT,
    BUSINESS_ANALYTICS_KPI,
    BANGLADESH_COMMERCIAL_COMPLIANCE,
    LEADERSHIP_TEAM,
    BUSINESS_GROWTH,
    PROFESSIONAL_SKILL_DEVELOPMENT,
    PROBLEM_SOLVING_DECISION_SUPPORT,
    MARKET_INTELLIGENCE,
    AI_BUSINESS_PRODUCTIVITY,
    CROSS_INDUSTRY_PROFESSION
}

/**
 * Metadata descriptor for each canonical Knowledge Domain.
 */
data class KnowledgeCategoryInfo(
    val domainNumber: Int,
    val code: KnowledgeCategory,
    val displayName: String,
    val description: String
)

/**
 * Registry of all 25 canonical Sucharu AI Knowledge Domains.
 */
object KnowledgeCategoryRegistry {
    val ALL_25_DOMAINS: List<KnowledgeCategoryInfo> = listOf(
        KnowledgeCategoryInfo(1, KnowledgeCategory.PRINTING_TECHNICAL, "01. Printing Technical Knowledge", "Paper, GSM, prepress, CTP, offset/digital printing, finishing & binding"),
        KnowledgeCategoryInfo(2, KnowledgeCategory.PRINTING_BUSINESS, "02. Printing Business Knowledge", "Commercial printing operations, service models, customer workflow"),
        KnowledgeCategoryInfo(3, KnowledgeCategory.JOB_COSTING_COMMERCIAL, "03. Job Costing & Commercial Knowledge", "Production costing concepts, margins, commercial calculation principles"),
        KnowledgeCategoryInfo(4, KnowledgeCategory.QUOTATION_SALES, "04. Quotation & Sales Knowledge", "Specification extraction, quotation preparation, sales communications"),
        KnowledgeCategoryInfo(5, KnowledgeCategory.CUSTOMER_CRM, "05. Customer Relationship / CRM Knowledge", "Customer lifecycle, needs discovery, customer history & retention"),
        KnowledgeCategoryInfo(6, KnowledgeCategory.MARKETING_CUSTOMER_ACQUISITION, "06. Marketing & Customer Acquisition", "Lead generation, B2B/B2C acquisition, promotional campaigns"),
        KnowledgeCategoryInfo(7, KnowledgeCategory.DIGITAL_MARKETING_CONTENT, "07. Digital Marketing & Content Strategy", "Social media strategy, copywriting, campaign messaging"),
        KnowledgeCategoryInfo(8, KnowledgeCategory.B2B_B2C_SALES, "08. B2B/B2C Sales", "Sales prospecting, qualification, discovery, proposal & closing"),
        KnowledgeCategoryInfo(9, KnowledgeCategory.RETENTION_UPSELL_CROSSSELL, "09. Repeat Order / Retention / Upselling / Cross-selling", "Customer retention, repeat orders, cross-selling & upselling"),
        KnowledgeCategoryInfo(10, KnowledgeCategory.PRODUCTION_PLANNING, "10. Production Planning & Workflow", "Scheduling, 13 production stages, workload & delivery planning"),
        KnowledgeCategoryInfo(11, KnowledgeCategory.QUALITY_REWORK, "11. Quality Control & Rework Management", "QC inspection, defect classification, rework & proof checking"),
        KnowledgeCategoryInfo(12, KnowledgeCategory.INVENTORY_DISTRIBUTION, "12. Inventory & Distribution", "Finished-product stock, delivery challans, shipment & dispatch"),
        KnowledgeCategoryInfo(13, KnowledgeCategory.VENDOR_PROCUREMENT, "13. Vendor & Procurement", "Vendor selection, purchase orders, outsourced services & payables"),
        KnowledgeCategoryInfo(14, KnowledgeCategory.FINANCE_CASHFLOW, "14. Finance & Cash-flow Management", "Receivables, payables, cash flow, payment allocation & invoicing"),
        KnowledgeCategoryInfo(15, KnowledgeCategory.OFFICE_MANAGEMENT, "15. Office Management", "Task management, daily planning, customer follow-up & coordination"),
        KnowledgeCategoryInfo(16, KnowledgeCategory.SOP_PROCESS_MANAGEMENT, "16. SOP & Process Management", "Standard Operating Procedures, workflow controls & process compliance"),
        KnowledgeCategoryInfo(17, KnowledgeCategory.BUSINESS_ANALYTICS_KPI, "17. Business Analytics & KPI", "Executive KPIs, sales trends, operational analytics & decision support"),
        KnowledgeCategoryInfo(18, KnowledgeCategory.BANGLADESH_COMMERCIAL_COMPLIANCE, "18. Bangladesh Commercial / Compliance Knowledge", "Bangladesh VAT, invoices, BIN/TIN identification & compliance"),
        KnowledgeCategoryInfo(19, KnowledgeCategory.LEADERSHIP_TEAM, "19. Leadership & Team Management", "Delegation, team communication, accountability & productivity"),
        KnowledgeCategoryInfo(20, KnowledgeCategory.BUSINESS_GROWTH, "20. Business Growth Strategy", "Positioning, expansion, operational scaling & strategic planning"),
        KnowledgeCategoryInfo(21, KnowledgeCategory.PROFESSIONAL_SKILL_DEVELOPMENT, "21. Professional Skill Development", "Communication, negotiation, time management & learning"),
        KnowledgeCategoryInfo(22, KnowledgeCategory.PROBLEM_SOLVING_DECISION_SUPPORT, "22. Problem-solving & Decision-support Knowledge", "Root-cause analysis, decision frameworks & risk assessment"),
        KnowledgeCategoryInfo(23, KnowledgeCategory.MARKET_INTELLIGENCE, "23. Market Intelligence", "Market trends, industry developments & competitor observation"),
        KnowledgeCategoryInfo(24, KnowledgeCategory.AI_BUSINESS_PRODUCTIVITY, "24. AI-assisted Business Productivity", "AI-assisted drafting, task organization & workflow automation"),
        KnowledgeCategoryInfo(25, KnowledgeCategory.CROSS_INDUSTRY_PROFESSION, "25. Cross-Industry / Profession-Specific Knowledge", "Adaptive guidance for Teachers, Doctors, Farmers, Retailers, Freelancers, etc.")
    )
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
    val category: KnowledgeCategory = KnowledgeCategory.PRINTING_TECHNICAL,
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
    val totalDomainsCount: Int = 25,
    val categoriesSummary: Map<String, Int> = emptyMap(),
    val isCanonicalErpBoundaryPreserved: Boolean = true, // Critical Invariant: Always true!
    val generatedAt: String
)
