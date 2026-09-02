package com.sucharu.sucharupro.businesscostcontrol

import com.sucharu.sucharupro.data.api.model.AuthenticatedPrincipal
import com.sucharu.sucharupro.data.api.model.UserRole
import com.sucharu.sucharupro.data.datasource.businesscostcontrol.BusinessCostCommitmentFilter
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

class BusinessCostCommitmentIsolationTest {

    private lateinit var dataSource: FakeBusinessCostControlDataSource
    private lateinit var repository: BusinessCostControlRepositoryImpl
    private lateinit var servicePrj1: BusinessCostControlServiceImpl
    private lateinit var servicePrj2: BusinessCostControlServiceImpl

    private val tenantId = "TENANT-001"
    private val prj1 = "PRJ-001"
    private val prj2 = "PRJ-002"

    private val adminPrj1 = AuthenticatedPrincipal("ADM-1", prj1, "admin1", UserRole.ADMIN)
    private val adminPrj2 = AuthenticatedPrincipal("ADM-2", prj2, "admin2", UserRole.ADMIN)

    @Before
    fun setup() {
        runBlocking {
            dataSource = FakeBusinessCostControlDataSource()
            repository = BusinessCostControlRepositoryImpl(dataSource)
            servicePrj1 = BusinessCostControlServiceImpl(repository, defaultTenantId = tenantId)
            servicePrj2 = BusinessCostControlServiceImpl(repository, defaultTenantId = tenantId)
        }
    }

    @Test
    fun testMultiTenantProjectIsolation() = runBlocking {
        // Create commitment in Project 1
        val cmd1 = CreateCostCommitmentCommand(
            commitmentNumber = "CMT-PRJ1-001",
            costCategoryId = "CAT-PAPER",
            description = "Project 1 Commitment",
            committedAmount = BigDecimal("10000.0000")
        )
        val res1 = servicePrj1.createCommitment(adminPrj1, cmd1)
        assertTrue(res1 is DomainResult.Success)
        val c1 = (res1 as DomainResult.Success).data

        // Create commitment in Project 2
        val cmd2 = CreateCostCommitmentCommand(
            commitmentNumber = "CMT-PRJ2-001",
            costCategoryId = "CAT-INK",
            description = "Project 2 Commitment",
            committedAmount = BigDecimal("20000.0000")
        )
        val res2 = servicePrj2.createCommitment(adminPrj2, cmd2)
        assertTrue(res2 is DomainResult.Success)

        // List commitments for Project 1
        val listPrj1 = (servicePrj1.listCommitments(adminPrj1) as DomainResult.Success).data
        assertEquals(1, listPrj1.size)
        assertEquals("CMT-PRJ1-001", listPrj1[0].commitmentNumber)

        // List commitments for Project 2
        val listPrj2 = (servicePrj2.listCommitments(adminPrj2) as DomainResult.Success).data
        assertEquals(1, listPrj2.size)
        assertEquals("CMT-PRJ2-001", listPrj2[0].commitmentNumber)

        // Admin of Project 2 cannot read commitment of Project 1 directly
        val crossReadRes = servicePrj2.getCommitmentById(adminPrj2, c1.id)
        assertTrue(crossReadRes is DomainResult.Error)
    }
}
