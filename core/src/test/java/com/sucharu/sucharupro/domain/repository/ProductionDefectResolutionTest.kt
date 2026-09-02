package com.sucharu.sucharupro.domain.repository

import com.sucharu.sucharupro.data.datasource.FakeProductionDefectDataSource
import com.sucharu.sucharupro.data.repository.ProductionDefectRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.qc.DefectCategory
import com.sucharu.sucharupro.domain.model.qc.DefectSeverity
import com.sucharu.sucharupro.domain.model.qc.DefectSource
import com.sucharu.sucharupro.domain.model.qc.DefectStatus
import com.sucharu.sucharupro.domain.model.qc.ProductionDefect
import com.sucharu.sucharupro.domain.model.user.UserRole
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Tests for [ProductionDefect] resolution actions and prerequisites (Module 06 Step 04).
 */
class ProductionDefectResolutionTest {

    private lateinit var dataSource: FakeProductionDefectDataSource
    private lateinit var repository: ProductionDefectRepository

    private val pendingDefect = ProductionDefect(
        defectId = "def-res-01",
        productionJobId = "job-01",
        category = DefectCategory.FOLDING_ERROR,
        severity = DefectSeverity.MAJOR,
        source = DefectSource.PRODUCTION_STAGE,
        status = DefectStatus.RESOLUTION_PENDING,
        title = "Buckled fold on 16pp brochure",
        description = "Brochures buckling on 2nd right-angle fold.",
        affectedQuantity = 300,
        detectedAt = "2026-08-17T10:00:00Z",
        detectedBy = "insp-01",
        createdAt = "2026-08-17T10:00:00Z",
        updatedAt = "2026-08-17T10:00:00Z"
    )

    @Before
    fun setUp() {
        dataSource = FakeProductionDefectDataSource(initialDefects = listOf(pendingDefect))
        repository = ProductionDefectRepositoryImpl(dataSource)
    }

    @Test
    fun resolveDefect_withValidNotesAndActor_transitionsToResolved() = runBlocking {
        val result = repository.resolveDefect(
            defectId = "def-res-01",
            resolutionNotes = "Adjusted folding roller gap for 170gsm stock and verified crisp folds.",
            resolvedBy = "insp-01",
            resolvedByName = "Inspector 1",
            timestamp = "2026-08-17T10:30:00Z",
            callerRole = UserRole.QC_INSPECTOR
        )

        assertTrue(result is DomainResult.Success)
        val defect = (result as DomainResult.Success).data
        assertEquals(DefectStatus.RESOLVED, defect.status)
        assertEquals("Adjusted folding roller gap for 170gsm stock and verified crisp folds.", defect.resolutionNotes)
        assertEquals("insp-01", defect.resolvedBy)
        assertNotNull(defect.resolvedAt)
    }

    @Test
    fun resolveDefect_withBlankNotes_fails() = runBlocking {
        val result = repository.resolveDefect(
            defectId = "def-res-01",
            resolutionNotes = "   ",
            resolvedBy = "insp-01",
            timestamp = "2026-08-17T10:30:00Z",
            callerRole = UserRole.QC_INSPECTOR
        )

        assertTrue(result is DomainResult.Error)
        val error = result as DomainResult.Error
        assertTrue(error.message.contains("Resolution notes are mandatory"))
    }
}
