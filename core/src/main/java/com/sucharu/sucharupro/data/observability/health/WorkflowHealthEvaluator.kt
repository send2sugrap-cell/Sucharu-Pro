package com.sucharu.sucharupro.data.observability.health

import com.sucharu.sucharupro.data.workflow.observability.WorkflowMetrics
import com.sucharu.sucharupro.domain.observability.OperationalHealthStatus
import com.sucharu.sucharupro.domain.observability.WorkflowHealth

/**
 * Health evaluator for Saga Workflow engine (INFRA-04 Step 09).
 */
class WorkflowHealthEvaluator(
    private val workflowMetrics: WorkflowMetrics = WorkflowMetrics()
) {

    fun evaluate(
        activeWorkflowsCount: Long = 0L,
        approvalsWaitingCount: Long = 0L
    ): WorkflowHealth {
        val issues = mutableListOf<String>()

        val started = workflowMetrics.getStartedCount()
        val completed = workflowMetrics.getCompletedCount()
        val failed = workflowMetrics.getFailedCount()
        val compensated = workflowMetrics.getCompensatedCount()

        var status = OperationalHealthStatus.HEALTHY
        if (failed > 50 || approvalsWaitingCount > 100) {
            status = OperationalHealthStatus.CRITICAL
            if (failed > 50) issues.add("Critical workflow failure count: $failed")
            if (approvalsWaitingCount > 100) issues.add("Approval backlog critical: $approvalsWaitingCount")
        } else if (failed > 5 || approvalsWaitingCount > 20 || compensated > 10) {
            status = OperationalHealthStatus.DEGRADED
            if (failed > 5) issues.add("Workflow failures elevated: $failed")
            if (approvalsWaitingCount > 20) issues.add("Approval backlog elevated: $approvalsWaitingCount")
            if (compensated > 10) issues.add("Workflow compensation rate elevated: $compensated")
        }

        return WorkflowHealth(
            status = status,
            workflowsStarted = started,
            workflowsCompleted = completed,
            workflowsFailed = failed,
            workflowsCompensated = compensated,
            approvalsWaiting = approvalsWaitingCount,
            activeWorkflows = activeWorkflowsCount,
            issues = issues
        )
    }
}
