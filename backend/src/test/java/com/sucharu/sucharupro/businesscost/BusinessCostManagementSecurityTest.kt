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

class BusinessCostManagementSecurityTest {

    private lateinit var service: BusinessCostManagementServiceImpl
    private val tenantId = "TENANT-001"
    private val projectId = "PRJ-001"

    private val customer = AuthenticatedPrincipal("CUST-1", projectId, "cust", UserRole.CUSTOMER)
    private val vendor = AuthenticatedPrincipal("VEND-1", projectId, "vend", UserRole.VENDOR)
    private val affiliate = AuthenticatedPrincipal("AFF-1", projectId, "aff", UserRole.AFFILIATE)
    private val guest = AuthenticatedPrincipal("GST-1", projectId, "guest", UserRole.GUEST)
    private val staff = AuthenticatedPrincipal("STF-1", projectId, "staff", UserRole.STAFF)
    private val admin = AuthenticatedPrincipal("ADM-1", projectId, "admin", UserRole.ADMIN)

    @Before
    fun setup() {
        runBlocking {
            val costDs = FakeBusinessCostManagementDataSource()
            val costRepo = BusinessCostManagementRepositoryImpl(costDs)
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
                    id = "CC-01",
                    code = "CC-01",
                    name = "Printing",
                    description = null,
                    tenantId = tenantId,
                    projectId = projectId
                )
            )
            costRepo.createCostCategory(
                BusinessCostCategory(
                    id = "CAT-01",
                    code = "CAT-01",
                    name = "Paper",
                    description = null,
                    tenantId = tenantId,
                    projectId = projectId
                )
            )
            costRepo.createCostTracking(
                BusinessCostTracking(
                    id = "TRK-01",
                    sourceType = BusinessCostTrackingSourceType.MANUAL_OPERATIONAL_REFERENCE,
                    sourceId = "MAN-1",
                    costCenterId = "CC-01",
                    costCategoryId = "CAT-01",
                    jobId = null,
                    amount = BigDecimal("1000.0000"),
                    currency = "BDT",
                    tenantId = tenantId,
                    projectId = projectId,
                    allocationStatus = BusinessCostAllocationStatus.UNALLOCATED,
                    classificationStatus = BusinessCostClassificationStatus.UNCLASSIFIED
                )
            )
        }
    }

    @Test
    fun testForbiddenRolesCannotAccessCostManagement() = runBlocking {
        val forbiddenUsers = listOf(customer, vendor, affiliate, guest)
        for (user in forbiddenUsers) {
            val listRes = service.listCostCenters(user)
            assertTrue("Expected error for $user", listRes is DomainResult.Error)
            assertEquals("Access denied: Role '${user.role}' is not authorized to access internal business cost management.", (listRes as DomainResult.Error).message)

            val trackRes = service.trackOperationalCost(
                user,
                TrackOperationalCostCommand(
                    sourceType = BusinessCostTrackingSourceType.MANUAL_OPERATIONAL_REFERENCE,
                    sourceId = "REF-1",
                    ledgerPostingId = null,
                    costCenterId = "CC-01",
                    costCategoryId = "CAT-01",
                    jobId = null,
                    amount = BigDecimal("100.0000"),
                    currency = "BDT"
                )
            )
            assertTrue("Expected error for $user", trackRes is DomainResult.Error)
        }
    }

    @Test
    fun testStaffCannotCreateOrReclassifyCostCenters() = runBlocking {
        val createCenterRes = service.createCostCenter(
            staff,
            CreateCostCenterCommand("CC-NEW", "New Center", null, null)
        )
        assertTrue(createCenterRes is DomainResult.Error)
        assertEquals("Access denied: Only ADMIN or MANAGER can perform create cost centers (current role: STAFF).", (createCenterRes as DomainResult.Error).message)

        val reclassifyRes = service.reclassifyCost(
            staff,
            ReclassifyCostCommand(
                trackingId = "TRK-01",
                newCostCenterId = "CC-01",
                newCostCategoryId = "CAT-01",
                newJobId = "JOB-1",
                reason = "Operational adjustment"
            )
        )
        assertTrue(reclassifyRes is DomainResult.Error)
        assertEquals("Access denied: Only ADMIN or MANAGER can perform reclassify operational costs (current role: STAFF).", (reclassifyRes as DomainResult.Error).message)
    }

    @Test
    fun testAdminCanPerformPrivilegedActions() = runBlocking {
        val createCenterRes = service.createCostCenter(
            admin,
            CreateCostCenterCommand("CC-NEW", "New Center", null, null)
        )
        assertTrue(createCenterRes is DomainResult.Success)

        val reclassifyRes = service.reclassifyCost(
            admin,
            ReclassifyCostCommand(
                trackingId = "TRK-01",
                newCostCenterId = "CC-01",
                newCostCategoryId = "CAT-01",
                newJobId = "JOB-1",
                reason = "Administrative correction"
            )
        )
        assertTrue(reclassifyRes is DomainResult.Success)
    }
}
