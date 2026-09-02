package com.sucharu.sucharupro.data.event.integration.aiagent

import com.sucharu.sucharupro.data.auth.authorization.AuthorizationCapability
import com.sucharu.sucharupro.domain.event.model.DomainEventType

/**
 * Human confirmation metadata for AI-assisted workflows requiring human authorization.
 */
data class HumanConfirmationMetadata(
    val requiresConfirmation: Boolean = false,
    val confirmationId: String? = null,
    val requestedByAgentId: String? = null,
    val approvedByHumanId: String? = null,
    val approvalTimestamp: Long? = null,
    val confirmationNotes: String? = null
)

/**
 * Data-minimized, sanitized event payload provided to AI agents.
 */
data class AiAgentEventFrame(
    val eventId: String,
    val eventType: DomainEventType,
    val eventVersion: String,
    val projectId: String,
    val aggregateType: String,
    val aggregateId: String,
    val aggregateVersion: Long,
    val correlationId: String,
    val occurredAt: Long,
    val grantedCapability: AuthorizationCapability,
    val contextSummary: Map<String, String>,
    val confirmationMetadata: HumanConfirmationMetadata = HumanConfirmationMetadata()
)
