package com.sucharu.sucharupro.domain.repository

import com.sucharu.sucharupro.data.datasource.FakeProductionJobDataSource
import com.sucharu.sucharupro.data.repository.ProductionJobRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.job.ProductionJob
import com.sucharu.sucharupro.domain.model.job.ProductionJobStage
import com.sucharu.sucharupro.domain.model.job.ProductionJobStatus
import com.sucharu.sucharupro.domain.model.job.StageAssignmentStatus
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
 * Concurrency, snapshot invariance, and audit safety tests for Stage Operator Assignments.
 */
class ProductionStageAssignmentConcurrencyTest {

    private lateinit var dataSource: FakeProductionJobDataSource
    private lateinit var repository: ProductionJobRepository

    private val sampleJob = ProductionJob(
        jobId = "job-conc-01",
        jobNumber = "JOB-2026-0001",
        orderId = "ord-001",
        orderNumber = "ORD-2026-0001",
        customerId = "cus-001",
        handoffId = "hnd-001",
        title = "পুস্তিকা মুদ্রণ",
        quantity = 3000,
        unit = "কপি",
        priority = OrderPriority.URGENT,
        status = ProductionJobStatus.READY_FOR_PRODUCTION,
        stages = ProductionJobStage.createInitialStages("job-conc-01"),
        createdAt = "2026-08-16T10:00:00Z",
        updatedAt = "2026-08-16T10:00:00Z"
    )

    private val stage6Id = sampleJob.stages.first { it.stageType == ProductionStageType.PRINTING }.stageId

    @Before
    fun setUp() {
        runBlocking {
            dataSource = FakeProductionJobDataSource()
            repository = ProductionJobRepositoryImpl(dataSource)
            repository.createJob(sampleJob)
        }
    }

    @Test
    fun simultaneousAssignmentAttempts_onlyOneWins_noStateCorruption() = runBlocking {
        // 10 concurrent assignment attempts on the same stage
        val results = (1..10).map { index ->
            async {
                repository.assignStageOperator(
                    jobId = sampleJob.jobId,
                    stageId = stage6Id,
                    operatorId = "op-$index",
                    operatorName = "Operator $index",
                    timestamp = "2026-08-16T10:30:00Z"
                )
            }
        }.awaitAll()

        val successCount = results.count { it is DomainResult.Success }
        val errorCount = results.count { it is DomainResult.Error }

        assertEquals(1, successCount)
        assertEquals(9, errorCount)

        val activeAssignment = repository.getStageAssignment(sampleJob.jobId, stage6Id).first()
        assertTrue(activeAssignment != null && activeAssignment.status == StageAssignmentStatus.ASSIGNED)
    }

    @Test
    fun failedAssignment_producesNoStateMutation() = runBlocking {
        val blankOperatorResult = repository.assignStageOperator(
            jobId = sampleJob.jobId,
            stageId = stage6Id,
            operatorId = "",
            operatorName = "",
            timestamp = "2026-08-16T10:30:00Z"
        )

        assertTrue(blankOperatorResult is DomainResult.Error)

        val assignments = repository.getAssignmentsForJob(sampleJob.jobId).first()
        assertEquals(0, assignments.size)

        val fetchedJob = (repository.findJobById(sampleJob.jobId) as DomainResult.Success).data
        val stage = fetchedJob.stages.find { it.stageId == stage6Id }
        assertEquals(null, stage?.assignedUserId)
    }

    @Test
    fun assignmentMutations_doNotAlterCommercialQuantitiesOrTotals() = runBlocking {
        repository.assignStageOperator(
            jobId = sampleJob.jobId,
            stageId = stage6Id,
            operatorId = "op-01",
            operatorName = "Rahim",
            timestamp = "2026-08-16T10:30:00Z"
        )

        val fetchedJob = (repository.findJobById(sampleJob.jobId) as DomainResult.Success).data
        assertEquals(3000, fetchedJob.quantity)
        assertEquals("কপি", fetchedJob.unit)
        assertEquals(OrderPriority.URGENT, fetchedJob.priority)
        assertEquals("ord-001", fetchedJob.orderId)
        assertEquals("hnd-001", fetchedJob.handoffId)
    }

    @Test
    fun assignmentDoesNotStartStage_stageRemainsPending() = runBlocking {
        repository.assignStageOperator(
            jobId = sampleJob.jobId,
            stageId = stage6Id,
            operatorId = "op-01",
            operatorName = "Rahim",
            timestamp = "2026-08-16T10:30:00Z"
        )

        val fetchedJob = (repository.findJobById(sampleJob.jobId) as DomainResult.Success).data
        val stage = fetchedJob.stages.find { it.stageId == stage6Id }
        assertEquals(ProductionStageStatus.PENDING, stage?.status)
    }
}
