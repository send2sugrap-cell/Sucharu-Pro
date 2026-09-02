package com.sucharu.sucharupro.businesscost

import com.sucharu.sucharupro.data.datasource.businesscost.BusinessCostTrackingFilter
import com.sucharu.sucharupro.data.datasource.businesscost.FakeBusinessCostManagementDataSource
import com.sucharu.sucharupro.data.repository.businesscost.BusinessCostManagementRepositoryImpl
import com.sucharu.sucharupro.domain.model.businesscost.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

class BusinessCostManagementRepositoryTest {

    private lateinit var dataSource: FakeBusinessCostManagementDataSource
    private lateinit var repository: BusinessCostManagementRepositoryImpl
    private val tenantId = "TENANT-001"
    private val projectId = "PRJ-001"

    @Before
    fun setup() {
        dataSource = FakeBusinessCostManagementDataSource()
        repository = BusinessCostManagementRepositoryImpl(dataSource)
    }

    @Test
    fun testCostCenterCrud() = runBlocking {
        val center = BusinessCostCenter(
            id = "CC-TEST",
            code = "CC-TEST",
            name = "Test Center",
            tenantId = tenantId,
            projectId = projectId
        )
        val created = repository.createCostCenter(center)
        assertEquals("CC-TEST", created.id)

        val fetched = repository.findCostCenterById("CC-TEST", tenantId, projectId)
        assertNotNull(fetched)
        assertEquals("Test Center", fetched?.name)

        val updated = repository.updateCostCenter(fetched!!.copy(name = "Updated Test Center"))
        assertEquals("Updated Test Center", updated.name)

        val list = repository.listCostCenters(tenantId, projectId, null)
        assertEquals(1, list.size)
    }

    @Test
    fun testCostCategoryCrud() = runBlocking {
        val category = BusinessCostCategory(
            id = "CAT-TEST",
            code = "CAT-TEST",
            name = "Test Category",
            tenantId = tenantId,
            projectId = projectId
        )
        val created = repository.createCostCategory(category)
        assertEquals("CAT-TEST", created.id)

        val fetched = repository.findCostCategoryById("CAT-TEST", tenantId, projectId)
        assertNotNull(fetched)
        assertEquals("Test Category", fetched?.name)

        val list = repository.listCostCategories(tenantId, projectId, null)
        assertEquals(1, list.size)
    }

    @Test
    fun testCostTrackingAndAuditEvents() = runBlocking {
        val tracking = BusinessCostTracking(
            id = "TRK-001",
            sourceType = BusinessCostTrackingSourceType.BUSINESS_EXPENSE,
            sourceId = "EXP-1001",
            costCenterId = "CC-01",
            costCategoryId = "CAT-01",
            jobId = "JOB-101",
            amount = BigDecimal("5000.0000"),
            currency = "BDT",
            tenantId = tenantId,
            projectId = projectId,
            allocationStatus = BusinessCostAllocationStatus.UNALLOCATED,
            classificationStatus = BusinessCostClassificationStatus.CLASSIFIED
        )
        val created = repository.createCostTracking(tracking)
        assertEquals("TRK-001", created.id)

        val audit = BusinessCostClassificationAuditEvent(
            eventId = "AUD-001",
            trackingId = "TRK-001",
            action = "CLASSIFY",
            actorId = "USER-1",
            actorRole = "ADMIN",
            reason = "Initial classification",
            tenantId = tenantId,
            projectId = projectId
        )
        repository.recordAuditEvent(audit)

        val audits = repository.listAuditEvents(tenantId, projectId, "TRK-001")
        assertEquals(1, audits.size)
        assertEquals("Initial classification", audits[0].reason)
    }
}
