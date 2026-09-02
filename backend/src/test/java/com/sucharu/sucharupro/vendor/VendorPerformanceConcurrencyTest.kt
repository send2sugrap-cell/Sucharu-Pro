package com.sucharu.sucharupro.vendor

import com.sucharu.sucharupro.data.datasource.FakeVendorDataSource
import com.sucharu.sucharupro.data.datasource.FakeVendorPerformanceDataSource
import com.sucharu.sucharupro.data.repository.VendorPerformanceRepositoryImpl
import com.sucharu.sucharupro.data.repository.VendorRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.vendor.CorrectiveActionPriority
import com.sucharu.sucharupro.domain.model.vendor.Vendor
import com.sucharu.sucharupro.domain.model.vendor.VendorCorrectiveAction
import com.sucharu.sucharupro.domain.model.vendor.VendorStatus
import com.sucharu.sucharupro.domain.service.vendor.VendorPerformanceServiceImpl
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.time.Instant

class VendorPerformanceConcurrencyTest {

    private lateinit var vendorRepo: VendorRepositoryImpl
    private lateinit var repo: VendorPerformanceRepositoryImpl
    private lateinit var service: VendorPerformanceServiceImpl

    @Before
    fun setUp() {
        runBlocking {
            vendorRepo = VendorRepositoryImpl(FakeVendorDataSource())
            val ds = FakeVendorPerformanceDataSource()
            repo = VendorPerformanceRepositoryImpl(ds)
            service = VendorPerformanceServiceImpl(
                performanceRepository = repo,
                vendorRepository = vendorRepo
            )

            vendorRepo.createVendor(
                Vendor(
                    vendorId = "VND-CONC",
                    projectId = "PRJ-01",
                    vendorCode = "VCONC",
                    vendorName = "Concurrent Vendor",
                    status = VendorStatus.ACTIVE
                )
            )
        }
    }

    @Test
    fun testConcurrentCorrectiveActionCreation() = runBlocking {
        val count = 20
        val deferred = (1..count).map { i ->
            async(Dispatchers.Default) {
                val action = VendorCorrectiveAction(
                    actionId = "CAPA-CONC-$i",
                    projectId = "PRJ-01",
                    tenantId = "PRJ-01",
                    vendorId = "VND-CONC",
                    sourceType = "KPI",
                    issueDescription = "Concurrent issue $i",
                    actionPlan = "Concurrent plan $i",
                    assignedTo = "worker_$i",
                    assignedToName = "Worker $i",
                    priority = CorrectiveActionPriority.MEDIUM,
                    dueDate = Instant.now().plusSeconds(86400 * 7),
                    createdBy = "test"
                )
                service.createCorrectiveAction(action)
            }
        }

        val results = deferred.awaitAll()
        assertTrue(results.all { it is DomainResult.Success })

        val listRes = service.listCorrectiveActions("PRJ-01", "VND-CONC")
        assertTrue(listRes is DomainResult.Success)
        val list = (listRes as DomainResult.Success).data
        assertEquals(count, list.size)
    }
}
