package com.sucharu.sucharupro.data.job.integration

import com.sucharu.sucharupro.data.event.serialization.EventSerializationHelper
import com.sucharu.sucharupro.data.job.postgres.JobRepository
import com.sucharu.sucharupro.data.persistence.postgres.TenantContext
import com.sucharu.sucharupro.domain.event.model.DomainEvent
import com.sucharu.sucharupro.domain.event.model.EventEnvelope
import com.sucharu.sucharupro.domain.job.model.JobDefinition
import com.sucharu.sucharupro.domain.job.model.JobPriority
import com.sucharu.sucharupro.domain.job.model.JobStatus
import com.sucharu.sucharupro.domain.job.model.JobTriggerType
import java.util.UUID

/**
 * Bridges domain event envelopes to asynchronous background jobs (INFRA-04 Step 04).
 */
class EventToJobDispatcher(
    private val jobRepository: JobRepository
) {
    /**
     * Dispatches an event to a background job with deterministic idempotency and trace propagation.
     */
    suspend fun dispatchEventToJob(
        envelope: EventEnvelope<*>,
        targetJobType: String,
        targetJobVersion: String = "v1",
        priority: JobPriority = JobPriority.NORMAL,
        delayMs: Long = 0L,
        tenantContext: TenantContext
    ): String {
        require(envelope.projectId == tenantContext.projectId) {
            "Tenant isolation mismatch: envelope projectId '${envelope.projectId}' != tenant '${tenantContext.projectId}'"
        }

        val jobId = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()
        val availableAt = now + delayMs
        val payloadJson = EventSerializationHelper.serializePayload(envelope.payload)

        val job = JobDefinition(
            jobId = jobId,
            projectId = tenantContext.projectId,
            jobType = targetJobType,
            jobVersion = targetJobVersion,
            triggerType = JobTriggerType.EVENT,
            priority = priority,
            status = JobStatus.QUEUED,
            scheduledAt = now,
            availableAt = availableAt,
            payloadJson = payloadJson,
            metadata = envelope.metadata,
            correlationId = envelope.correlationId,
            causationId = envelope.causationId ?: envelope.eventId,
            requestId = envelope.requestId,
            actorType = envelope.actorType,
            actorId = envelope.actorId,
            principalType = envelope.principalType,
            source = envelope.source,
            idempotencyKey = "event:${envelope.eventId}:$targetJobType",
            createdAt = now,
            updatedAt = now
        )

        jobRepository.enqueueJob(job, tenantContext)
        return jobId
    }
}
