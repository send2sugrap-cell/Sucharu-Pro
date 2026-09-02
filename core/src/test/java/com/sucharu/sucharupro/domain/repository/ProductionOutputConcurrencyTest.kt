package com.sucharu.sucharupro.domain.repository

import com.sucharu.sucharupro.data.datasource.FakeProductionJobDataSource
import com.sucharu.sucharupro.data.repository.ProductionJobRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
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

class ProductionOutputConcurrencyTest {

    private lateinit var dataSource: FakeProductionJobDataSource
    private lateinit var repository: ProductionJobRepository

    private val sampleJob = ProductionJob(
        jobId = "job-conc-out-01",
        jobNumber = "JOB-2026-CONCOUT01",
        orderId = "ord-01",
        orderNumber = "ORD-2026-0001",
        handoffId = "hnd-conc-01",
        customerId = "cust-01",
        title = "কনকারেন্ট আউটপুট টেস্ট জব",
        quantity = 1000,
        unit = "কপি",
        priority = OrderPriority.URGENT,
        status = ProductionJobStatus.READY_FOR_PRODUCTION,
        items = listOf(
            ProductionJobItem(
                itemId = "item-01",
                description = "কনকারেন্ট আউটপুট টেস্ট জব",
                quantity = 1000,
                unit = "কপি"
            )
        ),
        stages = ProductionJobStage.createInitialStages("job-conc-out-01"),
        createdAt = "2026-08-16T10:00:00Z",
        updatedAt = "2026-08-16T10:00:00Z"
    )

    @Before
    fun setUp() {
        dataSource = FakeProductionJobDataSource()
        repository = ProductionJobRepositoryImpl(dataSource)
    }

    @Test
    fun concurrentOutputRecording_aggregatesDeterministicSum() = runBlocking {
        repository.createJob(sampleJob)
        val stageId = sampleJob.stages[0].stageId

        repository.startStage(sampleJob.jobId, stageId, timestamp = "2026-08-16T10:05:00Z")

        // 10 concurrent requests of 50 units each
        val coroutines = List(10) { idx ->
            launch(Dispatchers.Default) {
                repository.recordStageOutput(
                    jobId = sampleJob.jobId,
                    stageId = stageId,
                    quantity = 50,
                    unit = "কপি",
                    timestamp = "2026-08-16T10:${10 + idx}:00Z"
                )
            }
        }
        coroutines.joinAll()

        val outputs = repository.getStageOutputs(sampleJob.jobId, stageId).first()
        assertEquals(10, outputs.size)
        val totalRecorded = outputs.sumOf { it.quantity }
        assertEquals(500, totalRecorded)

        val reconciliation = repository.observeProductionOutputReconciliation(sampleJob.jobId).first()
        assertTrue(reconciliation is DomainResult.Success)
        val r = (reconciliation as DomainResult.Success).data
        assertEquals(500, r.recordedQuantity)
        assertEquals(500, r.remainingQuantity)
        assertEquals(50.0, r.completionPercentage, 0.001)
    }

    @Test
    fun failedOutputRecording_doesNotCorruptState() = runBlocking {
        repository.createJob(sampleJob)
        val stageId = sampleJob.stages[0].stageId

        repository.startStage(sampleJob.jobId, stageId, timestamp = "2026-08-16T10:05:00Z")

        // Record a valid 100 output
        repository.recordStageOutput(sampleJob.jobId, stageId, 100, "কপি", timestamp = "2026-08-16T10:10:00Z")

        // Launch 5 invalid outputs (negative or zero)
        val coroutines = List(5) { idx ->
            launch(Dispatchers.Default) {
                repository.recordStageOutput(
                    jobId = sampleJob.jobId,
                    stageId = stageId,
                    quantity = -10 * (idx + 1),
                    unit = "কপি",
                    timestamp = "2026-08-16T10:15:00Z"
                )
            }
        }
        coroutines.joinAll()

        val outputs = repository.getStageOutputs(sampleJob.jobId, stageId).first()
        assertEquals(1, outputs.size)
        assertEquals(100, outputs[0].quantity)
    }
}
