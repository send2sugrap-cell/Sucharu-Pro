package com.sucharu.sucharupro.data.workflow.observability

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

/**
 * Thread-safe metrics collector for workflow execution (INFRA-04 Step 05).
 */
class WorkflowMetrics {
    private val workflowsStarted = AtomicLong(0)
    private val workflowsCompleted = AtomicLong(0)
    private val workflowsFailed = AtomicLong(0)
    private val workflowsCompensated = AtomicLong(0)
    private val approvalsSubmitted = AtomicLong(0)
    private val approvalsDecided = AtomicLong(0)
    private val activeWorkflowsByTenant = ConcurrentHashMap<String, AtomicLong>()

    fun recordWorkflowStarted(projectId: String) {
        workflowsStarted.incrementAndGet()
        activeWorkflowsByTenant.computeIfAbsent(projectId) { AtomicLong(0) }.incrementAndGet()
    }

    fun recordWorkflowCompleted(projectId: String) {
        workflowsCompleted.incrementAndGet()
        activeWorkflowsByTenant[projectId]?.decrementAndGet()
    }

    fun recordWorkflowFailed(projectId: String) {
        workflowsFailed.incrementAndGet()
        activeWorkflowsByTenant[projectId]?.decrementAndGet()
    }

    fun recordWorkflowCompensated() {
        workflowsCompensated.incrementAndGet()
    }

    fun recordApprovalSubmitted() {
        approvalsSubmitted.incrementAndGet()
    }

    fun recordApprovalDecided() {
        approvalsDecided.incrementAndGet()
    }

    fun getStartedCount(): Long = workflowsStarted.get()
    fun getCompletedCount(): Long = workflowsCompleted.get()
    fun getFailedCount(): Long = workflowsFailed.get()
    fun getCompensatedCount(): Long = workflowsCompensated.get()
    fun getApprovalsSubmittedCount(): Long = approvalsSubmitted.get()
    fun getApprovalsDecidedCount(): Long = approvalsDecided.get()
    fun getActiveWorkflowCount(projectId: String): Long = activeWorkflowsByTenant[projectId]?.get() ?: 0L
}
