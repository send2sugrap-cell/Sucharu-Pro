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

class AffiliateNotificationSecurityEdgeTest {

    private lateinit var fakeCommDs: FakeAffiliateCommunicationDataSource
    private lateinit var fakeAffDs: FakeAffiliateDataSource
    private lateinit var commRepo: AffiliateCommunicationRepositoryImpl
    private lateinit var affRepo: AffiliateRepositoryImpl
    private lateinit var service: AffiliateCommunicationServiceImpl

    private val tenantA = "tenant-A"
    private val tenantB = "tenant-B"
    private val affiliateA = "aff-A"
    private val affiliateB = "aff-B"
    private val userA = "user-A"
    private val userB = "user-B"

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
                    tenantId = tenantA,
                    affiliateId = affiliateA,
                    userId = userA,
                    displayName = "Affiliate A",
                    affiliateCode = "CODEA",
                    status = AffiliateStatus.ACTIVE
                )
            )
            fakeAffDs.saveAffiliate(
                AffiliateProfile(
                    tenantId = tenantB,
                    affiliateId = affiliateB,
                    userId = userB,
                    displayName = "Affiliate B",
                    affiliateCode = "CODEB",
                    status = AffiliateStatus.ACTIVE
                )
            )
        }
    }

    @Test
    fun `cross-tenant queries are strictly isolated`() = runBlocking {
        val recA = service.createCommunication(
            tenantId = tenantA,
            affiliateId = affiliateA,
            recipientUserId = userA,
            communicationType = AffiliateCommunicationType.PROGRAM,
            title = "Notice A",
            message = "Message A",
            actorUserId = "admin-1",
            actorRole = "ADMIN",
            correlationId = "CORR-A"
        )

        // Querying tenant B should return null or empty list
        val recFromB = service.findCommunicationById(tenantB, recA.communicationId)
        assertNull(recFromB)

        val commsB = service.listCommunications(tenantB, affiliateB)
        assertTrue(commsB.isEmpty())
    }

    @Test
    fun `cannot mark read for non-existent or cross-tenant communication`() = runBlocking {
        val recA = service.createCommunication(
            tenantId = tenantA,
            affiliateId = affiliateA,
            recipientUserId = userA,
            communicationType = AffiliateCommunicationType.PROGRAM,
            title = "Notice A",
            message = "Message A",
            actorUserId = "admin-1",
            actorRole = "ADMIN",
            correlationId = "CORR-A"
        )

        try {
            service.markRead(tenantB, recA.communicationId, userB, "AFFILIATE")
            fail("Expected NoSuchElementException for cross-tenant markRead")
        } catch (e: NoSuchElementException) {
            assertTrue(e.message?.contains("not found") == true)
        }
    }

    @Test
    fun `createCommunication for non-existent affiliate throws Exception`() = runBlocking {
        try {
            service.createCommunication(
                tenantId = tenantA,
                affiliateId = "non-existent-affiliate",
                recipientUserId = "user-unknown",
                communicationType = AffiliateCommunicationType.SYSTEM,
                title = "Test",
                message = "Test",
                actorUserId = "admin-1",
                actorRole = "ADMIN",
                correlationId = "CORR-X"
            )
            fail("Expected NoSuchElementException for invalid affiliate")
        } catch (e: NoSuchElementException) {
            assertTrue(e.message?.contains("not found") == true)
        }
    }

    @Test
    fun `audit logs reflect actor identity and role accurately`() = runBlocking {
        service.createCommunication(
            tenantId = tenantA,
            affiliateId = affiliateA,
            recipientUserId = userA,
            communicationType = AffiliateCommunicationType.SECURITY,
            title = "Security Alert",
            message = "Login attempt from new location",
            actorUserId = "sec-agent-77",
            actorRole = "SECURITY_SYSTEM",
            actorType = AffiliateActorType.SYSTEM,
            correlationId = "CORR-SEC-77"
        )

        val audits = service.listAuditRecords(tenantA, affiliateA)
        val audit = audits.first()
        assertEquals("sec-agent-77", audit.actorUserId)
        assertEquals("SECURITY_SYSTEM", audit.actorRole)
        assertEquals(AffiliateActorType.SYSTEM, audit.actorType)
    }
}
