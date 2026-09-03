package com.sucharu.sucharupro.domain.service.affiliate

import com.sucharu.sucharupro.data.datasource.affiliate.FakeAffiliateCommunicationDataSource
import com.sucharu.sucharupro.data.datasource.affiliate.FakeAffiliateDataSource
import com.sucharu.sucharupro.data.repository.affiliate.AffiliateCommunicationRepositoryImpl
import com.sucharu.sucharupro.data.repository.affiliate.AffiliateRepositoryImpl
import com.sucharu.sucharupro.domain.model.affiliate.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class AffiliateCommunicationServiceTest {

    private lateinit var fakeCommDs: FakeAffiliateCommunicationDataSource
    private lateinit var fakeAffDs: FakeAffiliateDataSource
    private lateinit var commRepo: AffiliateCommunicationRepositoryImpl
    private lateinit var affRepo: AffiliateRepositoryImpl
    private lateinit var service: AffiliateCommunicationServiceImpl

    private val tenantId = "test-tenant-001"
    private val affiliateId = "aff-001"
    private val recipientUserId = "user-123"
    private val actorId = "admin-1"

    @Before
    fun setUp() {
        fakeCommDs = FakeAffiliateCommunicationDataSource()
        fakeAffDs = FakeAffiliateDataSource()
        commRepo = AffiliateCommunicationRepositoryImpl(fakeCommDs)
        affRepo = AffiliateRepositoryImpl(fakeAffDs)
        service = AffiliateCommunicationServiceImpl(
            communicationRepository = commRepo,
            affiliateRepository = affRepo
        )

        runBlocking {
            fakeAffDs.saveAffiliate(
                AffiliateProfile(
                    tenantId = tenantId,
                    affiliateId = affiliateId,
                    userId = recipientUserId,
                    displayName = "Partner Alpha",
                    affiliateCode = "ALPHA2026",
                    status = AffiliateStatus.ACTIVE
                )
            )
        }
    }

    @Test
    fun `createCommunication creates record and audit entry idempotently`() = runBlocking {
        val record1 = service.createCommunication(
            tenantId = tenantId,
            affiliateId = affiliateId,
            recipientUserId = recipientUserId,
            communicationType = AffiliateCommunicationType.APPLICATION,
            title = "Application Received",
            message = "Your affiliate application is under review.",
            actorUserId = actorId,
            actorRole = "ADMIN",
            actorType = AffiliateActorType.SYSTEM,
            idempotencyKey = "KEY-IDEMP-001",
            correlationId = "CORR-001"
        )

        assertNotNull(record1.communicationId)
        assertEquals(AffiliateCommunicationType.APPLICATION, record1.communicationType)
        assertEquals(AffiliateCommunicationStatus.DELIVERED, record1.status)
        assertFalse(record1.isRead)

        // Repeat with same idempotencyKey returns exact same record
        val record2 = service.createCommunication(
            tenantId = tenantId,
            affiliateId = affiliateId,
            recipientUserId = recipientUserId,
            communicationType = AffiliateCommunicationType.APPLICATION,
            title = "Application Received",
            message = "Your affiliate application is under review.",
            actorUserId = actorId,
            actorRole = "ADMIN",
            actorType = AffiliateActorType.SYSTEM,
            idempotencyKey = "KEY-IDEMP-001",
            correlationId = "CORR-002"
        )

        assertEquals(record1.communicationId, record2.communicationId)

        // Verify audit record exists
        val audits = service.listAuditRecords(tenantId, affiliateId)
        assertTrue(audits.isNotEmpty())
        assertEquals("COMMUNICATION_CREATED", audits.first().action)
    }

    @Test
    fun `mandatory communication types bypass disabled preferences`() = runBlocking {
        // Disable all channels for SECURITY preference
        service.upsertPreference(
            tenantId = tenantId,
            affiliateId = affiliateId,
            userId = recipientUserId,
            communicationType = AffiliateCommunicationType.SECURITY,
            inAppEnabled = false,
            pushEnabled = false,
            emailEnabled = false,
            smsEnabled = false,
            actorUserId = recipientUserId,
            actorRole = "AFFILIATE",
            correlationId = "CORR-PREF-001"
        )

        val record = service.createCommunication(
            tenantId = tenantId,
            affiliateId = affiliateId,
            recipientUserId = recipientUserId,
            communicationType = AffiliateCommunicationType.SECURITY,
            title = "Security Alert",
            message = "Password reset initiated.",
            actorUserId = actorId,
            actorRole = "ADMIN",
            actorType = AffiliateActorType.SYSTEM,
            correlationId = "CORR-SEC-001"
        )

        assertTrue(record.communicationType.isMandatory)
        assertTrue(record.channelsJson.contains("IN_APP"))
    }

    @Test
    fun `markRead and markAllRead update statuses correctly`() = runBlocking {
        val rec1 = service.createCommunication(
            tenantId = tenantId,
            affiliateId = affiliateId,
            recipientUserId = recipientUserId,
            communicationType = AffiliateCommunicationType.PROGRAM,
            title = "Notice 1",
            message = "Message 1",
            actorUserId = actorId,
            actorRole = "ADMIN",
            correlationId = "CORR-1"
        )
        val rec2 = service.createCommunication(
            tenantId = tenantId,
            affiliateId = affiliateId,
            recipientUserId = recipientUserId,
            communicationType = AffiliateCommunicationType.PROGRAM,
            title = "Notice 2",
            message = "Message 2",
            actorUserId = actorId,
            actorRole = "ADMIN",
            correlationId = "CORR-2"
        )

        val unread1 = service.getUnreadCount(tenantId, affiliateId)
        assertEquals(2L, unread1.totalUnread)

        val readRec1 = service.markRead(tenantId, rec1.communicationId, recipientUserId, "AFFILIATE")
        assertTrue(readRec1.isRead)
        assertEquals(AffiliateCommunicationStatus.READ, readRec1.status)

        val unread2 = service.getUnreadCount(tenantId, affiliateId)
        assertEquals(1L, unread2.totalUnread)

        val markedCount = service.markAllRead(tenantId, affiliateId, recipientUserId, "AFFILIATE")
        assertEquals(1, markedCount)

        val unread3 = service.getUnreadCount(tenantId, affiliateId)
        assertEquals(0L, unread3.totalUnread)
    }

    @Test
    fun `governance summary and handoff contract calculate accurate metrics`() = runBlocking {
        service.createCommunication(
            tenantId = tenantId,
            affiliateId = affiliateId,
            recipientUserId = recipientUserId,
            communicationType = AffiliateCommunicationType.GOVERNANCE,
            title = "Policy Update",
            message = "Terms have been updated.",
            actorUserId = actorId,
            actorRole = "ADMIN",
            correlationId = "CORR-GOV-001"
        )

        val summary = service.getGovernanceSummary(tenantId)
        assertEquals(1L, summary.totalCommunications)
        assertEquals(1L, summary.deliveredCount)

        val handoff = service.getHandoffContract(tenantId, affiliateId, recipientUserId)
        assertEquals(tenantId, handoff.tenantId)
        assertEquals(affiliateId, handoff.affiliateId)
        assertNotNull(handoff.integritySealHash)
        assertTrue(handoff.forbiddenAiActions.contains("DISCARD_MANDATORY_NOTIFICATIONS"))
    }
}
