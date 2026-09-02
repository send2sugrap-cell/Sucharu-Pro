package com.sucharu.sucharupro.domain.validation

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.job.ProductionJob
import com.sucharu.sucharupro.domain.model.job.ProductionJobStage
import com.sucharu.sucharupro.domain.model.job.ProductionJobStatus
import com.sucharu.sucharupro.domain.model.job.ProductionStageAssignment
import com.sucharu.sucharupro.domain.model.job.StageAssignmentStatus
import com.sucharu.sucharupro.domain.model.order.OrderPriority
import com.sucharu.sucharupro.domain.model.production.ProductionStageStatus
import com.sucharu.sucharupro.domain.model.production.ProductionStageType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Comprehensive unit tests for [ProductionStageAssignmentValidator].
 */
class ProductionStageAssignmentValidatorTest {

    private val baseJob = ProductionJob(
        jobId = "job-val-01",
        jobNumber = "JOB-2026-0001",
        orderId = "ord-001",
        orderNumber = "ORD-2026-0001",
        customerId = "cus-001",
        handoffId = "hnd-001",
        title = "বই মুদ্রণ",
        quantity = 1000,
        unit = "কপি",
        priority = OrderPriority.NORMAL,
        status = ProductionJobStatus.READY_FOR_PRODUCTION,
        stages = ProductionJobStage.createInitialStages("job-val-01"),
        createdAt = "2026-08-16T10:00:00Z",
        updatedAt = "2026-08-16T10:00:00Z"
    )

    private val designStageId = baseJob.stages.first { it.stageType == ProductionStageType.DESIGN }.stageId
    private val activeAssignment = ProductionStageAssignment(
        assignmentId = "asg-001",
        jobId = "job-val-01",
        stageId = designStageId,
        stageType = ProductionStageType.DESIGN,
        operatorId = "op-01",
        operatorName = "Rahim",
        assignedAt = "2026-08-16T10:05:00Z",
        status = StageAssignmentStatus.ASSIGNED
    )

    @Test
    fun validAssignment_succeeds() {
        val result = ProductionStageAssignmentValidator.validateAssignment(
            job = baseJob,
            stageId = designStageId,
            operatorId = "op-01",
            operatorName = "Rahim Ahmed"
        )
        assertTrue(result is DomainResult.Success)
        assertEquals(designStageId, (result as DomainResult.Success).data.stageId)
    }

    @Test
    fun blankOperatorId_isRejected() {
        val result = ProductionStageAssignmentValidator.validateAssignment(
            job = baseJob,
            stageId = designStageId,
            operatorId = "   ",
            operatorName = "Rahim Ahmed"
        )
        assertTrue(result is DomainResult.Error)
        assertTrue((result as DomainResult.Error).message.contains("Operator ID"))
    }

    @Test
    fun blankOperatorName_isRejected() {
        val result = ProductionStageAssignmentValidator.validateAssignment(
            job = baseJob,
            stageId = designStageId,
            operatorId = "op-01",
            operatorName = "   "
        )
        assertTrue(result is DomainResult.Error)
        assertTrue((result as DomainResult.Error).message.contains("Operator Name"))
    }

    @Test
    fun invalidStageId_isRejected() {
        val result = ProductionStageAssignmentValidator.validateAssignment(
            job = baseJob,
            stageId = "non-existent-stage",
            operatorId = "op-01",
            operatorName = "Rahim Ahmed"
        )
        assertTrue(result is DomainResult.Error)
        assertTrue((result as DomainResult.Error).message.contains("not found"))
    }

    @Test
    fun terminalDeliveredJob_rejectsAssignment() {
        val deliveredJob = baseJob.copy(status = ProductionJobStatus.DELIVERED)
        val result = ProductionStageAssignmentValidator.validateAssignment(
            job = deliveredJob,
            stageId = designStageId,
            operatorId = "op-01",
            operatorName = "Rahim Ahmed"
        )
        assertTrue(result is DomainResult.Error)
        assertTrue((result as DomainResult.Error).message.contains("Delivered"))
    }

    @Test
    fun terminalCancelledJob_rejectsAssignment() {
        val cancelledJob = baseJob.copy(status = ProductionJobStatus.CANCELLED)
        val result = ProductionStageAssignmentValidator.validateAssignment(
            job = cancelledJob,
            stageId = designStageId,
            operatorId = "op-01",
            operatorName = "Rahim Ahmed"
        )
        assertTrue(result is DomainResult.Error)
        assertTrue((result as DomainResult.Error).message.contains("Cancelled"))
    }

    @Test
    fun completedStage_rejectsAssignment() {
        val jobWithCompletedStage = baseJob.copy(
            stages = baseJob.stages.map {
                if (it.stageId == designStageId) it.copy(status = ProductionStageStatus.COMPLETED) else it
            }
        )
        val result = ProductionStageAssignmentValidator.validateAssignment(
            job = jobWithCompletedStage,
            stageId = designStageId,
            operatorId = "op-01",
            operatorName = "Rahim Ahmed"
        )
        assertTrue(result is DomainResult.Error)
        assertTrue((result as DomainResult.Error).message.contains("completed"))
    }

    @Test
    fun skippedStage_rejectsAssignment() {
        val jobWithSkippedStage = baseJob.copy(
            stages = baseJob.stages.map {
                if (it.stageId == designStageId) it.copy(status = ProductionStageStatus.SKIPPED) else it
            }
        )
        val result = ProductionStageAssignmentValidator.validateAssignment(
            job = jobWithSkippedStage,
            stageId = designStageId,
            operatorId = "op-01",
            operatorName = "Rahim Ahmed"
        )
        assertTrue(result is DomainResult.Error)
        assertTrue((result as DomainResult.Error).message.contains("skipped"))
    }

    @Test
    fun validReassignment_succeeds() {
        val result = ProductionStageAssignmentValidator.validateReassignment(
            job = baseJob,
            stageId = designStageId,
            currentAssignment = activeAssignment,
            newOperatorId = "op-02",
            newOperatorName = "Karim Chowdhury"
        )
        assertTrue(result is DomainResult.Success)
    }

    @Test
    fun reassignment_withoutCurrentActiveAssignment_isRejected() {
        val inactiveAssignment = activeAssignment.copy(status = StageAssignmentStatus.REASSIGNED)
        val result = ProductionStageAssignmentValidator.validateReassignment(
            job = baseJob,
            stageId = designStageId,
            currentAssignment = inactiveAssignment,
            newOperatorId = "op-02",
            newOperatorName = "Karim Chowdhury"
        )
        assertTrue(result is DomainResult.Error)
        assertTrue((result as DomainResult.Error).message.contains("No active operator assignment"))
    }

    @Test
    fun validUnassignment_succeeds() {
        val result = ProductionStageAssignmentValidator.validateUnassignment(
            job = baseJob,
            stageId = designStageId,
            currentAssignment = activeAssignment
        )
        assertTrue(result is DomainResult.Success)
    }

    @Test
    fun unassignment_whenStageInProgress_isRejected() {
        val inProgressJob = baseJob.copy(
            stages = baseJob.stages.map {
                if (it.stageId == designStageId) it.copy(status = ProductionStageStatus.IN_PROGRESS) else it
            }
        )
        val result = ProductionStageAssignmentValidator.validateUnassignment(
            job = inProgressJob,
            stageId = designStageId,
            currentAssignment = activeAssignment
        )
        assertTrue(result is DomainResult.Error)
        assertTrue((result as DomainResult.Error).message.contains("in progress"))
    }

    @Test
    fun unassignment_withoutActiveAssignment_isRejected() {
        val result = ProductionStageAssignmentValidator.validateUnassignment(
            job = baseJob,
            stageId = designStageId,
            currentAssignment = null
        )
        assertTrue(result is DomainResult.Error)
        assertTrue((result as DomainResult.Error).message.contains("No active operator assignment"))
    }
}
