package com.sucharu.sucharupro.data.notification.security

import com.sucharu.sucharupro.data.api.model.AuthenticatedPrincipal
import com.sucharu.sucharupro.data.api.model.PrincipalType
import com.sucharu.sucharupro.data.api.model.UserRole
import com.sucharu.sucharupro.domain.event.boundary.NotificationChannel
import com.sucharu.sucharupro.domain.event.boundary.NotificationIntent
import com.sucharu.sucharupro.domain.event.model.DomainEventType
import com.sucharu.sucharupro.domain.notification.security.NotificationDataClassification
import com.sucharu.sucharupro.domain.notification.security.NotificationSecurityContext
import com.sucharu.sucharupro.domain.notification.security.NotificationSecurityDecision
import com.sucharu.sucharupro.domain.notification.security.NotificationSecurityReason
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Tenant isolation security tests for notification dispatch (INFRA-04 Step 07).
 * Verifies that cross-tenant notifications are denied at the policy engine.
 */
class NotificationTenantIsolationTest {

    private lateinit var policy: NotificationSecurityPolicy

    private fun staffPrincipal(projectId: String) = AuthenticatedPrincipal(
        userId = "staff-001",
        projectId = projectId,
        username = "staff001",
        role = UserRole.STAFF,
        principalType = PrincipalType.HUMAN
    )

    private fun intent(projectId: String) = NotificationIntent(
        eventId = "evt-001",
        eventType = DomainEventType.ORDER_CREATED,
        projectId = projectId,
        targetRecipientId = "CUST-1",
        targetChannels = setOf(NotificationChannel.IN_APP),
        title = "Order Created",
        body = "Your order is confirmed.",
        correlationId = "corr-001"
    )

    @Before
    fun setUp() {
        policy = NotificationSecurityPolicy()
    }

    @Test
    fun test01_crossTenantIntent_isDenied() = runBlocking {
        val context = NotificationSecurityContext(
            principal = staffPrincipal("tenant-A"),
            projectId = "tenant-A",
            intent = intent("tenant-B"),
            classification = NotificationDataClassification.PUBLIC,
            correlationId = "corr-001",
            requestId = "req-001"
        )

        val decision = policy.evaluateDispatch(context)
        assertTrue("Expected Deny for cross-tenant intent", decision is NotificationSecurityDecision.Deny)
        val deny = decision as NotificationSecurityDecision.Deny
        assertEquals(NotificationSecurityReason.TENANT_MISMATCH, deny.reason)
    }

    @Test
    fun test02_crossTenantPrincipal_isDenied() = runBlocking {
        val context = NotificationSecurityContext(
            principal = staffPrincipal("tenant-B"),
            projectId = "tenant-A",
            intent = intent("tenant-A"),
            classification = NotificationDataClassification.PUBLIC,
            correlationId = "corr-002",
            requestId = "req-002"
        )

        val decision = policy.evaluateDispatch(context)
        assertTrue("Expected Deny for cross-tenant principal", decision is NotificationSecurityDecision.Deny)
        val deny = decision as NotificationSecurityDecision.Deny
        assertEquals(NotificationSecurityReason.TENANT_MISMATCH, deny.reason)
    }

    @Test
    fun test03_nullPrincipal_isUnauthenticatedDeny() = runBlocking {
        val context = NotificationSecurityContext(
            principal = null,
            projectId = "tenant-A",
            intent = intent("tenant-A"),
            classification = NotificationDataClassification.PUBLIC,
            correlationId = "corr-003",
            requestId = "req-003"
        )

        val decision = policy.evaluateDispatch(context)
        assertTrue("Expected Deny for unauthenticated principal", decision is NotificationSecurityDecision.Deny)
        val deny = decision as NotificationSecurityDecision.Deny
        assertEquals(NotificationSecurityReason.UNAUTHENTICATED_PRINCIPAL, deny.reason)
    }

    @Test
    fun test04_sameTenantStaff_isAllowed() = runBlocking {
        val context = NotificationSecurityContext(
            principal = staffPrincipal("tenant-A"),
            projectId = "tenant-A",
            intent = intent("tenant-A"),
            classification = NotificationDataClassification.PUBLIC,
            correlationId = "corr-004",
            requestId = "req-004"
        )

        val decision = policy.evaluateDispatch(context)
        assertTrue("Expected Allow for same-tenant staff", decision is NotificationSecurityDecision.Allow)
    }
}
