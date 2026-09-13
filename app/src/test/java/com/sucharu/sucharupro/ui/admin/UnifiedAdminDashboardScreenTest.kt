package com.sucharu.sucharupro.ui.admin

import com.sucharu.sucharupro.data.api.model.AuthenticatedPrincipal
import com.sucharu.sucharupro.data.api.model.UserRole
import com.sucharu.sucharupro.data.repository.FakeDashboardRepository
import com.sucharu.sucharupro.domain.model.production.ProductionStageType
import com.sucharu.sucharupro.ui.features.dashboard.DashboardUiState
import com.sucharu.sucharupro.ui.features.dashboard.DashboardViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
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
class UnifiedAdminDashboardScreenTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var repository: FakeDashboardRepository
    private lateinit var viewModel: DashboardViewModel

    private val adminPrincipal = AuthenticatedPrincipal(
        userId = "adm-001",
        projectId = "PRJ-001",
        username = "admin_user",
        role = UserRole.ADMIN
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        repository = FakeDashboardRepository()
        viewModel = DashboardViewModel(repository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testDashboardViewModel_loadsSummaryAndReachesSuccessState() = runTest {
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue("Dashboard state must be Success when summary is loaded", state is DashboardUiState.Success)

        val successState = state as DashboardUiState.Success
        val summary = successState.summary

        assertNotNull(summary.shopHeader)
        assertNotNull(summary.kpis)
        assertNotNull(summary.paymentBreakdown)
        assertTrue(summary.stageCounts.isNotEmpty())
        assertEquals(13, ProductionStageType.orderedStages.size)
    }

    @Test
    fun testDashboardSummary_canonicalStagePipelineCount() = runTest {
        val summary = repository.getDashboardSummary().first()
        val stageCounts = summary.stageCounts

        assertTrue("Stage counts must cover canonical production pipeline", stageCounts.isNotEmpty())
        val printingStage = stageCounts.find { it.stage == ProductionStageType.PRINTING }
        assertNotNull(printingStage)
        assertTrue(printingStage!!.count >= 0)
    }

    @Test
    fun testDashboardSummary_3WayFinanceBreakdown() = runTest {
        val summary = repository.getDashboardSummary().first()
        val breakdown = summary.paymentBreakdown

        assertNotNull(breakdown.totalInvoicedToday)
        assertNotNull(breakdown.totalCollectedToday)
        assertNotNull(breakdown.totalOutstandingDue)
    }
}
