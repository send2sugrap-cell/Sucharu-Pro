package com.sucharu.sucharupro.ui.affiliate.myarea

import com.sucharu.sucharupro.data.api.model.AuthenticatedPrincipal
import com.sucharu.sucharupro.data.api.model.UserRole
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AffiliateMyAreaViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var viewModel: AffiliateMyAreaViewModel

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
        viewModel = AffiliateMyAreaViewModel()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testLoadMyArea_affiliatePrincipal_populatesBusinessSummary() = runTest {
        viewModel.loadMyArea(affiliatePrincipal)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue("State must be Success for affiliate principal", state is AffiliateMyAreaUiState.Success)

        val successState = state as AffiliateMyAreaUiState.Success
        val summary = successState.summary

        assertNotNull(summary.profile)
        assertNotNull(summary.referrals)
        assertNotNull(summary.performance)
        assertNotNull(summary.wallet)

        assertEquals("aff-001", summary.profile.affiliateId)
        assertEquals("APEX2026", summary.profile.affiliateCode)
        assertEquals("৳42,300.00", summary.wallet.availableToWithdrawFormatted)
        assertEquals("৳142,500.00", summary.performance.totalEarnedFormatted)
    }

    @Test
    fun testRefresh_updatesStateCleanly() = runTest {
        viewModel.refresh(affiliatePrincipal)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is AffiliateMyAreaUiState.Success)
        val summary = (state as AffiliateMyAreaUiState.Success).summary
        assertNotNull(summary.profile)
    }
}
