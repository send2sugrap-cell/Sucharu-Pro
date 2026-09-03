package com.sucharu.sucharupro.domain.service.affiliate

import com.sucharu.sucharupro.domain.model.affiliate.*
import com.sucharu.sucharupro.domain.model.notification.NotificationChannel
import org.junit.Assert.*
import org.junit.Test
import kotlinx.coroutines.runBlocking

class AffiliateNotificationPolicyTest {

    @Test
    fun `resolveChannels applies default channels when preferences are null`() {
        val mandatoryChannels = AffiliateCommunicationPolicyEngine.resolveChannels(
            communicationType = AffiliateCommunicationType.GOVERNANCE,
            preference = null
        )
        assertEquals(listOf(NotificationChannel.IN_APP), mandatoryChannels)

        val nonMandatoryChannels = AffiliateCommunicationPolicyEngine.resolveChannels(
            communicationType = AffiliateCommunicationType.PROGRAM,
            preference = null
        )
        assertEquals(listOf(NotificationChannel.IN_APP, NotificationChannel.PUSH), nonMandatoryChannels)
    }

    @Test
    fun `resolveChannels respects affiliate preference settings`() {
        val pref = AffiliateNotificationPreference(
            preferenceId = "pref-1",
            tenantId = "tenant-1",
            affiliateId = "aff-1",
            userId = "user-1",
            communicationType = AffiliateCommunicationType.PROGRAM,
            inAppEnabled = true,
            pushEnabled = false,
            emailEnabled = true,
            smsEnabled = false
        )

        val resolved = AffiliateCommunicationPolicyEngine.resolveChannels(
            communicationType = AffiliateCommunicationType.PROGRAM,
            preference = pref
        )

        assertTrue(resolved.contains(NotificationChannel.IN_APP))
        assertTrue(resolved.contains(NotificationChannel.EMAIL))
        assertFalse(resolved.contains(NotificationChannel.PUSH))
    }

    @Test
    fun `buildIntent constructs deterministic notification intent`() {
        val intent = AffiliateCommunicationPolicyEngine.buildIntent(
            tenantId = "tenant-1",
            affiliateId = "aff-1",
            recipientUserId = "user-1",
            communicationType = AffiliateCommunicationType.SECURITY,
            title = "Security Alert",
            message = "Unusual login attempt detected",
            idempotencyKey = "IDEMP-KEY-123",
            correlationId = "CORR-KEY-123"
        )

        assertEquals("tenant-1", intent.tenantId)
        assertEquals("aff-1", intent.affiliateId)
        assertEquals("user-1", intent.recipientUserId)
        assertEquals(AffiliateCommunicationType.SECURITY, intent.communicationType)
        assertEquals(listOf(NotificationChannel.IN_APP), intent.channels)
        assertEquals("IDEMP-KEY-123", intent.idempotencyKey)
        assertEquals("CORR-KEY-123", intent.correlationId)
    }

    @Test
    fun `generateIdempotencyKey produces consistent hash string`() {
        val key1 = AffiliateCommunicationPolicyEngine.generateIdempotencyKey(
            tenantId = "t1",
            affiliateId = "a1",
            communicationType = AffiliateCommunicationType.VERIFICATION,
            correlationId = "corr-99"
        )
        val key2 = AffiliateCommunicationPolicyEngine.generateIdempotencyKey(
            tenantId = "t1",
            affiliateId = "a1",
            communicationType = AffiliateCommunicationType.VERIFICATION,
            correlationId = "corr-99"
        )

        assertEquals(key1, key2)
        assertEquals(32, key1.length)
    }

    @Test
    fun `audit chain computation links records cryptographically`() = runBlocking {
        val recHash = AffiliateCommunicationPolicyEngine.computeAuditRecordHash(
            tenantId = "t1",
            auditId = "aud-1",
            affiliateId = "a1",
            communicationId = "c1",
            actorUserId = "user-admin",
            action = "COMMUNICATION_CREATED",
            previousStatus = null,
            newStatus = "DELIVERED",
            correlationId = "corr-1",
            timestamp = 1000000L
        )

        val chainHash1 = AffiliateCommunicationPolicyEngine.computeAuditChainHash(
            previousChainHash = null,
            recordHash = recHash
        )

        val chainHash2 = AffiliateCommunicationPolicyEngine.computeAuditChainHash(
            previousChainHash = chainHash1,
            recordHash = "another-record-hash"
        )

        assertNotEquals(chainHash1, chainHash2)
        assertFalse(chainHash1.isEmpty())
        assertFalse(chainHash2.isEmpty())
    }

    @Test
    fun `buildDefaultNotificationContent provides standard title and body for types`() {
        val (title, body) = AffiliateCommunicationPolicyEngine.buildDefaultNotificationContent(
            communicationType = AffiliateCommunicationType.VERIFICATION
        )

        assertEquals("Verification Result Available", title)
        assertTrue(body.contains("verification result"))
    }
}

