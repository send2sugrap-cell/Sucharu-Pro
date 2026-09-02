package com.sucharu.sucharupro.domain.validation

import com.sucharu.sucharupro.data.datasource.FakeReturnDataSource
import com.sucharu.sucharupro.data.repository.ReturnRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.returns.InspectionChecklistItem
import com.sucharu.sucharupro.domain.model.returns.ReturnInspection
import com.sucharu.sucharupro.domain.model.returns.ReturnInspectionStatus
import com.sucharu.sucharupro.domain.model.returns.ReturnItem
import com.sucharu.sucharupro.domain.model.returns.ReturnReason
import com.sucharu.sucharupro.domain.model.returns.ReturnRequest
import com.sucharu.sucharupro.domain.model.returns.ReturnStatus
import com.sucharu.sucharupro.domain.model.user.UserRole
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ReturnInspectionRbacTest {

    private lateinit var dataSource: FakeReturnDataSource
    private lateinit var repository: ReturnRepositoryImpl

    private val testReturn = ReturnRequest(
        returnId = "RET-RBAC-01",
        projectId = "PRJ-01",
        returnNo = "RET-2026-001",
        customerId = "CUST-01",
        originalChallanId = null,
        status = ReturnStatus.UNDER_INSPECTION,
        reason = ReturnReason.PRINTING_DEFECT,
        requestedBy = "user-1",
        version = 1L
    )

    private val testItem = ReturnItem(
        returnItemId = "RI-01",
        returnId = "RET-RBAC-01",
        productId = "PROD-01",
        originalChallanItemId = null,
        requestedQuantity = 5,
        acceptedQuantity = 5,
        rejectedQuantity = 0
    )

    @Before
    fun setup() = runBlocking {
        dataSource = FakeReturnDataSource()
        repository = ReturnRepositoryImpl(dataSource)
        dataSource.insertReturn(testReturn, listOf(testItem))
    }

    @Test
    fun `QC_INSPECTOR and WAREHOUSE can record inspection`() = runBlocking {
        val roles = listOf(UserRole.QC_INSPECTOR, UserRole.WAREHOUSE, UserRole.ADMIN, UserRole.MANAGER)
        for (role in roles) {
            val inspection = ReturnInspection(
                inspectionId = "INSP-${role.name}",
                returnId = testReturn.returnId,
                projectId = testReturn.projectId,
                inspectorId = "user-${role.name}",
                status = ReturnInspectionStatus.IN_PROGRESS,
                checklist = listOf(InspectionChecklistItem("c1", "Check 1", true))
            )
            val res = repository.recordInspection(
                inspection = inspection,
                actorId = "user-${role.name}",
                callerRole = role,
                callerProjectId = testReturn.projectId
            )
            assertTrue("Role $role should be authorized to record inspection", res is DomainResult.Success)
        }
    }

    @Test
    fun `STAFF, DESIGNER, ACCOUNTS, CUSTOMER, VENDOR cannot record inspection`() = runBlocking {
        val unauthorized = listOf(
            UserRole.STAFF,
            UserRole.DESIGNER,
            UserRole.ACCOUNTS,
            UserRole.CUSTOMER,
            UserRole.VENDOR,
            UserRole.AFFILIATE
        )
        for (role in unauthorized) {
            val inspection = ReturnInspection(
                inspectionId = "INSP-UNAUTH",
                returnId = testReturn.returnId,
                projectId = testReturn.projectId,
                inspectorId = "user-unauth",
                status = ReturnInspectionStatus.IN_PROGRESS
            )
            val res = repository.recordInspection(
                inspection = inspection,
                actorId = "user-unauth",
                callerRole = role,
                callerProjectId = testReturn.projectId
            )
            assertTrue("Role $role should NOT be authorized to record inspection", res is DomainResult.Error)
        }
    }

    @Test
    fun `ADMIN and MANAGER can approve or reject return`() = runBlocking {
        val authorized = listOf(UserRole.ADMIN, UserRole.MANAGER)
        for (role in authorized) {
            val res = repository.approveReturn(
                returnId = testReturn.returnId,
                actorId = "admin-1",
                expectedVersion = testReturn.version,
                items = listOf(testItem),
                callerRole = role,
                callerProjectId = testReturn.projectId
            )
            assertTrue("Role $role should be authorized to approve return", res is DomainResult.Success)
            // Reset return status for next role
            dataSource.updateReturn(testReturn)
        }
    }

    @Test
    fun `QC_INSPECTOR and WAREHOUSE cannot approve or reject return`() = runBlocking {
        val unauth = listOf(UserRole.QC_INSPECTOR, UserRole.WAREHOUSE, UserRole.STAFF, UserRole.DESIGNER)
        for (role in unauth) {
            val approveRes = repository.approveReturn(
                returnId = testReturn.returnId,
                actorId = "user-1",
                expectedVersion = testReturn.version,
                callerRole = role,
                callerProjectId = testReturn.projectId
            )
            assertTrue("Role $role should NOT be authorized to approve return", approveRes is DomainResult.Error)

            val rejectRes = repository.rejectReturn(
                returnId = testReturn.returnId,
                actorId = "user-1",
                expectedVersion = testReturn.version,
                rejectionReason = "Reason",
                callerRole = role,
                callerProjectId = testReturn.projectId
            )
            assertTrue("Role $role should NOT be authorized to reject return", rejectRes is DomainResult.Error)
        }
    }
}
