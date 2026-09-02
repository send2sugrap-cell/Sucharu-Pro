package com.sucharu.sucharupro.ui.features.production.job.details

import com.sucharu.sucharupro.data.datasource.FakeProductionJobDataSource
import com.sucharu.sucharupro.data.repository.ProductionJobRepositoryImpl
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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ProductionCompletionViewModelTest {

    private lateinit var dataSource: FakeProductionJobDataSource
    private lateinit var repository: ProductionJobRepository
    private lateinit var viewModel: ProductionJobDetailsViewModel

    private val sampleJob = ProductionJob(
        jobId = "job-vm-comp-01",
        jobNumber = "JOB-2026-VMCOMP01",
        orderId = "ord-01",
        orderNumber = "ORD-2026-0001",
        customerId = "cust-01",
        handoffId = "hnd-vm-comp-01",
        title = "বাংলা ব্যাকরণ ও নির্মিতি বই",
        quantity = 500,
        unit = "কপি",
        priority = OrderPriority.URGENT,
        status = ProductionJobStatus.IN_PROGRESS,
        items = listOf(
            ProductionJobItem(
                itemId = "item-01",
                description = "বাংলা ব্যাকরণ ও নির্মিতি বই",
                quantity = 500,
                unit = "কপি"
            )
        ),
        stages = ProductionJobStage.createInitialStages("job-vm-comp-01"),
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

    private suspend fun setupJobForCompletion(job: ProductionJob) {
        val completedStages = job.stages.map { stage ->
            if (stage.sequence < ProductionStageType.READY.displayOrder) {
                stage.copy(status = ProductionStageStatus.COMPLETED)
            } else {
                stage
            }
        }
        val preparedJob = job.copy(stages = completedStages)
        repository.createJob(preparedJob)

        dataSource.insertOutput(
            com.sucharu.sucharupro.domain.model.job.ProductionStageOutput(
                outputId = "out-vm-setup-${job.jobId}",
                jobId = job.jobId,
                stageId = job.stages[0].stageId,
                stageType = job.stages[0].stageType,
                quantity = job.quantity,
                unit = job.unit,
                recordedAt = "2026-08-16T10:30:00Z"
            )
        )
    }

    @Test
    fun loadJob_exposesCompletionChecklistInSuccessState() = runBlocking {
        setupJobForCompletion(sampleJob)
        viewModel.loadJob(sampleJob.jobId)

        val state = viewModel.uiState.value
        assertTrue(state is ProductionJobDetailsUiState.Success)
        val success = state as ProductionJobDetailsUiState.Success
        assertNotNull(success.completionChecklist)
        assertTrue(success.completionChecklist?.isEligible == true)
        assertEquals(4, success.completionChecklist?.passedCount)
    }

    @Test
    fun confirmProductionCompletion_updatesJobAndUiState() = runBlocking {
        setupJobForCompletion(sampleJob)
        viewModel.loadJob(sampleJob.jobId)

        viewModel.confirmProductionCompletion(
            actorId = "supervisor-01",
            actorName = "Akhtaruzzaman",
            remarks = "উৎপাদন সফলভাবে সমাপ্ত"
        )

        val state = viewModel.uiState.value as ProductionJobDetailsUiState.Success
        assertEquals(ProductionJobStatus.READY, state.job.status)
        assertNotNull(state.actionMessage)
        assertTrue(state.actionMessage?.contains("উৎপাদন সফলভাবে সম্পন্ন") == true)
    }

    @Test
    fun confirmProductionCompletion_blockedWhenIncomplete_setsActionError() = runBlocking {
        // Job has PENDING stages
        repository.createJob(sampleJob)
        viewModel.loadJob(sampleJob.jobId)

        viewModel.confirmProductionCompletion(
            actorId = "supervisor-01",
            actorName = "Akhtaruzzaman"
        )

        val state = viewModel.uiState.value as ProductionJobDetailsUiState.Success
        assertEquals(ProductionJobStatus.IN_PROGRESS, state.job.status)
        assertNotNull(state.actionError)
    }
}
