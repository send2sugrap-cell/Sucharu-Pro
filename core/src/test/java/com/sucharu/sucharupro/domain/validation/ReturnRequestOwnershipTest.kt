package com.sucharu.sucharupro.domain.validation

import com.sucharu.sucharupro.data.datasource.FakeReturnDataSource
import com.sucharu.sucharupro.data.repository.ReturnRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.returns.ReturnItem
import com.sucharu.sucharupro.domain.model.returns.ReturnReason
import com.sucharu.sucharupro.domain.model.returns.ReturnRequest
import com.sucharu.sucharupro.domain.model.returns.ReturnStatus
import com.sucharu.sucharupro.domain.model.user.UserRole
import com.sucharu.sucharupro.domain.validation.returns.ReturnDomainValidator
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Customer ownership and isolation unit tests for Return Requests (Module 11 Step 02).
 */
class ReturnRequestOwnershipTest {

    private lateinit var dataSource: FakeReturnDataSource
    private lateinit var repository: ReturnRepositoryImpl

    private val projectId = "PRJ-TEST"
    private val customerA = "CUST-A"
    private val customerB = "CUST-B"

    @Before
    fun setUp() {
        dataSource = FakeReturnDataSource()
        repository = ReturnRepositoryImpl(dataSource)
    }

    private fun createReturn(
        returnId: String,
        customerId: String
    ) = ReturnRequest(
        returnId = returnId,
        projectId = projectId,
        returnNo = "RN-$returnId",
        customerId = customerId,
        originalChallanId = "CHAL-$returnId",
        status = ReturnStatus.REQUESTED,
        reason = ReturnReason.CUSTOMER_COMPLAINT,
        requestedBy = "USER-1",
        createdAt = System.currentTimeMillis(),
        updatedAt = System.currentTimeMillis(),
        version = 1L
    )

    @Test
    fun `Customer ownership validator passes for matching customer`() {
        val req = createReturn("RET-A1", customerA)
        val res = ReturnDomainValidator.validateCustomerOwnership(req, customerA)
        assertTrue(res is DomainResult.Success)
    }

    @Test
    fun `Customer ownership validator fails for mismatching customer`() {
        val req = createReturn("RET-A1", customerA)
        val res = ReturnDomainValidator.validateCustomerOwnership(req, customerB)
        assertTrue("Cross-customer ownership check should fail", res is DomainResult.Error)
        val err = (res as DomainResult.Error).message
        assertTrue(err.contains("Customer ownership violation") || err.contains("Access denied"))
    }

    @Test
    fun `listReturnsByCustomer returns only returns for specified customer`() = runBlocking {
        dataSource.insertReturn(createReturn("RET-A1", customerA), emptyList())
        dataSource.insertReturn(createReturn("RET-A2", customerA), emptyList())
        dataSource.insertReturn(createReturn("RET-B1", customerB), emptyList())

        val listA = repository.listReturnsByCustomer(
            projectId = projectId,
            customerId = customerA,
            callerRole = UserRole.ADMIN,
            callerProjectId = projectId
        )
        assertTrue(listA is DomainResult.Success)
        val returnsA = (listA as DomainResult.Success).data
        assertEquals(2, returnsA.size)
        assertTrue(returnsA.all { it.customerId == customerA })

        val listB = repository.listReturnsByCustomer(
            projectId = projectId,
            customerId = customerB,
            callerRole = UserRole.ADMIN,
            callerProjectId = projectId
        )
        assertTrue(listB is DomainResult.Success)
        val returnsB = (listB as DomainResult.Success).data
        assertEquals(1, returnsB.size)
        assertEquals(customerB, returnsB[0].customerId)
        Unit
    }

    @Test
    fun `source challan match validator rejects challan from different customer`() {
        val res = ReturnDomainValidator.validateSourceChallanMatch(
            challanProjectId = projectId,
            requestProjectId = projectId,
            challanCustomerId = customerB,
            requestCustomerId = customerA
        )
        assertTrue("Source challan belonging to customer B cannot be used for customer A", res is DomainResult.Error)
        val msg = (res as DomainResult.Error).message
        assertTrue(msg.contains("customer mismatch"))
    }

    @Test
    fun `source challan match validator passes when customer and project match`() {
        val res = ReturnDomainValidator.validateSourceChallanMatch(
            challanProjectId = projectId,
            requestProjectId = projectId,
            challanCustomerId = customerA,
            requestCustomerId = customerA
        )
        assertTrue(res is DomainResult.Success)
    }
}
