package com.sucharu.sucharupro.domain.repository

import com.sucharu.sucharupro.data.datasource.FakeProductionJobDataSource
import com.sucharu.sucharupro.data.repository.ProductionJobRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.job.ProductionActivityType
import com.sucharu.sucharupro.domain.model.job.ProductionJob
import com.sucharu.sucharupro.domain.model.job.ProductionJobStage
import com.sucharu.sucharupro.domain.model.job.ProductionJobStatus
import com.sucharu.sucharupro.domain.model.order.OrderPriority
import com.sucharu.sucharupro.domain.model.production.ProductionStageStatus
import com.sucharu.sucharupro.domain.model.production.ProductionStageType
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Concurrency, race-condition safety, and snapshot invariance tests for Stage Execution (Module 04 Step 05).
 */
class ProductionStageExecutionConcurrencyTest {

    private lateinit var dataSource: FakeProductionJobDataSource
    private lateinit var repository: ProductionJobRepository

    private val sampleJob = ProductionJob(
        jobId = "job-conc-exec-01",
        jobNumber = "JOB-2026-0001",
        orderId = "ord-001",
        orderNumber = "ORD-2026-0001",
        customerId = "cus-001",
        handoffId = "hnd-001",
        title = "ম্যাগাজিন প্রিন্টিং",
        quantity = 5000,
        unit = "Pcs",
        priority = OrderPriority.HIGH,
        status = ProductionJobStatus.READY_FOR_PRODUCTION,
        stages = ProductionJobStage.createInitialStages("job-conc-exec-01"),
        createdAt = "2026-08-16T10:00:00Z",
        updatedAt = "2026-08-16T10:00:00Z"
    )

    private val stage1Id = sampleJob.stages.first { it.stageType == ProductionStageType.DESIGN }.stageId

    @Before
    fun setUp() {
        runBlocking {
            dataSource = FakeProductionJobDataSource()
            repository = ProductionJobRepositoryImpl(dataSource)
            repository.createJob(sampleJob)
        }
    }

    @Test
    fun simultaneousStartCalls_onlyOneWins_exactlyOneEventRecorded() = runBlocking {
        // 10 concurrent calls to start the same stage
        val results = (1..10).map { index ->
            async {
                repository.startStage(
                    jobId = sampleJob.jobId,
                    stageId = stage1Id,
                    actorId = "op-$index",
                    actorName = "Operator $index",
                    notes = "Concurrent start attempt $index",
                    timestamp = "2026-08-16T10:30:00Z"
                )
            }
        }.awaitAll()

        val successCount = results.count { it is DomainResult.Success }
        val errorCount = results.count { it is DomainResult.Error }

        assertEquals(1, successCount)
        assertEquals(9, errorCount)

        val executions = repository.getStageExecutionsForJob(sampleJob.jobId).first()
        assertEquals(1, executions.size)

        val activities = repository.getProductionActivityEvents(sampleJob.jobId).first()
        val startEvents = activities.filter { it.eventType == ProductionActivityType.STAGE_STARTED }
        assertEquals(1, startEvents.size)
    }

    @Test
    fun executionMutations_doNotAlterCommercialQuantitiesOrOrderReferences() = runBlocking {
        repository.startStage(
            jobId = sampleJob.jobId,
            stageId = stage1Id,
            actorId = "op-01",
            actorName = "Rahim",
            notes = "শুরু",
            timestamp = "2026-08-16T10:30:00Z"
        )
        repository.completeStage(
            jobId = sampleJob.jobId,
            stageId = stage1Id,
            actorId = "op-01",
            notes = "শেষ",
            timestamp = "2026-08-16T11:00:00Z"
        )

        val fetchedJob = (repository.findJobById(sampleJob.jobId) as DomainResult.Success).data
        assertEquals(5000, fetchedJob.quantity)
        assertEquals("Pcs", fetchedJob.unit)
        assertEquals(OrderPriority.HIGH, fetchedJob.priority)
        assertEquals("ord-001", fetchedJob.orderId)
        assertEquals("hnd-001", fetchedJob.handoffId)
        assertEquals("JOB-2026-0001", fetchedJob.jobNumber)
    }

    @Test
    fun banglaUnicodeFidelity_preservedInExecutionAndActivityRecords() = runBlocking {
        repository.startStage(
            jobId = sampleJob.jobId,
            stageId = stage1Id,
            actorId = "op-01",
            actorName = "রহিম আহমেদ",
            notes = "ডিজাইন লেআউট অনুমোদন অনুযায়ী প্রস্তুত",
            timestamp = "2026-08-16T10:30:00Z"
        )

        val execution = repository.getStageExecution(sampleJob.jobId, stage1Id).first()
        assertEquals("রহিম আহমেদ", execution?.operatorName)
        assertEquals("ডিজাইন লেআউট অনুমোদন অনুযায়ী প্রস্তুত", execution?.startRemarks)

        val activities = repository.getProductionActivityEvents(sampleJob.jobId).first()
        assertEquals("রহিম আহমেদ", activities[0].operatorName)
        assertEquals("ডিজাইন লেআউট অনুমোদন অনুযায়ী প্রস্তুত", activities[0].message)
    }
}
