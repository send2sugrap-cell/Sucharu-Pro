package com.sucharu.sucharupro.domain.preflight

/**
 * Pure deterministic aggregator for computing overall preflight run results.
 */
object PreflightResultAggregator {

    fun aggregate(
        executions: List<PreflightRuleExecution>,
        findings: List<PreflightFinding>
    ): PreflightOverallResult {
        if (executions.isEmpty() && findings.isEmpty()) {
            return PreflightOverallResult.NOT_EVALUATED
        }

        val hasErrors = findings.any { it.severity == PreflightRuleSeverity.ERROR } ||
                executions.any { it.result == PreflightExecutionResult.ERROR }
        if (hasErrors) {
            return PreflightOverallResult.ERROR
        }

        val hasWarnings = findings.any { it.severity == PreflightRuleSeverity.WARNING } ||
                executions.any { it.result == PreflightExecutionResult.WARNING }
        if (hasWarnings) {
            return PreflightOverallResult.WARNING
        }

        return PreflightOverallResult.PASS
    }
}
