package com.sucharu.sucharupro.businesscostcontrol

import com.sucharu.sucharupro.data.datasource.businesscostcontrol.BusinessCostCommitmentFilter
import com.sucharu.sucharupro.data.datasource.businesscostcontrol.FakeBusinessCostControlDataSource
import com.sucharu.sucharupro.data.repository.businesscostcontrol.BusinessCostControlRepositoryImpl
import com.sucharu.sucharupro.domain.model.businesscostcontrol.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

class BusinessCostCommitmentRepositoryTest {

    private lateinit var dataSource: FakeBusinessCostControlDataSource
    private lateinit var repository: BusinessCostControlRepositoryImpl

    private val tenantId = "TENANT-001"
    private val projectId = "PRJ-001"

    @Before
    fun setup() {
        runBlocking {
            dataSource = FakeBusinessCostControlDataSource()
            repository = BusinessCostControlRepositoryImpl(dataSource)
        }
    }

    @Test
    fun testCreateAndFindCommitment() = runBlocking {
        val commitment = BusinessCostCommitment(
            id = "CMT-100",
            tenantId = tenantId,
            projectId = projectId,
            commitmentNumber = "CMT-100",
            costCategoryId = "CAT-PAPER",
            description = "Paper PO",
            committedAmount = BigDecimal("50000.0000"),
            consumedAmount = BigDecimal.ZERO,
            remainingAmount = BigDecimal("50000.0000"),
            currency = "BDT",
            status = BusinessCostCommitmentStatus.DRAFT,
            sourceType = BusinessCostCommitmentSourceType.PURCHASE_COMMITMENT,
            sourceId = "PO-001",
            createdBy = "USR-01"
        )
        val created = repository.createCommitment(commitment)
        assertEquals("CMT-100", created.id)

        val byId = repository.findCommitmentById("CMT-100", tenantId, projectId)
        assertNotNull(byId)
        assertEquals("CMT-100", byId?.commitmentNumber)

        val byNumber = repository.findCommitmentByNumber("CMT-100", tenantId, projectId)
        assertNotNull(byNumber)
        assertEquals(BigDecimal("50000.0000"), byNumber?.committedAmount)
    }

    @Test
    fun testUpdateCommitment() = runBlocking {
        val commitment = BusinessCostCommitment(
            id = "CMT-101",
            tenantId = tenantId,
            projectId = projectId,
            commitmentNumber = "CMT-101",
            costCategoryId = "CAT-PAPER",
            description = "Initial",
            committedAmount = BigDecimal("30000.0000"),
            consumedAmount = BigDecimal.ZERO,
            remainingAmount = BigDecimal("30000.0000"),
            currency = "BDT",
            status = BusinessCostCommitmentStatus.DRAFT,
            sourceType = BusinessCostCommitmentSourceType.MANUAL,
            sourceId = "CMT-101",
            createdBy = "USR-01"
        )
        repository.createCommitment(commitment)

        val updated = commitment.copy(
            description = "Updated description",
            committedAmount = BigDecimal("35000.0000"),
            remainingAmount = BigDecimal("35000.0000"),
            status = BusinessCostCommitmentStatus.SUBMITTED
        )
        val saved = repository.updateCommitment(updated)
        assertEquals("Updated description", saved.description)
        assertEquals(BusinessCostCommitmentStatus.SUBMITTED, saved.status)
    }

    @Test
    fun testRecordAndListConsumptions() = runBlocking {
        val consumption = BusinessCostCommitmentConsumption(
            id = "CON-100",
            commitmentId = "CMT-100",
            tenantId = tenantId,
            projectId = projectId,
            sourceType = BusinessCostCommitmentSourceType.SERVICE_COMMITMENT,
            sourceId = "INV-001",
            amount = BigDecimal("15000.0000"),
            currency = "BDT",
            consumedAt = System.currentTimeMillis(),
            createdBy = "USR-01"
        )
        repository.recordConsumption(consumption)

        val list = repository.listConsumptions(tenantId, projectId, "CMT-100")
        assertEquals(1, list.size)
        assertEquals("CON-100", list[0].id)
        assertEquals(BigDecimal("15000.0000"), list[0].amount)
    }

    @Test
    fun testFilterCommitments() = runBlocking {
        val c1 = BusinessCostCommitment(
            id = "C-1",
            tenantId = tenantId,
            projectId = projectId,
            commitmentNumber = "C-1",
            costCategoryId = "CAT-PAPER",
            vendorId = "V-1",
            jobId = "JOB-1",
            description = "C1",
            committedAmount = BigDecimal("1000.0000"),
            consumedAmount = BigDecimal.ZERO,
            remainingAmount = BigDecimal("1000.0000"),
            currency = "BDT",
            status = BusinessCostCommitmentStatus.ACTIVE,
            sourceType = BusinessCostCommitmentSourceType.MANUAL,
            sourceId = "C-1",
            createdBy = "USR-1"
        )
        val c2 = BusinessCostCommitment(
            id = "C-2",
            tenantId = tenantId,
            projectId = projectId,
            commitmentNumber = "C-2",
            costCategoryId = "CAT-INK",
            vendorId = "V-2",
            jobId = "JOB-2",
            description = "C2",
            committedAmount = BigDecimal("2000.0000"),
            consumedAmount = BigDecimal.ZERO,
            remainingAmount = BigDecimal("2000.0000"),
            currency = "BDT",
            status = BusinessCostCommitmentStatus.CLOSED,
            sourceType = BusinessCostCommitmentSourceType.MANUAL,
            sourceId = "C-2",
            createdBy = "USR-1"
        )
        repository.createCommitment(c1)
        repository.createCommitment(c2)

        val filterVendor = repository.listCommitments(tenantId, projectId, BusinessCostCommitmentFilter(vendorId = "V-1"))
        assertEquals(1, filterVendor.size)
        assertEquals("C-1", filterVendor[0].id)

        val filterStatus = repository.listCommitments(tenantId, projectId, BusinessCostCommitmentFilter(status = BusinessCostCommitmentStatus.CLOSED))
        assertEquals(1, filterStatus.size)
        assertEquals("C-2", filterStatus[0].id)
    }
}
