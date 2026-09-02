package com.sucharu.sucharupro.domain.repository

import com.sucharu.sucharupro.data.datasource.FakeProductionJobDataSource
import com.sucharu.sucharupro.data.repository.ProductionJobRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.job.ProductionJob
import com.sucharu.sucharupro.domain.model.job.ProductionJobItem
import com.sucharu.sucharupro.domain.model.job.ProductionJobStage
import com.sucharu.sucharupro.domain.model.job.ProductionJobStatus
import com.sucharu.sucharupro.domain.model.order.OrderPriority
import com.sucharu.sucharupro.domain.model.production.ProductionStageType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ProductionHistoryRepositoryTest {

    private lateinit var dataSource: FakeProductionJobDataSource
    private lateinit var repository: ProductionJobRepository

    private val sampleJob = ProductionJob(
        jobId = "job-hist-repo-01",
        jobNumber = "JOB-2026-HIST01",
        orderId = "ord-hist-01",
        orderNumber = "ORD-2026-HIST01",
        handoffId = "hnd-hist-01",
        customerId = "cust-01",
        title = "পুস্তিকা মুদ্রণ ও বাঁধাই",
        quantity = 500,
        unit = "কপি",
        priority = OrderPriority.URGENT,
        status = ProductionJobStatus.READY_FOR_PRODUCTION,
        items = listOf(
            ProductionJobItem(
                itemId = "item-hist-01",
                description = "পুস্তিকা মুদ্রণ ও বাঁধাই",
                quantity = 500,
                unit = "কপি"
            )
        ),
        stages = ProductionJobStage.createInitialStages("job-hist-repo-01"),
        createdAt = "2026-08-16T10:00:00Z",
        updatedAt = "2026-08-16T10:00:00Z"
    )

    private val stage1Id = sampleJob.stages[0].stageId

    @Before
    fun setUp() {
        dataSource = FakeProductionJobDataSource()
        repository = ProductionJobRepositoryImpl(dataSource)
    }

    @Test
    fun observeHistory_emitsInitialHistoricalSummary() = runBlocking {
        repository.createJob(sampleJob)

        val historyList = repository.observeProductionHistory().first()
        assertEquals(1, historyList.size)
        val history = historyList[0]
        assertEquals(sampleJob.jobId, history.jobId)
        assertEquals(sampleJob.jobNumber, history.jobNumber)
        assertEquals(500, history.quantity)
        assertEquals(0, history.completedStageCount)
    }

    @Test
    fun stageCompletion_andOutputRecording_updatesHistoricalSummary() = runBlocking {
        repository.createJob(sampleJob)

        repository.startStage(sampleJob.jobId, stage1Id, timestamp = "2026-08-16T10:05:00Z")
        repository.recordStageOutput(
            jobId = sampleJob.jobId,
            stageId = stage1Id,
            quantity = 500,
            unit = "কপি",
            timestamp = "2026-08-16T10:20:00Z"
        )
        repository.completeStage(sampleJob.jobId, stage1Id, timestamp = "2026-08-16T10:30:00Z")

        val historyList = repository.observeProductionHistory().first()
        val history = historyList[0]
        assertEquals(1, history.completedStageCount)
        assertEquals(500, history.totalRecordedOutput)
        assertEquals(0, history.remainingQuantity)
    }

    @Test
    fun deliveryTransition_updatesHistoricalStatusToDelivered() = runBlocking {
        repository.createJob(sampleJob)

        // Complete stages 1..11
        for (i in 0..10) {
            val stageId = sampleJob.stages[i].stageId
            repository.startStage(sampleJob.jobId, stageId, timestamp = "2026-08-16T10:00:00Z")
            repository.completeStage(sampleJob.jobId, stageId, timestamp = "2026-08-16T10:30:00Z")
        }

        repository.markJobReady(sampleJob.jobId, timestamp = "2026-08-16T11:00:00Z")
        repository.deliverJob(sampleJob.jobId, timestamp = "2026-08-16T12:00:00Z")

        val historyList = repository.observeProductionHistory().first()
        assertEquals(ProductionJobStatus.DELIVERED, historyList[0].finalStatus)
    }

    @Test
    fun getProductionJobCompletionSummary_returnsStructuredSuccess() = runBlocking {
        repository.createJob(sampleJob)

        val summaryResult = repository.getProductionJobCompletionSummary(sampleJob.jobId).first()
        assertTrue(summaryResult is DomainResult.Success)
        val summary = (summaryResult as DomainResult.Success).data
        assertEquals(sampleJob.jobNumber, summary.jobNumber)
        assertEquals(13, summary.stageHistory.size)
    }
}
