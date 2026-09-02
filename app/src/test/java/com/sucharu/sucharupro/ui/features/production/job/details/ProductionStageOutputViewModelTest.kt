package com.sucharu.sucharupro.ui.features.production.job.details

import com.sucharu.sucharupro.data.datasource.FakeProductionJobDataSource
import com.sucharu.sucharupro.data.repository.ProductionJobRepositoryImpl
import com.sucharu.sucharupro.domain.model.job.ProductionActivityType
import com.sucharu.sucharupro.domain.model.job.ProductionJob
import com.sucharu.sucharupro.domain.model.job.ProductionJobItem
import com.sucharu.sucharupro.domain.model.job.ProductionJobStage
import com.sucharu.sucharupro.domain.model.job.ProductionJobStatus
import com.sucharu.sucharupro.domain.model.order.OrderPriority
import com.sucharu.sucharupro.domain.model.production.ProductionStageType
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
 * ViewModel unit tests for Stage Output quantity tracking flows (Module 04 Step 06).
 */
class ProductionStageOutputViewModelTest {

    private lateinit var dataSource: FakeProductionJobDataSource
    private lateinit var repository: ProductionJobRepository
    private lateinit var viewModel: ProductionJobDetailsViewModel

    private val sampleJob = ProductionJob(
        jobId = "job-vm-out-01",
        jobNumber = "JOB-2026-VMOUT01",
        orderId = "ord-vm-01",
        orderNumber = "ORD-2026-VM01",
        customerId = "cus-vm-01",
        handoffId = "hnd-vm-01",
        title = "পুস্তিকা মুদ্রণ ও বাঁধাই",
        quantity = 1500,
        unit = "কপি",
        priority = OrderPriority.URGENT,
        status = ProductionJobStatus.READY_FOR_PRODUCTION,
        items = listOf(
            ProductionJobItem(
                itemId = "item-01",
                description = "পুস্তিকা",
                quantity = 1500,
                unit = "কপি"
            )
        ),
        stages = ProductionJobStage.createInitialStages("job-vm-out-01"),
        createdAt = "2026-08-16T10:00:00Z",
        updatedAt = "2026-08-16T10:00:00Z"
    )

    private val stage1Id = sampleJob.stages.first { it.stageType == ProductionStageType.DESIGN }.stageId

    @Before
    fun setUp() {
        dataSource = FakeProductionJobDataSource()
        repository = ProductionJobRepositoryImpl(dataSource)
        viewModel = ProductionJobDetailsViewModel(
            repository = repository,
            externalScope = CoroutineScope(Dispatchers.Unconfined)
        )
    }

    @Test
    fun loadJob_emitsStageOutputsInSuccessState() = runBlocking {
        repository.createJob(sampleJob)
        viewModel.loadJob(sampleJob.jobId)

        val state = viewModel.uiState.value
        assertTrue("Expected Success state, got $state", state is ProductionJobDetailsUiState.Success)
        val success = state as ProductionJobDetailsUiState.Success
        assertEquals(0, success.stageOutputs.size)
    }

    @Test
    fun recordStageOutput_updatesUiStateAndShowsMessage() = runBlocking {
        repository.createJob(sampleJob)
        viewModel.loadJob(sampleJob.jobId)
        viewModel.startStage(stage1Id)

        viewModel.recordStageOutput(
            stageId = stage1Id,
            quantity = 500,
            unit = "কপি",
            remarks = "১ম লট সম্পন্ন"
        )

        val state = viewModel.uiState.value
        assertTrue("Expected Success state, got $state", state is ProductionJobDetailsUiState.Success)
        val success = state as ProductionJobDetailsUiState.Success

        assertEquals(1, success.stageOutputs.size)
        assertEquals(500, success.stageOutputs[0].quantity)
        assertEquals("১ম লট সম্পন্ন", success.stageOutputs[0].remarks)
        assertTrue(success.actionMessage?.contains("500 কপি") == true)
    }

    @Test
    fun validationError_setsActionErrorInUiState() = runBlocking {
        repository.createJob(sampleJob)
        viewModel.loadJob(sampleJob.jobId)
        viewModel.startStage(stage1Id)

        // Attempt exceeding quantity
        viewModel.recordStageOutput(
            stageId = stage1Id,
            quantity = 2000, // 2000 > 1500
            unit = "কপি"
        )

        val state = viewModel.uiState.value
        assertTrue("Expected Success state, got $state", state is ProductionJobDetailsUiState.Success)
        val success = state as ProductionJobDetailsUiState.Success

        assertNotNull(success.actionError)
        assertTrue(success.actionError?.contains("exceeds planned quantity") == true)
        assertEquals(0, success.stageOutputs.size)
    }

    @Test
    fun remainingQuantity_refreshesReactively() = runBlocking {
        repository.createJob(sampleJob)
        viewModel.loadJob(sampleJob.jobId)
        viewModel.startStage(stage1Id)

        viewModel.recordStageOutput(stage1Id, quantity = 600, unit = "কপি")

        val state = viewModel.uiState.value as ProductionJobDetailsUiState.Success
        val produced = state.stageOutputs.filter { it.stageId == stage1Id }.sumOf { it.quantity }
        val remaining = sampleJob.quantity - produced

        assertEquals(600, produced)
        assertEquals(900, remaining)
    }

    @Test
    fun outputHistory_refreshesReactively() = runBlocking {
        repository.createJob(sampleJob)
        viewModel.loadJob(sampleJob.jobId)
        viewModel.startStage(stage1Id)

        viewModel.recordStageOutput(stage1Id, quantity = 300, unit = "কপি", remarks = "লট ১")
        viewModel.recordStageOutput(stage1Id, quantity = 400, unit = "কপি", remarks = "লট ২")

        val state = viewModel.uiState.value as ProductionJobDetailsUiState.Success
        assertEquals(2, state.stageOutputs.size)
        assertEquals(400, state.stageOutputs[0].quantity) // Newest first
        assertEquals(300, state.stageOutputs[1].quantity)
    }

    @Test
    fun activityTimeline_receivesOutputEvent() = runBlocking {
        repository.createJob(sampleJob)
        viewModel.loadJob(sampleJob.jobId)
        viewModel.startStage(stage1Id)

        viewModel.recordStageOutput(stage1Id, quantity = 750, unit = "কপি", remarks = "অর্ধেক সম্পন্ন")

        val state = viewModel.uiState.value as ProductionJobDetailsUiState.Success
        val outputEvent = state.activities.find { it.eventType == ProductionActivityType.STAGE_OUTPUT_RECORDED }
        assertNotNull(outputEvent)
        assertTrue(outputEvent?.message?.contains("750 কপি") == true)
    }
}
