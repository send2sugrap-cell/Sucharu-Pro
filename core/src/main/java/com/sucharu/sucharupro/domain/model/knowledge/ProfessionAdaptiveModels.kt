package com.sucharu.sucharupro.domain.model.knowledge

/**
 * Canonical 9 Profession Types.
 */
enum class ProfessionType {
    TEACHER,
    DOCTOR_HEALTHCARE_PROFESSIONAL,
    FARMER,
    SHOPKEEPER_RETAILER,
    FREELANCER,
    MANUFACTURER,
    ACCOUNTANT,
    DESIGNER,
    BUSINESS_OWNER_ENTREPRENEUR,
    OTHER
}

/**
 * Canonical Primary Workflows for Profession Adaptation.
 */
enum class ProfessionWorkflowType {
    TEACHING_WORKFLOW,
    PRACTICE_OFFICE_WORKFLOW,
    FARM_BUSINESS_MANAGEMENT,
    RETAIL_CUSTOMER_MANAGEMENT,
    CLIENT_PROJECT_MANAGEMENT,
    PRODUCTION_OPERATIONS,
    ACCOUNTING_WORKFLOW,
    CREATIVE_CLIENT_WORKFLOW,
    SALES_GROWTH_CASHFLOW,
    GENERAL_WORKFLOW
}

/**
 * Structured Profession Profile.
 */
data class ProfessionProfile(
    val professionType: ProfessionType,
    val displayName: String,
    val primaryWorkflow: ProfessionWorkflowType,
    val relevantDomains: List<KnowledgeCategory> = emptyList()
)

/**
 * Context carried in a Profession-Adaptive AI query.
 */
data class ProfessionContext(
    val professionType: ProfessionType = ProfessionType.OTHER,
    val industry: String? = null,
    val primaryGoal: String? = null,
    val experienceLevel: String = "MID_LEVEL"
)

/**
 * Generated Profession-Adaptive Guidance Response.
 */
data class ProfessionAdaptiveGuidance(
    val professionType: ProfessionType,
    val displayName: String,
    val primaryWorkflow: ProfessionWorkflowType,
    val query: String,
    val guidanceText: String,
    val mappedDomains: List<KnowledgeCategory> = emptyList(),
    val recommendedActionSteps: List<String> = emptyList(),
    val isHealthcareSafetyBoundActive: Boolean = false,
    val generatedAt: String
)
