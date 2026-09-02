package com.sucharu.sucharupro.businesscostcontrol

import com.sucharu.sucharupro.data.api.model.AuthenticatedPrincipal
import com.sucharu.sucharupro.data.api.model.UserRole
import com.sucharu.sucharupro.data.datasource.businesscostcontrol.FakeBusinessCostControlDataSource
import com.sucharu.sucharupro.data.repository.businesscostcontrol.BusinessCostControlRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.service.businesscostcontrol.BusinessCostControlServiceImpl
import com.sucharu.sucharupro.domain.service.businesscostcontrol.CreateCostCommitmentCommand
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

class BusinessCostCommitmentSecurityTest {

    private lateinit var dataSource: FakeBusinessCostControlDataSource
    private lateinit var repository: BusinessCostControlRepositoryImpl
    private lateinit var service: BusinessCostControlServiceImpl

    private val tenantId = "TENANT-001"
    private val projectId = "PRJ-001"

    private val manager1 = AuthenticatedPrincipal("MGR-1", projectId, "manager1", UserRole.MANAGER)
    private val manager2 = AuthenticatedPrincipal("MGR-2", projectId, "manager2", UserRole.MANAGER)
    private val customer = AuthenticatedPrincipal("CUST-1", projectId, "customer", UserRole.CUSTOMER)
    private val vendor = AuthenticatedPrincipal("VEND-1", projectId, "vendor", UserRole.VENDOR)

    @Before
    fun setup() {
        runBlocking {
            dataSource = FakeBusinessCostControlDataSource()
            repository = BusinessCostControlRepositoryImpl(dataSource)
            service = BusinessCostControlServiceImpl(
                repository = repository,
                defaultTenantId = tenantId
            )
        }
    }

    @Test
    fun testExternalRolesCannotAccessCommitments() = runBlocking {
        val cmd = CreateCostCommitmentCommand(
            costCategoryId = "CAT-PAPER",
            description = "Unauthorized attempt",
            committedAmount = BigDecimal("5000.0000")
        )
        val custRes = service.createCommitment(customer, cmd)
        assertTrue(custRes is DomainResult.Error)
        assertTrue((custRes as DomainResult.Error).message.contains("Access denied"))

        val vendRes = service.listCommitments(vendor)
        assertTrue(vendRes is DomainResult.Error)
        assertTrue((vendRes as DomainResult.Error).message.contains("Access denied"))
    }

    @Test
    fun testSeparationOfDutiesOnApproval() = runBlocking {
        // Manager 1 creates a commitment
        val cmd = CreateCostCommitmentCommand(
            costCategoryId = "CAT-PAPER",
            description = "Self-approval test",
            committedAmount = BigDecimal("50000.0000")
        )
        val commitment = (service.createCommitment(manager1, cmd) as DomainResult.Success).data
        service.submitCommitment(manager1, commitment.id)

        // Manager 1 attempts to approve their own commitment -> SoD violation!
        val selfApproveRes = service.approveCommitment(manager1, commitment.id)
        assertTrue(selfApproveRes is DomainResult.Error)
        assertTrue((selfApproveRes as DomainResult.Error).message.contains("Separation of Duties"))

        // Manager 2 (different actor) approves successfully
        val otherApproveRes = service.approveCommitment(manager2, commitment.id)
        assertTrue(otherApproveRes is DomainResult.Success)
    }
}
