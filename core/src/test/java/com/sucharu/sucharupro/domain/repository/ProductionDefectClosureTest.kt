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
 * Tests for [ProductionDefect] terminal closure permissions, rules, and lifecycle finalization (Module 06 Step 04).
 */
class ProductionDefectClosureTest {

    private lateinit var dataSource: FakeProductionDefectDataSource
    private lateinit var repository: ProductionDefectRepository

    private val resolvedDefect = ProductionDefect(
        defectId = "def-close-01",
        productionJobId = "job-01",
        category = DefectCategory.MACHINE_ERROR,
        severity = DefectSeverity.CRITICAL,
        source = DefectSource.PRODUCTION_STAGE,
        status = DefectStatus.RESOLVED,
        title = "Feeder double-sheet sensor trip",
        description = "Feeder fed double sheets causing blanket indentation.",
        affectedQuantity = 20,
        resolutionNotes = "Replaced blanket and reset caliper detector",
        resolvedBy = "insp-01",
        resolvedAt = "2026-08-17T10:00:00Z",
        detectedAt = "2026-08-17T09:00:00Z",
        detectedBy = "insp-01",
        createdAt = "2026-08-17T09:00:00Z",
        updatedAt = "2026-08-17T10:00:00Z"
    )

    private val openDefect = resolvedDefect.copy(
        defectId = "def-close-02",
        status = DefectStatus.OPEN,
        resolutionNotes = null,
        resolvedBy = null,
        resolvedAt = null
    )

    @Before
    fun setUp() {
        dataSource = FakeProductionDefectDataSource(initialDefects = listOf(resolvedDefect, openDefect))
        repository = ProductionDefectRepositoryImpl(dataSource)
    }

    @Test
    fun closeDefect_byAdmin_onResolvedDefect_transitionsToClosed() = runBlocking {
        val result = repository.closeDefect(
            defectId = "def-close-01",
            closedBy = "admin-01",
            closedByName = "Admin Sumon",
            notes = "Signed off after verifying machine run test.",
            timestamp = "2026-08-17T10:30:00Z",
            callerRole = UserRole.ADMIN
        )

        assertTrue(result is DomainResult.Success)
        val defect = (result as DomainResult.Success).data
        assertEquals(DefectStatus.CLOSED, defect.status)
        assertEquals("admin-01", defect.closedBy)
        assertNotNull(defect.closedAt)
        assertTrue(defect.isTerminal)
    }

    @Test
    fun closeDefect_byInspector_isRejected() = runBlocking {
        val result = repository.closeDefect(
            defectId = "def-close-01",
            closedBy = "insp-01",
            timestamp = "2026-08-17T10:30:00Z",
            callerRole = UserRole.QC_INSPECTOR // Inspectors cannot close
        )

        assertTrue(result is DomainResult.Error)
        val error = result as DomainResult.Error
        assertTrue(error.message.contains("not authorized to close QC defects"))
    }

    @Test
    fun closeDefect_onOpenDefect_isRejected() = runBlocking {
        val result = repository.closeDefect(
            defectId = "def-close-02",
            closedBy = "admin-01",
            timestamp = "2026-08-17T10:30:00Z",
            callerRole = UserRole.ADMIN
        )

        assertTrue(result is DomainResult.Error)
        val error = result as DomainResult.Error
        assertTrue(error.message.contains("not in RESOLVED status") || error.message.contains("Invalid defect status transition"))
    }
}
