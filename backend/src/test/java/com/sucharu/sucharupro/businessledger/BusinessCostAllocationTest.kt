package com.sucharu.sucharupro.businessledger

import com.sucharu.sucharupro.data.api.model.AuthenticatedPrincipal
import com.sucharu.sucharupro.data.api.model.UserRole
import com.sucharu.sucharupro.data.datasource.businessexpense.FakeBusinessExpenseDataSource
import com.sucharu.sucharupro.data.datasource.businessledger.FakeBusinessLedgerDataSource
import com.sucharu.sucharupro.data.repository.businessexpense.BusinessExpenseRepositoryImpl
import com.sucharu.sucharupro.data.repository.businessledger.BusinessLedgerRepositoryImpl
import com.sucharu.sucharupro.domain.model.businessexpense.BusinessExpense
import com.sucharu.sucharupro.domain.model.businessexpense.BusinessExpenseStatus
import com.sucharu.sucharupro.domain.model.businessledger.BusinessLedgerAccountCategory
import com.sucharu.sucharupro.domain.model.businessledger.BusinessLedgerSourceType
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.service.businessledger.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

class BusinessCostAllocationTest {

    private lateinit var ledgerDataSource: FakeBusinessLedgerDataSource
    private lateinit var ledgerRepository: BusinessLedgerRepositoryImpl
    private lateinit var expenseDataSource: FakeBusinessExpenseDataSource
    private lateinit var expenseRepository: BusinessExpenseRepositoryImpl
    private lateinit var service: BusinessLedgerServiceImpl

    private val tenantId = "TENANT-001"
    private val projectId = "PRJ-001"

    private val adminPrincipal = AuthenticatedPrincipal(
        userId = "USER-ADMIN-1",
        projectId = projectId,
        username = "admin1",
        role = UserRole.ADMIN
    )

    @Before
    fun setup() {
        ledgerDataSource = FakeBusinessLedgerDataSource()
        ledgerRepository = BusinessLedgerRepositoryImpl(ledgerDataSource)
        expenseDataSource = FakeBusinessExpenseDataSource()
        expenseRepository = BusinessExpenseRepositoryImpl(expenseDataSource)

        service = BusinessLedgerServiceImpl(
            repository = ledgerRepository,
            expenseRepository = expenseRepository,
            defaultTenantId = tenantId
        )
    }

    @Test
    fun testJobCostAllocationAndBreakdown() = runBlocking {
        // Create source expense of 20,000 BDT
        val expense = BusinessExpense(
            expenseId = "EXP-ALLOC-101",
            tenantId = tenantId,
            projectId = projectId,
            expenseNumber = "EXP-2026-ALLOC",
            expenseCategoryId = "CAT-001",
            amount = BigDecimal("20000.0000"),
            status = BusinessExpenseStatus.APPROVED,
            description = "Specialty Paper & Inks",
            createdBy = "USER-1"
        )
        expenseRepository.createExpense(expense)

        // 1. Allocate 12,000 to JOB-1025 (Production Cost)
        val alloc1 = (service.allocateCost(
            adminPrincipal,
            AllocateCostCommand(
                sourceType = BusinessLedgerSourceType.BUSINESS_EXPENSE,
                sourceId = "EXP-ALLOC-101",
                jobId = "JOB-1025",
                allocatedAmount = BigDecimal("12000.0000"),
                costCategory = BusinessLedgerAccountCategory.PRODUCTION_COST
            )
        ) as DomainResult.Success).data
        assertEquals("JOB-1025", alloc1.jobId)

        // 2. Allocate 5,000 to JOB-1025 (Transport Cost)
        val alloc2 = (service.allocateCost(
            adminPrincipal,
            AllocateCostCommand(
                sourceType = BusinessLedgerSourceType.BUSINESS_EXPENSE,
                sourceId = "EXP-ALLOC-101",
                jobId = "JOB-1025",
                allocatedAmount = BigDecimal("5000.0000"),
                costCategory = BusinessLedgerAccountCategory.TRANSPORT_COST
            )
        ) as DomainResult.Success).data
        assertEquals("JOB-1025", alloc2.jobId)

        // 3. Query Job Cost Summary for JOB-1025: Total 17,000
        val jobSumRes = service.getJobCostSummary(adminPrincipal, "JOB-1025")
        assertTrue(jobSumRes is DomainResult.Success)
        val jobSum = (jobSumRes as DomainResult.Success).data
        assertEquals(BigDecimal("17000.0000"), jobSum.totalAllocatedCost)
        assertEquals(2, jobSum.allocationCount)
        assertEquals(BigDecimal("12000.0000"), jobSum.breakdownByCategory["PRODUCTION_COST"])
        assertEquals(BigDecimal("5000.0000"), jobSum.breakdownByCategory["TRANSPORT_COST"])

        // 4. Query Unallocated Cost Summary: 20,000 - 17,000 = 3,000 remaining (85% allocated)
        val unallocRes = service.getUnallocatedCostSummary(adminPrincipal, BusinessLedgerSourceType.BUSINESS_EXPENSE, "EXP-ALLOC-101")
        assertTrue(unallocRes is DomainResult.Success)
        val unalloc = (unallocRes as DomainResult.Success).data
        assertEquals(BigDecimal("20000.0000"), unalloc.totalSourceAmount)
        assertEquals(BigDecimal("17000.0000"), unalloc.allocatedAmount)
        assertEquals(BigDecimal("3000.0000"), unalloc.unallocatedAmount)
        assertEquals(BigDecimal("85.00"), unalloc.allocationPercentage)
    }

    @Test
    fun testReverseCostAllocationRestoresUnallocatedAmount() = runBlocking {
        val expense = BusinessExpense(
            expenseId = "EXP-REV-ALLOC",
            tenantId = tenantId,
            projectId = projectId,
            expenseNumber = "EXP-2026-REV-A",
            expenseCategoryId = "CAT-001",
            amount = BigDecimal("10000.0000"),
            status = BusinessExpenseStatus.APPROVED,
            description = "Packaging Carton Stock",
            createdBy = "USER-1"
        )
        expenseRepository.createExpense(expense)

        val alloc = (service.allocateCost(
            adminPrincipal,
            AllocateCostCommand(
                sourceType = BusinessLedgerSourceType.BUSINESS_EXPENSE,
                sourceId = "EXP-REV-ALLOC",
                jobId = "JOB-99",
                allocatedAmount = BigDecimal("10000.0000")
            )
        ) as DomainResult.Success).data

        // Unallocated is 0
        var unalloc = (service.getUnallocatedCostSummary(adminPrincipal, BusinessLedgerSourceType.BUSINESS_EXPENSE, "EXP-REV-ALLOC") as DomainResult.Success).data
        assertEquals(BigDecimal("0.0000"), unalloc.unallocatedAmount)

        // Reverse allocation
        val revRes = service.reverseCostAllocation(
            adminPrincipal,
            ReverseCostAllocationCommand(allocationId = alloc.id, reason = "Job was cancelled")
        )
        assertTrue(revRes is DomainResult.Success)

        // Unallocated restored to 10,000
        unalloc = (service.getUnallocatedCostSummary(adminPrincipal, BusinessLedgerSourceType.BUSINESS_EXPENSE, "EXP-REV-ALLOC") as DomainResult.Success).data
        assertEquals(BigDecimal("10000.0000"), unalloc.unallocatedAmount)
        assertEquals(BigDecimal("0.0000"), unalloc.allocatedAmount)
    }
}
