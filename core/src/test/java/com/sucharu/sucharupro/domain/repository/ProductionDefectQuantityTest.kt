package com.sucharu.sucharupro.domain.repository

import com.sucharu.sucharupro.data.datasource.FakeProductionDefectDataSource
import com.sucharu.sucharupro.data.repository.ProductionDefectRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.qc.DefectCategory
import com.sucharu.sucharupro.domain.model.qc.DefectSeverity
import com.sucharu.sucharupro.domain.model.qc.DefectSource
import com.sucharu.sucharupro.domain.model.qc.ProductionDefect
import com.sucharu.sucharupro.domain.model.user.UserRole
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Tests for affected quantity tracking and validation rules on [ProductionDefect] (Module 06 Step 04).
 */
class ProductionDefectQuantityTest {

    private lateinit var dataSource: FakeProductionDefectDataSource
    private lateinit var repository: ProductionDefectRepository

    @Before
    fun setUp() {
        dataSource = FakeProductionDefectDataSource()
        repository = ProductionDefectRepositoryImpl(dataSource)
    }

    @Test
    fun createDefect_zeroQuantity_isAllowedForGeneralProcessDefects() = runBlocking {
        val result = repository.createDefect(
            productionJobId = "job-01",
            title = "Process checklist missed step",
            description = "Drying time step was shortened.",
            category = DefectCategory.PROCESS_ERROR,
            severity = DefectSeverity.MINOR,
            source = DefectSource.SUPERVISOR_REPORTED,
            affectedQuantity = 0,
            affectedUnit = "pcs",
            detectedBy = "insp-01",
            timestamp = "2026-08-17T10:00:00Z",
            callerRole = UserRole.QC_INSPECTOR
        )

        assertTrue(result is DomainResult.Success)
        val defect = (result as DomainResult.Success).data
        assertEquals(0, defect.affectedQuantity)
    }

    @Test
    fun createDefect_largeQuantity_succeeds() = runBlocking {
        val result = repository.createDefect(
            productionJobId = "job-01",
            title = "Large print run smudge",
            description = "Offset smudge across batch.",
            category = DefectCategory.PRINT_QUALITY,
            severity = DefectSeverity.MAJOR,
            source = DefectSource.PRODUCTION_STAGE,
            affectedQuantity = 100000,
            affectedUnit = "copies",
            detectedBy = "insp-01",
            timestamp = "2026-08-17T10:00:00Z",
            callerRole = UserRole.QC_INSPECTOR
        )

        assertTrue(result is DomainResult.Success)
        val defect = (result as DomainResult.Success).data
        assertEquals(100000, defect.affectedQuantity)
        assertEquals("copies", defect.affectedUnit)
    }
}
