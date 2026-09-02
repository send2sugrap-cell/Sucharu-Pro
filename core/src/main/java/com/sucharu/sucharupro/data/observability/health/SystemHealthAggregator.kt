package com.sucharu.sucharupro.data.observability.health

import com.sucharu.sucharupro.domain.observability.*

/**
 * Aggregates all subsystem health metrics into cohesive System and Tenant Health summaries (INFRA-04 Step 09).
 */
class SystemHealthAggregator(
    private val eventEvaluator: EventInfrastructureHealthEvaluator,
    private val notificationEvaluator: NotificationHealthEvaluator,
    private val jobEvaluator: BackgroundJobHealthEvaluator,
    private val workflowEvaluator: WorkflowHealthEvaluator,
    private val aiAgentEvaluator: AiAgentHealthEvaluator,
    private val n8nEvaluator: N8nHealthEvaluator
) {

    fun aggregateSystemHealth(openAlertsCount: Int = 0): SystemHealthSummary {
        val eventHealth = eventEvaluator.evaluate()
        val notifHealth = notificationEvaluator.evaluate()
        val jobHealth = jobEvaluator.evaluate()
        val wfHealth = workflowEvaluator.evaluate()
        val aiHealth = aiAgentEvaluator.evaluate()
        val n8nHealth = n8nEvaluator.evaluate()

        val allSubsystemStatuses = listOf(
            eventHealth.status,
            notifHealth.status,
            jobHealth.status,
            wfHealth.status,
            aiHealth.status,
            n8nHealth.status
        )

        val overallStatus = when {
            allSubsystemStatuses.any { it == OperationalHealthStatus.CRITICAL } || openAlertsCount > 5 -> OperationalHealthStatus.CRITICAL
            allSubsystemStatuses.any { it == OperationalHealthStatus.DEGRADED } || openAlertsCount > 0 -> OperationalHealthStatus.DEGRADED
            else -> OperationalHealthStatus.HEALTHY
        }

        val allIssues = mutableListOf<String>()
        allIssues.addAll(eventHealth.issues)
        allIssues.addAll(notifHealth.issues)
        allIssues.addAll(jobHealth.issues)
        allIssues.addAll(wfHealth.issues)
        allIssues.addAll(aiHealth.issues)
        allIssues.addAll(n8nHealth.issues)

        val totalHealthy = allSubsystemStatuses.count { it == OperationalHealthStatus.HEALTHY }
        val sloCompliance = (totalHealthy.toDouble() / allSubsystemStatuses.size) * 100.0

        return SystemHealthSummary(
            status = overallStatus,
            eventInfrastructure = eventHealth,
            notificationInfrastructure = notifHealth,
            backgroundJobInfrastructure = jobHealth,
            workflowInfrastructure = wfHealth,
            aiAgentIntegration = aiHealth,
            n8nIntegration = n8nHealth,
            openAlertsCount = openAlertsCount,
            systemSloCompliancePercentage = sloCompliance,
            globalIssues = allIssues
        )
    }

    fun aggregateTenantHealth(projectId: String, activeAlertsCount: Int = 0): TenantHealthSummary {
        val system = aggregateSystemHealth(activeAlertsCount)
        return TenantHealthSummary(
            projectId = projectId,
            status = system.status,
            eventPendingCount = system.eventInfrastructure.outboxHealth.pendingCount,
            notificationDeliveryRate = system.notificationInfrastructure.overallDeliveryRate,
            activeJobsCount = system.backgroundJobInfrastructure.pendingJobs,
            activeWorkflowsCount = system.workflowInfrastructure.activeWorkflows,
            activeAlertsCount = activeAlertsCount,
            sloCompliancePercentage = system.systemSloCompliancePercentage
        )
    }
}
