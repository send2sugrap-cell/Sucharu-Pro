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

class BusinessCostReclassificationTest {

    private lateinit var costRepo: BusinessCostManagementRepositoryImpl
    private lateinit var service: BusinessCostManagementServiceImpl
    private val tenantId = "TENANT-RECLASS"
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
                    id = "CC-PACKAGING",
                    code = "CC-PACKAGING",
                    name = "Packaging & Box Making",
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
                    id = "CAT-BOARD",
                    code = "CAT-BOARD",
                    name = "Corrugated Board",
                    description = null,
                    tenantId = tenantId,
                    projectId = projectId
                )
            )
        }
    }

    @Test
    fun testReclassifyRequiresMandatoryReason() = runBlocking {
        val trackRes = service.trackOperationalCost(
            principal = admin,
            command = TrackOperationalCostCommand(
                sourceType = BusinessCostTrackingSourceType.MANUAL_OPERATIONAL_REFERENCE,
                sourceId = "REF-RECLASS-01",
                ledgerPostingId = null,
                costCenterId = "CC-PRINT",
                costCategoryId = "CAT-PAPER",
                jobId = "JOB-101",
                amount = BigDecimal("14000.0000"),
                currency = "BDT"
            )
        )
        val trackingId = (trackRes as DomainResult.Success).data.id

        // Empty reason fails
        val failRes = service.reclassifyCost(
            principal = admin,
            command = ReclassifyCostCommand(
                trackingId = trackingId,
                newCostCenterId = "CC-PACKAGING",
                newCostCategoryId = "CAT-BOARD",
                newJobId = "JOB-202",
                reason = "  "
            )
        )
        assertTrue(failRes is DomainResult.Error)
        assertEquals("A mandatory reason (at least 3 characters) must be provided when reclassifying costs.", (failRes as DomainResult.Error).message)
    }

    @Test
    fun testReclassifyMaintainsHistoricalAuditTrailAndIncrementsVersion() = runBlocking {
        val trackRes = service.trackOperationalCost(
            principal = admin,
            command = TrackOperationalCostCommand(
                sourceType = BusinessCostTrackingSourceType.MANUAL_OPERATIONAL_REFERENCE,
                sourceId = "REF-RECLASS-02",
                ledgerPostingId = null,
                costCenterId = "CC-PRINT",
                costCategoryId = "CAT-PAPER",
                jobId = "JOB-101",
                amount = BigDecimal("14000.0000"),
                currency = "BDT"
            )
        )
        val originalTracking = (trackRes as DomainResult.Success).data
        val trackingId = originalTracking.id
        assertEquals(1, originalTracking.version)

        // Perform valid reclassification
        val reclassRes = service.reclassifyCost(
            principal = admin,
            command = ReclassifyCostCommand(
                trackingId = trackingId,
                newCostCenterId = "CC-PACKAGING",
                newCostCategoryId = "CAT-BOARD",
                newJobId = "JOB-202",
                reason = "Material was rerouted to rigid box manufacturing line"
            )
        )
        assertTrue(reclassRes is DomainResult.Success)
        val reclassified = (reclassRes as DomainResult.Success).data
        assertEquals("CC-PACKAGING", reclassified.costCenterId)
        assertEquals("CAT-BOARD", reclassified.costCategoryId)
        assertEquals("JOB-202", reclassified.jobId)
        assertEquals(BusinessCostAllocationStatus.RECLASSIFIED, reclassified.allocationStatus)
        assertEquals(BusinessCostClassificationStatus.RECLASSIFIED, reclassified.classificationStatus)
        assertEquals(2, reclassified.version)

        // Verify audit event captured
        val audits = costRepo.listAuditEvents(tenantId, projectId, trackingId)
        assertEquals(2, audits.size)
        assertTrue(audits.any { it.action == "RECLASSIFY_COST" })
    }
}
