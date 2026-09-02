package com.sucharu.sucharupro.customersettlement

import com.sucharu.sucharupro.data.datasource.customersettlement.FakeCustomerPaymentAllocationDataSource
import com.sucharu.sucharupro.data.repository.customersettlement.CustomerPaymentAllocationRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.customersettlement.CustomerPaymentAllocation
import com.sucharu.sucharupro.domain.model.customersettlement.CustomerPaymentAllocationStatus
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

class CustomerPaymentAllocationRepositoryTest {

    private lateinit var repository: CustomerPaymentAllocationRepositoryImpl
    private val tenantId = "TENANT-001"
    private val projectId = "PRJ-001"
    private val customerId = "CUS-001"

    @Before
    fun setup() {
        val dataSource = FakeCustomerPaymentAllocationDataSource()
        repository = CustomerPaymentAllocationRepositoryImpl(dataSource)
    }

    @Test
    fun testCreateAndRetrieveAllocation() = runBlocking {
        val allocation = CustomerPaymentAllocation(
            allocationId = "ALC-1001",
            tenantId = tenantId,
            projectId = projectId,
            customerId = customerId,
            customerFinancialAccountId = "CFA-1001",
            paymentId = "PAY-1001",
            invoiceId = "INV-1001",
            allocatedAmount = BigDecimal("3500.0000"),
            status = CustomerPaymentAllocationStatus.ALLOCATED
        )

        val createRes = repository.createAllocation(allocation)
        assertTrue(createRes is DomainResult.Success)

        val getRes = repository.getAllocationById(tenantId, projectId, "ALC-1001")
        assertTrue(getRes is DomainResult.Success)
        val retrieved = (getRes as DomainResult.Success).data
        assertEquals("ALC-1001", retrieved.allocationId)
        assertEquals(BigDecimal("3500.0000"), retrieved.allocatedAmount)

        val listRes = repository.listAllocations(tenantId, projectId, paymentId = "PAY-1001")
        assertTrue(listRes is DomainResult.Success)
        assertEquals(1, (listRes as DomainResult.Success).data.size)
    }

    @Test
    fun testUpdateStatusAndOptimisticLocking() = runBlocking {
        val allocation = CustomerPaymentAllocation(
            allocationId = "ALC-1002",
            tenantId = tenantId,
            projectId = projectId,
            customerId = customerId,
            customerFinancialAccountId = "CFA-1001",
            paymentId = "PAY-1001",
            invoiceId = "INV-1001",
            allocatedAmount = BigDecimal("1000.0000"),
            status = CustomerPaymentAllocationStatus.ALLOCATED,
            version = 1L
        )
        repository.createAllocation(allocation)

        val updateRes = repository.updateAllocationStatus(
            tenantId = tenantId,
            projectId = projectId,
            allocationId = "ALC-1002",
            newStatus = CustomerPaymentAllocationStatus.REVERSED,
            reversalReason = "Refund requested",
            actorId = "admin_01",
            expectedVersion = 1L
        )
        assertTrue(updateRes is DomainResult.Success)
        val updated = (updateRes as DomainResult.Success).data
        assertEquals(CustomerPaymentAllocationStatus.REVERSED, updated.status)
        assertEquals(2L, updated.version)
    }
}
