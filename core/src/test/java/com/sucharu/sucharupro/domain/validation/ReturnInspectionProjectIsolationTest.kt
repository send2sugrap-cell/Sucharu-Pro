package com.sucharu.sucharupro.domain.validation

import com.sucharu.sucharupro.data.datasource.FakeReturnDataSource
import com.sucharu.sucharupro.data.repository.ReturnRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
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

class ReturnInspectionProjectIsolationTest {

    private lateinit var dataSource: FakeReturnDataSource
    private lateinit var repository: ReturnRepositoryImpl

    private val returnA = ReturnRequest(
        returnId = "RET-A",
        projectId = "PRJ-A",
        returnNo = "RET-PRJ-A",
        customerId = "CUST-A",
        originalChallanId = null,
        status = ReturnStatus.UNDER_INSPECTION,
        reason = ReturnReason.PRINTING_DEFECT,
        requestedBy = "user-A",
        version = 1L
    )

    private val itemA = ReturnItem(
        returnItemId = "RI-A",
        returnId = "RET-A",
        productId = "PROD-A",
        originalChallanItemId = null,
        requestedQuantity = 5,
        acceptedQuantity = 5,
        rejectedQuantity = 0
    )

    @Before
    fun setup() = runBlocking {
        dataSource = FakeReturnDataSource()
        repository = ReturnRepositoryImpl(dataSource)
        dataSource.insertReturn(returnA, listOf(itemA))
    }

    @Test
    fun `cannot record inspection from different project`() = runBlocking {
        val inspection = ReturnInspection(
            inspectionId = "INSP-A",
            returnId = returnA.returnId,
            projectId = "PRJ-A",
            inspectorId = "qc-1",
            status = ReturnInspectionStatus.IN_PROGRESS
        )

        val res = repository.recordInspection(
            inspection = inspection,
            actorId = "qc-1",
            callerRole = UserRole.QC_INSPECTOR,
            callerProjectId = "PRJ-B" // Project mismatch
        )

        assertTrue("Should reject cross-project inspection", res is DomainResult.Error)
    }

    @Test
    fun `cannot approve return from different project`() = runBlocking {
        val res = repository.approveReturn(
            returnId = returnA.returnId,
            actorId = "admin-1",
            expectedVersion = returnA.version,
            callerRole = UserRole.ADMIN,
            callerProjectId = "PRJ-B" // Project mismatch
        )

        assertTrue("Should reject cross-project return approval", res is DomainResult.Error)
    }

    @Test
    fun `cannot reject return from different project`() = runBlocking {
        val res = repository.rejectReturn(
            returnId = returnA.returnId,
            actorId = "manager-1",
            expectedVersion = returnA.version,
            rejectionReason = "Bad quality",
            callerRole = UserRole.MANAGER,
            callerProjectId = "PRJ-B" // Project mismatch
        )

        assertTrue("Should reject cross-project return rejection", res is DomainResult.Error)
    }
}
