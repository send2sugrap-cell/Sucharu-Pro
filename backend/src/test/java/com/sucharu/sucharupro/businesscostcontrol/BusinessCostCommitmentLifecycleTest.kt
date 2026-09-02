package com.sucharu.sucharupro.businesscostcontrol

import com.sucharu.sucharupro.data.api.model.AuthenticatedPrincipal
import com.sucharu.sucharupro.data.api.model.UserRole
import com.sucharu.sucharupro.data.datasource.businesscostcontrol.FakeBusinessCostControlDataSource
import com.sucharu.sucharupro.data.repository.businesscostcontrol.BusinessCostControlRepositoryImpl
import com.sucharu.sucharupro.domain.model.businesscostcontrol.BusinessCostCommitmentStatus
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.service.businesscostcontrol.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

class BusinessCostCommitmentLifecycleTest {

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
    fun testCancellationAndClosureRules() = runBlocking {
        // Create DRAFT commitment
        val cmd = CreateCostCommitmentCommand(
            costCategoryId = "CAT-PLATE",
            description = "CTP Plates",
            committedAmount = BigDecimal("15000.0000")
        )
        val commitment = (service.createCommitment(admin, cmd) as DomainResult.Success).data
        assertEquals(BusinessCostCommitmentStatus.DRAFT, commitment.status)

        // Cancel DRAFT commitment
        val cancelRes = service.cancelCommitment(admin, commitment.id, "Vendor out of stock")
        assertTrue(cancelRes is DomainResult.Success)
        val cancelled = (cancelRes as DomainResult.Success).data
        assertEquals(BusinessCostCommitmentStatus.CANCELLED, cancelled.status)
        assertEquals(BigDecimal("0.0000"), cancelled.remainingAmount)

        // Terminal state rejects further state transitions
        val tryApprove = service.approveCommitment(admin, commitment.id)
        assertTrue(tryApprove is DomainResult.Error)

        // Create another commitment and close after partial consumption
        val c2 = (service.createCommitment(admin, cmd.copy(description = "Second PO")) as DomainResult.Success).data
        service.approveCommitment(admin, c2.id)
        service.activateCommitment(admin, c2.id)
        service.consumeCommitment(admin, ConsumeCostCommitmentCommand(c2.id, BigDecimal("5000.0000"), sourceId = "INV-01"))

        // Cancellation of consumed commitment is blocked (must use close)
        val tryCancelConsumed = service.cancelCommitment(admin, c2.id, "Cancel attempt")
        assertTrue(tryCancelConsumed is DomainResult.Error)

        // Close partially consumed commitment
        val closeRes = service.closeCommitment(admin, c2.id, "Contract closed early by mutual agreement")
        assertTrue(closeRes is DomainResult.Success)
        val closed = (closeRes as DomainResult.Success).data
        assertEquals(BusinessCostCommitmentStatus.CLOSED, closed.status)
        assertEquals(BigDecimal("0.0000"), closed.remainingAmount)
    }
}
