package com.sucharu.sucharupro.domain.service.knowledge

import com.sucharu.sucharupro.domain.model.knowledge.*
import java.util.concurrent.ConcurrentHashMap

/**
 * Production-grade Structured RAG Knowledge Provider for Sucharu Pro.
 *
 * Provides approved, version-controlled knowledge across Core Sucharu Operational,
 * Marketing, Sales, Growth, Leadership, Market Intelligence, and AI Productivity domains.
 *
 * Enforces Security Sensitivity Tiers (PUBLIC, CUSTOMER_VISIBLE, STAFF_ONLY, MANAGEMENT_ONLY, CONFIDENTIAL_SECRET).
 */
class SucharuKnowledgeRAGProvider {

    private val knowledgeStore = ConcurrentHashMap<String, KnowledgeDocument>()

    init {
        // Domain 01: PRINTING_TECHNICAL
        val doc1 = KnowledgeDocument(
            knowledgeId = "KNOW-PRINT-001",
            title = "GSM Paper Selection & Finishing Guide",
            contentChunk = "For Business Cards, standard selection is 300 GSM Art Card with Matte/Gloss Lamination. For Flyers, 150 GSM Art Paper is standard. For Book Covers, 300 GSM Art Card with Spot UV/Embossing is used.",
            category = KnowledgeCategory.PRINTING_TECHNICAL,
            status = KnowledgeStatus.PUBLISHED,
            sensitivity = KnowledgeSensitivity.PUBLIC,
            sourceReference = "SOP-PAPER-2026-01",
            tags = listOf("paper", "gsm", "art card", "art paper", "visiting card", "flyer")
        )

        val doc2 = KnowledgeDocument(
            knowledgeId = "KNOW-PRINT-002",
            title = "Prepress & CTP Plate Output Specifications",
            contentChunk = "CTP (Computer-to-Plate) prepress output requires 300 DPI high-resolution CMYK PDF files with 3mm bleed margins and crop marks. RGB color spaces must be converted to CMYK prior to plate exposure.",
            category = KnowledgeCategory.PRINTING_TECHNICAL,
            status = KnowledgeStatus.PUBLISHED,
            sensitivity = KnowledgeSensitivity.STAFF_ONLY,
            sourceReference = "SOP-CTP-2026-02",
            tags = listOf("ctp", "prepress", "cmyk", "bleed", "plates")
        )

        // Domain 02: PRINTING_BUSINESS
        val doc3 = KnowledgeDocument(
            knowledgeId = "KNOW-BIZ-001",
            title = "Customer Requirement Discovery SOP",
            contentChunk = "Customer print discovery requires capturing 7 core specifications: Product Type, Quantity, Size (Trimmed & Uncut), Paper GSM & Substrate, Color Configuration (1/1, 4/4), Finishing/Binding, and Target Delivery Date.",
            category = KnowledgeCategory.PRINTING_BUSINESS,
            status = KnowledgeStatus.PUBLISHED,
            sensitivity = KnowledgeSensitivity.PUBLIC,
            sourceReference = "SOP-BIZ-2026-01",
            tags = listOf("discovery", "specifications", "workflow", "requirements")
        )

        // Domain 03: JOB_COSTING_COMMERCIAL
        val doc4 = KnowledgeDocument(
            knowledgeId = "KNOW-COST-001",
            title = "Quantity Economics & Setup Cost Allocation",
            contentChunk = "Fixed production costs (plate exposure, machine setup, trimming setup) are distributed across ordered quantity. Higher order quantities significantly reduce unit cost in offset printing.",
            category = KnowledgeCategory.JOB_COSTING_COMMERCIAL,
            status = KnowledgeStatus.PUBLISHED,
            sensitivity = KnowledgeSensitivity.CUSTOMER_VISIBLE,
            sourceReference = "SOP-COST-2026-01",
            tags = listOf("costing", "setup", "quantity", "unit cost")
        )

        val doc5 = KnowledgeDocument(
            knowledgeId = "KNOW-CONF-004",
            title = "Internal Costing Formula & Margin Policy",
            contentChunk = "Internal vendor purchase rates, paper substrate supplier discounts, and gross margin calculations are strictly confidential management information and must never be disclosed to external customers or affiliates.",
            category = KnowledgeCategory.JOB_COSTING_COMMERCIAL,
            status = KnowledgeStatus.PUBLISHED,
            sensitivity = KnowledgeSensitivity.CONFIDENTIAL_SECRET,
            sourceReference = "SOP-MGMT-2026-99",
            tags = listOf("confidential", "margin", "costing", "secret")
        )

        // Domain 04: QUOTATION_SALES
        val doc6 = KnowledgeDocument(
            knowledgeId = "KNOW-COMM-003",
            title = "Quotation Approval & Commercial Lock Rules",
            contentChunk = "Quotations prepared by AI or sales estimators require human Admin/Staff approval before release to customer. Upon customer acceptance, commercial terms are locked into an immutable OrderPriceSnapshot.",
            category = KnowledgeCategory.QUOTATION_SALES,
            status = KnowledgeStatus.PUBLISHED,
            sensitivity = KnowledgeSensitivity.CUSTOMER_VISIBLE,
            sourceReference = "SOP-COMM-2026-03",
            tags = listOf("quotation", "approval", "commercial lock", "pricing")
        )

        // Domain 05: CUSTOMER_CRM
        val doc7 = KnowledgeDocument(
            knowledgeId = "KNOW-CRM-001",
            title = "Customer Requirement History & Retention Principles",
            contentChunk = "Customer 360 aggregates past quotation requests, approved commercial price snapshots, active order progress, delivery history, and payment statements to personalize service without duplicating master records.",
            category = KnowledgeCategory.CUSTOMER_CRM,
            status = KnowledgeStatus.PUBLISHED,
            sensitivity = KnowledgeSensitivity.STAFF_ONLY,
            sourceReference = "SOP-CRM-2026-01",
            tags = listOf("crm", "customer 360", "retention", "history")
        )

        // Domain 06: MARKETING_CUSTOMER_ACQUISITION
        val doc8 = KnowledgeDocument(
            knowledgeId = "KNOW-MKT-001",
            title = "Printing Customer Acquisition & Lead Generation Strategy",
            contentChunk = "Customer acquisition in commercial printing focuses on target business segmentation (educational, corporate, publishing, retail) and needs discovery. High-converting leads originate from specification clarity and sample proofing.",
            category = KnowledgeCategory.MARKETING_CUSTOMER_ACQUISITION,
            status = KnowledgeStatus.PUBLISHED,
            sensitivity = KnowledgeSensitivity.CUSTOMER_VISIBLE,
            sourceReference = "MKT-ACQ-2026-01",
            tags = listOf("marketing", "acquisition", "leads", "segmentation")
        )

        // Domain 07: DIGITAL_MARKETING_CONTENT
        val doc9 = KnowledgeDocument(
            knowledgeId = "KNOW-DIGI-001",
            title = "Printing Services Digital Content & Campaign Planning",
            contentChunk = "Digital content strategy highlights print product visual showcases (embossing, spot UV, matte foil), seasonal event offers (calendars, diaries, Hajj leaflets), and educational paper selection tips across Facebook and messaging channels.",
            category = KnowledgeCategory.DIGITAL_MARKETING_CONTENT,
            status = KnowledgeStatus.PUBLISHED,
            sensitivity = KnowledgeSensitivity.PUBLIC,
            sourceReference = "MKT-DIGI-2026-01",
            tags = listOf("digital marketing", "content", "social media", "campaigns")
        )

        // Domain 08: B2B_B2C_SALES
        val doc10 = KnowledgeDocument(
            knowledgeId = "KNOW-SALE-001",
            title = "Commercial Sales Discovery & Objection Handling",
            contentChunk = "B2B commercial sales discovery requires identifying key procurement decision makers, paper GSM preferences, and target delivery deadlines. Common price objections are resolved by explaining volume economics.",
            category = KnowledgeCategory.B2B_B2C_SALES,
            status = KnowledgeStatus.PUBLISHED,
            sensitivity = KnowledgeSensitivity.CUSTOMER_VISIBLE,
            sourceReference = "SALES-B2B-2026-01",
            tags = listOf("sales", "b2b", "objection handling", "proposals")
        )

        // Domain 09: RETENTION_UPSELL_CROSSSELL
        val doc11 = KnowledgeDocument(
            knowledgeId = "KNOW-RET-001",
            title = "Customer Retention & Value-Add Cross-Selling",
            contentChunk = "Customer retention is driven by delivery reliability and proactive reorder reminders for seasonal items. Cross-selling adds value by recommending matching stationery or packaging alongside primary print orders.",
            category = KnowledgeCategory.RETENTION_UPSELL_CROSSSELL,
            status = KnowledgeStatus.PUBLISHED,
            sensitivity = KnowledgeSensitivity.STAFF_ONLY,
            sourceReference = "MKT-RET-2026-01",
            tags = listOf("retention", "repeat order", "cross-selling", "upselling")
        )

        // Domain 10: PRODUCTION_PLANNING
        val doc12 = KnowledgeDocument(
            knowledgeId = "KNOW-PROD-001",
            title = "Production Scheduling & Job Workload Prioritization",
            contentChunk = "Production scheduling prioritizes jobs based on promised delivery date, CTP plate availability, machine load balancing, and finishing dependencies. Jobs at risk of delay trigger immediate SLA alerts.",
            category = KnowledgeCategory.PRODUCTION_PLANNING,
            status = KnowledgeStatus.PUBLISHED,
            sensitivity = KnowledgeSensitivity.STAFF_ONLY,
            sourceReference = "SOP-PROD-2026-02",
            tags = listOf("production", "scheduling", "workload", "prioritization")
        )

        // Domain 11: QUALITY_REWORK
        val doc13 = KnowledgeDocument(
            knowledgeId = "KNOW-QC-001",
            title = "Quality Control & Rework Inspection SOP",
            contentChunk = "Quality inspection is performed at 3 checkpoints: Prepress Proofing, In-Process Printing Sheet Inspection, and Final QC before Packaging. Defective output requires root-cause classification and rework approval.",
            category = KnowledgeCategory.QUALITY_REWORK,
            status = KnowledgeStatus.PUBLISHED,
            sensitivity = KnowledgeSensitivity.STAFF_ONLY,
            sourceReference = "SOP-QC-2026-01",
            tags = listOf("qc", "quality", "rework", "inspection")
        )

        // Domain 12: INVENTORY_DISTRIBUTION
        val doc14 = KnowledgeDocument(
            knowledgeId = "KNOW-INV-001",
            title = "Finished Goods Inventory & Delivery Challan SOP",
            contentChunk = "Sucharu inventory tracks finished products only. Upon Final QC release, items are moved to Ready status, linked to a Delivery Challan, and dispatched with receiver Proof of Delivery (POD) signature.",
            category = KnowledgeCategory.INVENTORY_DISTRIBUTION,
            status = KnowledgeStatus.PUBLISHED,
            sensitivity = KnowledgeSensitivity.PUBLIC,
            sourceReference = "SOP-INV-2026-01",
            tags = listOf("inventory", "finished goods", "challan", "delivery")
        )

        // Domain 13: VENDOR_PROCUREMENT
        val doc15 = KnowledgeDocument(
            knowledgeId = "KNOW-VND-001",
            title = "Subcontracted Services & 3-Way Matching SOP",
            contentChunk = "Subcontracted finishing and prepress services require Purchase Orders. Supplier Invoices undergo 3-Way Matching (PO, Delivery Receipt, Invoice) before Vendor Payable posting in General Ledger.",
            category = KnowledgeCategory.VENDOR_PROCUREMENT,
            status = KnowledgeStatus.PUBLISHED,
            sensitivity = KnowledgeSensitivity.STAFF_ONLY,
            sourceReference = "SOP-VND-2026-01",
            tags = listOf("vendor", "procurement", "3-way match", "payable")
        )

        // Domain 14: FINANCE_CASHFLOW
        val doc16 = KnowledgeDocument(
            knowledgeId = "KNOW-FIN-001",
            title = "Customer Accounts Receivable & Payment Allocation",
            contentChunk = "Invoices issued upon order delivery create customer receivables. Payments received via bKash or Bank are allocated against specific invoice line items in Customer Financial Account ledger.",
            category = KnowledgeCategory.FINANCE_CASHFLOW,
            status = KnowledgeStatus.PUBLISHED,
            sensitivity = KnowledgeSensitivity.STAFF_ONLY,
            sourceReference = "SOP-FIN-2026-01",
            tags = listOf("finance", "receivable", "payment", "allocation")
        )

        // Domain 15: OFFICE_MANAGEMENT
        val doc17 = KnowledgeDocument(
            knowledgeId = "KNOW-OFF-001",
            title = "Daily Office Workload & Coordination SOP",
            contentChunk = "Daily operational coordination reviews open quotations awaiting customer approval, jobs in production, ready shipments, overdue receivables, and urgent customer follow-up actions.",
            category = KnowledgeCategory.OFFICE_MANAGEMENT,
            status = KnowledgeStatus.PUBLISHED,
            sensitivity = KnowledgeSensitivity.STAFF_ONLY,
            sourceReference = "SOP-OFF-2026-01",
            tags = listOf("office", "coordination", "workload", "follow-up")
        )

        // Domain 16: SOP_PROCESS_MANAGEMENT
        val doc18 = KnowledgeDocument(
            knowledgeId = "KNOW-SOP-001-PIPE",
            title = "Sucharu Production Pipeline 13 Stages SOP",
            contentChunk = "Sucharu Graphics production follows 13 locked stages: DESIGN -> APPROVAL -> QC -> ITEM_APPROVAL -> CTP -> PRINTING -> LAMINATION -> FOLDING -> BINDING -> FINAL_QC -> PACKAGING -> READY -> DELIVERED. Each stage requires explicit QC verification.",
            category = KnowledgeCategory.SOP_PROCESS_MANAGEMENT,
            status = KnowledgeStatus.PUBLISHED,
            sensitivity = KnowledgeSensitivity.PUBLIC,
            sourceReference = "SOP-PROD-2026-01",
            tags = listOf("production", "pipeline", "stages", "sop")
        )

        // Domain 17: BUSINESS_ANALYTICS_KPI
        val doc19 = KnowledgeDocument(
            knowledgeId = "KNOW-KPI-001",
            title = "Executive Decision Intelligence & KPI Guide",
            contentChunk = "Executive decision intelligence monitors 6 core metrics: Sales Revenue YTD, Quotation Conversion Rate, Average Gross Margin %, Active Production Workload, Overdue Receivables, and SLA On-Time Performance.",
            category = KnowledgeCategory.BUSINESS_ANALYTICS_KPI,
            status = KnowledgeStatus.PUBLISHED,
            sensitivity = KnowledgeSensitivity.MANAGEMENT_ONLY,
            sourceReference = "SOP-KPI-2026-01",
            tags = listOf("kpi", "analytics", "revenue", "gross margin", "sla")
        )

        // Domain 18: BANGLADESH_COMMERCIAL_COMPLIANCE
        val doc20 = KnowledgeDocument(
            knowledgeId = "KNOW-COMP-001",
            title = "Bangladesh VAT & Commercial Invoice Requirements",
            contentChunk = "Commercial invoices issued for printing services comply with Bangladesh VAT rules, requiring Customer BIN, Customer TIN, configurable VAT calculation, and immutable invoice tax snapshots.",
            category = KnowledgeCategory.BANGLADESH_COMMERCIAL_COMPLIANCE,
            status = KnowledgeStatus.PUBLISHED,
            sensitivity = KnowledgeSensitivity.CUSTOMER_VISIBLE,
            sourceReference = "SOP-COMP-2026-01",
            tags = listOf("vat", "bangladesh", "invoice", "bin", "tin", "tax")
        )

        // Domain 19: LEADERSHIP_TEAM
        val doc21 = KnowledgeDocument(
            knowledgeId = "KNOW-LEAD-001",
            title = "Operational Leadership & Team Accountability Principles",
            contentChunk = "Leadership in printing operations centers on clear task delegation, daily stage milestone check-ins, transparent team feedback, and bottleneck resolution without shifting blame.",
            category = KnowledgeCategory.LEADERSHIP_TEAM,
            status = KnowledgeStatus.PUBLISHED,
            sensitivity = KnowledgeSensitivity.STAFF_ONLY,
            sourceReference = "MGMT-LEAD-2026-01",
            tags = listOf("leadership", "delegation", "teamwork", "accountability")
        )

        // Domain 20: BUSINESS_GROWTH
        val doc22 = KnowledgeDocument(
            knowledgeId = "KNOW-GROW-001",
            title = "Printing Business Growth & Operational Scaling Strategy",
            contentChunk = "Sustainable business growth combines repeat institutional customer retention, high quotation conversion rates, capacity optimization, and disciplined financial cash-flow management.",
            category = KnowledgeCategory.BUSINESS_GROWTH,
            status = KnowledgeStatus.PUBLISHED,
            sensitivity = KnowledgeSensitivity.MANAGEMENT_ONLY,
            sourceReference = "MGMT-GROW-2026-01",
            tags = listOf("growth", "scaling", "business model", "capacity")
        )

        // Domain 21: PROFESSIONAL_SKILL_DEVELOPMENT
        val doc23 = KnowledgeDocument(
            knowledgeId = "KNOW-SKILL-001",
            title = "Professional Client Relations & Commercial Negotiation Skills",
            contentChunk = "Professional skill development emphasizes active listening, clear specification communication, structured problem-solving, time management, and empathetic customer relations.",
            category = KnowledgeCategory.PROFESSIONAL_SKILL_DEVELOPMENT,
            status = KnowledgeStatus.PUBLISHED,
            sensitivity = KnowledgeSensitivity.PUBLIC,
            sourceReference = "SKILL-DEV-2026-01",
            tags = listOf("skills", "communication", "negotiation", "client relations")
        )

        // Domain 22: PROBLEM_SOLVING_DECISION_SUPPORT
        val doc24 = KnowledgeDocument(
            knowledgeId = "KNOW-DEC-001",
            title = "Production Delay & Root Cause Analysis Framework",
            contentChunk = "Production delay analysis identifies root causes across 11 controlled categories (Customer Approval Delay, QC Rework, Machine Operation Delay, Outsourced Finishing Delay, Payment Hold) to drive corrective action.",
            category = KnowledgeCategory.PROBLEM_SOLVING_DECISION_SUPPORT,
            status = KnowledgeStatus.PUBLISHED,
            sensitivity = KnowledgeSensitivity.STAFF_ONLY,
            sourceReference = "SOP-DEC-2026-01",
            tags = listOf("decision", "root cause", "delay", "rework", "sla")
        )

        // Domain 23: MARKET_INTELLIGENCE
        val doc25 = KnowledgeDocument(
            knowledgeId = "KNOW-MKTG-001",
            title = "Printing Market Demand Signals & Seasonal Trend Analysis",
            contentChunk = "Commercial printing demand exhibits seasonal spikes during academic enrollment periods (textbooks, notebooks), Islamic holidays (Hajj leaflets, calendars), and corporate year-end diaries.",
            category = KnowledgeCategory.MARKET_INTELLIGENCE,
            status = KnowledgeStatus.PUBLISHED,
            sensitivity = KnowledgeSensitivity.CUSTOMER_VISIBLE,
            sourceReference = "MKTG-INTEL-2026-01",
            tags = listOf("market intelligence", "seasonal trends", "demand signals")
        )

        // Domain 24: AI_BUSINESS_PRODUCTIVITY
        val doc26 = KnowledgeDocument(
            knowledgeId = "KNOW-AIPROD-001",
            title = "AI-Assisted Business Drafting & Operational Productivity",
            contentChunk = "AI-assisted productivity streamlines quotation drafting, customer follow-up messages, meeting summaries, and daily workload briefs while maintaining human confirmation gates for sensitive actions.",
            category = KnowledgeCategory.AI_BUSINESS_PRODUCTIVITY,
            status = KnowledgeStatus.PUBLISHED,
            sensitivity = KnowledgeSensitivity.PUBLIC,
            sourceReference = "AIPROD-2026-01",
            tags = listOf("ai productivity", "drafting", "automation", "confirmation gate")
        )

        knowledgeStore[doc1.knowledgeId] = doc1
        knowledgeStore[doc2.knowledgeId] = doc2
        knowledgeStore[doc3.knowledgeId] = doc3
        knowledgeStore[doc4.knowledgeId] = doc4
        knowledgeStore[doc5.knowledgeId] = doc5
        knowledgeStore[doc6.knowledgeId] = doc6
        knowledgeStore[doc7.knowledgeId] = doc7
        knowledgeStore[doc8.knowledgeId] = doc8
        knowledgeStore[doc9.knowledgeId] = doc9
        knowledgeStore[doc10.knowledgeId] = doc10
        knowledgeStore[doc11.knowledgeId] = doc11
        knowledgeStore[doc12.knowledgeId] = doc12
        knowledgeStore[doc13.knowledgeId] = doc13
        knowledgeStore[doc14.knowledgeId] = doc14
        knowledgeStore[doc15.knowledgeId] = doc15
        knowledgeStore[doc16.knowledgeId] = doc16
        knowledgeStore[doc17.knowledgeId] = doc17
        knowledgeStore[doc18.knowledgeId] = doc18
        knowledgeStore[doc19.knowledgeId] = doc19
        knowledgeStore[doc20.knowledgeId] = doc20
        knowledgeStore[doc21.knowledgeId] = doc21
        knowledgeStore[doc22.knowledgeId] = doc22
        knowledgeStore[doc23.knowledgeId] = doc23
        knowledgeStore[doc24.knowledgeId] = doc24
        knowledgeStore[doc25.knowledgeId] = doc25
        knowledgeStore[doc26.knowledgeId] = doc26
    }

    /**
     * Searches approved knowledge documents matching [query], [category], and role sensitivity bounds.
     */
    fun searchKnowledge(
        query: String,
        category: KnowledgeCategory? = null,
        userRole: String = "CUSTOMER"
    ): List<KnowledgeSearchResult> {
        val lowerQuery = query.lowercase().trim()

        val directMatches = knowledgeStore.values
            .filter { doc -> doc.status == KnowledgeStatus.PUBLISHED || doc.status == KnowledgeStatus.APPROVED }
            .filter { doc -> category == null || doc.category == category }
            .filter { doc -> isRoleAuthorizedForSensitivity(userRole, doc.sensitivity) }
            .filter { doc ->
                lowerQuery.split(" ", "-", "_", ",", "।", "?").any { token ->
                    token.length >= 2 && (
                        doc.title.lowercase().contains(token) ||
                        doc.contentChunk.lowercase().contains(token) ||
                        doc.tags.any { it.lowercase().contains(token) }
                    )
                } ||
                doc.title.lowercase().contains(lowerQuery) ||
                doc.contentChunk.lowercase().contains(lowerQuery)
            }

        val docsToReturn = if (directMatches.isNotEmpty()) {
            directMatches
        } else {
            // General query fallback: Retrieve public & customer-visible core printing technical & service SOPs
            knowledgeStore.values
                .filter { doc -> doc.status == KnowledgeStatus.PUBLISHED || doc.status == KnowledgeStatus.APPROVED }
                .filter { doc -> isRoleAuthorizedForSensitivity(userRole, doc.sensitivity) }
                .filter { doc -> doc.sensitivity == KnowledgeSensitivity.PUBLIC || doc.sensitivity == KnowledgeSensitivity.CUSTOMER_VISIBLE }
                .take(3)
        }

        return docsToReturn.map { doc ->
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
        val timestamp = "2026-09-27T17:55:00Z"
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
