package com.sucharu.sucharupro.ui.features.production.history

import com.sucharu.sucharupro.data.datasource.FakeProductionJobDataSource
import com.sucharu.sucharupro.data.repository.ProductionJobRepositoryImpl
import com.sucharu.sucharupro.domain.model.job.CompletionFilter
import com.sucharu.sucharupro.domain.model.job.ProductionHistorySortBy
import com.sucharu.sucharupro.domain.model.job.ProductionJob
import com.sucharu.sucharupro.domain.model.job.ProductionJobItem
import com.sucharu.sucharupro.domain.model.job.ProductionJobStage
import com.sucharu.sucharupro.domain.model.job.ProductionJobStatus
import com.sucharu.sucharupro.domain.model.order.OrderPriority
import com.sucharu.sucharupro.domain.repository.ProductionJobRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ProductionHistoryViewModelTest {

    private lateinit var dataSource: FakeProductionJobDataSource
    private lateinit var repository: ProductionJobRepository
    private lateinit var viewModel: ProductionHistoryViewModel

    private val sampleJob1 = ProductionJob(
        jobId = "job-vm-hist-01",
        jobNumber = "JOB-2026-VMHIST01",
        orderId = "ord-01",
        orderNumber = "ORD-2026-0001",
        handoffId = "hnd-01",
        customerId = "cust-01",
        title = "পুস্তিকা মুদ্রণ",
        quantity = 500,
        unit = "কপি",
        priority = OrderPriority.NORMAL,
        status = ProductionJobStatus.DELIVERED,
        items = listOf(
            ProductionJobItem(
                itemId = "item-01",
                description = "পুস্তিকা মুদ্রণ",
                quantity = 500,
                unit = "কপি"
            )
        ),
        stages = ProductionJobStage.createInitialStages("job-vm-hist-01"),
        createdAt = "2026-08-16T10:00:00Z",
        updatedAt = "2026-08-16T12:00:00Z"
    )

    private val sampleJob2 = ProductionJob(
        jobId = "job-vm-hist-02",
        jobNumber = "JOB-2026-VMHIST02",
        orderId = "ord-02",
        orderNumber = "ORD-2026-0002",
        handoffId = "hnd-02",
        customerId = "cust-02",
        title = "পোস্টার ডিজাইন",
        quantity = 1000,
        unit = "কপি",
        priority = OrderPriority.URGENT,
        status = ProductionJobStatus.IN_PROGRESS,
        items = listOf(
            ProductionJobItem(
                itemId = "item-02",
                description = "পোস্টার ডিজাইন",
                quantity = 1000,
                unit = "কপি"
            )
        ),
        stages = ProductionJobStage.createInitialStages("job-vm-hist-02"),
        createdAt = "2026-08-16T11:00:00Z",
        updatedAt = "2026-08-16T11:00:00Z"
    )

    @Before
    fun setUp() {
        dataSource = FakeProductionJobDataSource()
        repository = ProductionJobRepositoryImpl(dataSource)
        viewModel = ProductionHistoryViewModel(
            repository = repository,
            externalScope = CoroutineScope(Dispatchers.Unconfined)
        )
    }

    @Test
    fun initialState_startsLoading_andEmitsSuccess() = runBlocking {
        repository.createJob(sampleJob1)

        val state = viewModel.uiState.first { it is ProductionHistoryUiState.Success }
        assertTrue(state is ProductionHistoryUiState.Success)
        val successState = state as ProductionHistoryUiState.Success
        assertEquals(1, successState.allSummaries.size)
        assertFalse(successState.isEmpty)
    }

    @Test
    fun banglaSearchQuery_filtersAccurately() = runBlocking {
        repository.createJob(sampleJob1)
        repository.createJob(sampleJob2)

        viewModel.setSearchQuery("পুস্তিকা")

        val state = viewModel.uiState.first {
            it is ProductionHistoryUiState.Success && it.searchQuery == "পুস্তিকা"
        } as ProductionHistoryUiState.Success
        assertEquals(1, state.filteredSummaries.size)
        assertEquals("JOB-2026-VMHIST01", state.filteredSummaries[0].jobNumber)
    }

    @Test
    fun statusAndPriorityFilter_filtersAccurately() = runBlocking {
        repository.createJob(sampleJob1)
        repository.createJob(sampleJob2)

        viewModel.setStatusFilter(ProductionJobStatus.IN_PROGRESS)
        viewModel.setPriorityFilter(OrderPriority.URGENT)

        val state = viewModel.uiState.first {
            it is ProductionHistoryUiState.Success && it.filter.status == ProductionJobStatus.IN_PROGRESS
        } as ProductionHistoryUiState.Success
        assertEquals(1, state.filteredSummaries.size)
        assertEquals("JOB-2026-VMHIST02", state.filteredSummaries[0].jobNumber)
    }

    @Test
    fun clearFilters_resetsState() = runBlocking {
        repository.createJob(sampleJob1)
        repository.createJob(sampleJob2)

        viewModel.setSearchQuery("পুস্তিকা")
        viewModel.setStatusFilter(ProductionJobStatus.DELIVERED)

        viewModel.clearFilters()

        val state = viewModel.uiState.first {
            it is ProductionHistoryUiState.Success && it.searchQuery.isEmpty() && it.filter.status == null
        } as ProductionHistoryUiState.Success
        assertEquals("", state.searchQuery)
        assertEquals(null, state.filter.status)
        assertEquals(2, state.filteredSummaries.size)
    }
}
