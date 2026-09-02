package com.sucharu.sucharupro.data.workflow.integration

import com.sucharu.sucharupro.data.event.serialization.EventSerializationHelper
import com.sucharu.sucharupro.data.persistence.postgres.TenantContext
import com.sucharu.sucharupro.data.workflow.postgres.WorkflowDefinitionRepository
import com.sucharu.sucharupro.data.workflow.postgres.WorkflowInstanceRepository
import com.sucharu.sucharupro.domain.event.model.EventEnvelope
import com.sucharu.sucharupro.domain.workflow.engine.WorkflowOrchestrator
import com.sucharu.sucharupro.domain.workflow.model.WorkflowInstance
import java.util.UUID

/**
 * Bridges Domain Event envelopes to Workflow instance triggers (INFRA-04 Step 05).
 */
class EventToWorkflowTrigger(
    private val definitionRepository: WorkflowDefinitionRepository,
    private val instanceRepository: WorkflowInstanceRepository,
    private val orchestrator: WorkflowOrchestrator
) {

    /**
     * Evaluates an incoming domain event envelope and triggers a matching workflow definition.
     */
    suspend fun triggerWorkflowFromEvent(
        envelope: EventEnvelope<*>,
        definitionId: String,
        versionId: String,
        tenantContext: TenantContext
    ): String? {
        require(envelope.projectId == tenantContext.projectId) {
            "Tenant isolation mismatch: envelope '${envelope.projectId}' != tenant '${tenantContext.projectId}'"
        }

        val version = definitionRepository.getVersion(definitionId, versionId, tenantContext)
            ?: return null

        val workflowId = UUID.randomUUID().toString()
        val idempotencyKey = "wf:event:${envelope.eventId}:$definitionId:$versionId"

        val instance = WorkflowInstance(
            workflowId = workflowId,
            projectId = tenantContext.projectId,
            definitionId = definitionId,
            versionId = versionId,
            executionId = UUID.randomUUID().toString(),
            context = mapOf(
                "triggerEventId" to envelope.eventId,
                "triggerEventType" to envelope.eventType.name,
                "aggregateId" to envelope.aggregateId,
                "aggregateType" to envelope.aggregateType
            ),
            correlationId = envelope.correlationId,
            causationId = envelope.causationId ?: envelope.eventId,
            requestId = envelope.requestId,
            actorType = envelope.actorType,
            actorId = envelope.actorId,
            principalType = envelope.principalType,
            idempotencyKey = idempotencyKey
        )

        val created = instanceRepository.createInstance(instance, tenantContext)
        if (!created) {
            return null // Idempotently skipped duplicate trigger
        }

        val (startedInstance, _) = orchestrator.startWorkflow(instance, version)
        instanceRepository.updateInstance(startedInstance, tenantContext)

        return workflowId
    }
}
