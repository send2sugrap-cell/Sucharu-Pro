package com.sucharu.sucharupro.domain.service.affiliate

import com.sucharu.sucharupro.data.datasource.affiliate.FakeAffiliateCommunicationDataSource
import com.sucharu.sucharupro.data.datasource.affiliate.FakeAffiliateDataSource
import com.sucharu.sucharupro.data.repository.affiliate.AffiliateCommunicationRepositoryImpl
import com.sucharu.sucharupro.data.repository.affiliate.AffiliateRepositoryImpl
import com.sucharu.sucharupro.domain.model.affiliate.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class AffiliateNotificationConcurrencyTest {

    private lateinit var fakeCommDs: FakeAffiliateCommunicationDataSource
    private lateinit var fakeAffDs: FakeAffiliateDataSource
    private lateinit var commRepo: AffiliateCommunicationRepositoryImpl
    private lateinit var affRepo: AffiliateRepositoryImpl
    private lateinit var service: AffiliateCommunicationServiceImpl

    private val tenantId = "tenant-conc-001"
    private val affiliateId = "aff-conc-001"
    private val recipientUserId = "user-conc-123"

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
                    displayName = "Concurrent Partner",
                    affiliateCode = "CONC2026",
                    status = AffiliateStatus.ACTIVE
                )
            )
        }
    }

    @Test
    fun `concurrent communication creation maintains thread safety and integrity`() = runBlocking {
        val jobs = (1..50).map { i ->
            async(Dispatchers.Default) {
                service.createCommunication(
                    tenantId = tenantId,
                    affiliateId = affiliateId,
                    recipientUserId = recipientUserId,
                    communicationType = AffiliateCommunicationType.PROGRAM,
                    title = "Notice $i",
                    message = "Message $i",
                    actorUserId = "admin-1",
                    actorRole = "ADMIN",
                    correlationId = "CORR-CONC-$i"
                )
            }
        }
        val records = jobs.awaitAll()
        assertEquals(50, records.size)

        val list = service.listCommunications(tenantId, affiliateId)
        assertEquals(50, list.size)

        val unread = service.getUnreadCount(tenantId, affiliateId)
        assertEquals(50L, unread.totalUnread)
    }

    @Test
    fun `concurrent markRead and preference updates perform safely`() = runBlocking {
        // Create 20 notifications first
        val created = (1..20).map { i ->
            service.createCommunication(
                tenantId = tenantId,
                affiliateId = affiliateId,
                recipientUserId = recipientUserId,
                communicationType = AffiliateCommunicationType.PROGRAM,
                title = "Item $i",
                message = "Content $i",
                actorUserId = "admin-1",
                actorRole = "ADMIN",
                correlationId = "CORR-PRE-$i"
            )
        }

        // Concurrently mark them read and update preferences
        val readJobs = created.map { rec ->
            async(Dispatchers.Default) {
                service.markRead(tenantId, rec.communicationId, recipientUserId, "AFFILIATE")
            }
        }

        val prefJobs = (1..10).map { i ->
            async(Dispatchers.Default) {
                service.upsertPreference(
                    tenantId = tenantId,
                    affiliateId = affiliateId,
                    userId = recipientUserId,
                    communicationType = AffiliateCommunicationType.PROGRAM,
                    inAppEnabled = i % 2 == 0,
                    pushEnabled = i % 2 == 1,
                    emailEnabled = true,
                    smsEnabled = false,
                    actorUserId = recipientUserId,
                    actorRole = "AFFILIATE",
                    correlationId = "CORR-PREF-CONC-$i"
                )
            }
        }

        readJobs.awaitAll()
        prefJobs.awaitAll()

        val unread = service.getUnreadCount(tenantId, affiliateId)
        assertEquals(0L, unread.totalUnread)

        val prefs = service.getPreferences(tenantId, affiliateId)
        assertTrue(prefs.isNotEmpty())
    }
}
