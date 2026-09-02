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
import com.sucharu.sucharupro.domain.service.businesscost.BusinessCostManagementServiceImpl
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

class BusinessCostManagementIsolationTest {

    private lateinit var serviceA: BusinessCostManagementServiceImpl
    private lateinit var serviceB: BusinessCostManagementServiceImpl
    private lateinit var costRepo: BusinessCostManagementRepositoryImpl

    private val tenantA = "TENANT-AAA"
    private val tenantB = "TENANT-BBB"

    private val adminA = AuthenticatedPrincipal("ADM-A", "PRJ-A", "adminA", UserRole.ADMIN)
    private val adminB = AuthenticatedPrincipal("ADM-B", "PRJ-B", "adminB", UserRole.ADMIN)

    @Before
    fun setup() {
        runBlocking {
            val costDs = FakeBusinessCostManagementDataSource()
            costRepo = BusinessCostManagementRepositoryImpl(costDs)
            val expenseRepo = BusinessExpenseRepositoryImpl(FakeBusinessExpenseDataSource())
            val payableRepo = VendorPayableRepositoryImpl(FakeVendorPayableDataSource())
            val ledgerRepo = BusinessLedgerRepositoryImpl(FakeBusinessLedgerDataSource())

            serviceA = BusinessCostManagementServiceImpl(
                repository = costRepo,
                expenseRepository = expenseRepo,
                payableRepository = payableRepo,
                ledgerRepository = ledgerRepo,
                defaultTenantId = tenantA
            )

            serviceB = BusinessCostManagementServiceImpl(
                repository = costRepo,
                expenseRepository = expenseRepo,
                payableRepository = payableRepo,
                ledgerRepository = ledgerRepo,
                defaultTenantId = tenantB
            )

            // Tenant A cost center
            costRepo.createCostCenter(
                BusinessCostCenter(
                    id = "CC-A1",
                    code = "CC-A1",
                    name = "Printing Dept A",
                    description = null,
                    tenantId = tenantA,
                    projectId = "PRJ-A"
                )
            )
            costRepo.createCostCategory(
                BusinessCostCategory(
                    id = "CAT-A1",
                    code = "CAT-A1",
                    name = "Paper Dept A",
                    description = null,
                    tenantId = tenantA,
                    projectId = "PRJ-A"
                )
            )

            // Tenant B cost center
            costRepo.createCostCenter(
                BusinessCostCenter(
                    id = "CC-B1",
                    code = "CC-B1",
                    name = "Printing Dept B",
                    description = null,
                    tenantId = tenantB,
                    projectId = "PRJ-B"
                )
            )
            costRepo.createCostCategory(
                BusinessCostCategory(
                    id = "CAT-B1",
                    code = "CAT-B1",
                    name = "Paper Dept B",
                    description = null,
                    tenantId = tenantB,
                    projectId = "PRJ-B"
                )
            )
        }
    }

    @Test
    fun testTenantCannotSeeAnotherTenantsCostCenters() = runBlocking {
        val listA = serviceA.listCostCenters(adminA)
        assertTrue(listA is DomainResult.Success)
        val centersA = (listA as DomainResult.Success).data
        assertEquals(1, centersA.size)
        assertEquals("CC-A1", centersA[0].id)

        val listB = serviceB.listCostCenters(adminB)
        assertTrue(listB is DomainResult.Success)
        val centersB = (listB as DomainResult.Success).data
        assertEquals(1, centersB.size)
        assertEquals("CC-B1", centersB[0].id)
    }

    @Test
    fun testTenantCannotAccessOtherTenantsTrackingRecord() = runBlocking {
        costRepo.createCostTracking(
            BusinessCostTracking(
                id = "TRK-A1",
                sourceType = BusinessCostTrackingSourceType.MANUAL_OPERATIONAL_REFERENCE,
                sourceId = "MAN-A",
                costCenterId = "CC-A1",
                costCategoryId = "CAT-A1",
                jobId = null,
                amount = BigDecimal("500.0000"),
                currency = "BDT",
                tenantId = tenantA,
                projectId = "PRJ-A",
                allocationStatus = BusinessCostAllocationStatus.UNALLOCATED,
                classificationStatus = BusinessCostClassificationStatus.UNCLASSIFIED
            )
        )

        // Admin A can access
        val resA = serviceA.getCostTrackingById(adminA, "TRK-A1")
        assertTrue(resA is DomainResult.Success)

        // Admin B cannot access
        val resB = serviceB.getCostTrackingById(adminB, "TRK-A1")
        assertTrue(resB is DomainResult.Error)
        assertEquals("Cost tracking record 'TRK-A1' not found.", (resB as DomainResult.Error).message)
    }
}
