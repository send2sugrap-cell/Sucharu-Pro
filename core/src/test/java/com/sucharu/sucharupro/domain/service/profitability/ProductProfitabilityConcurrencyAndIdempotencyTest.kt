package com.sucharu.sucharupro.domain.service.profitability

import com.sucharu.sucharupro.data.datasource.profitability.FakeProductProfitabilityDataSource
import com.sucharu.sucharupro.data.repository.profitability.ProductProfitabilityRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

class ProductProfitabilityConcurrencyAndIdempotencyTest {

    private lateinit var fakeDs: FakeProductProfitabilityDataSource
    private lateinit var repository: ProductProfitabilityRepositoryImpl
    private lateinit var sourceCollector: ProductProfitabilitySourceCollector
    private lateinit var reconciliationService: ProductProfitabilityReconciliationService
    private lateinit var service: ProductProfitabilityServiceImpl

    @Before
    fun setUp() {
        fakeDs = FakeProductProfitabilityDataSource()
        repository = ProductProfitabilityRepositoryImpl(fakeDs)
        sourceCollector = ProductProfitabilitySourceCollectorImpl()
        reconciliationService = ProductProfitabilityReconciliationServiceImpl()
        service = ProductProfitabilityServiceImpl(repository, sourceCollector, reconciliationService)
    }

    @Test
    fun testIdempotencyReturnsIdenticalSnapshot() = runBlocking {
        val key = "IDEMPOTENCY-PROD-001"
        val res1 = service.calculateProductProfitability(
            tenantId = "T1",
            projectId = "P1",
            productId = "PROD-100",
            idempotencyKey = key,
            actor = "ADMIN"
        )
        val res2 = service.calculateProductProfitability(
            tenantId = "T1",
            projectId = "P1",
            productId = "PROD-100",
            idempotencyKey = key,
            actor = "ADMIN"
        )

        assertTrue(res1 is DomainResult.Success)
        assertTrue(res2 is DomainResult.Success)
        val snap1 = (res1 as DomainResult.Success).data
        val snap2 = (res2 as DomainResult.Success).data

        assertEquals(snap1.snapshotId, snap2.snapshotId)
        assertEquals(snap1.integrityHash, snap2.integrityHash)
    }

    @Test
    fun testConcurrentCalculationsSingleFlightSafety() = runBlocking {
        val jobs = (1..10).map { i ->
            async {
                service.calculateProductProfitability(
                    tenantId = "T1",
                    projectId = "P1",
                    productId = "PROD-CONCURRENT-$i",
                    actor = "TEST_USER_$i"
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
