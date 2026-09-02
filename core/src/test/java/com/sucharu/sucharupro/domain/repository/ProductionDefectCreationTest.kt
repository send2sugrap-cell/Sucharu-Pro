package com.sucharu.sucharupro.domain.repository

import com.sucharu.sucharupro.data.datasource.FakeProductionDefectDataSource
import com.sucharu.sucharupro.data.repository.ProductionDefectRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.qc.DefectCategory
import com.sucharu.sucharupro.domain.model.qc.DefectSeverity
import com.sucharu.sucharupro.domain.model.qc.DefectSource
import com.sucharu.sucharupro.domain.model.qc.DefectStatus
import com.sucharu.sucharupro.domain.model.user.UserRole
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Tests for Production Defect creation and registration (Module 06 Step 04).
 */
class ProductionDefectCreationTest {

    private lateinit var dataSource: FakeProductionDefectDataSource
    private lateinit var repository: ProductionDefectRepository

    @Before
    fun setUp() {
        dataSource = FakeProductionDefectDataSource()
        repository = ProductionDefectRepositoryImpl(dataSource)
    }

    @Test
    fun createDefect_validParameters_createsOpenDefect() = runBlocking {
        val result = repository.createDefect(
            productionJobId = "job-01",
            title = "কালার মিসম্যাচ এবং ডেল্টা-ই বেশি",
            description = "Cyan and Magenta balance is off by 3.5 delta-E.",
            category = DefectCategory.COLOR_MISMATCH,
            severity = DefectSeverity.MAJOR,
            source = DefectSource.CHECKLIST_INSPECTION,
            affectedQuantity = 500,
            affectedUnit = "sheets",
            productionStageId = "stage-print-01",
            detectedBy = "insp-01",
            detectedByName = "Rafiq Inspector",
            notes = "Spotted on sheet 1200",
            timestamp = "2026-08-17T10:00:00Z",
            callerRole = UserRole.QC_INSPECTOR
        )

        assertTrue(result is DomainResult.Success)
        val defect = (result as DomainResult.Success).data
        assertNotNull(defect.defectId)
        assertEquals("job-01", defect.productionJobId)
        assertEquals(DefectStatus.OPEN, defect.status)
        assertEquals(DefectSeverity.MAJOR, defect.severity)
        assertEquals(DefectCategory.COLOR_MISMATCH, defect.category)
        assertEquals(500, defect.affectedQuantity)
        assertEquals("sheets", defect.affectedUnit)
        assertEquals("insp-01", defect.detectedBy)

        val list = repository.observeDefectList().first()
        assertEquals(1, list.size)
        assertEquals(defect.defectId, list[0].defectId)
    }

    @Test
    fun createDefect_blankTitle_fails() = runBlocking {
        val result = repository.createDefect(
            productionJobId = "job-01",
            title = "",
            description = "Description",
            category = DefectCategory.PRINT_QUALITY,
            severity = DefectSeverity.MINOR,
            source = DefectSource.PRODUCTION_STAGE,
            affectedQuantity = 10,
            detectedBy = "insp-01",
            timestamp = "2026-08-17T10:00:00Z",
            callerRole = UserRole.QC_INSPECTOR
        )

        assertTrue(result is DomainResult.Error)
        val error = result as DomainResult.Error
        assertTrue(error.message.contains("Title cannot be blank"))
    }

    @Test
    fun createDefect_negativeQuantity_fails() = runBlocking {
        val result = repository.createDefect(
            productionJobId = "job-01",
            title = "Cutting error",
            description = "Description",
            category = DefectCategory.CUTTING_ERROR,
            severity = DefectSeverity.MAJOR,
            source = DefectSource.PRODUCTION_STAGE,
            affectedQuantity = -5,
            detectedBy = "insp-01",
            timestamp = "2026-08-17T10:00:00Z",
            callerRole = UserRole.QC_INSPECTOR
        )

        assertTrue(result is DomainResult.Error)
        val error = result as DomainResult.Error
        assertTrue(error.message.contains("Affected quantity cannot be negative"))
    }
}
