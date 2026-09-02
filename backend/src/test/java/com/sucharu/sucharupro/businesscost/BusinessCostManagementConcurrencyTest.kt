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
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

class BusinessCostManagementConcurrencyTest {

    private lateinit var costRepo: BusinessCostManagementRepositoryImpl
    private lateinit var service: BusinessCostManagementServiceImpl
    private val tenantId = "TENANT-CONCURRENCY"
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
    fun testConcurrentTrackingCreation() = runBlocking {
        val threadCount = 20
        val results = mutableListOf<DomainResult<BusinessCostTracking>>()

        coroutineScope {
            val jobs = (1..threadCount).map { i ->
                async(Dispatchers.Default) {
                    service.trackOperationalCost(
                        principal = admin,
                        command = TrackOperationalCostCommand(
                            sourceType = BusinessCostTrackingSourceType.MANUAL_OPERATIONAL_REFERENCE,
                            sourceId = "REF-CONC-$i",
                            ledgerPostingId = null,
                            costCenterId = "CC-PRINT",
                            costCategoryId = "CAT-PAPER",
                            jobId = "JOB-$i",
                            amount = BigDecimal("100.0000"),
                            currency = "BDT",
                            notes = "Concurrent test iteration $i",
                            idempotencyKey = "IDEM-CONC-$i"
                        )
                    )
                }
            }
            results.addAll(jobs.awaitAll())
        }

        assertEquals(threadCount, results.size)
        assertTrue(results.all { it is DomainResult.Success })

        val all = costRepo.listCostTracking(tenantId, projectId, BusinessCostTrackingFilter())
        assertEquals(threadCount, all.size)
    }
}
