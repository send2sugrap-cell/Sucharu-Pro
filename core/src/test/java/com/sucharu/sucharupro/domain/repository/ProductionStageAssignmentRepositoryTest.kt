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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for repository-level stage operator assignment operations.
 */
class ProductionStageAssignmentRepositoryTest {

    private lateinit var dataSource: FakeProductionJobDataSource
    private lateinit var repository: ProductionJobRepository

    private val job = ProductionJob(
        jobId = "job-repo-01",
        jobNumber = "JOB-2026-0001",
        orderId = "ord-001",
        orderNumber = "ORD-2026-0001",
        customerId = "cus-001",
        handoffId = "hnd-001",
        title = "ক্যাটালগ মুদ্রণ",
        quantity = 500,
        unit = "Pcs",
        priority = OrderPriority.HIGH,
        status = ProductionJobStatus.READY_FOR_PRODUCTION,
        stages = ProductionJobStage.createInitialStages("job-repo-01"),
        createdAt = "2026-08-16T10:00:00Z",
        updatedAt = "2026-08-16T10:00:00Z"
    )

    private val printingStageId = job.stages.first { it.stageType == ProductionStageType.PRINTING }.stageId

    @Before
    fun setUp() {
        runBlocking {
            dataSource = FakeProductionJobDataSource()
            repository = ProductionJobRepositoryImpl(dataSource)
            repository.createJob(job)
        }
    }

    @Test
    fun assignStageOperator_succeeds_andUpdatesJobStage() = runBlocking {
        val result = repository.assignStageOperator(
            jobId = job.jobId,
            stageId = printingStageId,
            operatorId = "op-01",
            operatorName = "রহিম আহমেদ",
            assignedBy = "Supervisor",
            notes = "৪ কালার অফসেট প্রিন্ট",
            timestamp = "2026-08-16T10:30:00Z"
        )

        assertTrue("Expected Success, got $result", result is DomainResult.Success)
        val updatedJob = (result as DomainResult.Success).data
        val updatedStage = updatedJob.stages.find { it.stageId == printingStageId }
        assertNotNull(updatedStage)
        assertEquals("op-01", updatedStage?.assignedUserId)
        assertEquals("রহিম আহমেদ", updatedStage?.assignedUserName)

        // Verify stage remains in PENDING (assignment does NOT start stage)
        assertEquals(ProductionStageStatus.PENDING, updatedStage?.status)

        // Verify assignment record in repository
        val assignment = repository.getStageAssignment(job.jobId, printingStageId).first()
        assertNotNull(assignment)
        assertEquals("op-01", assignment?.operatorId)
        assertEquals(StageAssignmentStatus.ASSIGNED, assignment?.status)
    }

    @Test
    fun duplicateActiveAssignment_isRejected() = runBlocking {
        repository.assignStageOperator(
            jobId = job.jobId,
            stageId = printingStageId,
            operatorId = "op-01",
            operatorName = "রহিম আহমেদ",
            timestamp = "2026-08-16T10:30:00Z"
        )

        val duplicateResult = repository.assignStageOperator(
            jobId = job.jobId,
            stageId = printingStageId,
            operatorId = "op-02",
            operatorName = "করিম চৌধুরী",
            timestamp = "2026-08-16T10:35:00Z"
        )

        assertTrue(duplicateResult is DomainResult.Error)
        assertTrue((duplicateResult as DomainResult.Error).message.contains("already has an active operator"))
    }

    @Test
    fun reassignStageOperator_supersedesOldAssignment_andUpdatesActiveOperator() = runBlocking {
        repository.assignStageOperator(
            jobId = job.jobId,
            stageId = printingStageId,
            operatorId = "op-01",
            operatorName = "রহিম আহমেদ",
            timestamp = "2026-08-16T10:30:00Z"
        )

        val reassignResult = repository.reassignStageOperator(
            jobId = job.jobId,
            stageId = printingStageId,
            newOperatorId = "op-02",
            newOperatorName = "করিম চৌধুরী",
            reassignedBy = "Shift Incharge",
            notes = "নাইট শিফট পরিবর্তন",
            timestamp = "2026-08-16T14:00:00Z"
        )

        assertTrue(reassignResult is DomainResult.Success)
        val updatedJob = (reassignResult as DomainResult.Success).data
        val stage = updatedJob.stages.find { it.stageId == printingStageId }
        assertEquals("op-02", stage?.assignedUserId)
        assertEquals("করিম চৌধুরী", stage?.assignedUserName)

        // All assignments for Job should have 2 records (1 REASSIGNED, 1 ASSIGNED)
        val allAssignments = repository.getAssignmentsForJob(job.jobId).first()
        assertEquals(2, allAssignments.size)
        val oldAssignment = allAssignments.find { it.operatorId == "op-01" }
        val newAssignment = allAssignments.find { it.operatorId == "op-02" }

        assertEquals(StageAssignmentStatus.REASSIGNED, oldAssignment?.status)
        assertEquals(StageAssignmentStatus.ASSIGNED, newAssignment?.status)
        assertEquals("2026-08-16T14:00:00Z", oldAssignment?.reassignedAt)
    }

    @Test
    fun unassignStageOperator_clearsAssignedUser_andMarksStatusUnassigned() = runBlocking {
        repository.assignStageOperator(
            jobId = job.jobId,
            stageId = printingStageId,
            operatorId = "op-01",
            operatorName = "রহিম আহমেদ",
            timestamp = "2026-08-16T10:30:00Z"
        )

        val unassignResult = repository.unassignStageOperator(
            jobId = job.jobId,
            stageId = printingStageId,
            unassignedBy = "Manager",
            reason = "অপারেটর ছুটিতে আছেন",
            timestamp = "2026-08-16T11:00:00Z"
        )

        assertTrue(unassignResult is DomainResult.Success)
        val updatedJob = (unassignResult as DomainResult.Success).data
        val stage = updatedJob.stages.find { it.stageId == printingStageId }
        assertNull(stage?.assignedUserId)
        assertNull(stage?.assignedUserName)

        val activeAssignment = repository.getStageAssignment(job.jobId, printingStageId).first()
        assertNull(activeAssignment)

        val allAssignments = repository.getAssignmentsForJob(job.jobId).first()
        assertEquals(1, allAssignments.size)
        assertEquals(StageAssignmentStatus.UNASSIGNED, allAssignments[0].status)
    }

    @Test
    fun getAssignmentsForOperator_returnsOnlyTargetOperatorAssignments() = runBlocking {
        val bindingStageId = job.stages.first { it.stageType == ProductionStageType.BINDING }.stageId

        repository.assignStageOperator(job.jobId, printingStageId, "op-01", "Rahim", timestamp = "2026-08-16T10:00:00Z")
        repository.assignStageOperator(job.jobId, bindingStageId, "op-02", "Karim", timestamp = "2026-08-16T10:05:00Z")

        val rahimAssignments = repository.getAssignmentsForOperator("op-01").first()
        assertEquals(1, rahimAssignments.size)
        assertEquals(ProductionStageType.PRINTING, rahimAssignments[0].stageType)

        val karimAssignments = repository.getAssignmentsForOperator("op-02").first()
        assertEquals(1, karimAssignments.size)
        assertEquals(ProductionStageType.BINDING, karimAssignments[0].stageType)
    }
}
