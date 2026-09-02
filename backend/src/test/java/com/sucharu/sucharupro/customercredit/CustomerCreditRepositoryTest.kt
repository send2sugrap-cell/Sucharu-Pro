package com.sucharu.sucharupro.customercredit

import com.sucharu.sucharupro.data.datasource.customercredit.FakeCustomerCreditDataSource
import com.sucharu.sucharupro.data.repository.customercredit.CustomerCreditRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.customercredit.CustomerAdjustment
import com.sucharu.sucharupro.domain.model.customercredit.CustomerAdjustmentType
import com.sucharu.sucharupro.domain.model.customercredit.CustomerAdvance
import com.sucharu.sucharupro.domain.model.customercredit.CustomerAdvanceStatus
import com.sucharu.sucharupro.domain.model.customercredit.CustomerRefund
import com.sucharu.sucharupro.domain.model.customercredit.CustomerRefundStatus
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

class CustomerCreditRepositoryTest {

    private lateinit var dataSource: FakeCustomerCreditDataSource
    private lateinit var repository: CustomerCreditRepositoryImpl

    private val tenantId = "TENANT-001"
    private val projectId = "PRJ-001"
    private val customerId = "CUST-001"
    private val accountId = "CFA-001"

    @Before
    fun setup() {
        dataSource = FakeCustomerCreditDataSource()
        repository = CustomerCreditRepositoryImpl(dataSource)
    }

    @Test
    fun testCreateAndRetrieveAdvance() = runBlocking {
        val advance = CustomerAdvance(
            advanceId = "ADV-001",
            tenantId = tenantId,
            projectId = projectId,
            customerId = customerId,
            customerFinancialAccountId = accountId,
            advanceNumber = "ADV-2026-0001",
            amount = BigDecimal("1000.0000"),
            availableAmount = BigDecimal("1000.0000")
        )

        val createRes = repository.createAdvance(advance)
        assertTrue(createRes is DomainResult.Success)

        val fetchRes = repository.getAdvanceById(tenantId, projectId, "ADV-001")
        assertTrue(fetchRes is DomainResult.Success)
        val fetched = (fetchRes as DomainResult.Success).data
        assertEquals("ADV-2026-0001", fetched.advanceNumber)
        assertEquals(BigDecimal("1000.0000"), fetched.amount)
    }

    @Test
    fun testSummaryCalculation() = runBlocking {
        // Add Advance of 5000
        val adv = CustomerAdvance(
            advanceId = "ADV-001",
            tenantId = tenantId,
            projectId = projectId,
            customerId = customerId,
            customerFinancialAccountId = accountId,
            advanceNumber = "ADV-001",
            amount = BigDecimal("5000.0000"),
            allocatedAmount = BigDecimal("1000.0000"),
            availableAmount = BigDecimal("4000.0000"),
            status = CustomerAdvanceStatus.ALLOCATED
        )
        repository.createAdvance(adv)

        // Add Credit Adjustment of 500
        val adj = CustomerAdjustment(
            adjustmentId = "ADJ-001",
            tenantId = tenantId,
            projectId = projectId,
            customerId = customerId,
            customerFinancialAccountId = accountId,
            adjustmentNumber = "ADJ-001",
            adjustmentType = CustomerAdjustmentType.CREDIT,
            amount = BigDecimal("500.0000"),
            reason = "Discount credit"
        )
        repository.createAdjustment(adj)

        val summaryRes = repository.getCustomerCreditSummary(tenantId, projectId, customerId)
        assertTrue(summaryRes is DomainResult.Success)
        val summary = (summaryRes as DomainResult.Success).data

        assertEquals(BigDecimal("5000.0000"), summary.totalAdvances)
        assertEquals(BigDecimal("1000.0000"), summary.totalAllocated)
        assertEquals(BigDecimal("500.0000"), summary.totalAdjustmentsCredit)
        assertEquals(BigDecimal("4500.0000"), summary.totalAvailableCredit) // 4000 available advance + 500 credit adj
    }

    @Test
    fun testOptimisticLockingFailureOnAdvance() = runBlocking {
        val adv = CustomerAdvance(
            advanceId = "ADV-002",
            tenantId = tenantId,
            projectId = projectId,
            customerId = customerId,
            customerFinancialAccountId = accountId,
            advanceNumber = "ADV-002",
            amount = BigDecimal("1000.0000"),
            version = 1L
        )
        repository.createAdvance(adv)

        // Pass wrong expected version
        val updateRes = repository.updateAdvanceAllocation(
            tenantId, projectId, "ADV-002",
            BigDecimal("500.0000"), BigDecimal("500.0000"),
            CustomerAdvanceStatus.ALLOCATED, "user1", 2L
        )
        assertTrue(updateRes is DomainResult.Error)
    }
}
