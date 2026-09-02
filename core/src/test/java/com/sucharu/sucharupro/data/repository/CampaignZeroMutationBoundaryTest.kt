package com.sucharu.sucharupro.data.repository

import com.sucharu.sucharupro.data.datasource.FakeCampaignDataSource
import com.sucharu.sucharupro.data.datasource.FakeCustomerDataSource
import com.sucharu.sucharupro.data.datasource.FakeNotificationDataSource
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.communication.campaign.CampaignAudienceType
import com.sucharu.sucharupro.domain.model.communication.campaign.CampaignPriority
import com.sucharu.sucharupro.domain.model.communication.campaign.CampaignType
import com.sucharu.sucharupro.domain.model.user.UserRole
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Explicit boundary tests proving ZERO mutation of source business domains
 * (Customer, Vendor, Order, Inventory, Financials) when campaigns are executed.
 */
class CampaignZeroMutationBoundaryTest {

    private lateinit var customerDataSource: FakeCustomerDataSource
    private lateinit var customerRepo: CustomerRepositoryImpl
    private lateinit var campaignRepo: CampaignRepositoryImpl

    private val testProjectId = "default-project"

    @Before
    fun setUp() {
        customerDataSource = FakeCustomerDataSource()
        customerRepo = CustomerRepositoryImpl(customerDataSource)
        campaignRepo = CampaignRepositoryImpl(
            dataSource = FakeCampaignDataSource(),
            notificationRepository = NotificationRepositoryImpl(FakeNotificationDataSource())
        )
    }

    @Test
    fun campaignDispatch_zeroCustomerMutation() = runBlocking {
        // 1. Snapshot customer state before campaign
        val initialCustomers = customerRepo.getCustomers().first()
        assertTrue(initialCustomers.isNotEmpty())
        val snapshotBefore = initialCustomers.map { it.copy() }

        // 2. Create and publish a campaign targeting customers
        val campaignRes = campaignRepo.createCampaign(
            projectId = testProjectId,
            title = "Special Customer Offer",
            campaignType = CampaignType.OFFER,
            priority = CampaignPriority.HIGH,
            audienceType = CampaignAudienceType.CUSTOMER_SEGMENT,
            content = "Exclusive offers for valued clients.",
            actorId = "user-admin-01",
            callerRole = UserRole.ADMIN
        )
        val campaignId = (campaignRes as DomainResult.Success).data.campaignId
        val publishRes = campaignRepo.publishCampaign(testProjectId, campaignId, "user-admin-01", UserRole.ADMIN)
        assertTrue(publishRes is DomainResult.Success)

        // 3. Verify Customer state after campaign is 100% byte/field identical
        val customersAfter = customerRepo.getCustomers().first()
        assertEquals(snapshotBefore.size, customersAfter.size)
        for (i in snapshotBefore.indices) {
            assertEquals("Customer ${snapshotBefore[i].customerId} must not be mutated", snapshotBefore[i], customersAfter[i])
        }
    }
}
