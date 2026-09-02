package com.sucharu.sucharupro.domain.validation

import com.sucharu.sucharupro.data.datasource.FakeReturnDataSource
import com.sucharu.sucharupro.data.repository.ReturnRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.common.Money
import com.sucharu.sucharupro.domain.model.returns.ReturnItem
import com.sucharu.sucharupro.domain.model.returns.ReturnReason
import com.sucharu.sucharupro.domain.model.returns.ReturnRequest
import com.sucharu.sucharupro.domain.model.returns.ReturnResolutionType
import com.sucharu.sucharupro.domain.model.returns.ReturnSettlement
import com.sucharu.sucharupro.domain.model.returns.ReturnSettlementStatus
import com.sucharu.sucharupro.domain.model.returns.ReturnStatus
import com.sucharu.sucharupro.domain.model.user.UserRole
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * RBAC authorization tests for Return Settlement (Module 11 Step 05 Chunk 03).
 */
class ReturnSettlementRbacTest {

    private lateinit var dataSource: FakeReturnDataSource
    private lateinit var repository: ReturnRepositoryImpl

    private val projectId = "PRJ-RBAC-01"
    private val customerId = "CUST-RBAC-01"
    private val returnId = "RET-RBAC-101"

    private val testReturn = ReturnRequest(
        returnId = returnId,
        projectId = projectId,
        returnNo = "RN-RBAC-101",
        customerId = customerId,
        originalChallanId = "CHAL-01",
        status = ReturnStatus.PROCESSED,
        reason = ReturnReason.PRINTING_DEFECT,
        requestedBy = "CUST-USER",
        version = 1L
    )

    private val testItem = ReturnItem(
        returnItemId = "RI-RBAC-01",
        returnId = returnId,
        productId = "PROD-01",
        originalChallanItemId = "CI-01",
        requestedQuantity = 10,
        acceptedQuantity = 10,
        rejectedQuantity = 0,
        unit = "PCS"
    )

    private fun sampleSettlement(idempKey: String) = ReturnSettlement(
        settlementId = "SETTLE-$idempKey",
        returnId = returnId,
        projectId = projectId,
        customerId = customerId,
        resolutionType = ReturnResolutionType.CREDIT_NOTE,
        amount = Money(500.0),
        status = ReturnSettlementStatus.COMPLETED,
        creditNoteId = "CN-$idempKey",
        settledBy = "ACTOR",
        version = 1L,
        idempotencyKey = idempKey
    )

    @Before
    fun setUp() = runBlocking {
        dataSource = FakeReturnDataSource()
        repository = ReturnRepositoryImpl(dataSource)
        dataSource.insertReturn(testReturn, listOf(testItem))
    }

    @Test
    fun `authorized roles ADMIN, MANAGER, and ACCOUNTS can settle returns`() = runBlocking {
        val authorizedRoles = listOf(UserRole.ADMIN, UserRole.MANAGER, UserRole.ACCOUNTS)

        for ((index, role) in authorizedRoles.withIndex()) {
            val req = testReturn.copy(returnId = "RET-AUTH-$index", version = 1L)
            dataSource.insertReturn(req, listOf(testItem.copy(returnId = req.returnId)))

            val result = repository.settleReturn(
                settlement = sampleSettlement("KEY-AUTH-$index").copy(returnId = req.returnId),
                actorId = "ACTOR-$role",
                expectedVersion = 1L,
                callerRole = role,
                callerProjectId = projectId
            )

            assertTrue("Role $role must be authorized to settle", result is DomainResult.Success)
        }
    }

    @Test
    fun `unauthorized roles are rejected from settling returns`() = runBlocking {
        val unauthorizedRoles = listOf(
            UserRole.CUSTOMER,
            UserRole.STAFF,
            UserRole.WAREHOUSE,
            UserRole.QC_INSPECTOR,
            UserRole.DESIGNER,
            UserRole.VENDOR,
            UserRole.AFFILIATE
        )

        for (role in unauthorizedRoles) {
            val result = repository.settleReturn(
                settlement = sampleSettlement("KEY-UNAUTH-$role"),
                actorId = "ACTOR-UNAUTH",
                expectedVersion = 1L,
                callerRole = role,
                callerProjectId = projectId
            )

            assertTrue("Role $role must be rejected from settling returns", result is DomainResult.Error)
            val error = (result as DomainResult.Error).message
            assertTrue(error.contains("unauthorized", ignoreCase = true) || error.contains("Requires ADMIN", ignoreCase = true))
        }
    }
}
