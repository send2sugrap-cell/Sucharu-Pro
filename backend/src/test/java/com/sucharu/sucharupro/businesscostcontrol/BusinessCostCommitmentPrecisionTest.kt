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

class BusinessCostCommitmentPrecisionTest {

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
    fun testFourDecimalMonetaryPrecision() = runBlocking {
        val preciseAmount = BigDecimal("12345.6789")
        val cmd = CreateCostCommitmentCommand(
            costCategoryId = "CAT-PAPER",
            description = "Precision paper costing",
            committedAmount = preciseAmount
        )
        val commitment = (service.createCommitment(admin, cmd) as DomainResult.Success).data
        assertEquals(BigDecimal("12345.6789"), commitment.committedAmount)
        assertEquals(4, commitment.committedAmount.scale())

        service.approveCommitment(admin, commitment.id)
        service.activateCommitment(admin, commitment.id)

        val conCmd = ConsumeCostCommitmentCommand(
            commitmentId = commitment.id,
            amount = BigDecimal("2345.1234"),
            sourceId = "INV-PRECISION-01"
        )
        val con = (service.consumeCommitment(admin, conCmd) as DomainResult.Success).data
        assertEquals(BigDecimal("2345.1234"), con.amount)
        assertEquals(4, con.amount.scale())

        val updated = (service.getCommitmentById(admin, commitment.id) as DomainResult.Success).data
        assertEquals(BigDecimal("10000.5555"), updated.remainingAmount)
        assertEquals(4, updated.remainingAmount.scale())
    }
}
