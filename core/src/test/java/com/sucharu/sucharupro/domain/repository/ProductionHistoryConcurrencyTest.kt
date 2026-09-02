package com.sucharu.sucharupro.domain.repository

import com.sucharu.sucharupro.data.datasource.FakeProductionJobDataSource
import com.sucharu.sucharupro.data.repository.ProductionJobRepositoryImpl
import com.sucharu.sucharupro.domain.model.job.ProductionJob
import com.sucharu.sucharupro.domain.model.job.ProductionJobItem
import com.sucharu.sucharupro.domain.model.job.ProductionJobStage
import com.sucharu.sucharupro.domain.model.job.ProductionJobStatus
import com.sucharu.sucharupro.domain.model.order.OrderPriority
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ProductionHistoryConcurrencyTest {

    private lateinit var dataSource: FakeProductionJobDataSource
    private lateinit var repository: ProductionJobRepository

    private val sampleJob = ProductionJob(
        jobId = "job-concurrent-hist-01",
        jobNumber = "JOB-2026-CONC01",
        orderId = "ord-conc-01",
        orderNumber = "ORD-2026-0001",
        handoffId = "hnd-conc-01",
        customerId = "cust-01",
        title = "কনকারেন্ট টেস্ট জব",
        quantity = 1000,
        unit = "কপি",
        priority = OrderPriority.URGENT,
        status = ProductionJobStatus.READY_FOR_PRODUCTION,
        items = listOf(
            ProductionJobItem(
                itemId = "item-conc-01",
                description = "কনকারেন্ট টেস্ট জব",
                quantity = 1000,
                unit = "কপি"
            )
        ),
        stages = ProductionJobStage.createInitialStages("job-concurrent-hist-01"),
        createdAt = "2026-08-16T10:00:00Z",
        updatedAt = "2026-08-16T10:00:00Z"
    )

    @Before
    fun setUp() {
        dataSource = FakeProductionJobDataSource()
        repository = ProductionJobRepositoryImpl(dataSource)
    }

    @Test
    fun concurrentOutputRecording_preservesAccurateAggregateHistory() = runBlocking {
        repository.createJob(sampleJob)
        val stageId = sampleJob.stages[0].stageId

        repository.startStage(sampleJob.jobId, stageId, timestamp = "2026-08-16T10:05:00Z")

        // Record 10 concurrent outputs of 100 units each
        val jobs = List(10) { idx ->
            launch(Dispatchers.Default) {
                repository.recordStageOutput(
                    jobId = sampleJob.jobId,
                    stageId = stageId,
                    quantity = 100,
                    unit = "কপি",
                    timestamp = "2026-08-16T10:${10 + idx}:00Z"
                )
            }
        }
        jobs.joinAll()

        val historyList = repository.observeProductionHistory().first()
        val history = historyList.find { it.jobId == sampleJob.jobId }
        assertEquals(1000, history?.totalRecordedOutput)
        assertEquals(0, history?.remainingQuantity)
        assertEquals(10, history?.outputRecordCount)
    }

    @Test
    fun concurrentHistoryRead_remainsConsistentDuringMutations() = runBlocking {
        repository.createJob(sampleJob)
        val stageId = sampleJob.stages[0].stageId

        val writerJob = launch(Dispatchers.Default) {
            repository.startStage(sampleJob.jobId, stageId, timestamp = "2026-08-16T10:05:00Z")
            repository.recordStageOutput(sampleJob.jobId, stageId, 500, "কপি", timestamp = "2026-08-16T10:15:00Z")
            repository.completeStage(sampleJob.jobId, stageId, timestamp = "2026-08-16T10:30:00Z")
        }

        val readerJob = launch(Dispatchers.Default) {
            repeat(10) {
                val history = repository.observeProductionHistory().first()
                assertTrue(history.isNotEmpty())
            }
        }

        joinAll(writerJob, readerJob)

        val finalHistory = repository.observeProductionHistory().first()
        assertEquals(1, finalHistory[0].completedStageCount)
        assertEquals(500, finalHistory[0].totalRecordedOutput)
    }
}
