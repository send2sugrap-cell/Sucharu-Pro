package com.sucharu.sucharupro.domain.preflight

/**
 * Result returned by an individual PreflightRule execution.
 */
data class PreflightRuleExecutionResult(
    val result: PreflightExecutionResult,
    val findings: List<PreflightFinding> = emptyList(),
    val errorMessage: String? = null
)

/**
 * Abstract contract for a technical Preflight Rule.
 */
interface PreflightRule {
    val ruleId: String
    val ruleCode: String
    val ruleName: String
    val category: PreflightRuleCategory
    val defaultSeverity: PreflightRuleSeverity

    fun isApplicable(context: PreflightExecutionContext): Boolean
    suspend fun execute(context: PreflightExecutionContext): PreflightRuleExecutionResult
}
