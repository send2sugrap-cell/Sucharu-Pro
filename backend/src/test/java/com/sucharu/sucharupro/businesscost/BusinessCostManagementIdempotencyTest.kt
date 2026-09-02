package com.sucharu.sucharupro.businesscost

import com.sucharu.sucharupro.data.api.model.AuthenticatedPrincipal
import com.sucharu.sucharupro.data.api.model.UserRole
import com.sucharu.sucharupro.data.datasource.businesscost.BusinessCostTrackingFilter
import com.sucharu.sucharupro.data.datasource.businesscost.FakeBusinessCostManagementDataSource
import com.sucharu.sucharupro.data.datasource.businessexpense.FakeBusinessExpenseDataSource
import com.sucharu.sucharupro.data.datasource.businessledger.FakeBusinessLedgerDataSource
import com.sucharu.sucharupro.data.datasource.vendorpayable.FakeVendorPayableDataSource
import com.sucharu.sucharupro.data.repository.businesscost.BusinessCostManagementRepositoryImpl
import com.sucharu.sucharupro.data.repository.businessexpense.BusinessExpenseRepositoryImpl
import com.sucharu.sucharupro.data.repository.businessledger.BusinessLedgerRepositoryImpl
import com.sucharu.sucharupro.data.repository.vendorpayable.VendorPayableRepositoryImpl
import com.sucharu.sucharupro.domain.model.businesscost.*
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.service.businesscost.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

class BusinessCostManagementIdempotencyTest {

    private lateinit var costRepo: BusinessCostManagementRepositoryImpl
    private lateinit var service: BusinessCostManagementServiceImpl
    private val tenantId = "TENANT-IDEM"
    private val projectId = "PRJ-001"
    private val admin = AuthenticatedPrincipal("ADM-1", projectId, "admin", UserRole.ADMIN)

    @Before
    fun setup() {
        runBlocking {
            val costDs = FakeBusinessCostManagementDataSource()
            costRepo = BusinessCostManagementRepositoryImpl(costDs)
            val expenseRepo = BusinessExpenseRepositoryImpl(FakeBusinessExpenseDataSource())
            val payableRepo = VendorPayableRepositoryImpl(FakeVendorPayableDataSource())
            val ledgerRepo = BusinessLedgerRepositoryImpl(FakeBusinessLedgerDataSource())

            service = BusinessCostManagementServiceImpl(
                repository = costRepo,
                expenseRepository = expenseRepo,
                payableRepository = payableRepo,
                ledgerRepository = ledgerRepo,
                defaultTenantId = tenantId
            )

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
    fun testTrackCostIdempotencyReturnsExistingRecord() = runBlocking {
        val idempotencyKey = "IDEM-KEY-UNIQUE-123"

        val res1 = service.trackOperationalCost(
            principal = admin,
            command = TrackOperationalCostCommand(
                sourceType = BusinessCostTrackingSourceType.MANUAL_OPERATIONAL_REFERENCE,
                sourceId = "REF-100",
                ledgerPostingId = null,
                costCenterId = "CC-PRINT",
                costCategoryId = "CAT-PAPER",
                jobId = "JOB-100",
                amount = BigDecimal("5000.0000"),
                currency = "BDT",
                notes = "First attempt",
                idempotencyKey = idempotencyKey
            )
        )
        assertTrue(res1 is DomainResult.Success)
        val rec1 = (res1 as DomainResult.Success).data

        // Retry with same idempotency key
        val res2 = service.trackOperationalCost(
            principal = admin,
            command = TrackOperationalCostCommand(
                sourceType = BusinessCostTrackingSourceType.MANUAL_OPERATIONAL_REFERENCE,
                sourceId = "REF-100",
                ledgerPostingId = null,
                costCenterId = "CC-PRINT",
                costCategoryId = "CAT-PAPER",
                jobId = "JOB-100",
                amount = BigDecimal("5000.0000"),
                currency = "BDT",
                notes = "Duplicate retry attempt",
                idempotencyKey = idempotencyKey
            )
        )
        assertTrue(res2 is DomainResult.Success)
        val rec2 = (res2 as DomainResult.Success).data

        assertEquals(rec1.id, rec2.id)
        assertEquals(rec1.createdAt, rec2.createdAt)

        val all = costRepo.listCostTracking(tenantId, projectId, BusinessCostTrackingFilter())
        assertEquals(1, all.size)
    }
}
