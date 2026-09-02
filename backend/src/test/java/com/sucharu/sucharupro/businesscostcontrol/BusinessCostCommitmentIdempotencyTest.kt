package com.sucharu.sucharupro.businesscostcontrol

import com.sucharu.sucharupro.data.api.model.AuthenticatedPrincipal
import com.sucharu.sucharupro.data.api.model.UserRole
import com.sucharu.sucharupro.data.datasource.businesscostcontrol.FakeBusinessCostControlDataSource
import com.sucharu.sucharupro.data.repository.businesscostcontrol.BusinessCostControlRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.service.businesscostcontrol.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

class BusinessCostCommitmentIdempotencyTest {

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
    fun testCommitmentCreationIdempotency() = runBlocking {
        val idemKey = "IDEM-CMT-KEY-999"
        val cmd = CreateCostCommitmentCommand(
            costCategoryId = "CAT-PAPER",
            description = "Idempotent paper order",
            committedAmount = BigDecimal("25000.0000"),
            idempotencyKey = idemKey
        )

        val res1 = service.createCommitment(admin, cmd)
        assertTrue(res1 is DomainResult.Success)
        val c1 = (res1 as DomainResult.Success).data

        // Replay with identical idempotency key
        val res2 = service.createCommitment(admin, cmd)
        assertTrue(res2 is DomainResult.Success)
        val c2 = (res2 as DomainResult.Success).data

        assertEquals(c1.id, c2.id)
        assertEquals(c1.commitmentNumber, c2.commitmentNumber)

        val list = (service.listCommitments(admin) as DomainResult.Success).data
        assertEquals(1, list.size)
    }

    @Test
    fun testConsumptionIdempotency() = runBlocking {
        val cmd = CreateCostCommitmentCommand(
            costCategoryId = "CAT-PAPER",
            description = "Idempotent consumption test",
            committedAmount = BigDecimal("50000.0000")
        )
        val commitment = (service.createCommitment(admin, cmd) as DomainResult.Success).data
        service.approveCommitment(admin, commitment.id)
        service.activateCommitment(admin, commitment.id)

        val conKey = "IDEM-CON-KEY-111"
        val conCmd = ConsumeCostCommitmentCommand(
            commitmentId = commitment.id,
            amount = BigDecimal("10000.0000"),
            sourceId = "INV-IDEM-01",
            idempotencyKey = conKey
        )

        val con1 = service.consumeCommitment(admin, conCmd)
        assertTrue(con1 is DomainResult.Success)

        // Replay consumption with same idempotency key
        val con2 = service.consumeCommitment(admin, conCmd)
        assertTrue(con2 is DomainResult.Success)
        assertEquals((con1 as DomainResult.Success).data.id, (con2 as DomainResult.Success).data.id)

        val updated = (service.getCommitmentById(admin, commitment.id) as DomainResult.Success).data
        // Amount must be consumed only once (10,000 not 20,000)
        assertEquals(BigDecimal("10000.0000"), updated.consumedAmount)
        assertEquals(BigDecimal("40000.0000"), updated.remainingAmount)
    }
}
