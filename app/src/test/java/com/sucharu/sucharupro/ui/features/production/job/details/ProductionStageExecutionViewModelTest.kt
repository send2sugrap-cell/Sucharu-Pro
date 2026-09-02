package com.sucharu.sucharupro.ui.features.production.job.details

import com.sucharu.sucharupro.data.datasource.FakeProductionJobDataSource
import com.sucharu.sucharupro.data.repository.ProductionJobRepositoryImpl
import com.sucharu.sucharupro.domain.model.job.ProductionActivityType
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

/**
 * Unit tests for Stage Execution and Activity Timeline flows in [ProductionJobDetailsViewModel].
 */
class ProductionStageExecutionViewModelTest {

    private lateinit var dataSource: FakeProductionJobDataSource
    private lateinit var repository: ProductionJobRepository
    private lateinit var viewModel: ProductionJobDetailsViewModel

    private val sampleJob = ProductionJob(
        jobId = "job-vm-exec-01",
        jobNumber = "JOB-2026-EXEC01",
        orderId = "ord-exec-01",
        orderNumber = "ORD-2026-EXEC01",
        customerId = "cus-exec-01",
        handoffId = "hnd-exec-01",
        title = "ক্যাটালগ মুদ্রণ ও ফিনিশিং",
        quantity = 3000,
        unit = "Pcs",
        priority = OrderPriority.URGENT,
        status = ProductionJobStatus.READY_FOR_PRODUCTION,
        items = listOf(
            ProductionJobItem(
                itemId = "item-01",
                description = "প্রোডাক্ট ক্যাটালগ",
                quantity = 3000,
                unit = "Pcs"
            )
        ),
        stages = ProductionJobStage.createInitialStages("job-vm-exec-01"),
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
    fun startStageWithRemarks_updatesExecutionAndEmitsActivity() = runBlocking {
        repository.createJob(sampleJob)
        viewModel.loadJob(sampleJob.jobId)

        viewModel.startStage(
            stageId = stage1Id,
            actorId = "op-01",
            actorName = "তানভীর হাসান",
            notes = "ডিজাইন কাজ শুরু"
        )

        val state = viewModel.uiState.value
        assertTrue("Expected Success state, got $state", state is ProductionJobDetailsUiState.Success)
        val success = state as ProductionJobDetailsUiState.Success
        assertEquals(ProductionJobStatus.IN_PROGRESS, success.job.status)

        val stage = success.job.stages.find { it.stageId == stage1Id }
        assertEquals(ProductionStageStatus.IN_PROGRESS, stage?.status)
        assertEquals("ডিজাইন কাজ শুরু", stage?.notes)

        // Verify activities
        assertEquals(1, success.activities.size)
        assertEquals(ProductionActivityType.STAGE_STARTED, success.activities[0].eventType)
        assertEquals("ডিজাইন কাজ শুরু", success.activities[0].message)

        // Verify executions
        assertEquals(1, success.stageExecutions.size)
        assertEquals(ProductionStageStatus.IN_PROGRESS, success.stageExecutions[0].status)
    }

    @Test
    fun completeStageWithRemarks_calculatesDurationAndEmitsActivity() = runBlocking {
        repository.createJob(sampleJob)
        viewModel.loadJob(sampleJob.jobId)

        viewModel.startStage(stage1Id, actorId = "op-01", actorName = "তানভীর", notes = "শুরু")
        viewModel.completeStage(stage1Id, actorId = "op-01", notes = "ডিজাইন সম্পন্ন ও অনুমোদিত")

        val state = viewModel.uiState.value
        assertTrue("Expected Success state, got $state", state is ProductionJobDetailsUiState.Success)
        val success = state as ProductionJobDetailsUiState.Success

        val stage = success.job.stages.find { it.stageId == stage1Id }
        assertEquals(ProductionStageStatus.COMPLETED, stage?.status)

        // Verify activities (STAGE_COMPLETED newest, STAGE_STARTED older)
        assertEquals(2, success.activities.size)
        assertEquals(ProductionActivityType.STAGE_COMPLETED, success.activities[0].eventType)
        assertEquals("ডিজাইন সম্পন্ন ও অনুমোদিত", success.activities[0].message)

        // Verify executions
        val execution = success.stageExecutions.find { it.stageId == stage1Id }
        assertNotNull(execution)
        assertEquals(ProductionStageStatus.COMPLETED, execution?.status)
        assertEquals("ডিজাইন সম্পন্ন ও অনুমোদিত", execution?.completionRemarks)
    }

    @Test
    fun addStageExecutionNote_addsNoteToStageAndEmitsActivity() = runBlocking {
        repository.createJob(sampleJob)
        viewModel.loadJob(sampleJob.jobId)

        viewModel.addStageExecutionNote(
            stageId = stage1Id,
            note = "কালার প্রুফ যাচাইকরণ সম্পন্ন",
            actorId = "op-02",
            actorName = "করিম চৌধুরী"
        )

        val state = viewModel.uiState.value
        assertTrue("Expected Success state, got $state", state is ProductionJobDetailsUiState.Success)
        val success = state as ProductionJobDetailsUiState.Success

        assertEquals(1, success.activities.size)
        assertEquals(ProductionActivityType.STAGE_EXECUTION_NOTE, success.activities[0].eventType)
        assertEquals("কালার প্রুফ যাচাইকরণ সম্পন্ন", success.activities[0].message)
    }

    @Test
    fun actionFeedback_canBeDismissed() = runBlocking {
        repository.createJob(sampleJob)
        viewModel.loadJob(sampleJob.jobId)

        viewModel.startStage(stage1Id)

        var success = viewModel.uiState.value as ProductionJobDetailsUiState.Success
        assertNotNull(success.actionMessage)

        viewModel.dismissActionFeedback()
        success = viewModel.uiState.value as ProductionJobDetailsUiState.Success
        assertEquals(null, success.actionMessage)
        assertEquals(null, success.actionError)
    }
}
