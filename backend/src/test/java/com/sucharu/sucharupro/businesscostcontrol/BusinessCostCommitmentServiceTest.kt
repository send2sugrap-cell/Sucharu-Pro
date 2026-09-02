package com.sucharu.sucharupro.businesscostcontrol

import com.sucharu.sucharupro.data.api.model.AuthenticatedPrincipal
import com.sucharu.sucharupro.data.api.model.UserRole
import com.sucharu.sucharupro.data.datasource.businesscostcontrol.FakeBusinessCostControlDataSource
import com.sucharu.sucharupro.data.repository.businesscostcontrol.BusinessCostControlRepositoryImpl
import com.sucharu.sucharupro.domain.model.businesscostcontrol.*
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.service.businesscostcontrol.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

class BusinessCostCommitmentServiceTest {

    private lateinit var dataSource: FakeBusinessCostControlDataSource
    private lateinit var repository: BusinessCostControlRepositoryImpl
    private lateinit var service: BusinessCostControlServiceImpl

    private val tenantId = "TENANT-001"
    private val projectId = "PRJ-001"

    private val admin = AuthenticatedPrincipal("ADM-1", projectId, "admin", UserRole.ADMIN)
    private val manager = AuthenticatedPrincipal("MGR-1", projectId, "manager", UserRole.MANAGER)
    private val staff = AuthenticatedPrincipal("STF-1", projectId, "staff", UserRole.STAFF)

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
    fun testFullCommitmentLifecycleFlow() = runBlocking {
        // 1. Create DRAFT commitment by staff
        val createCmd = CreateCostCommitmentCommand(
            commitmentNumber = "CMT-2026-001",
            costCategoryId = "CAT-PAPER",
            description = "100 reams art paper",
            committedAmount = BigDecimal("80000.0000"),
            sourceType = BusinessCostCommitmentSourceType.PURCHASE_COMMITMENT,
            sourceId = "PO-501"
        )
        val createRes = service.createCommitment(staff, createCmd)
        assertTrue(createRes is DomainResult.Success)
        val commitment = (createRes as DomainResult.Success).data
        assertEquals(BusinessCostCommitmentStatus.DRAFT, commitment.status)
        assertEquals(BigDecimal("80000.0000"), commitment.remainingAmount)

        // 2. Submit commitment
        val subRes = service.submitCommitment(staff, commitment.id)
        assertTrue(subRes is DomainResult.Success)
        assertEquals(BusinessCostCommitmentStatus.SUBMITTED, (subRes as DomainResult.Success).data.status)

        // 3. Approve commitment by manager (SoD: creator is staff, approver is manager)
        val appRes = service.approveCommitment(manager, commitment.id)
        assertTrue(appRes is DomainResult.Success)
        assertEquals(BusinessCostCommitmentStatus.APPROVED, (appRes as DomainResult.Success).data.status)

        // 4. Activate commitment
        val actRes = service.activateCommitment(staff, commitment.id)
        assertTrue(actRes is DomainResult.Success)
        assertEquals(BusinessCostCommitmentStatus.ACTIVE, (actRes as DomainResult.Success).data.status)

        // 5. Consume partial amount
        val con1Cmd = ConsumeCostCommitmentCommand(
            commitmentId = commitment.id,
            amount = BigDecimal("30000.0000"),
            sourceType = BusinessCostCommitmentSourceType.SERVICE_COMMITMENT,
            sourceId = "INV-101",
            notes = "First batch delivery"
        )
        val con1Res = service.consumeCommitment(staff, con1Cmd)
        assertTrue(con1Res is DomainResult.Success)

        val afterCon1 = (service.getCommitmentById(staff, commitment.id) as DomainResult.Success).data
        assertEquals(BusinessCostCommitmentStatus.PARTIALLY_CONSUMED, afterCon1.status)
        assertEquals(BigDecimal("30000.0000"), afterCon1.consumedAmount)
        assertEquals(BigDecimal("50000.0000"), afterCon1.remainingAmount)

        // 6. Consume remaining amount -> fully consumed
        val con2Cmd = ConsumeCostCommitmentCommand(
            commitmentId = commitment.id,
            amount = BigDecimal("50000.0000"),
            sourceType = BusinessCostCommitmentSourceType.SERVICE_COMMITMENT,
            sourceId = "INV-102"
        )
        val con2Res = service.consumeCommitment(staff, con2Cmd)
        assertTrue(con2Res is DomainResult.Success)

        val afterCon2 = (service.getCommitmentById(staff, commitment.id) as DomainResult.Success).data
        assertEquals(BusinessCostCommitmentStatus.FULLY_CONSUMED, afterCon2.status)
        assertEquals(BigDecimal("80000.0000"), afterCon2.consumedAmount)
        assertEquals(BigDecimal("0.0000"), afterCon2.remainingAmount)
    }

    @Test
    fun testOverConsumptionFails() = runBlocking {
        val createCmd = CreateCostCommitmentCommand(
            costCategoryId = "CAT-INK",
            description = "Cyan Ink drums",
            committedAmount = BigDecimal("20000.0000")
        )
        val commitment = (service.createCommitment(admin, createCmd) as DomainResult.Success).data
        service.approveCommitment(admin, commitment.id)
        service.activateCommitment(admin, commitment.id)

        val overConsumeCmd = ConsumeCostCommitmentCommand(
            commitmentId = commitment.id,
            amount = BigDecimal("25000.0000"),
            sourceId = "INV-99"
        )
        val res = service.consumeCommitment(admin, overConsumeCmd)
        assertTrue(res is DomainResult.Error)
        assertTrue((res as DomainResult.Error).message.contains("exceeds remaining commitment"))
    }
}
