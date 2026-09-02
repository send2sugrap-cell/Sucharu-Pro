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

class BusinessCostClassificationTest {

    private lateinit var costRepo: BusinessCostManagementRepositoryImpl
    private lateinit var service: BusinessCostManagementServiceImpl
    private val tenantId = "TENANT-CLASS"
    private val projectId = "PRJ-001"
    private val staff = AuthenticatedPrincipal("STF-1", projectId, "staff", UserRole.STAFF)

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
    fun testClassifyUnallocatedCostToJob() = runBlocking {
        // Track unallocated
        val trackRes = service.trackOperationalCost(
            principal = staff,
            command = TrackOperationalCostCommand(
                sourceType = BusinessCostTrackingSourceType.MANUAL_OPERATIONAL_REFERENCE,
                sourceId = "REF-UNALLOC-1",
                ledgerPostingId = null,
                costCenterId = "CC-PRINT",
                costCategoryId = "CAT-PAPER",
                jobId = null,
                amount = BigDecimal("7500.0000"),
                currency = "BDT"
            )
        )
        assertTrue(trackRes is DomainResult.Success)
        val trackingId = (trackRes as DomainResult.Success).data.id

        // Classify with job
        val classifyRes = service.classifyCost(
            principal = staff,
            command = ClassifyCostCommand(
                trackingId = trackingId,
                costCenterId = "CC-PRINT",
                costCategoryId = "CAT-PAPER",
                jobId = "JOB-CLASS-999",
                notes = "Assigned to poster job"
            )
        )
        assertTrue(classifyRes is DomainResult.Success)
        val classified = (classifyRes as DomainResult.Success).data
        assertEquals("JOB-CLASS-999", classified.jobId)
        assertEquals(BusinessCostAllocationStatus.FULLY_ALLOCATED, classified.allocationStatus)
        assertEquals(BusinessCostClassificationStatus.CLASSIFIED, classified.classificationStatus)

        val audits = costRepo.listAuditEvents(tenantId, projectId, trackingId)
        assertEquals(2, audits.size)
        assertTrue(audits.any { it.action == "CLASSIFY_COST" })
    }

    @Test
    fun testClassifyWithInactiveCostCenterFails() = runBlocking {
        runBlocking {
            costRepo.createCostCenter(
                BusinessCostCenter(
                    id = "CC-INACTIVE",
                    code = "CC-INACTIVE",
                    name = "Inactive Dept",
                    description = null,
                    isActive = false,
                    tenantId = tenantId,
                    projectId = projectId
                )
            )
        }

        val trackRes = service.trackOperationalCost(
            principal = staff,
            command = TrackOperationalCostCommand(
                sourceType = BusinessCostTrackingSourceType.MANUAL_OPERATIONAL_REFERENCE,
                sourceId = "REF-INACT-1",
                ledgerPostingId = null,
                costCenterId = "CC-PRINT",
                costCategoryId = "CAT-PAPER",
                jobId = null,
                amount = BigDecimal("2000.0000"),
                currency = "BDT"
            )
        )
        val trackingId = (trackRes as DomainResult.Success).data.id

        val classifyRes = service.classifyCost(
            principal = staff,
            command = ClassifyCostCommand(
                trackingId = trackingId,
                costCenterId = "CC-INACTIVE",
                costCategoryId = "CAT-PAPER",
                jobId = "JOB-1",
                notes = "Should fail"
            )
        )
        assertTrue(classifyRes is DomainResult.Error)
        assertEquals("Target cost center 'Inactive Dept' is inactive.", (classifyRes as DomainResult.Error).message)
    }
}
