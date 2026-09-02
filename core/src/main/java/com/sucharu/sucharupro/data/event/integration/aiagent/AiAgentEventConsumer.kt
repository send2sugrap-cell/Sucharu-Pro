package com.sucharu.sucharupro.data.event.integration.aiagent

import com.sucharu.sucharupro.data.api.model.AuthenticatedPrincipal
import com.sucharu.sucharupro.domain.event.boundary.AiAgentEventAccessDecision
import com.sucharu.sucharupro.domain.event.boundary.AiAgentEventBoundary
import com.sucharu.sucharupro.domain.event.consumer.DomainEventConsumer
import com.sucharu.sucharupro.domain.event.consumer.EventConsumerResult
import com.sucharu.sucharupro.domain.event.consumer.EventFailureClassification
import com.sucharu.sucharupro.domain.event.model.DomainEvent
import com.sucharu.sucharupro.domain.event.model.DomainEventType
import com.sucharu.sucharupro.domain.event.model.EventEnvelope
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Production-grade AI Agent event consumer enforcing capability-based security boundaries (INFRA-04 Step 03).
 */
class AiAgentEventConsumer<T : DomainEvent>(
    override val supportedEventType: DomainEventType,
    override val supportedVersion: String = supportedEventType.currentVersion,
    override val consumerId: String = "ai_agent.${supportedEventType.name.lowercase()}",
    private val targetAgentPrincipal: AuthenticatedPrincipal,
    private val onFrameReceived: ((AiAgentEventFrame) -> Unit)? = null
) : DomainEventConsumer<T> {

    private val _receivedFrames = CopyOnWriteArrayList<AiAgentEventFrame>()
    val receivedFrames: List<AiAgentEventFrame> get() = _receivedFrames.toList()

    override suspend fun consume(envelope: EventEnvelope<T>): EventConsumerResult {
        // Evaluate capability and tenant authorization via canonical boundary
        val decision = AiAgentEventBoundary.evaluateAccess(targetAgentPrincipal, envelope)

        return when (decision) {
            is AiAgentEventAccessDecision.Denied -> {
                EventConsumerResult.Failure(
                    reason = decision.reason,
                    classification = decision.classification
                )
            }
            is AiAgentEventAccessDecision.Allowed -> {
                // Build data-minimized frame
                val summary = mutableMapOf(
                    "aggregateType" to envelope.aggregateType,
                    "aggregateId" to envelope.aggregateId,
                    "aggregateVersion" to envelope.aggregateVersion.toString(),
                    "source" to envelope.source
                )
                summary.putAll(decision.sanitizedMetadata)

                val confirmation = HumanConfirmationMetadata(
                    requiresConfirmation = envelope.metadata["requiresConfirmation"]?.toBooleanStrictOrNull() ?: false,
                    confirmationId = envelope.metadata["confirmationId"],
                    requestedByAgentId = envelope.metadata["requestedByAgentId"],
                    approvedByHumanId = envelope.metadata["approvedByHumanId"]
                )

                val frame = AiAgentEventFrame(
                    eventId = envelope.eventId,
                    eventType = envelope.eventType,
                    eventVersion = envelope.eventVersion,
                    projectId = envelope.projectId,
                    aggregateType = envelope.aggregateType,
                    aggregateId = envelope.aggregateId,
                    aggregateVersion = envelope.aggregateVersion,
                    correlationId = envelope.correlationId,
                    occurredAt = envelope.occurredAt,
                    grantedCapability = decision.grantedCapability,
                    contextSummary = summary,
                    confirmationMetadata = confirmation
                )

                _receivedFrames.add(frame)
                onFrameReceived?.invoke(frame)

                EventConsumerResult.Success(
                    message = "AI Agent consumed frame with capability '${decision.grantedCapability.name}'"
                )
            }
        }
    }

    fun clear() {
        _receivedFrames.clear()
    }
}
