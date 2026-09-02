package com.sucharu.sucharupro.domain.validation

import com.sucharu.sucharupro.data.datasource.FakeReturnDataSource
import com.sucharu.sucharupro.data.repository.ReturnRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.returns.InspectionChecklistItem
import com.sucharu.sucharupro.domain.model.returns.ReturnDecision
import com.sucharu.sucharupro.domain.model.returns.ReturnInspection
import com.sucharu.sucharupro.domain.model.returns.ReturnInspectionStatus
import com.sucharu.sucharupro.domain.model.returns.ReturnItem
import com.sucharu.sucharupro.domain.model.returns.ReturnReason
import com.sucharu.sucharupro.domain.model.returns.ReturnRequest
import com.sucharu.sucharupro.domain.model.returns.ReturnStatus
import com.sucharu.sucharupro.domain.model.user.UserRole
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ReturnInspectionLifecycleTest {

    private lateinit var dataSource: FakeReturnDataSource
    private lateinit var repository: ReturnRepositoryImpl

    private val testReturn = ReturnRequest(
        returnId = "RET-INSP-01",
        projectId = "PRJ-01",
        returnNo = "RET-2026-001",
        customerId = "CUST-01",
        originalChallanId = "CH-01",
        status = ReturnStatus.UNDER_INSPECTION,
        reason = ReturnReason.PRINTING_DEFECT,
        requestedBy = "user-1",
        version = 2L
    )

    private val testItem = ReturnItem(
        returnItemId = "RI-01",
        returnId = "RET-INSP-01",
        productId = "PROD-01",
        originalChallanItemId = "CHI-01",
        requestedQuantity = 10,
        acceptedQuantity = 0,
        rejectedQuantity = 0
    )

    @Before
    fun setup() = runBlocking {
        dataSource = FakeReturnDataSource()
        repository = ReturnRepositoryImpl(dataSource)
        dataSource.insertReturn(testReturn, listOf(testItem))
    }

    @Test
    fun `recordInspection saves draft inspection in UNDER_INSPECTION status`() = runBlocking {
        val inspection = ReturnInspection(
            inspectionId = "INSP-01",
            returnId = testReturn.returnId,
            projectId = testReturn.projectId,
            inspectorId = "qc-inspector-1",
            status = ReturnInspectionStatus.IN_PROGRESS,
            checklist = listOf(InspectionChecklistItem("chk-1", "Seals intact", true)),
            findings = "Preliminary check ok"
        )

        val res = repository.recordInspection(
            inspection = inspection,
            actorId = "qc-inspector-1",
            callerRole = UserRole.QC_INSPECTOR,
            callerProjectId = testReturn.projectId
        )

        assertTrue(res is DomainResult.Success)
        val saved = (res as DomainResult.Success).data
        assertEquals(ReturnInspectionStatus.IN_PROGRESS, saved.status)

        val fetched = repository.getInspection(testReturn.returnId, UserRole.QC_INSPECTOR, testReturn.projectId)
        assertTrue(fetched is DomainResult.Success)
        assertNotNull((fetched as DomainResult.Success).data)
    }

    @Test
    fun `approveReturn transitions Return to APPROVED and completes inspection`() = runBlocking {
        val updatedItem = testItem.copy(acceptedQuantity = 10, rejectedQuantity = 0)
        val inspection = ReturnInspection(
            inspectionId = "INSP-01",
            returnId = testReturn.returnId,
            projectId = testReturn.projectId,
            inspectorId = "admin-1",
            status = ReturnInspectionStatus.COMPLETED,
            decision = ReturnDecision.APPROVE,
            findings = "All items match specifications"
        )

        val res = repository.approveReturn(
            returnId = testReturn.returnId,
            actorId = "admin-1",
            expectedVersion = testReturn.version,
            inspection = inspection,
            items = listOf(updatedItem),
            callerRole = UserRole.ADMIN,
            callerProjectId = testReturn.projectId
        )

        assertTrue(res is DomainResult.Success)
        val updated = (res as DomainResult.Success).data
        assertEquals(ReturnStatus.APPROVED, updated.status)
        assertEquals(testReturn.version + 1L, updated.version)

        val itemsRes = repository.getReturnItems(testReturn.returnId, UserRole.ADMIN, testReturn.projectId)
        assertTrue(itemsRes is DomainResult.Success)
        assertEquals(10, (itemsRes as DomainResult.Success).data.first().acceptedQuantity)
    }

    @Test
    fun `rejectReturn transitions Return to REJECTED with mandatory reason`() = runBlocking {
        val updatedItem = testItem.copy(acceptedQuantity = 0, rejectedQuantity = 10)
        val res = repository.rejectReturn(
            returnId = testReturn.returnId,
            actorId = "manager-1",
            expectedVersion = testReturn.version,
            rejectionReason = "Item damaged by customer negligence",
            items = listOf(updatedItem),
            callerRole = UserRole.MANAGER,
            callerProjectId = testReturn.projectId
        )

        assertTrue(res is DomainResult.Success)
        val updated = (res as DomainResult.Success).data
        assertEquals(ReturnStatus.REJECTED, updated.status)
        assertEquals(testReturn.version + 1L, updated.version)

        val inspectionRes = repository.getInspection(testReturn.returnId, UserRole.MANAGER, testReturn.projectId)
        assertTrue(inspectionRes is DomainResult.Success)
        val insp = (inspectionRes as DomainResult.Success).data
        assertNotNull(insp)
        assertEquals(ReturnDecision.REJECT, insp!!.decision)
        assertEquals("Item damaged by customer negligence", insp.decisionReason)
    }

    @Test
    fun `rejectReturn fails with blank rejection reason`() = runBlocking {
        val res = repository.rejectReturn(
            returnId = testReturn.returnId,
            actorId = "manager-1",
            expectedVersion = testReturn.version,
            rejectionReason = "  ",
            callerRole = UserRole.MANAGER,
            callerProjectId = testReturn.projectId
        )

        assertTrue(res is DomainResult.Error)
    }
}
