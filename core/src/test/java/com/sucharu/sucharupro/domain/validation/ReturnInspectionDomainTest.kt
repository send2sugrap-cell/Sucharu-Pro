package com.sucharu.sucharupro.domain.validation

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.returns.InspectionChecklistItem
import com.sucharu.sucharupro.domain.model.returns.ReturnDecision
import com.sucharu.sucharupro.domain.model.returns.ReturnInspection
import com.sucharu.sucharupro.domain.model.returns.ReturnInspectionStatus
import com.sucharu.sucharupro.domain.model.returns.ReturnItem
import com.sucharu.sucharupro.domain.model.returns.ReturnReason
import com.sucharu.sucharupro.domain.model.returns.ReturnRequest
import com.sucharu.sucharupro.domain.model.returns.ReturnStatus
import com.sucharu.sucharupro.domain.validation.returns.ReturnInspectionValidator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReturnInspectionDomainTest {

    @Test
    fun `valid ReturnInspection passes validation`() {
        val inspection = ReturnInspection(
            inspectionId = "INSP-01",
            returnId = "RET-01",
            projectId = "PRJ-01",
            inspectorId = "user-qc-1",
            status = ReturnInspectionStatus.IN_PROGRESS,
            checklist = listOf(
                InspectionChecklistItem("chk-1", "Physical package checked", true)
            ),
            findings = "Minor scratch observed"
        )
        val res = ReturnInspectionValidator.validateInspection(inspection)
        assertTrue(res is DomainResult.Success)
    }

    @Test
    fun `completed inspection requires decision`() {
        val inspection = ReturnInspection(
            inspectionId = "INSP-01",
            returnId = "RET-01",
            projectId = "PRJ-01",
            inspectorId = "user-qc-1",
            status = ReturnInspectionStatus.COMPLETED,
            decision = ReturnDecision.APPROVE
        )
        val res = ReturnInspectionValidator.validateInspection(inspection)
        assertTrue(res is DomainResult.Success)
    }

    @Test
    fun `completed inspection with reject requires decisionReason`() {
        val inspection = ReturnInspection(
            inspectionId = "INSP-01",
            returnId = "RET-01",
            projectId = "PRJ-01",
            inspectorId = "user-qc-1",
            status = ReturnInspectionStatus.COMPLETED,
            decision = ReturnDecision.REJECT,
            decisionReason = "Physical damage beyond repair"
        )
        val res = ReturnInspectionValidator.validateInspection(inspection)
        assertTrue(res is DomainResult.Success)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `completed inspection with reject without reason throws IllegalArgumentException`() {
        ReturnInspection(
            inspectionId = "INSP-01",
            returnId = "RET-01",
            projectId = "PRJ-01",
            inspectorId = "user-qc-1",
            status = ReturnInspectionStatus.COMPLETED,
            decision = ReturnDecision.REJECT,
            decisionReason = null
        )
    }

    @Test
    fun `validateEligibleForInspection passes only for UNDER_INSPECTION`() {
        val underInspection = ReturnRequest(
            returnId = "RET-01",
            projectId = "PRJ-01",
            returnNo = "RET-2026-001",
            customerId = "CUST-01",
            originalChallanId = null,
            status = ReturnStatus.UNDER_INSPECTION,
            reason = ReturnReason.PRINTING_DEFECT,
            requestedBy = "user-1"
        )
        val res = ReturnInspectionValidator.validateEligibleForInspection(underInspection)
        assertTrue(res is DomainResult.Success)

        val requested = underInspection.copy(status = ReturnStatus.REQUESTED)
        val resRequested = ReturnInspectionValidator.validateEligibleForInspection(requested)
        assertTrue(resRequested is DomainResult.Error)

        val approved = underInspection.copy(status = ReturnStatus.APPROVED)
        val resApproved = ReturnInspectionValidator.validateEligibleForInspection(approved)
        assertTrue(resApproved is DomainResult.Error)
    }

    @Test
    fun `validateInspectionItemQuantities validates item quantities for approval and rejection`() {
        val item = ReturnItem(
            returnItemId = "RI-01",
            returnId = "RET-01",
            productId = "PROD-01",
            originalChallanItemId = null,
            requestedQuantity = 10,
            acceptedQuantity = 8,
            rejectedQuantity = 2
        )

        val approveRes = ReturnInspectionValidator.validateInspectionItemQuantities(
            listOf(item),
            ReturnDecision.APPROVE
        )
        assertTrue(approveRes is DomainResult.Success)

        val rejectRes = ReturnInspectionValidator.validateInspectionItemQuantities(
            listOf(item),
            ReturnDecision.REJECT
        )
        assertTrue(rejectRes is DomainResult.Success)

        // Item with 0 accepted should fail approve if all items have 0 accepted
        val zeroAccepted = item.copy(acceptedQuantity = 0, rejectedQuantity = 10)
        val failApprove = ReturnInspectionValidator.validateInspectionItemQuantities(
            listOf(zeroAccepted),
            ReturnDecision.APPROVE
        )
        assertTrue(failApprove is DomainResult.Error)
    }
}
