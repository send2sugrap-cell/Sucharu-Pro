package com.sucharu.sucharupro.domain.service.knowledge

import com.sucharu.sucharupro.domain.model.knowledge.*
import java.util.concurrent.ConcurrentHashMap

/**
 * Production-grade Service for Profession-Adaptive AI Intelligence.
 *
 * Adapts knowledge retrieval, workflow guidance, and action steps based on the user's
 * authorized profession and work context using ONE unified Sucharu AI Agent boundary.
 *
 * Enforces Healthcare Safety Boundaries (disclaiming clinical medical advice for healthcare roles).
 */
class ProfessionAdaptiveIntelligenceService(
    private val ragProvider: SucharuKnowledgeRAGProvider = SucharuKnowledgeRAGProvider()
) {

    private val profileRegistry = ConcurrentHashMap<ProfessionType, ProfessionProfile>()

    init {
        // Register Canonical 9 Profession Profiles
        registerProfile(
            ProfessionProfile(
                professionType = ProfessionType.TEACHER,
                displayName = "Teacher / Educator",
                primaryWorkflow = ProfessionWorkflowType.TEACHING_WORKFLOW,
                relevantDomains = listOf(
                    KnowledgeCategory.PROFESSIONAL_SKILL_DEVELOPMENT,
                    KnowledgeCategory.OFFICE_MANAGEMENT,
                    KnowledgeCategory.SOP_PROCESS_MANAGEMENT,
                    KnowledgeCategory.AI_BUSINESS_PRODUCTIVITY,
                    KnowledgeCategory.CROSS_INDUSTRY_PROFESSION
                )
            )
        )

        registerProfile(
            ProfessionProfile(
                professionType = ProfessionType.DOCTOR_HEALTHCARE_PROFESSIONAL,
                displayName = "Doctor / Healthcare Professional",
                primaryWorkflow = ProfessionWorkflowType.PRACTICE_OFFICE_WORKFLOW,
                relevantDomains = listOf(
                    KnowledgeCategory.OFFICE_MANAGEMENT,
                    KnowledgeCategory.SOP_PROCESS_MANAGEMENT,
                    KnowledgeCategory.PROFESSIONAL_SKILL_DEVELOPMENT,
                    KnowledgeCategory.AI_BUSINESS_PRODUCTIVITY,
                    KnowledgeCategory.CROSS_INDUSTRY_PROFESSION
                )
            )
        )

        registerProfile(
            ProfessionProfile(
                professionType = ProfessionType.FARMER,
                displayName = "Farmer / Agricultural Producer",
                primaryWorkflow = ProfessionWorkflowType.FARM_BUSINESS_MANAGEMENT,
                relevantDomains = listOf(
                    KnowledgeCategory.BUSINESS_GROWTH,
                    KnowledgeCategory.FINANCE_CASHFLOW,
                    KnowledgeCategory.MARKET_INTELLIGENCE,
                    KnowledgeCategory.CROSS_INDUSTRY_PROFESSION
                )
            )
        )

        registerProfile(
            ProfessionProfile(
                professionType = ProfessionType.SHOPKEEPER_RETAILER,
                displayName = "Shopkeeper / Retailer",
                primaryWorkflow = ProfessionWorkflowType.RETAIL_CUSTOMER_MANAGEMENT,
                relevantDomains = listOf(
                    KnowledgeCategory.CUSTOMER_CRM,
                    KnowledgeCategory.B2B_B2C_SALES,
                    KnowledgeCategory.RETENTION_UPSELL_CROSSSELL,
                    KnowledgeCategory.FINANCE_CASHFLOW,
                    KnowledgeCategory.CROSS_INDUSTRY_PROFESSION
                )
            )
        )

        registerProfile(
            ProfessionProfile(
                professionType = ProfessionType.FREELANCER,
                displayName = "Freelancer / Independent Contractor",
                primaryWorkflow = ProfessionWorkflowType.CLIENT_PROJECT_MANAGEMENT,
                relevantDomains = listOf(
                    KnowledgeCategory.QUOTATION_SALES,
                    KnowledgeCategory.CUSTOMER_CRM,
                    KnowledgeCategory.B2B_B2C_SALES,
                    KnowledgeCategory.PROFESSIONAL_SKILL_DEVELOPMENT,
                    KnowledgeCategory.CROSS_INDUSTRY_PROFESSION
                )
            )
        )

        registerProfile(
            ProfessionProfile(
                professionType = ProfessionType.MANUFACTURER,
                displayName = "Manufacturer / Production Manager",
                primaryWorkflow = ProfessionWorkflowType.PRODUCTION_OPERATIONS,
                relevantDomains = listOf(
                    KnowledgeCategory.PRODUCTION_PLANNING,
                    KnowledgeCategory.QUALITY_REWORK,
                    KnowledgeCategory.VENDOR_PROCUREMENT,
                    KnowledgeCategory.BUSINESS_ANALYTICS_KPI,
                    KnowledgeCategory.CROSS_INDUSTRY_PROFESSION
                )
            )
        )

        registerProfile(
            ProfessionProfile(
                professionType = ProfessionType.ACCOUNTANT,
                displayName = "Accountant / Finance Professional",
                primaryWorkflow = ProfessionWorkflowType.ACCOUNTING_WORKFLOW,
                relevantDomains = listOf(
                    KnowledgeCategory.FINANCE_CASHFLOW,
                    KnowledgeCategory.BUSINESS_ANALYTICS_KPI,
                    KnowledgeCategory.SOP_PROCESS_MANAGEMENT,
                    KnowledgeCategory.CROSS_INDUSTRY_PROFESSION
                )
            )
        )

        registerProfile(
            ProfessionProfile(
                professionType = ProfessionType.DESIGNER,
                displayName = "Graphic Designer / Creative Professional",
                primaryWorkflow = ProfessionWorkflowType.CREATIVE_CLIENT_WORKFLOW,
                relevantDomains = listOf(
                    KnowledgeCategory.QUOTATION_SALES,
                    KnowledgeCategory.CUSTOMER_CRM,
                    KnowledgeCategory.PRINTING_TECHNICAL,
                    KnowledgeCategory.PROFESSIONAL_SKILL_DEVELOPMENT,
                    KnowledgeCategory.CROSS_INDUSTRY_PROFESSION
                )
            )
        )

        registerProfile(
            ProfessionProfile(
                professionType = ProfessionType.BUSINESS_OWNER_ENTREPRENEUR,
                displayName = "Business Owner / Entrepreneur",
                primaryWorkflow = ProfessionWorkflowType.SALES_GROWTH_CASHFLOW,
                relevantDomains = listOf(
                    KnowledgeCategory.MARKETING_CUSTOMER_ACQUISITION,
                    KnowledgeCategory.B2B_B2C_SALES,
                    KnowledgeCategory.FINANCE_CASHFLOW,
                    KnowledgeCategory.LEADERSHIP_TEAM,
                    KnowledgeCategory.BUSINESS_GROWTH,
                    KnowledgeCategory.CROSS_INDUSTRY_PROFESSION
                )
            )
        )
    }

    fun registerProfile(profile: ProfessionProfile) {
        profileRegistry[profile.professionType] = profile
    }

    fun getRegisteredProfiles(): List<ProfessionProfile> {
        return profileRegistry.values.toList()
    }

    /**
     * Generates Profession-Adaptive Guidance for [context] and [userQuery].
     */
    fun generateAdaptiveGuidance(
        context: ProfessionContext,
        userQuery: String,
        userRole: String = "CUSTOMER"
    ): ProfessionAdaptiveGuidance {
        val timestamp = "2026-09-27T18:00:00Z"
        val profile = profileRegistry[context.professionType] ?: ProfessionProfile(
            professionType = ProfessionType.OTHER,
            displayName = "General Professional / Business User",
            primaryWorkflow = ProfessionWorkflowType.GENERAL_WORKFLOW,
            relevantDomains = listOf(KnowledgeCategory.CROSS_INDUSTRY_PROFESSION)
        )

        val isHealthcare = context.professionType == ProfessionType.DOCTOR_HEALTHCARE_PROFESSIONAL
        val disclaimerText = if (isHealthcare) {
            "\n\n⚠️ Notice: Guidance provided is strictly for practice & office administration workflow optimization, not clinical diagnosis or medical treatment instructions."
        } else ""

        // Fetch relevant RAG documents for mapped domains
        val searchResults = ragProvider.searchKnowledge(userQuery, userRole = userRole)
        val matchedContent = searchResults.firstOrNull()?.document?.contentChunk ?: "Consider structured planning and process controls."

        val guidanceText = "Professional Guidance for ${profile.displayName} (${profile.primaryWorkflow}):\n" +
                "Based on your primary goal '${context.primaryGoal ?: "Workflow Efficiency"}', recommended approach:\n" +
                "$matchedContent$disclaimerText"

        val actionSteps = listOf(
            "1. Review current workflow for ${profile.primaryWorkflow.name}",
            "2. Apply structured SOP controls and task scheduling",
            "3. Coordinate with team and measure outcomes using decision intelligence KPIs"
        )

        return ProfessionAdaptiveGuidance(
            professionType = profile.professionType,
            displayName = profile.displayName,
            primaryWorkflow = profile.primaryWorkflow,
            query = userQuery,
            guidanceText = guidanceText,
            mappedDomains = profile.relevantDomains,
            recommendedActionSteps = actionSteps,
            isHealthcareSafetyBoundActive = isHealthcare,
            generatedAt = timestamp
        )
    }
}
