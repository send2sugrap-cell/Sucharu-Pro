package com.sucharu.sucharupro.data.workflow.control

import com.sucharu.sucharupro.domain.event.boundary.RealTimeEventFrame
import com.sucharu.sucharupro.domain.workflow.governance.TimelineEventType
import com.sucharu.sucharupro.domain.workflow.model.WorkflowInstance
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Real-time event streaming bridge for Workflow Control Plane and Operations Console (INFRA-04 Step 06).
 */
class WorkflowRealTimeBridge {

    private val tenantListeners = ConcurrentHashMap<String, CopyOnWriteArrayList<(RealTimeEventFrame) -> Unit>>()

    /**
     * Registers a live listener for a specific tenant's workflow events.
     */
    fun subscribe(projectId: String, listener: (RealTimeEventFrame) -> Unit): () -> Unit {
        val list = tenantListeners.computeIfAbsent(projectId) { CopyOnWriteArrayList() }
        list.add(listener)
        return { list.remove(listener) }
    }

    /**
     * Broadcasts a sanitized workflow lifecycle event frame to tenant subscribers.
     */
    fun publishWorkflowEvent(
        instance: WorkflowInstance,
        eventType: TimelineEventType,
        title: String,
        details: Map<String, String> = emptyMap()
    ) {
        val sanitizedDetails = sanitizeDetails(details)
        val frame = RealTimeEventFrame(
            frameId = UUID.randomUUID().toString(),
            eventId = "wf-evt-${UUID.randomUUID().toString().take(8)}",
            eventType = "workflow.${eventType.name.lowercase()}",
            eventVersion = "1.0.0",
            projectId = instance.projectId,
            topic = "tenant.${instance.projectId}.workflows.${instance.workflowId}",
            timestamp = System.currentTimeMillis(),
            aggregateType = "WorkflowInstance",
            aggregateId = instance.workflowId,
            correlationId = instance.correlationId ?: "corr-${instance.workflowId.take(8)}",
            payloadSummary = sanitizedDetails + mapOf(
                "workflowId" to instance.workflowId,
                "definitionId" to instance.definitionId,
                "versionId" to instance.versionId,
                "status" to instance.status.name,
                "currentStepId" to (instance.currentStepId ?: "none"),
                "eventTitle" to title
            )
        )

        val listeners = tenantListeners[instance.projectId]
        listeners?.forEach { listener ->
            try {
                listener(frame)
            } catch (_: Throwable) {
                // Ignore listener exceptions to prevent streaming failure
            }
        }
    }

    private fun sanitizeDetails(details: Map<String, String>): Map<String, String> {
        val sensitiveKeys = setOf("password", "token", "secret", "apikey", "authorization", "hmac", "credit_card")
        return details.mapValues { (k, v) ->
            if (sensitiveKeys.any { k.lowercase().contains(it) }) "[REDACTED]" else v
        }
    }
}
