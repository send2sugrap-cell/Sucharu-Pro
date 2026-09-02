package com.sucharu.sucharupro.ui.features.production.job.details

import com.sucharu.sucharupro.data.datasource.FakeProductionJobDataSource
import com.sucharu.sucharupro.data.repository.ProductionJobRepositoryImpl
import com.sucharu.sucharupro.domain.model.job.ProductionJob
import com.sucharu.sucharupro.domain.model.job.ProductionJobStage
import com.sucharu.sucharupro.domain.model.job.ProductionJobStatus
import com.sucharu.sucharupro.domain.model.order.OrderPriority
import com.sucharu.sucharupro.domain.model.production.ProductionStageStatus
import com.sucharu.sucharupro.domain.repository.ProductionJobRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for [ProductionJobDetailsViewModel].
 */
class ProductionJobDetailsViewModelTest {

    private lateinit var dataSource: FakeProductionJobDataSource
    private lateinit var repository: ProductionJobRepository
    private lateinit var viewModel: ProductionJobDetailsViewModel

    private val sampleJob = ProductionJob(
        jobId = "job-vm-01",
        jobNumber = "JOB-2026-VM01",
        orderId = "ord-vm-01",
        orderNumber = "ORD-2026-VM01",
        customerId = "cus-vm-01",
        handoffId = "hnd-vm-01",
        title = "বই প্রিন্টিং",
        quantity = 500,
        unit = "কপি",
        priority = OrderPriority.NORMAL,
        status = ProductionJobStatus.READY_FOR_PRODUCTION,
        stages = ProductionJobStage.createInitialStages("job-vm-01"),
        createdAt = "2026-08-16T10:00:00Z",
        updatedAt = "2026-08-16T10:00:00Z"
    )

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
    fun loadJob_emitsLoadingThenSuccess() = runBlocking {
        repository.createJob(sampleJob)

        viewModel.loadJob("job-vm-01")

        val state = viewModel.uiState.value
        assertTrue("Expected Success state, got $state", state is ProductionJobDetailsUiState.Success)
        val success = state as ProductionJobDetailsUiState.Success
        assertEquals("job-vm-01", success.job.jobId)
        assertEquals("JOB-2026-VM01", success.job.jobNumber)
    }

    @Test
    fun loadJob_notFound_emitsNotFound() = runBlocking {
        viewModel.loadJob("non-existent-id")

        val state = viewModel.uiState.value
        assertTrue("Expected NotFound state, got $state", state is ProductionJobDetailsUiState.NotFound)
    }

    @Test
    fun startStage_updatesStageAndJobStatus() = runBlocking {
        repository.createJob(sampleJob)
        viewModel.loadJob("job-vm-01")

        val stage1Id = sampleJob.stages[0].stageId
        viewModel.startStage(stage1Id, actorId = "user-1", actorName = "ডিজাইনার")

        val state = viewModel.uiState.value
        assertTrue("Expected Success state, got $state", state is ProductionJobDetailsUiState.Success)
        val success = state as ProductionJobDetailsUiState.Success
        assertEquals(ProductionJobStatus.IN_PROGRESS, success.job.status)
        assertEquals(ProductionStageStatus.IN_PROGRESS, success.job.stages[0].status)
    }

    @Test
    fun completeStage_advancesStageProgress() = runBlocking {
        repository.createJob(sampleJob)
        viewModel.loadJob("job-vm-01")

        val stage1Id = sampleJob.stages[0].stageId
        viewModel.startStage(stage1Id)
        viewModel.completeStage(stage1Id, notes = "নকশা সম্পন্ন")

        val state = viewModel.uiState.value
        assertTrue("Expected Success state, got $state", state is ProductionJobDetailsUiState.Success)
        val success = state as ProductionJobDetailsUiState.Success
        assertEquals(ProductionStageStatus.COMPLETED, success.job.stages[0].status)
        assertEquals(1, success.job.completedStagesCount)
    }

    @Test
    fun holdAndResumeJob_controlsJobState() = runBlocking {
        repository.createJob(sampleJob)
        viewModel.loadJob("job-vm-01")

        val stage1Id = sampleJob.stages[0].stageId
        viewModel.startStage(stage1Id)

        viewModel.holdJob("গ্রাহকের অনুরোধে স্থগিত")

        var success = viewModel.uiState.value as ProductionJobDetailsUiState.Success
        assertEquals(ProductionJobStatus.ON_HOLD, success.job.status)

        viewModel.resumeJob()

        success = viewModel.uiState.value as ProductionJobDetailsUiState.Success
        assertEquals(ProductionJobStatus.IN_PROGRESS, success.job.status)
    }

    @Test
    fun cancelJob_marksJobCancelled() = runBlocking {
        repository.createJob(sampleJob)
        viewModel.loadJob("job-vm-01")

        viewModel.cancelJob("অর্ডার বাতিল")

        val success = viewModel.uiState.value as ProductionJobDetailsUiState.Success
        assertEquals(ProductionJobStatus.CANCELLED, success.job.status)
        assertTrue(success.job.status.isTerminal)
    }
}
