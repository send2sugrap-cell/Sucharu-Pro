package com.sucharu.sucharupro.data.observability

import com.sucharu.sucharupro.data.observability.health.WorkflowHealthEvaluator
import com.sucharu.sucharupro.data.workflow.observability.WorkflowMetrics
import com.sucharu.sucharupro.domain.observability.OperationalHealthStatus
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Saga workflow engine health & stuck approval monitoring test suite (INFRA-04 Step 09).
 */
class WorkflowHealthTest {

    private lateinit var workflowMetrics: WorkflowMetrics
    private lateinit var evaluator: WorkflowHealthEvaluator

    @Before
    fun setUp() {
        workflowMetrics = WorkflowMetrics()
        evaluator = WorkflowHealthEvaluator(workflowMetrics)
    }

    @Test
    fun test01_normalWorkflowExecution_isHealthy() {
        workflowMetrics.recordWorkflowStarted("p-001")
        workflowMetrics.recordWorkflowCompleted("p-001")
        val health = evaluator.evaluate(activeWorkflowsCount = 0, approvalsWaitingCount = 0)
        assertEquals(OperationalHealthStatus.HEALTHY, health.status)
    }

    @Test
    fun test02_highApprovalBacklog_isDegraded() {
        val health = evaluator.evaluate(activeWorkflowsCount = 10, approvalsWaitingCount = 25)
        assertEquals(OperationalHealthStatus.DEGRADED, health.status)
        assertTrue(health.issues.any { it.contains("Approval backlog elevated") })
    }

    @Test
    fun test03_criticalFailureCount_isCritical() {
        repeat(55) { workflowMetrics.recordWorkflowFailed("p-001") }
        val health = evaluator.evaluate(activeWorkflowsCount = 5, approvalsWaitingCount = 2)
        assertEquals(OperationalHealthStatus.CRITICAL, health.status)
        assertTrue(health.issues.any { it.contains("Critical workflow failure count") })
    }
}
