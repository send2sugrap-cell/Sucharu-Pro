package com.sucharu.sucharupro.businesscost

import com.sucharu.sucharupro.data.api.model.AuthenticatedPrincipal
import com.sucharu.sucharupro.data.api.model.UserRole
import com.sucharu.sucharupro.data.datasource.businesscost.FakeBusinessCostManagementDataSource
import com.sucharu.sucharupro.data.datasource.businessexpense.FakeBusinessExpenseDataSource
import com.sucharu.sucharupro.data.datasource.businessledger.FakeBusinessLedgerDataSource
import com.sucharu.sucharupro.data.datasource.vendorpayable.FakeVendorPayableDataSource
import com.sucharu.sucharupro.data.repository.businesscost.BusinessCostManagementRepositoryImpl
import com.sucharu.sucharupro.data.repository.businessexpense.BusinessExpenseRepositoryImpl
import com.sucharu.sucharupro.data.repository.businessledger.BusinessLedgerRepositoryImpl
import com.sucharu.sucharupro.data.repository.vendorpayable.VendorPayableRepositoryImpl
import com.sucharu.sucharupro.domain.model.businesscost.*
import com.sucharu.sucharupro.domain.model.businessexpense.BusinessExpense
import com.sucharu.sucharupro.domain.model.businessexpense.BusinessExpenseStatus
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.service.businesscost.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

class BusinessCostManagementServiceTest {

    private lateinit var costDs: FakeBusinessCostManagementDataSource
    private lateinit var costRepo: BusinessCostManagementRepositoryImpl
    private lateinit var expenseDs: FakeBusinessExpenseDataSource
    private lateinit var expenseRepo: BusinessExpenseRepositoryImpl
    private lateinit var payableDs: FakeVendorPayableDataSource
    private lateinit var payableRepo: VendorPayableRepositoryImpl
    private lateinit var ledgerDs: FakeBusinessLedgerDataSource
    private lateinit var ledgerRepo: BusinessLedgerRepositoryImpl
    private lateinit var service: BusinessCostManagementServiceImpl

    private val tenantId = "TENANT-001"
    private val projectId = "PRJ-001"

    private val admin = AuthenticatedPrincipal("ADM-1", projectId, "admin", UserRole.ADMIN)
    private val staff = AuthenticatedPrincipal("STF-1", projectId, "staff", UserRole.STAFF)

    @Before
    fun setup() {
        runBlocking {
            costDs = FakeBusinessCostManagementDataSource()
            costRepo = BusinessCostManagementRepositoryImpl(costDs)
            expenseDs = FakeBusinessExpenseDataSource()
            expenseRepo = BusinessExpenseRepositoryImpl(expenseDs)
            payableDs = FakeVendorPayableDataSource()
            payableRepo = VendorPayableRepositoryImpl(payableDs)
            ledgerDs = FakeBusinessLedgerDataSource()
            ledgerRepo = BusinessLedgerRepositoryImpl(ledgerDs)

            service = BusinessCostManagementServiceImpl(
                repository = costRepo,
                expenseRepository = expenseRepo,
                payableRepository = payableRepo,
                ledgerRepository = ledgerRepo,
                defaultTenantId = tenantId
            )

            // Seed default cost center & category
            costRepo.createCostCenter(
                BusinessCostCenter(
                    id = "CC-PRINT",
                    code = "CC-PRINT",
                    name = "Offset Printing",
                    description = null,
                    tenantId = tenantId,
                    projectId = projectId
                )
            )
            costRepo.createCostCategory(
                BusinessCostCategory(
                    id = "CAT-PAPER",
                    code = "CAT-PAPER",
                    name = "Paper Stock",
                    description = null,
                    tenantId = tenantId,
                    projectId = projectId
                )
            )
        }
    }

    @Test
    fun testCreateCostCenterByAdmin() = runBlocking {
        val res = service.createCostCenter(
            admin,
            CreateCostCenterCommand("CC-BIND", "Binding & Finishing", "Finishing department", null)
        )
        assertTrue(res is DomainResult.Success)
        val center = (res as DomainResult.Success).data
        assertEquals("CC-BIND", center.code)
    }

    @Test
    fun testTrackExpenseOperationalCost() = runBlocking {
        expenseRepo.createExpense(
            BusinessExpense(
                expenseId = "EXP-999",
                tenantId = tenantId,
                projectId = projectId,
                expenseNumber = "EXP-999",
                expenseCategoryId = "CAT-CONSUMABLE",
                amount = BigDecimal("12000.0000"),
                currency = "BDT",
                description = "Plate ink expense",
                status = BusinessExpenseStatus.APPROVED,
                expenseDate = System.currentTimeMillis(),
                createdBy = "admin"
            )
        )

        val res = service.trackOperationalCost(
            principal = staff,
            command = TrackOperationalCostCommand(
                sourceType = BusinessCostTrackingSourceType.BUSINESS_EXPENSE,
                sourceId = "EXP-999",
                ledgerPostingId = null,
                costCenterId = "CC-PRINT",
                costCategoryId = "CAT-PAPER",
                jobId = "JOB-4001",
                amount = BigDecimal("12000.0000"),
                currency = "BDT",
                notes = "Plate ink allocated to job 4001",
                idempotencyKey = "IDEM-EXP-999"
            )
        )
        assertTrue(res is DomainResult.Success)
        val tracking = (res as DomainResult.Success).data
        assertEquals(BusinessCostAllocationStatus.FULLY_ALLOCATED, tracking.allocationStatus)
        assertEquals("JOB-4001", tracking.jobId)
    }

    @Test
    fun testTrackCostWithMismatchedAmountFails() = runBlocking {
        expenseRepo.createExpense(
            BusinessExpense(
                expenseId = "EXP-888",
                tenantId = tenantId,
                projectId = projectId,
                expenseNumber = "EXP-888",
                expenseCategoryId = "CAT-TRANSPORT",
                amount = BigDecimal("5000.0000"),
                currency = "BDT",
                description = "Delivery gas",
                status = BusinessExpenseStatus.APPROVED,
                expenseDate = System.currentTimeMillis(),
                createdBy = "admin"
            )
        )

        val res = service.trackOperationalCost(
            principal = staff,
            command = TrackOperationalCostCommand(
                sourceType = BusinessCostTrackingSourceType.BUSINESS_EXPENSE,
                sourceId = "EXP-888",
                ledgerPostingId = null,
                costCenterId = "CC-PRINT",
                costCategoryId = "CAT-PAPER",
                jobId = null,
                amount = BigDecimal("9999.0000"), // Mismatch
                currency = "BDT"
            )
        )
        assertTrue(res is DomainResult.Error)
        assertTrue((res as DomainResult.Error).message.contains("Amount 9999.0000 does not match canonical source"))
    }
}
