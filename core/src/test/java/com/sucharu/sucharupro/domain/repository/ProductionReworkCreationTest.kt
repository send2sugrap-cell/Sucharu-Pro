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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Tests for Production Rework creation and initial registration (Module 06 Step 05).
 */
class ProductionReworkCreationTest {

    private lateinit var dataSource: FakeProductionReworkDataSource
    private lateinit var repository: ProductionReworkRepository

    @Before
    fun setUp() {
        dataSource = FakeProductionReworkDataSource()
        repository = ProductionReworkRepositoryImpl(dataSource)
    }

    @Test
    fun createRework_validParameters_createsRequestedRework() = runBlocking {
        val result = repository.createRework(
            projectId = "proj-01",
            productionJobId = "job-01",
            reworkType = ReworkType.COLOR_CORRECTION,
            reason = ReworkReason.DEFECT_CORRECTION,
            affectedQuantity = 200,
            quantityUnit = "sheets",
            description = "Cyan and Magenta calibration off by 2.8 delta-E",
            requestedBy = "insp-01",
            requestedByName = "Tariq Inspector",
            notes = "Spotted during inline inspection",
            timestamp = "2026-08-17T10:00:00Z",
            callerRole = UserRole.QC_INSPECTOR
        )

        assertTrue(result is DomainResult.Success)
        val rework = (result as DomainResult.Success).data
        assertNotNull(rework.reworkId)
        assertEquals("proj-01", rework.projectId)
        assertEquals("job-01", rework.productionJobId)
        assertEquals(ReworkStatus.REQUESTED, rework.status)
        assertEquals(ReworkType.COLOR_CORRECTION, rework.reworkType)
        assertEquals(ReworkReason.DEFECT_CORRECTION, rework.reason)
        assertEquals(200, rework.affectedQuantity)
        assertEquals("sheets", rework.quantityUnit)
        assertEquals("insp-01", rework.requestedBy)

        val list = repository.observeReworkList().first()
        assertEquals(1, list.size)
        assertEquals(rework.reworkId, list[0].reworkId)
    }

    @Test
    fun createRework_blankJobId_fails() = runBlocking {
        val result = repository.createRework(
            projectId = "proj-01",
            productionJobId = "",
            reworkType = ReworkType.PRINT_CORRECTION,
            reason = ReworkReason.PRINT_ERROR,
            affectedQuantity = 50,
            quantityUnit = "pcs",
            description = "Print error",
            requestedBy = "insp-01",
            timestamp = "2026-08-17T10:00:00Z",
            callerRole = UserRole.QC_INSPECTOR
        )

        assertTrue(result is DomainResult.Error)
        val error = result as DomainResult.Error
        assertTrue(error.message.contains("Production Job ID cannot be blank"))
    }

    @Test
    fun createRework_zeroQuantity_fails() = runBlocking {
        val result = repository.createRework(
            projectId = "proj-01",
            productionJobId = "job-01",
            reworkType = ReworkType.PRINT_CORRECTION,
            reason = ReworkReason.PRINT_ERROR,
            affectedQuantity = 0,
            quantityUnit = "pcs",
            description = "Print error",
            requestedBy = "insp-01",
            timestamp = "2026-08-17T10:00:00Z",
            callerRole = UserRole.QC_INSPECTOR
        )

        assertTrue(result is DomainResult.Error)
        val error = result as DomainResult.Error
        assertTrue(error.message.contains("Affected quantity must be greater than 0"))
    }
}
