package com.sucharu.sucharupro.data.repository

import com.sucharu.sucharupro.data.datasource.FakeCampaignDataSource
import com.sucharu.sucharupro.data.datasource.FakeNotificationDataSource
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.communication.campaign.*
import com.sucharu.sucharupro.domain.model.user.UserRole
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class CampaignRepositoryTest {

    private lateinit var campaignDataSource: FakeCampaignDataSource
    private lateinit var notificationRepo: NotificationRepositoryImpl
    private lateinit var repository: CampaignRepositoryImpl

    private val testProjectId = "proj-test-01"

    @Before
    fun setUp() {
        campaignDataSource = FakeCampaignDataSource()
        notificationRepo = NotificationRepositoryImpl(FakeNotificationDataSource())
        repository = CampaignRepositoryImpl(
            dataSource = campaignDataSource,
            notificationRepository = notificationRepo
        )
    }

    @Test
    fun createCampaign_withValidInputs_createsDraftAndRecordsAudit() = runBlocking {
        val result = repository.createCampaign(
            projectId = testProjectId,
            title = "Special Discount",
            description = "10% off for retail customers",
            campaignType = CampaignType.PROMOTION,
            priority = CampaignPriority.NORMAL,
            audienceType = CampaignAudienceType.CUSTOMER_SEGMENT,
            content = "Visit our showroom for special discounts!",
            actorId = "user-admin-01",
            callerRole = UserRole.ADMIN
        )

        assertTrue(result is DomainResult.Success)
        val campaign = (result as DomainResult.Success).data
        assertEquals(CampaignStatus.DRAFT, campaign.status)
        assertEquals("Special Discount", campaign.title)

        // Verify audit event
        val audits = repository.getActivityEvents(testProjectId, campaign.campaignId, "user-admin-01", UserRole.ADMIN)
        assertTrue(audits is DomainResult.Success)
        assertEquals(1, (audits as DomainResult.Success).data.size)
        assertEquals(CampaignActivityEventType.CAMPAIGN_CREATED, audits.data[0].eventType)
    }

    @Test
    fun createCampaign_idempotencyKey_returnsExistingWithoutDuplicating() = runBlocking {
        val idempotencyKey = "idem-key-campaign-123"

        val res1 = repository.createCampaign(
            projectId = testProjectId,
            title = "First Attempt",
            campaignType = CampaignType.GENERAL,
            audienceType = CampaignAudienceType.ALL_PROJECT_USERS,
            content = "Hello world",
            idempotencyKey = idempotencyKey,
            actorId = "user-admin-01",
            callerRole = UserRole.ADMIN
        )
        assertTrue(res1 is DomainResult.Success)
        val id1 = (res1 as DomainResult.Success).data.campaignId

        val res2 = repository.createCampaign(
            projectId = testProjectId,
            title = "First Attempt",
            campaignType = CampaignType.GENERAL,
            audienceType = CampaignAudienceType.ALL_PROJECT_USERS,
            content = "Hello world",
            idempotencyKey = idempotencyKey,
            actorId = "user-admin-01",
            callerRole = UserRole.ADMIN
        )
        assertTrue(res2 is DomainResult.Success)
        val id2 = (res2 as DomainResult.Success).data.campaignId

        assertEquals("Idempotent retry must return the exact same campaign ID", id1, id2)
    }

    @Test
    fun publishCampaign_resolvesAudience_andDispatchesCanonicalNotifications() = runBlocking {
        val createRes = repository.createCampaign(
            projectId = testProjectId,
            title = "Customer Announcement",
            campaignType = CampaignType.ANNOUNCEMENT,
            audienceType = CampaignAudienceType.CUSTOMER_SEGMENT,
            content = "Dear valued customer, our catalog is updated.",
            actorId = "user-admin-01",
            callerRole = UserRole.ADMIN
        )
        val campaignId = (createRes as DomainResult.Success).data.campaignId

        // Publish campaign
        val publishRes = repository.publishCampaign(
            projectId = testProjectId,
            campaignId = campaignId,
            actorId = "user-admin-01",
            callerRole = UserRole.ADMIN
        )
        assertTrue(publishRes is DomainResult.Success)
        val published = (publishRes as DomainResult.Success).data
        assertEquals(CampaignStatus.PUBLISHED, published.status)

        // Verify resolved recipients
        val recipientsRes = repository.getRecipients(testProjectId, campaignId, "user-admin-01", UserRole.ADMIN)
        assertTrue(recipientsRes is DomainResult.Success)
        val recipients = (recipientsRes as DomainResult.Success).data
        assertTrue(recipients.isNotEmpty())
        assertTrue(recipients.all { it.notificationId != null }) // Verified linked canonical notification
    }

    @Test
    fun concurrentPublish_dispatchesExactlyOnceWithoutDuplicateNotifications() = runBlocking {
        val createRes = repository.createCampaign(
            projectId = testProjectId,
            title = "Concurrent Campaign",
            campaignType = CampaignType.GENERAL,
            audienceType = CampaignAudienceType.ALL_PROJECT_USERS,
            content = "Testing concurrency safety.",
            actorId = "user-admin-01",
            callerRole = UserRole.ADMIN
        )
        val campaignId = (createRes as DomainResult.Success).data.campaignId

        // Simulate 5 simultaneous publish requests
        val jobs = (1..5).map {
            async {
                repository.publishCampaign(
                    projectId = testProjectId,
                    campaignId = campaignId,
                    actorId = "user-admin-01",
                    callerRole = UserRole.ADMIN
                )
            }
        }
        val results = jobs.awaitAll()
        assertTrue(results.all { it is DomainResult.Success })

        // Check recipients count (must not be multiplied by 5!)
        val recipients = (repository.getRecipients(testProjectId, campaignId, "user-admin-01", UserRole.ADMIN) as DomainResult.Success).data
        val candidates = campaignDataSource.getCandidateRecipients(testProjectId)
        assertEquals(candidates.filter { it.isActive }.size, recipients.size)
    }

    @Test
    fun projectIsolation_cannotAccessCampaignAcrossDifferentProject() = runBlocking {
        val createRes = repository.createCampaign(
            projectId = "proj-ALPHA",
            title = "Alpha Only Campaign",
            campaignType = CampaignType.GENERAL,
            audienceType = CampaignAudienceType.ALL_PROJECT_USERS,
            content = "Alpha project content",
            actorId = "user-admin-01",
            callerRole = UserRole.ADMIN
        )
        val campaignId = (createRes as DomainResult.Success).data.campaignId

        // Querying from proj-BETA should fail
        val accessFromBeta = repository.getCampaignById("proj-BETA", campaignId, "user-admin-01", UserRole.ADMIN)
        assertTrue(accessFromBeta is DomainResult.Error)
    }

    @Test
    fun recipientReadAndAcknowledge_updatesEngagementRates() = runBlocking {
        val createRes = repository.createCampaign(
            projectId = testProjectId,
            title = "Engagement Test",
            campaignType = CampaignType.GENERAL,
            audienceType = CampaignAudienceType.CUSTOMER_SEGMENT,
            content = "Read and acknowledge test",
            actorId = "user-admin-01",
            callerRole = UserRole.ADMIN
        )
        val campaignId = (createRes as DomainResult.Success).data.campaignId
        repository.publishCampaign(testProjectId, campaignId, "user-admin-01", UserRole.ADMIN)

        val recipients = (repository.getRecipients(testProjectId, campaignId, "user-admin-01", UserRole.ADMIN) as DomainResult.Success).data
        assertTrue(recipients.isNotEmpty())
        val firstRecipient = recipients[0]

        // Record Read
        val readRes = repository.recordRecipientRead(testProjectId, campaignId, firstRecipient.recipientId, firstRecipient.userId)
        assertTrue(readRes is DomainResult.Success)

        // Record Acknowledge
        val ackRes = repository.recordRecipientAcknowledged(testProjectId, campaignId, firstRecipient.recipientId, firstRecipient.userId)
        assertTrue(ackRes is DomainResult.Success)

        // Check engagement summary
        val engSummary = (repository.getEngagementSummary(testProjectId, campaignId, "user-admin-01", UserRole.ADMIN) as DomainResult.Success).data
        assertTrue(engSummary.readRate > 0.0)
        assertTrue(engSummary.acknowledgementRate > 0.0)
    }

    @Test
    fun announcement_lifecycleAndPublishing_succeeds() = runBlocking {
        val createRes = repository.createAnnouncement(
            projectId = testProjectId,
            title = "Holiday Schedule",
            content = "Press will remain closed on Sunday.",
            priority = CampaignPriority.HIGH,
            actorId = "user-admin-01",
            callerRole = UserRole.ADMIN
        )
        assertTrue(createRes is DomainResult.Success)
        val ann = (createRes as DomainResult.Success).data
        assertEquals(CampaignStatus.DRAFT, ann.status)

        val pubRes = repository.publishAnnouncement(testProjectId, ann.announcementId, "user-admin-01", UserRole.ADMIN)
        assertTrue(pubRes is DomainResult.Success)
        assertEquals(CampaignStatus.PUBLISHED, (pubRes as DomainResult.Success).data.status)
    }

    @Test
    fun broadcast_immediateDispatch_succeeds() = runBlocking {
        val sendRes = repository.sendBroadcast(
            projectId = testProjectId,
            title = "Urgent: Ink Level Low",
            message = "Cyan ink level reached threshold on Machine 01.",
            priority = CampaignPriority.URGENT,
            audienceType = CampaignAudienceType.ROLE,
            actorId = "user-manager-01",
            callerRole = UserRole.MANAGER
        )
        assertTrue(sendRes is DomainResult.Success)
        val broadcast = (sendRes as DomainResult.Success).data
        assertEquals(CampaignStatus.PUBLISHED, broadcast.status)
        assertNotNull(broadcast.sentAt)
    }
}
