package com.sucharu.sucharupro.data.workflow.integration

import com.sucharu.sucharupro.data.job.postgres.JobRepository
import com.sucharu.sucharupro.data.persistence.postgres.TenantContext
import com.sucharu.sucharupro.domain.job.model.JobDefinition
import com.sucharu.sucharupro.domain.job.model.JobPriority
import com.sucharu.sucharupro.domain.job.model.JobTriggerType
import com.sucharu.sucharupro.domain.workflow.model.WorkflowInstance
import com.sucharu.sucharupro.domain.workflow.model.WorkflowStepDefinition
import java.util.UUID

/**
 * Bridges asynchronous workflow steps to the PostgreSQL background job execution platform (INFRA-04 Step 05).
 */
class WorkflowJobStepAdapter(
    private val jobRepository: JobRepository
) {

    /**
     * Enqueues a background job corresponding to a workflow step execution.
     */
    suspend fun enqueueStepJob(
        step: WorkflowStepDefinition,
        instance: WorkflowInstance,
        jobType: String,
        payloadJson: String,
        priority: JobPriority = JobPriority.NORMAL,
        tenantContext: TenantContext
    ): String {
        require(instance.projectId == tenantContext.projectId) {
            "Tenant isolation mismatch: instance '${instance.projectId}' != tenant '${tenantContext.projectId}'"
        }

        val jobId = UUID.randomUUID().toString()
        val idempotencyKey = "wf:job:${instance.workflowId}:${step.stepId}:${instance.executionId}"

        val job = JobDefinition(
            jobId = jobId,
            projectId = tenantContext.projectId,
            jobType = jobType,
            jobVersion = "v1",
            triggerType = JobTriggerType.WORKFLOW,
            priority = priority,
            payloadJson = payloadJson,
            correlationId = instance.correlationId,
            causationId = instance.causationId ?: instance.workflowId,
            requestId = instance.requestId,
            actorType = instance.actorType,
            actorId = instance.actorId,
            principalType = instance.principalType,
            idempotencyKey = idempotencyKey,
            metadata = mapOf(
                "workflowId" to instance.workflowId,
                "stepId" to step.stepId,
                "executionId" to instance.executionId
            )
        )

        jobRepository.enqueueJob(job, tenantContext)
        return jobId
    }
}
