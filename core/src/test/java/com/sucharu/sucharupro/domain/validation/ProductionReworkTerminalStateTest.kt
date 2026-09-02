package com.sucharu.sucharupro.domain.validation

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.qc.ProductionRework
import com.sucharu.sucharupro.domain.model.qc.ReworkReason
import com.sucharu.sucharupro.domain.model.qc.ReworkStatus
import com.sucharu.sucharupro.domain.model.qc.ReworkType
import com.sucharu.sucharupro.domain.model.user.UserRole
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Terminal state and protected workflow boundary immutability tests (Module 06 Step 05).
 */
class ProductionReworkTerminalStateTest {

    private fun createRework(status: ReworkStatus): ProductionRework {
        return ProductionRework(
            reworkId = "rew-term-01",
            projectId = "proj-01",
            productionJobId = "job-01",
            reworkType = ReworkType.REPRINT,
            reason = ReworkReason.MACHINE_PROCESS_ERROR,
            status = status,
            affectedQuantity = 500,
            quantityUnit = "sheets",
            description = "Machine feeder jam damaged stock",
            requestedBy = "insp-01",
            requestedAt = "2026-08-17T10:00:00Z",
            assignedTo = "tech-01",
            assignedToName = "Karim Tech",
            assignedAt = "2026-08-17T10:30:00Z",
            createdAt = "2026-08-17T10:00:00Z",
            updatedAt = "2026-08-17T10:00:00Z"
        )
    }

    @Test
    fun cancelledState_cannotBeAssignedOrCompleted() {
        val cancelled = createRework(ReworkStatus.CANCELLED)

        val assignRes = ProductionReworkAssignmentValidator.validateAssignment(
            rework = cancelled,
            assigneeId = "tech-02",
            assigneeName = "New Tech",
            callerRole = UserRole.ADMIN
        )
        assertTrue(assignRes is DomainResult.Error)
        assertTrue((assignRes as DomainResult.Error).message.contains("Cannot assign terminal rework"))

        val completeRes = ProductionReworkValidator.validateCompletion(
            rework = cancelled,
            correctiveAction = "Fixed",
            actualReworkedQuantity = 500,
            completedBy = "tech-01",
            callerRole = UserRole.QC_INSPECTOR
        )
        assertTrue(completeRes is DomainResult.Error)
        assertTrue((completeRes as DomainResult.Error).message.contains("Cannot complete terminal rework"))
    }

    @Test
    fun rejectedState_cannotBeAssignedOrCompleted() {
        val rejected = createRework(ReworkStatus.REJECTED)

        val assignRes = ProductionReworkAssignmentValidator.validateAssignment(
            rework = rejected,
            assigneeId = "tech-02",
            assigneeName = "New Tech",
            callerRole = UserRole.MANAGER
        )
        assertTrue(assignRes is DomainResult.Error)

        val completeRes = ProductionReworkValidator.validateCompletion(
            rework = rejected,
            correctiveAction = "Fixed",
            actualReworkedQuantity = 500,
            completedBy = "tech-01",
            callerRole = UserRole.QC_INSPECTOR
        )
        assertTrue(completeRes is DomainResult.Error)
    }

    @Test
    fun returnedToQcState_cannotBeUnassignedOrModified() {
        val returned = createRework(ReworkStatus.RETURNED_TO_QC)

        val unassignRes = ProductionReworkAssignmentValidator.validateUnassignment(
            rework = returned,
            callerRole = UserRole.ADMIN
        )
        assertTrue(unassignRes is DomainResult.Error)
        assertTrue((unassignRes as DomainResult.Error).message.contains("after it has been returned to QC"))

        val returnAgain = ProductionReworkValidator.validateReturnToQc(
            rework = returned,
            actorId = "insp-01",
            callerRole = UserRole.QC_INSPECTOR
        )
        assertTrue(returnAgain is DomainResult.Error)
        assertTrue((returnAgain as DomainResult.Error).message.contains("not in COMPLETED status"))
    }
}
