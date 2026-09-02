package com.sucharu.sucharupro.domain.service.profitability

import com.sucharu.sucharupro.data.datasource.profitability.FakeCustomerProfitabilityDataSource
import com.sucharu.sucharupro.data.repository.profitability.CustomerProfitabilityRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.profitability.CustomerRevenueAttribution
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import java.math.BigDecimal

/**
 * Concurrency and Idempotency Test Suite for Customer Profitability (Module 16 Step 04).
 */
class CustomerProfitabilityConcurrencyAndIdempotencyTest {

    private val fakeDataSource = FakeCustomerProfitabilityDataSource()
    private val repository = CustomerProfitabilityRepositoryImpl(fakeDataSource)
    private val sourceCollector = CustomerProfitabilitySourceCollectorImpl()
    private val reconciliationService = CustomerProfitabilityReconciliationServiceImpl()
    private val service = CustomerProfitabilityServiceImpl(
        repository = repository,
        sourceCollector = sourceCollector,
        reconciliationService = reconciliationService
    )

    @Test
    fun testIdempotentCalculationReturnsIdenticalSnapshot() = runBlocking {
        val tenantId = "tenant-001"
        val projectId = "proj-001"
        val customerId = "cust-100"
        val idemKey = "IDEM-KEY-CUST-100"

        val revList = listOf(
            CustomerRevenueAttribution(
                revenueAttributionId = "REV-1",
                tenantId = tenantId,
                projectId = projectId,
                customerId = customerId,
                recognizedRevenue = BigDecimal("5000.0000"),
                sourceEntityId = "INV-1"
            )
        )

        val res1 = service.calculateCustomerProfitability(
            tenantId = tenantId,
            projectId = projectId,
            customerId = customerId,
            customRevenue = revList,
            idempotencyKey = idemKey
        )
        assertTrue(res1 is DomainResult.Success)
        val snap1 = (res1 as DomainResult.Success).data

        val res2 = service.calculateCustomerProfitability(
            tenantId = tenantId,
            projectId = projectId,
            customerId = customerId,
            customRevenue = revList,
            idempotencyKey = idemKey
        )
        assertTrue(res2 is DomainResult.Success)
        val snap2 = (res2 as DomainResult.Success).data

        // Must return the exact same snapshot
        assertEquals(snap1.snapshotId, snap2.snapshotId)
        assertEquals(snap1.integrityHash, snap2.integrityHash)
    }

    @Test
    fun testConcurrentSnapshotCalculations() = runBlocking {
        val tenantId = "tenant-001"
        val projectId = "proj-001"

        val jobs = (1..10).map { i ->
            async {
                service.calculateCustomerProfitability(
                    tenantId = tenantId,
                    projectId = projectId,
                    customerId = "cust-$i",
                    customRevenue = listOf(
                        CustomerRevenueAttribution(
                            revenueAttributionId = "REV-$i",
                            tenantId = tenantId,
                            projectId = projectId,
                            customerId = "cust-$i",
                            recognizedRevenue = BigDecimal("${i * 1000}.0000"),
                            sourceEntityId = "INV-$i"
                        )
                    )
                )
            }
        }

        val results = jobs.awaitAll()
        assertEquals(10, results.size)
        results.forEach { res ->
            assertTrue(res is DomainResult.Success)
        }
    }
}
