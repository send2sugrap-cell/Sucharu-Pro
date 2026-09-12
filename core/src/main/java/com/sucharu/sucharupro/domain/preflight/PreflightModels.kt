package com.sucharu.sucharupro.domain.preflight

/**
 * Status lifecycle of a Preflight Run execution.
 */
enum class PreflightRunStatus {
    REQUESTED,
    RUNNING,
    COMPLETED,
    FAILED,
    CANCELLED;

    val isTerminal: Boolean get() = this == COMPLETED || this == FAILED || this == CANCELLED
}

/**
 * Severity levels for technical preflight rules and findings.
 */
enum class PreflightRuleSeverity {
    INFO,
    WARNING,
    ERROR
}

/**
 * Technical categories for preflight rules.
 */
enum class PreflightRuleCategory {
    DOCUMENT,
    SPECIFICATION,
    COLOR,
    IMAGE,
    TYPOGRAPHY,
    GEOMETRY,
    PROOF,
    PRODUCTION_READINESS
}

/**
 * Execution outcome of an individual preflight rule.
 */
enum class PreflightExecutionResult {
    PASS,
    WARNING,
    ERROR,
    SKIPPED
}

/**
 * Aggregated overall preflight result.
 */
enum class PreflightOverallResult {
    PASS,
    WARNING,
    ERROR,
    NOT_EVALUATED
}

/**
 * Preflight Run entity.
 */
data class PreflightRun(
    val preflightRunId: String,
    val tenantId: String,
    val jobId: String? = null,
    val artworkId: String,
    val artworkVersionId: String? = null,
    val proofId: String? = null,
    val status: PreflightRunStatus = PreflightRunStatus.REQUESTED,
    val overallResult: PreflightOverallResult = PreflightOverallResult.NOT_EVALUATED,
    val engineVersion: String = "v1.0",
    val idempotencyKey: String? = null,
    val requestedBy: String,
    val startedAt: Long? = null,
    val completedAt: Long? = null,
    val summary: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

/**
 * Preflight Rule Definition metadata entity.
 */
data class PreflightRuleDefinition(
    val ruleId: String,
    val tenantId: String,
    val ruleCode: String,
    val ruleName: String,
    val category: PreflightRuleCategory,
    val severity: PreflightRuleSeverity = PreflightRuleSeverity.ERROR,
    val description: String? = null,
    val isEnabled: Boolean = true,
    val version: String = "v1.0",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

/**
 * Preflight Rule Execution record entity.
 */
data class PreflightRuleExecution(
    val executionId: String,
    val tenantId: String,
    val preflightRunId: String,
    val ruleId: String,
    val ruleCode: String,
    val result: PreflightExecutionResult,
    val startedAt: Long = System.currentTimeMillis(),
    val completedAt: Long = System.currentTimeMillis(),
    val errorMessage: String? = null
)

/**
 * Technical finding discovered during preflight execution.
 */
data class PreflightFinding(
    val findingId: String,
    val tenantId: String,
    val preflightRunId: String,
    val executionId: String? = null,
    val ruleCode: String,
    val category: PreflightRuleCategory,
    val severity: PreflightRuleSeverity,
    val message: String,
    val expectedValue: String? = null,
    val actualValue: String? = null,
    val locationContext: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * Execution Context passed to preflight rules during evaluation.
 */
data class PreflightExecutionContext(
    val tenantId: String,
    val jobId: String? = null,
    val artworkId: String,
    val artworkVersionId: String? = null,
    val proofId: String? = null,
    val orderSpecificationMap: Map<String, Any> = emptyMap(),
    val artworkMetadataMap: Map<String, Any> = emptyMap()
)
