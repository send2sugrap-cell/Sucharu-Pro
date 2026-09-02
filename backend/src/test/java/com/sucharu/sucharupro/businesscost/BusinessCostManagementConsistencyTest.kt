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
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.service.businesscost.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

class BusinessCostManagementConsistencyTest {

    private lateinit var costRepo: BusinessCostManagementRepositoryImpl
    private lateinit var service: BusinessCostManagementServiceImpl
    private val tenantId = "TENANT-CONSISTENCY"
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
            costRepo.createCostCenter(
                BusinessCostCenter(
                    id = "CC-DESIGN",
                    code = "CC-DESIGN",
                    name = "Graphic Design",
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
            costRepo.createCostCategory(
                BusinessCostCategory(
                    id = "CAT-DESIGN",
                    code = "CAT-DESIGN",
                    name = "Design Work",
                    description = null,
                    tenantId = tenantId,
                    projectId = projectId
                )
            )
        }
    }

    @Test
    fun testStatusTransitionsFromUnallocatedToAllocatedToReclassified() = runBlocking {
        // Step 1: Initial creation without job -> UNALLOCATED
        val res1 = service.trackOperationalCost(
            principal = admin,
            command = TrackOperationalCostCommand(
                sourceType = BusinessCostTrackingSourceType.MANUAL_OPERATIONAL_REFERENCE,
                sourceId = "REF-STATE-1",
                ledgerPostingId = null,
                costCenterId = "CC-PRINT",
                costCategoryId = "CAT-PAPER",
                jobId = null,
                amount = BigDecimal("8000.0000"),
                currency = "BDT"
            )
        )
        assertTrue(res1 is DomainResult.Success)
        val rec1 = (res1 as DomainResult.Success).data
        assertEquals(BusinessCostAllocationStatus.UNALLOCATED, rec1.allocationStatus)
        assertEquals(BusinessCostClassificationStatus.CLASSIFIED, rec1.classificationStatus)

        // Step 2: Classify with job -> FULLY_ALLOCATED
        val res2 = service.classifyCost(
            principal = admin,
            command = ClassifyCostCommand(
                trackingId = rec1.id,
                costCenterId = "CC-PRINT",
                costCategoryId = "CAT-PAPER",
                jobId = "JOB-STATE-101",
                notes = "Assigned to client booklet job"
            )
        )
        assertTrue(res2 is DomainResult.Success)
        val rec2 = (res2 as DomainResult.Success).data
        assertEquals(BusinessCostAllocationStatus.FULLY_ALLOCATED, rec2.allocationStatus)
        assertEquals(BusinessCostClassificationStatus.CLASSIFIED, rec2.classificationStatus)
        assertEquals("JOB-STATE-101", rec2.jobId)

        // Step 3: Reclassify to another center & category -> RECLASSIFIED
        val res3 = service.reclassifyCost(
            principal = admin,
            command = ReclassifyCostCommand(
                trackingId = rec1.id,
                newCostCenterId = "CC-DESIGN",
                newCostCategoryId = "CAT-DESIGN",
                newJobId = "JOB-STATE-102",
                reason = "Job belonged to design studio"
            )
        )
        assertTrue(res3 is DomainResult.Success)
        val rec3 = (res3 as DomainResult.Success).data
        assertEquals(BusinessCostAllocationStatus.RECLASSIFIED, rec3.allocationStatus)
        assertEquals(BusinessCostClassificationStatus.RECLASSIFIED, rec3.classificationStatus)
        assertEquals("CC-DESIGN", rec3.costCenterId)
        assertEquals("CAT-DESIGN", rec3.costCategoryId)
        assertEquals("JOB-STATE-102", rec3.jobId)
        assertEquals(3, rec3.version)
    }
}
