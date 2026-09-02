package com.sucharu.sucharupro.ui.features.production.monitoring

import com.sucharu.sucharupro.data.datasource.FakeProductionJobDataSource
import com.sucharu.sucharupro.data.repository.ProductionJobRepositoryImpl
import com.sucharu.sucharupro.domain.model.job.AttentionReasonType
import com.sucharu.sucharupro.domain.model.job.ProductionJob
import com.sucharu.sucharupro.domain.model.job.ProductionJobItem
import com.sucharu.sucharupro.domain.model.job.ProductionJobStage
import com.sucharu.sucharupro.domain.model.job.ProductionJobStatus
import com.sucharu.sucharupro.domain.model.order.OrderPriority
import com.sucharu.sucharupro.domain.repository.ProductionJobRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * ViewModel unit tests for Live Production Monitoring Dashboard (Module 04 Step 07).
 */
class ProductionMonitoringViewModelTest {

    private lateinit var dataSource: FakeProductionJobDataSource
    private lateinit var repository: ProductionJobRepository
    private lateinit var viewModel: ProductionMonitoringDashboardViewModel

    private val sampleJob = ProductionJob(
        jobId = "job-vm-mon-01",
        jobNumber = "JOB-2026-VMMON01",
        orderId = "ord-vm-01",
        orderNumber = "ORD-2026-VM01",
        customerId = "cus-vm-01",
        handoffId = "hnd-vm-01",
        title = "পুস্তিকা মুদ্রণ ও বাঁধাই",
        quantity = 1500,
        unit = "কপি",
        priority = OrderPriority.URGENT,
        status = ProductionJobStatus.READY_FOR_PRODUCTION,
        stages = ProductionJobStage.createInitialStages("job-vm-mon-01"),
        createdAt = "2026-08-16T10:00:00Z",
        updatedAt = "2026-08-16T10:00:00Z"
    )

    private val stage1Id = sampleJob.stages[0].stageId

    @Before
    fun setUp() {
        dataSource = FakeProductionJobDataSource()
        repository = ProductionJobRepositoryImpl(dataSource)
        viewModel = ProductionMonitoringDashboardViewModel(
            repository = repository,
            externalScope = CoroutineScope(Dispatchers.Unconfined)
        )
    }

    @Test
    fun initialState_startsLoading_andEmitsSuccess() = runBlocking {
        repository.createJob(sampleJob)

        val state = viewModel.uiState.value
        assertTrue("Expected Success state, got $state", state is ProductionMonitoringDashboardUiState.Success)
        val success = state as ProductionMonitoringDashboardUiState.Success
        assertEquals(1, success.snapshot.totalJobs)
        assertEquals(1, success.snapshot.activeJobs)
        assertEquals(1, success.snapshot.readyForProductionJobs)
    }

    @Test
    fun filterSelection_filtersActiveStagesAndAttentionItems() = runBlocking {
        repository.createJob(sampleJob)
        repository.holdJob(sampleJob.jobId, reason = "Paper delay", timestamp = "2026-08-16T10:00:00Z")

        viewModel.setFilter(ProductionMonitoringFilter.ON_HOLD)

        val state = viewModel.uiState.value as ProductionMonitoringDashboardUiState.Success
        assertEquals(ProductionMonitoringFilter.ON_HOLD, state.filter)
        assertEquals(1, state.filteredAttentionItems.size)
        assertEquals(AttentionReasonType.ON_HOLD_JOB, state.filteredAttentionItems[0].reasonType)
    }

    @Test
    fun banglaSearchQuery_filtersAccurately() = runBlocking {
        repository.createJob(sampleJob)

        viewModel.setSearchQuery("পুস্তিকা")

        val state = viewModel.uiState.value as ProductionMonitoringDashboardUiState.Success
        assertEquals("পুস্তিকা", state.searchQuery)
        assertTrue(state.filteredAttentionItems.isNotEmpty())
    }

    @Test
    fun caseInsensitiveSearch_matchesJobNumber() = runBlocking {
        repository.createJob(sampleJob)

        viewModel.setSearchQuery("vmmon01")

        val state = viewModel.uiState.value as ProductionMonitoringDashboardUiState.Success
        assertTrue(state.filteredAttentionItems.any { it.jobNumber == sampleJob.jobNumber })
    }

    @Test
    fun clearFilters_resetsFilterAndQuery() = runBlocking {
        repository.createJob(sampleJob)
        viewModel.setFilter(ProductionMonitoringFilter.URGENT)
        viewModel.setSearchQuery("test")

        viewModel.clearFilters()

        val state = viewModel.uiState.value as ProductionMonitoringDashboardUiState.Success
        assertEquals(ProductionMonitoringFilter.ALL, state.filter)
        assertEquals("", state.searchQuery)
    }

    @Test
    fun reactiveUpdate_refreshesUiStateAutomatically() = runBlocking {
        repository.createJob(sampleJob)
        repository.startStage(sampleJob.jobId, stage1Id, timestamp = "2026-08-16T10:00:00Z")

        val state = viewModel.uiState.value as ProductionMonitoringDashboardUiState.Success
        assertEquals(1, state.activeStages.size)
        assertEquals(sampleJob.jobNumber, state.activeStages[0].jobNumber)
    }
}
