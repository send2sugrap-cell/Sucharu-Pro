package com.sucharu.sucharupro.ui.features.production.operator

import com.sucharu.sucharupro.data.datasource.FakeProductionJobDataSource
import com.sucharu.sucharupro.data.repository.ProductionJobRepositoryImpl
import com.sucharu.sucharupro.domain.model.job.ProductionJob
import com.sucharu.sucharupro.domain.model.job.ProductionJobStage
import com.sucharu.sucharupro.domain.model.job.ProductionJobStatus
import com.sucharu.sucharupro.domain.model.job.StageAssignmentStatus
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
 * Unit tests for [ProductionOperatorWorkQueueViewModel].
 */
class ProductionOperatorWorkQueueViewModelTest {

    private lateinit var dataSource: FakeProductionJobDataSource
    private lateinit var repository: ProductionJobRepository
    private lateinit var viewModel: ProductionOperatorWorkQueueViewModel

    private val job1 = ProductionJob(
        jobId = "job-wq-01",
        jobNumber = "JOB-2026-0001",
        orderId = "ord-001",
        orderNumber = "ORD-2026-0001",
        customerId = "cus-001",
        handoffId = "hnd-001",
        title = "বাংলা ব্যাকরণ বই মুদ্রণ",
        quantity = 2000,
        unit = "কপি",
        priority = OrderPriority.URGENT,
        status = ProductionJobStatus.IN_PROGRESS,
        stages = ProductionJobStage.createInitialStages("job-wq-01"),
        createdAt = "2026-08-16T10:00:00Z",
        updatedAt = "2026-08-16T10:00:00Z"
    )

    private val job2 = ProductionJob(
        jobId = "job-wq-02",
        jobNumber = "JOB-2026-0002",
        orderId = "ord-002",
        orderNumber = "ORD-2026-0002",
        customerId = "cus-002",
        handoffId = "hnd-002",
        title = "বিজনেস কার্ড প্রিন্টিং",
        quantity = 500,
        unit = "Pcs",
        priority = OrderPriority.NORMAL,
        status = ProductionJobStatus.READY_FOR_PRODUCTION,
        stages = ProductionJobStage.createInitialStages("job-wq-02"),
        createdAt = "2026-08-16T11:00:00Z",
        updatedAt = "2026-08-16T11:00:00Z"
    )

    @Before
    fun setUp() = runBlocking {
        dataSource = FakeProductionJobDataSource()
        repository = ProductionJobRepositoryImpl(dataSource)
        repository.createJob(job1)
        repository.createJob(job2)

        // Assign operator Rahim (op-01) to job1 Stage 6 (PRINTING)
        val stage6 = job1.stages.find { it.stageType == ProductionStageType.PRINTING }!!
        repository.assignStageOperator(
            jobId = job1.jobId,
            stageId = stage6.stageId,
            operatorId = "op-01",
            operatorName = "রহিম আহমেদ",
            timestamp = "2026-08-16T10:15:00Z"
        )

        // Assign operator Karim (op-02) to job2 Stage 1 (DESIGN)
        val stage1 = job2.stages.find { it.stageType == ProductionStageType.DESIGN }!!
        repository.assignStageOperator(
            jobId = job2.jobId,
            stageId = stage1.stageId,
            operatorId = "op-02",
            operatorName = "করিম চৌধুরী",
            timestamp = "2026-08-16T11:15:00Z"
        )

        viewModel = ProductionOperatorWorkQueueViewModel(
            repository = repository,
            externalScope = CoroutineScope(Dispatchers.Unconfined)
        )
    }

    @Test
    fun initialQueueState_loadsAllWorkItems() {
        val state = viewModel.uiState.value
        assertTrue("Expected Success, got $state", state is ProductionOperatorWorkQueueUiState.Success)
        val success = state as ProductionOperatorWorkQueueUiState.Success
        assertEquals(2, success.totalCount)
        assertEquals(2, success.visibleCount)
        assertFalse(success.isFiltered)
    }

    @Test
    fun filterByOperator_showsOnlyTargetOperatorWork() {
        viewModel.onOperatorSelect("op-01")

        val state = viewModel.uiState.value as ProductionOperatorWorkQueueUiState.Success
        assertEquals(1, state.visibleCount)
        assertEquals("op-01", state.visibleWorkItems[0].assignment.operatorId)
        assertEquals("বাংলা ব্যাকরণ বই মুদ্রণ", state.visibleWorkItems[0].job.title)
    }

    @Test
    fun search_matchesBanglaTitleAndStageName() {
        // Search by Bangla text
        viewModel.onSearchQueryChange("  বাংলা  ")
        var state = viewModel.uiState.value as ProductionOperatorWorkQueueUiState.Success
        assertEquals(1, state.visibleCount)
        assertEquals("JOB-2026-0001", state.visibleWorkItems[0].job.jobNumber)

        // Search by Stage code
        viewModel.onSearchQueryChange("DSN")
        state = viewModel.uiState.value as ProductionOperatorWorkQueueUiState.Success
        assertEquals(1, state.visibleCount)
        assertEquals("JOB-2026-0002", state.visibleWorkItems[0].job.jobNumber)
    }

    @Test
    fun priorityFilter_worksCorrectly() {
        viewModel.onPriorityFilterChange(OrderPriority.URGENT)
        val state = viewModel.uiState.value as ProductionOperatorWorkQueueUiState.Success
        assertEquals(1, state.visibleCount)
        assertEquals(OrderPriority.URGENT, state.visibleWorkItems[0].job.priority)
    }

    @Test
    fun stageTypeFilter_worksCorrectly() {
        viewModel.onStageTypeFilterChange(ProductionStageType.PRINTING)
        val state = viewModel.uiState.value as ProductionOperatorWorkQueueUiState.Success
        assertEquals(1, state.visibleCount)
        assertEquals(ProductionStageType.PRINTING, state.visibleWorkItems[0].stage.stageType)
    }

    @Test
    fun compoundFilters_andClearFilters_worksCorrectly() {
        viewModel.onOperatorSelect("op-01")
        viewModel.onPriorityFilterChange(OrderPriority.NORMAL) // op-01 has URGENT, so 0 matches

        var state = viewModel.uiState.value as ProductionOperatorWorkQueueUiState.Success
        assertEquals(0, state.visibleCount)
        assertTrue(state.isFiltered)

        viewModel.clearFilters()
        state = viewModel.uiState.value as ProductionOperatorWorkQueueUiState.Success
        assertEquals(2, state.visibleCount)
        assertFalse(state.isFiltered)
    }

    @Test
    fun sorting_byPriority_placesUrgentFirst() {
        viewModel.onSortOrderChange(OperatorWorkSortOrder.PRIORITY_DESC)
        val state = viewModel.uiState.value as ProductionOperatorWorkQueueUiState.Success
        assertEquals(OrderPriority.URGENT, state.visibleWorkItems[0].job.priority)
        assertEquals(OrderPriority.NORMAL, state.visibleWorkItems[1].job.priority)
    }

    @Test
    fun sorting_byStageSequence_placesStage1BeforeStage6() {
        viewModel.onSortOrderChange(OperatorWorkSortOrder.STAGE_SEQUENCE_ASC)
        val state = viewModel.uiState.value as ProductionOperatorWorkQueueUiState.Success
        assertEquals(1, state.visibleWorkItems[0].stage.sequence)
        assertEquals(6, state.visibleWorkItems[1].stage.sequence)
    }
}
