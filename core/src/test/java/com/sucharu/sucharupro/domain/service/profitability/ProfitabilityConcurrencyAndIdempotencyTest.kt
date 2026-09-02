package com.sucharu.sucharupro.domain.service.profitability

import com.sucharu.sucharupro.data.datasource.profitability.FakeProfitabilityDataSource
import com.sucharu.sucharupro.data.repository.profitability.ProfitabilityRepositoryImpl
import com.sucharu.sucharupro.domain.model.businessintegrity.Module16FinancialHandoffContract
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.profitability.*
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

class ProfitabilityConcurrencyAndIdempotencyTest {

    private lateinit var service: ProfitabilityFoundationServiceImpl
    private lateinit var repository: ProfitabilityRepositoryImpl
    private val tenantId = "TENANT-001"
    private val projectId = "PROJ-101"

    @Before
    fun setUp() {
        val dataSource = FakeProfitabilityDataSource()
        repository = ProfitabilityRepositoryImpl(dataSource)

        val fakeHandoffAdapter = object : Module16FinancialHandoffAdapter {
            override suspend fun getVerifiedFinancialHandoff(
                tenantId: String,
                projectId: String,
                periodId: String
            ): DomainResult<ValidatedFinancialHandoff> {
                return DomainResult.Success(
                    ValidatedFinancialHandoff(
                        contract = Module16FinancialHandoffContract(
                            tenantId = tenantId,
                            projectId = projectId,
                            periodId = periodId,
                            periodCode = "2026-M08",
                            isPeriodClosed = false,
                            closureCertificateChecksum = null,
                            isLedgerBalanced = true
                        ),
                        integrityStatus = SourceIntegrityStatus.VERIFIED,
                        isLedgerBalanced = true,
                        isPeriodClosed = false,
                        hasValidClosureCertificate = false,
                        validationNotes = emptyList()
                    )
                )
            }

            override suspend fun verifyPeriodIntegrityStatus(
                tenantId: String,
                projectId: String,
                periodId: String
            ): DomainResult<SourceIntegrityStatus> {
                return DomainResult.Success(SourceIntegrityStatus.VERIFIED)
            }
        }

        val sourceRegistry = ProfitabilitySourceRegistryImpl(fakeHandoffAdapter)
        val reconService = ProfitabilityReconciliationServiceImpl(fakeHandoffAdapter, sourceRegistry)

        service = ProfitabilityFoundationServiceImpl(
            repository = repository,
            handoffAdapter = fakeHandoffAdapter,
            sourceRegistry = sourceRegistry,
            reconciliationService = reconService
        )
    }

    @Test
    fun testIdempotencyKeyReturnsSameSnapshot() = runBlocking {
        val key = "IDEM-PROFIT-001"

        val res1 = service.generateProfitabilitySnapshot(
            tenantId = tenantId,
            projectId = projectId,
            scope = ProfitabilityScope.JOB,
            targetEntityId = "JOB-101",
            customRevenue = BigDecimal("50000.0000"),
            customDirectCost = BigDecimal("30000.0000"),
            idempotencyKey = key,
            actor = "USER-1"
        )
        assertTrue(res1 is DomainResult.Success)
        val snap1 = (res1 as DomainResult.Success).data

        // Execute again with same idempotency key
        val res2 = service.generateProfitabilitySnapshot(
            tenantId = tenantId,
            projectId = projectId,
            scope = ProfitabilityScope.JOB,
            targetEntityId = "JOB-101",
            customRevenue = BigDecimal("99999.0000"), // Different payload attempt
            customDirectCost = BigDecimal("99999.0000"),
            idempotencyKey = key,
            actor = "USER-1"
        )
        assertTrue(res2 is DomainResult.Success)
        val snap2 = (res2 as DomainResult.Success).data

        assertEquals(snap1.id, snap2.id)
        assertEquals(BigDecimal("50000.0000"), snap2.metrics.revenue) // Remains original
    }

    @Test
    fun testConcurrentSnapshotGenerationSafety() = runBlocking {
        val jobs = mutableListOf<Deferred<DomainResult<ProfitabilitySnapshot>>>()

        coroutineScope {
            for (i in 1..20) {
                jobs.add(
                    async(Dispatchers.Default) {
                        service.generateProfitabilitySnapshot(
                            tenantId = tenantId,
                            projectId = projectId,
                            scope = ProfitabilityScope.JOB,
                            targetEntityId = "JOB-$i",
                            customRevenue = BigDecimal("${1000 * i}.0000"),
                            customDirectCost = BigDecimal("${600 * i}.0000"),
                            actor = "CONCURRENT-WORKER-$i"
                        )
                    }
                )
            }
        }

        val results = jobs.awaitAll()
        assertEquals(20, results.size)
        assertTrue(results.all { it is DomainResult.Success })

        val listRes = repository.listSnapshots(tenantId, projectId, limit = 50)
        assertTrue(listRes is DomainResult.Success)
        assertEquals(20, (listRes as DomainResult.Success).data.size)
    }
}
