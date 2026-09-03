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

class AffiliateNotificationHandoffContractTest {

    private lateinit var fakeCommDs: FakeAffiliateCommunicationDataSource
    private lateinit var fakeAffDs: FakeAffiliateDataSource
    private lateinit var commRepo: AffiliateCommunicationRepositoryImpl
    private lateinit var affRepo: AffiliateRepositoryImpl
    private lateinit var service: AffiliateCommunicationServiceImpl

    private val tenantId = "tenant-handoff-001"
    private val affiliateId = "aff-handoff-001"
    private val recipientUserId = "user-handoff-123"

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
                    displayName = "Handoff Partner",
                    affiliateCode = "HANDOFF2026",
                    status = AffiliateStatus.ACTIVE
                )
            )
        }
    }

    @Test
    fun `getHandoffContract synthesizes immutable sealed contract`() = runBlocking {
        service.createCommunication(
            tenantId = tenantId,
            affiliateId = affiliateId,
            recipientUserId = recipientUserId,
            communicationType = AffiliateCommunicationType.GOVERNANCE,
            title = "Notice 1",
            message = "Body 1",
            actorUserId = "admin-1",
            actorRole = "ADMIN",
            correlationId = "CORR-H-1"
        )
        service.createCommunication(
            tenantId = tenantId,
            affiliateId = affiliateId,
            recipientUserId = recipientUserId,
            communicationType = AffiliateCommunicationType.SECURITY,
            title = "Notice 2",
            message = "Body 2",
            actorUserId = "admin-1",
            actorRole = "ADMIN",
            correlationId = "CORR-H-2"
        )

        val contract = service.getHandoffContract(tenantId, affiliateId, recipientUserId)

        assertEquals("v1.0.0", contract.contractVersion)
        assertEquals(tenantId, contract.tenantId)
        assertEquals(affiliateId, contract.affiliateId)
        assertEquals(recipientUserId, contract.userId)
        assertEquals(2L, contract.totalNotifications)
        assertEquals(2L, contract.unreadCount)
        assertEquals(2L, contract.deliveredCount)
        assertEquals(0L, contract.failedCount)
        assertTrue(contract.isReadOnly)
        assertFalse(contract.integritySealHash.isEmpty())

        // Verify mandatory forbidden actions
        assertTrue(contract.forbiddenAiActions.contains("DISCARD_MANDATORY_NOTIFICATIONS"))
        assertTrue(contract.forbiddenAiActions.contains("SUPPRESS_SECURITY_ALERTS"))
        assertTrue(contract.forbiddenAiActions.contains("BYPASS_AUDIT_LOGGING"))
    }

    @Test
    fun `handoff contract seal integrity changes when notifications are created`() = runBlocking {
        val contract1 = service.getHandoffContract(tenantId, affiliateId, recipientUserId)

        service.createCommunication(
            tenantId = tenantId,
            affiliateId = affiliateId,
            recipientUserId = recipientUserId,
            communicationType = AffiliateCommunicationType.APPLICATION,
            title = "Status Update",
            message = "Application verified",
            actorUserId = "admin-1",
            actorRole = "ADMIN",
            correlationId = "CORR-H-3"
        )

        val contract2 = service.getHandoffContract(tenantId, affiliateId, recipientUserId)

        assertNotEquals(contract1.integritySealHash, contract2.integritySealHash)
        assertEquals(contract1.totalNotifications + 1, contract2.totalNotifications)
    }

    @Test
    fun `governance status in contract reflects unread state`() = runBlocking {
        val c1 = service.getHandoffContract(tenantId, affiliateId, recipientUserId)
        assertEquals("FULLY_CURRENT", c1.governanceStatus)

        service.createCommunication(
            tenantId = tenantId,
            affiliateId = affiliateId,
            recipientUserId = recipientUserId,
            communicationType = AffiliateCommunicationType.GOVERNANCE,
            title = "Governance Notice",
            message = "New policy",
            actorUserId = "admin-1",
            actorRole = "ADMIN",
            correlationId = "CORR-H-4"
        )

        val c2 = service.getHandoffContract(tenantId, affiliateId, recipientUserId)
        assertEquals("HAS_UNREAD", c2.governanceStatus)
    }
}
