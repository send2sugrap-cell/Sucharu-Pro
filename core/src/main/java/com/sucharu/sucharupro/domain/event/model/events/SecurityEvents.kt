package com.sucharu.sucharupro.domain.event.model.events

import com.sucharu.sucharupro.domain.event.model.DomainEvent
import com.sucharu.sucharupro.domain.event.model.DomainEventType

/**
 * Emitted when user authentication succeeds.
 *
 * Payload Safety: Never include password, hash, JWT, or refresh token in event payload.
 */
data class AuthenticationSucceededEvent(
    val userId: String,
    val username: String,
    val clientIpMasked: String? = null,
    val userAgentSummary: String? = null,
    override val aggregateVersion: Long = 1L
) : DomainEvent {
    override val eventType: DomainEventType get() = DomainEventType.AUTH_SUCCEEDED
    override val aggregateId: String get() = userId
    override val aggregateType: String get() = "AUTHENTICATION"

    init {
        require(userId.isNotBlank()) { "userId cannot be blank" }
        require(username.isNotBlank()) { "username cannot be blank" }
    }
}

/**
 * Emitted when user authentication fails.
 *
 * Payload Safety: Generic sanitized failure reason; never reveal plaintext attempted passwords or user existence.
 */
data class AuthenticationFailedEvent(
    val attemptedIdentifierMasked: String,
    val failureReason: String = "INVALID_CREDENTIALS",
    val clientIpMasked: String? = null,
    override val aggregateVersion: Long = 1L
) : DomainEvent {
    override val eventType: DomainEventType get() = DomainEventType.AUTH_FAILED
    override val aggregateId: String get() = attemptedIdentifierMasked
    override val aggregateType: String get() = "SECURITY"

    init {
        require(attemptedIdentifierMasked.isNotBlank()) { "attemptedIdentifierMasked cannot be blank" }
        require(failureReason.isNotBlank()) { "failureReason cannot be blank" }
    }
}

/**
 * Emitted when a new session is established.
 */
data class SessionCreatedEvent(
    val sessionId: String,
    val userId: String,
    val expiresAt: Long,
    val deviceInfo: String? = null,
    override val aggregateVersion: Long = 1L
) : DomainEvent {
    override val eventType: DomainEventType get() = DomainEventType.SESSION_CREATED
    override val aggregateId: String get() = sessionId
    override val aggregateType: String get() = "AUTHENTICATION"

    init {
        require(sessionId.isNotBlank()) { "sessionId cannot be blank" }
        require(userId.isNotBlank()) { "userId cannot be blank" }
    }
}

/**
 * Emitted when a session is revoked or terminated.
 */
data class SessionRevokedEvent(
    val sessionId: String,
    val userId: String,
    val revocationReason: String,
    val revokedByActorId: String,
    override val aggregateVersion: Long = 1L
) : DomainEvent {
    override val eventType: DomainEventType get() = DomainEventType.SESSION_REVOKED
    override val aggregateId: String get() = sessionId
    override val aggregateType: String get() = "SECURITY"

    init {
        require(sessionId.isNotBlank()) { "sessionId cannot be blank" }
        require(userId.isNotBlank()) { "userId cannot be blank" }
        require(revocationReason.isNotBlank()) { "revocationReason cannot be blank" }
        require(revokedByActorId.isNotBlank()) { "revokedByActorId cannot be blank" }
    }
}

/**
 * Emitted when an unauthorized action is denied by the security policy.
 */
data class AuthorizationDeniedEvent(
    val userId: String?,
    val attemptedAction: String,
    val resourceType: String,
    val denialReasonCode: String,
    override val aggregateVersion: Long = 1L
) : DomainEvent {
    override val eventType: DomainEventType get() = DomainEventType.AUTHZ_DENIED
    override val aggregateId: String get() = userId ?: "ANONYMOUS"
    override val aggregateType: String get() = "SECURITY"

    init {
        require(attemptedAction.isNotBlank()) { "attemptedAction cannot be blank" }
        require(resourceType.isNotBlank()) { "resourceType cannot be blank" }
        require(denialReasonCode.isNotBlank()) { "denialReasonCode cannot be blank" }
    }
}

/**
 * Emitted when an account is locked due to repeated authentication failures or security policy.
 */
data class AccountLockedEvent(
    val userId: String,
    val lockReason: String,
    val unlockTimestamp: Long? = null,
    override val aggregateVersion: Long = 1L
) : DomainEvent {
    override val eventType: DomainEventType get() = DomainEventType.ACCOUNT_LOCKED
    override val aggregateId: String get() = userId
    override val aggregateType: String get() = "SECURITY"

    init {
        require(userId.isNotBlank()) { "userId cannot be blank" }
        require(lockReason.isNotBlank()) { "lockReason cannot be blank" }
    }
}

/**
 * Emitted when a password is changed or reset.
 *
 * Payload Safety: Never include current or new passwords.
 */
data class PasswordChangedEvent(
    val userId: String,
    val changeMethod: String = "USER_INITIATED",
    val allSessionsRevoked: Boolean = true,
    override val aggregateVersion: Long = 1L
) : DomainEvent {
    override val eventType: DomainEventType get() = DomainEventType.PASSWORD_CHANGED
    override val aggregateId: String get() = userId
    override val aggregateType: String get() = "SECURITY"

    init {
        require(userId.isNotBlank()) { "userId cannot be blank" }
        require(changeMethod.isNotBlank()) { "changeMethod cannot be blank" }
    }
}
