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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Tests for [DefectSeverity] classifications and filtering (Module 06 Step 04).
 */
class ProductionDefectSeverityTest {

    private lateinit var dataSource: FakeProductionDefectDataSource
    private lateinit var repository: ProductionDefectRepository

    private val minorDefect = ProductionDefect(
        defectId = "def-sev-01",
        productionJobId = "job-01",
        category = DefectCategory.SPOT_UV_ERROR,
        severity = DefectSeverity.MINOR,
        source = DefectSource.PRODUCTION_STAGE,
        status = DefectStatus.OPEN,
        title = "Slight UV pinhole",
        description = "Minor pinholes on back cover.",
        affectedQuantity = 10,
        detectedAt = "2026-08-17T10:00:00Z",
        detectedBy = "insp-01",
        createdAt = "2026-08-17T10:00:00Z",
        updatedAt = "2026-08-17T10:00:00Z"
    )

    private val criticalDefect = ProductionDefect(
        defectId = "def-sev-02",
        productionJobId = "job-01",
        category = DefectCategory.CONTENT_ERROR,
        severity = DefectSeverity.CRITICAL,
        source = DefectSource.FINAL_QC,
        status = DefectStatus.OPEN,
        title = "Wrong company phone number printed",
        description = "Critical content error on 50,000 brochures.",
        affectedQuantity = 50000,
        detectedAt = "2026-08-17T10:30:00Z",
        detectedBy = "insp-01",
        createdAt = "2026-08-17T10:30:00Z",
        updatedAt = "2026-08-17T10:30:00Z"
    )

    @Before
    fun setUp() {
        dataSource = FakeProductionDefectDataSource(initialDefects = listOf(minorDefect, criticalDefect))
        repository = ProductionDefectRepositoryImpl(dataSource)
    }

    @Test
    fun observeDefectsBySeverity_returnsFilteredList() = runBlocking {
        val criticals = repository.observeDefectsBySeverity(DefectSeverity.CRITICAL).first()
        assertEquals(1, criticals.size)
        assertEquals("def-sev-02", criticals[0].defectId)
        assertTrue(criticals[0].isCritical)

        val minors = repository.observeDefectsBySeverity(DefectSeverity.MINOR).first()
        assertEquals(1, minors.size)
        assertEquals("def-sev-01", minors[0].defectId)
    }

    @Test
    fun updateSeverity_recordsSeverityChangedActivity() = runBlocking {
        val updated = minorDefect.copy(severity = DefectSeverity.CRITICAL, updatedAt = "2026-08-17T11:00:00Z")
        val result = repository.updateDefect(updated, callerRole = UserRole.ADMIN)

        assertTrue(result is DomainResult.Success)
        val activities = repository.observeDefectActivity("def-sev-01").first()
        assertTrue(activities.any { it.activityType.name == "DEFECT_SEVERITY_CHANGED" })
    }
}
