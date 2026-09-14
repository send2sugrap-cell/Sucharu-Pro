package com.sucharu.sucharupro.ui.customer.wall

import com.sucharu.sucharupro.data.api.model.AuthenticatedPrincipal
import com.sucharu.sucharupro.data.api.model.UserRole
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SucharuWallViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var viewModel: SucharuWallViewModel

    private val customerPrincipal = AuthenticatedPrincipal(
        userId = "cus-001",
        projectId = "PRJ-001",
        username = "customer_user",
        role = UserRole.CUSTOMER,
        customerId = "cus-001"
    )

    private val affiliatePrincipal = AuthenticatedPrincipal(
        userId = "aff-001",
        projectId = "PRJ-001",
        username = "affiliate_user",
        role = UserRole.AFFILIATE,
        affiliateId = "aff-001"
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        viewModel = SucharuWallViewModel()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testLoadWallFeed_customerRole_populatesCustomerPersonalActivities() = runTest {
        viewModel.loadWallFeed(customerPrincipal)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue("State must be Success for customer principal", state is SucharuWallUiState.Success)

        val successState = state as SucharuWallUiState.Success
        val feed = successState.feedData

        assertNotNull(feed.featuredOffer)
        assertTrue(feed.offers.isNotEmpty())
        assertTrue(feed.services.isNotEmpty())
        assertTrue(feed.products.isNotEmpty())
        assertTrue(feed.personalActivities.isNotEmpty())

        val firstActivity = feed.personalActivities.first()
        assertTrue("Customer personal activity must contain order/invoice context", firstActivity.title.contains("Order"))
    }

    @Test
    fun testLoadWallFeed_affiliateRole_populatesAffiliatePersonalActivities() = runTest {
        viewModel.loadWallFeed(affiliatePrincipal)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue("State must be Success for affiliate principal", state is SucharuWallUiState.Success)

        val successState = state as SucharuWallUiState.Success
        val feed = successState.feedData

        assertTrue(feed.personalActivities.isNotEmpty())
        val firstActivity = feed.personalActivities.first()
        assertTrue("Affiliate personal activity must contain referral/commission context", firstActivity.title.contains("Referral"))
    }
}
