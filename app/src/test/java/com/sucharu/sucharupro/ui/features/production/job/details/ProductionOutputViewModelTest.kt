package com.sucharu.sucharupro.ui.features.production.job.details

import com.sucharu.sucharupro.data.datasource.FakeProductionJobDataSource
import com.sucharu.sucharupro.data.repository.ProductionJobRepositoryImpl
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

class ProductionOutputViewModelTest {

    private lateinit var dataSource: FakeProductionJobDataSource
    private lateinit var repository: ProductionJobRepository
    private lateinit var viewModel: ProductionJobDetailsViewModel

    private val sampleJob = ProductionJob(
        jobId = "job-vm-recon-01",
        jobNumber = "JOB-2026-VMRECON01",
        orderId = "ord-01",
        orderNumber = "ORD-2026-0001",
        customerId = "cust-01",
        handoffId = "hnd-vm-recon-01",
        title = "বাংলা ব্যাকরণ ও নির্মিতি বই",
        quantity = 1000,
        unit = "কপি",
        priority = OrderPriority.URGENT,
        status = ProductionJobStatus.READY_FOR_PRODUCTION,
        items = listOf(
            ProductionJobItem(
                itemId = "item-01",
                description = "বাংলা ব্যাকরণ ও নির্মিতি বই",
                quantity = 1000,
                unit = "কপি"
            )
        ),
        stages = ProductionJobStage.createInitialStages("job-vm-recon-01"),
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
    fun loadJob_exposesReconciliationInSuccessState() = runBlocking {
        repository.createJob(sampleJob)
        viewModel.loadJob(sampleJob.jobId)

        val state = viewModel.uiState.value
        assertTrue(state is ProductionJobDetailsUiState.Success)
        val success = state as ProductionJobDetailsUiState.Success
        assertNotNull(success.reconciliation)
        assertEquals(1000, success.reconciliation?.plannedQuantity)
        assertEquals(0, success.reconciliation?.recordedQuantity)
        assertEquals(1000, success.reconciliation?.remainingQuantity)
    }

    @Test
    fun recordOutput_updatesReconciliationInSuccessState() = runBlocking {
        repository.createJob(sampleJob)
        viewModel.loadJob(sampleJob.jobId)

        // Start stage first
        viewModel.startStage(stage1Id)

        // Record output
        viewModel.recordStageOutput(
            stageId = stage1Id,
            quantity = 600,
            unit = "কপি",
            remarks = "৬০০ কপি সম্পন্ন"
        )

        val state = viewModel.uiState.value as ProductionJobDetailsUiState.Success
        assertEquals(1, state.stageOutputs.size)
        assertEquals(600, state.reconciliation?.recordedQuantity)
        assertEquals(400, state.reconciliation?.remainingQuantity)
        assertEquals(60.0, state.reconciliation?.completionPercentage ?: 0.0, 0.001)
    }

    @Test
    fun failedOutputRecording_setsActionError() = runBlocking {
        repository.createJob(sampleJob)
        viewModel.loadJob(sampleJob.jobId)

        // Attempt output without starting stage (in PENDING)
        viewModel.recordStageOutput(
            stageId = stage1Id,
            quantity = 500,
            unit = "কপি"
        )

        val state = viewModel.uiState.value as ProductionJobDetailsUiState.Success
        assertNotNull(state.actionError)
        assertTrue(state.actionError?.contains("Start the stage first") == true)
    }
}
