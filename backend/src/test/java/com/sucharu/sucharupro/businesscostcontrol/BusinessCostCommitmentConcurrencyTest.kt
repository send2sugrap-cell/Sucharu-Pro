package com.sucharu.sucharupro.businesscostcontrol

import com.sucharu.sucharupro.data.api.model.AuthenticatedPrincipal
import com.sucharu.sucharupro.data.api.model.UserRole
import com.sucharu.sucharupro.data.datasource.businesscostcontrol.FakeBusinessCostControlDataSource
import com.sucharu.sucharupro.data.repository.businesscostcontrol.BusinessCostControlRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.service.businesscostcontrol.*
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

class BusinessCostCommitmentConcurrencyTest {

    private lateinit var dataSource: FakeBusinessCostControlDataSource
    private lateinit var repository: BusinessCostControlRepositoryImpl
    private lateinit var service: BusinessCostControlServiceImpl

    private val tenantId = "TENANT-001"
    private val projectId = "PRJ-001"
    private val admin = AuthenticatedPrincipal("ADM-1", projectId, "admin", UserRole.ADMIN)

    @Before
    fun setup() {
        runBlocking {
            dataSource = FakeBusinessCostControlDataSource()
            repository = BusinessCostControlRepositoryImpl(dataSource)
            service = BusinessCostControlServiceImpl(repository, defaultTenantId = tenantId)
        }
    }

    @Test
    fun testConcurrentConsumptionsMaintainBalanceIntegrity() = runBlocking {
        // Create an active commitment of 10,000
        val createCmd = CreateCostCommitmentCommand(
            costCategoryId = "CAT-PAPER",
            description = "High-volume print stock",
            committedAmount = BigDecimal("10000.0000")
        )
        val commitment = (service.createCommitment(admin, createCmd) as DomainResult.Success).data
        service.approveCommitment(admin, commitment.id)
        service.activateCommitment(admin, commitment.id)

        // Launch 20 concurrent consumptions of 500 each = 10,000 total
        val jobs = (1..20).map { i ->
            async(Dispatchers.Default) {
                val cmd = ConsumeCostCommitmentCommand(
                    commitmentId = commitment.id,
                    amount = BigDecimal("500.0000"),
                    sourceId = "INV-CONCUR-$i",
                    notes = "Concurrent consumption $i"
                )
                service.consumeCommitment(admin, cmd)
            }
        }

        val results = jobs.awaitAll()
        val successCount = results.count { it is DomainResult.Success }
        assertEquals(20, successCount)

        val finalCommitment = (service.getCommitmentById(admin, commitment.id) as DomainResult.Success).data
        assertEquals(BigDecimal("10000.0000"), finalCommitment.consumedAmount)
        assertEquals(BigDecimal("0.0000"), finalCommitment.remainingAmount)
    }
}
