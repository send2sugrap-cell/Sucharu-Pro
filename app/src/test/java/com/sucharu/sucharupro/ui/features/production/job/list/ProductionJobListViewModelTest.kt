package com.sucharu.sucharupro.ui.features.production.job.list

import com.sucharu.sucharupro.data.datasource.FakeProductionJobDataSource
import com.sucharu.sucharupro.data.repository.ProductionJobRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.toMoney
import com.sucharu.sucharupro.domain.model.handoff.OrderJobHandoff
import com.sucharu.sucharupro.domain.model.handoff.OrderJobHandoffItem
import com.sucharu.sucharupro.domain.model.job.ProductionJob
import com.sucharu.sucharupro.domain.model.job.ProductionJobItem
import com.sucharu.sucharupro.domain.model.job.ProductionJobStage
import com.sucharu.sucharupro.domain.model.job.ProductionJobStatus
import com.sucharu.sucharupro.domain.model.order.OrderPriority
import com.sucharu.sucharupro.domain.model.production.ProductionStageStatus
import com.sucharu.sucharupro.domain.model.production.ProductionStageType
import com.sucharu.sucharupro.domain.repository.ProductionJobRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for [ProductionJobListViewModel] covering search, compound filtering,
 * sorting, empty states, and handoff conversion.
 */
class ProductionJobListViewModelTest {

    private lateinit var dataSource: FakeProductionJobDataSource
    private lateinit var repository: ProductionJobRepository
    private lateinit var viewModel: ProductionJobListViewModel

    private val job1 = ProductionJob(
        jobId = "job-list-01",
        jobNumber = "JOB-2026-0001",
        orderId = "ord-001",
        orderNumber = "ORD-2026-0001",
        customerId = "cus-001",
        handoffId = "hnd-001",
        title = "বাংলা ব্যাকরণ বই মুদ্রণ",
        specification = "চার কালার কভার",
        quantity = 2000,
        unit = "কপি",
        priority = OrderPriority.URGENT,
        status = ProductionJobStatus.IN_PROGRESS,
        stages = ProductionJobStage.createInitialStages("job-list-01").map {
            if (it.stageType == ProductionStageType.DESIGN) it.copy(status = ProductionStageStatus.COMPLETED)
            else if (it.stageType == ProductionStageType.APPROVAL) it.copy(status = ProductionStageStatus.IN_PROGRESS)
            else it
        },
        createdAt = "2026-08-16T10:00:00Z",
        updatedAt = "2026-08-16T10:00:00Z"
    )

    private val job2 = ProductionJob(
        jobId = "job-list-02",
        jobNumber = "JOB-2026-0002",
        orderId = "ord-002",
        orderNumber = "ORD-2026-0002",
        customerId = "cus-002",
        handoffId = "hnd-002",
        title = "ক্যালেন্ডার প্রিন্টিং",
        specification = "স্পাইরাল বাইন্ডিং",
        quantity = 500,
        unit = "Pcs",
        priority = OrderPriority.NORMAL,
        status = ProductionJobStatus.READY_FOR_PRODUCTION,
        stages = ProductionJobStage.createInitialStages("job-list-02"),
        createdAt = "2026-08-16T11:00:00Z",
        updatedAt = "2026-08-16T11:00:00Z"
    )

    private val job3 = ProductionJob(
        jobId = "job-list-03",
        jobNumber = "JOB-2026-0003",
        orderId = "ord-003",
        orderNumber = "ORD-2026-0003",
        customerId = "cus-003",
        handoffId = "hnd-003",
        title = "বিজনেস কার্ড প্রিন্টিং",
        specification = "ম্যাট লেমিনেশন",
        quantity = 1000,
        unit = "Pcs",
        priority = OrderPriority.HIGH,
        status = ProductionJobStatus.READY,
        stages = ProductionJobStage.createInitialStages("job-list-03").map {
            if (it.sequence <= 12) it.copy(status = ProductionStageStatus.COMPLETED) else it
        },
        createdAt = "2026-08-16T12:00:00Z",
        updatedAt = "2026-08-16T12:00:00Z"
    )

    @Before
    fun setUp() {
        dataSource = FakeProductionJobDataSource()
        repository = ProductionJobRepositoryImpl(dataSource)
        viewModel = ProductionJobListViewModel(
            repository = repository,
            externalScope = CoroutineScope(Dispatchers.Unconfined)
        )
    }

    @Test
    fun initialEmptyState_whenNoJobsExist() = runBlocking {
        val state = viewModel.uiState.value
        assertTrue("Expected Empty state, got $state", state is ProductionJobListUiState.Empty)
    }

    @Test
    fun loadJobs_emitsSuccessWithAllJobs() = runBlocking {
        repository.createJob(job1)
        repository.createJob(job2)
        repository.createJob(job3)

        viewModel.loadJobs()

        val state = viewModel.uiState.value
        assertTrue("Expected Success state, got $state", state is ProductionJobListUiState.Success)
        val success = state as ProductionJobListUiState.Success
        assertEquals(3, success.totalCount)
        assertEquals(3, success.visibleCount)
        assertFalse(success.isFiltered)
    }

    @Test
    fun search_isCaseInsensitive_andMatchesFields() = runBlocking {
        repository.createJob(job1)
        repository.createJob(job2)
        repository.createJob(job3)
        viewModel.loadJobs()

        // Match by title (Bangla Unicode)
        viewModel.onSearchQueryChange("  বাংলা  ")
        var success = viewModel.uiState.value as ProductionJobListUiState.Success
        assertEquals(1, success.visibleCount)
        assertEquals("JOB-2026-0001", success.visibleJobs[0].jobNumber)

        // Match by Job Number (case insensitive)
        viewModel.onSearchQueryChange("job-2026-0002")
        success = viewModel.uiState.value as ProductionJobListUiState.Success
        assertEquals(1, success.visibleCount)
        assertEquals("JOB-2026-0002", success.visibleJobs[0].jobNumber)

        // Match by Order Number
        viewModel.onSearchQueryChange("ORD-2026-0003")
        success = viewModel.uiState.value as ProductionJobListUiState.Success
        assertEquals(1, success.visibleCount)
        assertEquals("JOB-2026-0003", success.visibleJobs[0].jobNumber)
    }

    @Test
    fun statusAndPriorityFilter_worksCorrectly() = runBlocking {
        repository.createJob(job1)
        repository.createJob(job2)
        repository.createJob(job3)
        viewModel.loadJobs()

        // Filter by Status: IN_PROGRESS
        viewModel.onStatusFilterChange(ProductionJobStatus.IN_PROGRESS)
        var success = viewModel.uiState.value as ProductionJobListUiState.Success
        assertEquals(1, success.visibleCount)
        assertEquals("JOB-2026-0001", success.visibleJobs[0].jobNumber)

        // Filter by Priority: HIGH
        viewModel.onStatusFilterChange(null)
        viewModel.onPriorityFilterChange(OrderPriority.HIGH)
        success = viewModel.uiState.value as ProductionJobListUiState.Success
        assertEquals(1, success.visibleCount)
        assertEquals("JOB-2026-0003", success.visibleJobs[0].jobNumber)
    }

    @Test
    fun compoundFilters_useLogicalAnd() = runBlocking {
        repository.createJob(job1)
        repository.createJob(job2)
        repository.createJob(job3)
        viewModel.loadJobs()

        // Status: IN_PROGRESS AND Priority: URGENT -> Match job1
        viewModel.onStatusFilterChange(ProductionJobStatus.IN_PROGRESS)
        viewModel.onPriorityFilterChange(OrderPriority.URGENT)
        var success = viewModel.uiState.value as ProductionJobListUiState.Success
        assertEquals(1, success.visibleCount)

        // Status: IN_PROGRESS AND Priority: NORMAL -> 0 matches
        viewModel.onPriorityFilterChange(OrderPriority.NORMAL)
        success = viewModel.uiState.value as ProductionJobListUiState.Success
        assertEquals(0, success.visibleCount)
        assertTrue(success.isFiltered)

        // Clear filters
        viewModel.clearFilters()
        success = viewModel.uiState.value as ProductionJobListUiState.Success
        assertEquals(3, success.visibleCount)
        assertFalse(success.isFiltered)
    }

    @Test
    fun stageFilter_filtersByCurrentStage() = runBlocking {
        repository.createJob(job1) // current stage is APPROVAL
        repository.createJob(job2) // current stage is DESIGN
        repository.createJob(job3) // current stage is DELIVERED
        viewModel.loadJobs()

        viewModel.onStageFilterChange(ProductionStageType.APPROVAL)
        val success = viewModel.uiState.value as ProductionJobListUiState.Success
        assertEquals(1, success.visibleCount)
        assertEquals("JOB-2026-0001", success.visibleJobs[0].jobNumber)
    }

    @Test
    fun sorting_prioritizesUrgentFirst_thenHigh_thenNormal() = runBlocking {
        repository.createJob(job2) // NORMAL
        repository.createJob(job3) // HIGH
        repository.createJob(job1) // URGENT
        viewModel.loadJobs()

        viewModel.onSortOrderChange(ProductionJobSortOrder.PRIORITY_DESC)
        val success = viewModel.uiState.value as ProductionJobListUiState.Success
        assertEquals("JOB-2026-0001", success.visibleJobs[0].jobNumber) // URGENT
        assertEquals("JOB-2026-0003", success.visibleJobs[1].jobNumber) // HIGH
        assertEquals("JOB-2026-0002", success.visibleJobs[2].jobNumber) // NORMAL
    }

    @Test
    fun createJobFromHandoff_viaViewModel_succeedsAndUpdatesList() = runBlocking {
        val sampleHandoffItem = OrderJobHandoffItem(
            itemId = "item-01",
            description = "বিজ্ঞাপন পোস্টার",
            specification = "১৩০ জিএসএম আর্ট পেপার",
            quantity = 3000,
            unit = "Pcs",
            unitPrice = 15.toMoney(),
            lineSubtotal = 45000.toMoney()
        )
        val handoff = OrderJobHandoff(
            handoffId = "hnd-vm-01",
            orderId = "ord-vm-01",
            orderNumber = "ORD-2026-V01",
            customerId = "cus-vm-01",
            priority = OrderPriority.HIGH,
            items = listOf(sampleHandoffItem),
            commercialTotal = 45000.toMoney(),
            createdAt = "2026-08-16T10:00:00Z"
        )

        viewModel.createJobFromHandoff(handoff, createdBy = "Production Manager")

        val state = viewModel.uiState.value
        assertTrue(state is ProductionJobListUiState.Success)
        val success = state as ProductionJobListUiState.Success
        assertEquals(1, success.totalCount)
        assertEquals("ORD-2026-V01", success.visibleJobs[0].orderNumber)
        assertEquals(OrderPriority.HIGH, success.visibleJobs[0].priority)
    }
}
