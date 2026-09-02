package com.sucharu.sucharupro.domain.event

import com.sucharu.sucharupro.domain.event.model.DomainEventType
import com.sucharu.sucharupro.domain.event.model.events.AccountLockedEvent
import com.sucharu.sucharupro.domain.event.model.events.AuthenticationFailedEvent
import com.sucharu.sucharupro.domain.event.model.events.AuthenticationSucceededEvent
import com.sucharu.sucharupro.domain.event.model.events.AuthorizationDeniedEvent
import com.sucharu.sucharupro.domain.event.model.events.PasswordChangedEvent
import com.sucharu.sucharupro.domain.event.model.events.SessionCreatedEvent
import com.sucharu.sucharupro.domain.event.model.events.SessionRevokedEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class SecurityEventTest {

    @Test
    fun test01_securityEvents_doNotExposePasswordsOrSecrets() {
        val authSuccess = AuthenticationSucceededEvent(
            userId = "USR-100",
            username = "john_doe",
            clientIpMasked = "192.168.1.***"
        )
        val authFail = AuthenticationFailedEvent(
            attemptedIdentifierMasked = "jo***@example.com",
            failureReason = "INVALID_CREDENTIALS"
        )
        val sessCreated = SessionCreatedEvent(
            sessionId = "SESS-1",
            userId = "USR-100",
            expiresAt = System.currentTimeMillis() + 86400000L
        )
        val sessRevoked = SessionRevokedEvent(
            sessionId = "SESS-1",
            userId = "USR-100",
            revocationReason = "User logged out",
            revokedByActorId = "USR-100"
        )
        val authzDenied = AuthorizationDeniedEvent(
            userId = "USR-100",
            attemptedAction = "ADMIN_MANAGE_USERS",
            resourceType = "SYSTEM_CONFIG",
            denialReasonCode = "MISSING_CAPABILITY"
        )
        val accountLocked = AccountLockedEvent(
            userId = "USR-100",
            lockReason = "Max consecutive failed login attempts exceeded",
            aggregateVersion = 2L
        )
        val pwdChanged = PasswordChangedEvent(
            userId = "USR-100",
            changeMethod = "PASSWORD_RESET_TOKEN",
            allSessionsRevoked = true,
            aggregateVersion = 3L
        )

        assertEquals("AUTHENTICATION", authSuccess.aggregateType)
        assertEquals("SECURITY", authFail.aggregateType)
        assertEquals("AUTHENTICATION", sessCreated.aggregateType)
        assertEquals("SECURITY", sessRevoked.aggregateType)
        assertEquals("SECURITY", authzDenied.aggregateType)
        assertEquals("SECURITY", accountLocked.aggregateType)
        assertEquals("SECURITY", pwdChanged.aggregateType)

        assertEquals(DomainEventType.AUTH_SUCCEEDED, authSuccess.eventType)
        assertEquals(DomainEventType.AUTH_FAILED, authFail.eventType)
        assertEquals(DomainEventType.SESSION_CREATED, sessCreated.eventType)
        assertEquals(DomainEventType.SESSION_REVOKED, sessRevoked.eventType)
        assertEquals(DomainEventType.AUTHZ_DENIED, authzDenied.eventType)
        assertEquals(DomainEventType.ACCOUNT_LOCKED, accountLocked.eventType)
        assertEquals(DomainEventType.PASSWORD_CHANGED, pwdChanged.eventType)
    }
}
