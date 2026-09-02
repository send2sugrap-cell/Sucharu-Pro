package com.sucharu.sucharupro.domain.repository

import com.sucharu.sucharupro.data.datasource.FakeProductionReworkDataSource
import com.sucharu.sucharupro.data.repository.ProductionReworkRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.qc.ReworkReason
import com.sucharu.sucharupro.domain.model.qc.ReworkStatus
import com.sucharu.sucharupro.domain.model.qc.ReworkType
import com.sucharu.sucharupro.domain.model.user.UserRole
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * End-to-end repository query, filtering, and flow stream tests for [ProductionReworkRepository] (Module 06 Step 05).
 */
class ProductionReworkRepositoryTest {

    private lateinit var dataSource: FakeProductionReworkDataSource
    private lateinit var repository: ProductionReworkRepository

    @Before
    fun setUp() {
        dataSource = FakeProductionReworkDataSource()
        repository = ProductionReworkRepositoryImpl(dataSource)
    }

    @Test
    fun observeByFilters_returnsAccurateSubsets() = runBlocking {
        // Job 01 Rework
        val res1 = repository.createRework(
            projectId = "proj-alpha",
            productionJobId = "job-01",
            reworkType = ReworkType.COLOR_CORRECTION,
            reason = ReworkReason.DEFECT_CORRECTION,
            affectedQuantity = 100,
            quantityUnit = "sheets",
            description = "Job 01 Rework",
            requestedBy = "insp-01",
            timestamp = "2026-08-17T10:00:00Z",
            callerRole = UserRole.QC_INSPECTOR
        )
        val rew1Id = (res1 as DomainResult.Success).data.reworkId

        // Job 02 Rework
        val res2 = repository.createRework(
            projectId = "proj-beta",
            productionJobId = "job-02",
            reworkType = ReworkType.CUTTING_CORRECTION,
            reason = ReworkReason.FINISHING_ERROR,
            affectedQuantity = 200,
            quantityUnit = "pcs",
            description = "Job 02 Rework",
            requestedBy = "insp-02",
            timestamp = "2026-08-17T10:05:00Z",
            callerRole = UserRole.QC_INSPECTOR
        )
        val rew2Id = (res2 as DomainResult.Success).data.reworkId

        // Test observeByJob
        val job01List = repository.observeReworksByJob("job-01").first()
        assertEquals(1, job01List.size)
        assertEquals(rew1Id, job01List[0].reworkId)

        // Test observeByProject
        val betaList = repository.observeReworksByProject("proj-beta").first()
        assertEquals(1, betaList.size)
        assertEquals(rew2Id, betaList[0].reworkId)

        // Test observeById
        val singleRework = repository.observeReworkById(rew1Id).first()
        assertNotNull(singleRework)
        assertEquals("job-01", singleRework?.productionJobId)

        // Test findReworkById
        val lookupRes = repository.findReworkById(rew2Id)
        assertTrue(lookupRes is DomainResult.Success)
        assertEquals("job-02", (lookupRes as DomainResult.Success).data.productionJobId)

        // Non-existent ID lookup
        val notFoundRes = repository.findReworkById("rew-nonexistent")
        assertTrue(notFoundRes is DomainResult.Error)
    }

    @Test
    fun observeByStatus_filtersCorrectly() = runBlocking {
        val res1 = repository.createRework(
            projectId = "proj-alpha",
            productionJobId = "job-01",
            reworkType = ReworkType.COLOR_CORRECTION,
            reason = ReworkReason.DEFECT_CORRECTION,
            affectedQuantity = 100,
            quantityUnit = "sheets",
            description = "Job 01 Rework",
            requestedBy = "insp-01",
            timestamp = "2026-08-17T10:00:00Z",
            callerRole = UserRole.QC_INSPECTOR
        )
        val rew1Id = (res1 as DomainResult.Success).data.reworkId

        repository.approveRework(rew1Id, "admin-01", "Admin", null, "2026-08-17T10:10:00Z", UserRole.ADMIN)

        val approvedList = repository.observeReworksByStatus(ReworkStatus.APPROVED).first()
        assertEquals(1, approvedList.size)
        assertEquals(rew1Id, approvedList[0].reworkId)

        val requestedList = repository.observeReworksByStatus(ReworkStatus.REQUESTED).first()
        assertEquals(0, requestedList.size)
    }

    @Test
    fun observeByAssignee_filtersCorrectly() = runBlocking {
        val res1 = repository.createRework(
            projectId = "proj-alpha",
            productionJobId = "job-01",
            reworkType = ReworkType.COLOR_CORRECTION,
            reason = ReworkReason.DEFECT_CORRECTION,
            affectedQuantity = 100,
            quantityUnit = "sheets",
            description = "Job 01 Rework",
            requestedBy = "insp-01",
            timestamp = "2026-08-17T10:00:00Z",
            callerRole = UserRole.QC_INSPECTOR
        )
        val rew1Id = (res1 as DomainResult.Success).data.reworkId

        repository.approveRework(rew1Id, "admin-01", "Admin", null, "2026-08-17T10:10:00Z", UserRole.ADMIN)
        repository.assignRework(rew1Id, "tech-99", "Specialist", "admin-01", "Admin", null, "2026-08-17T10:20:00Z", UserRole.ADMIN)

        val tech99List = repository.observeReworksByAssignee("tech-99").first()
        assertEquals(1, tech99List.size)
        assertEquals(rew1Id, tech99List[0].reworkId)

        val otherTechList = repository.observeReworksByAssignee("tech-other").first()
        assertEquals(0, otherTechList.size)
    }
}
