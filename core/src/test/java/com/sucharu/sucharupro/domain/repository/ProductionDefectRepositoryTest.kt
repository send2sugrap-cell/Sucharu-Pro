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
 * End-to-end repository operation tests for [ProductionDefectRepository] (Module 06 Step 04).
 */
class ProductionDefectRepositoryTest {

    private lateinit var dataSource: FakeProductionDefectDataSource
    private lateinit var repository: ProductionDefectRepository

    @Before
    fun setUp() {
        dataSource = FakeProductionDefectDataSource()
        repository = ProductionDefectRepositoryImpl(dataSource)
    }

    @Test
    fun fullDefectWorkflow_fromCreationToClosure() = runBlocking {
        // 1. Create Defect
        val createRes = repository.createDefect(
            productionJobId = "job-repo-01",
            title = "Registration drift on Cyan plate",
            description = "Plate misaligned by 0.5mm across 4-up layout.",
            category = DefectCategory.REGISTRATION_ERROR,
            severity = DefectSeverity.MAJOR,
            source = DefectSource.PRODUCTION_STAGE,
            affectedQuantity = 200,
            detectedBy = "insp-01",
            detectedByName = "Inspector Alim",
            timestamp = "2026-08-17T10:00:00Z",
            callerRole = UserRole.QC_INSPECTOR
        )
        assertTrue(createRes is DomainResult.Success)
        val defectId = (createRes as DomainResult.Success).data.defectId

        // 2. Acknowledge
        val ackRes = repository.acknowledgeDefect(
            defectId = defectId,
            acknowledgedBy = "mgr-01",
            acknowledgedByName = "Manager Sumon",
            timestamp = "2026-08-17T10:10:00Z",
            callerRole = UserRole.MANAGER
        )
        assertTrue(ackRes is DomainResult.Success)
        assertEquals(DefectStatus.ACKNOWLEDGED, (ackRes as DomainResult.Success).data.status)

        // 3. Start Investigation
        val invRes = repository.investigateDefect(
            defectId = defectId,
            investigatorId = "insp-01",
            investigatorName = "Inspector Alim",
            notes = "Checked plate cylinder clamp tension.",
            timestamp = "2026-08-17T10:15:00Z",
            callerRole = UserRole.QC_INSPECTOR
        )
        assertTrue(invRes is DomainResult.Success)
        assertEquals(DefectStatus.UNDER_INVESTIGATION, (invRes as DomainResult.Success).data.status)

        // 4. Containment
        val conRes = repository.containDefect(
            defectId = defectId,
            containmentNotes = "Stopped press; quarantined 200 misregistered sheets.",
            containedBy = "insp-01",
            timestamp = "2026-08-17T10:20:00Z",
            callerRole = UserRole.QC_INSPECTOR
        )
        assertTrue(conRes is DomainResult.Success)
        assertEquals(DefectStatus.CONTAINED, (conRes as DomainResult.Success).data.status)

        // 5. Resolution Pending
        val pendingRes = repository.startResolution(
            defectId = defectId,
            notes = "Plate re-mounted and re-clamped.",
            initiatedBy = "insp-01",
            timestamp = "2026-08-17T10:30:00Z",
            callerRole = UserRole.QC_INSPECTOR
        )
        assertTrue(pendingRes is DomainResult.Success)
        assertEquals(DefectStatus.RESOLUTION_PENDING, (pendingRes as DomainResult.Success).data.status)

        // 6. Resolve
        val resolveRes = repository.resolveDefect(
            defectId = defectId,
            resolutionNotes = "Cyan plate remounted with calibrated gauge; test run of 50 sheets verified under 0.05mm register tolerance.",
            resolvedBy = "insp-01",
            resolvedByName = "Inspector Alim",
            timestamp = "2026-08-17T10:45:00Z",
            callerRole = UserRole.QC_INSPECTOR
        )
        assertTrue(resolveRes is DomainResult.Success)
        assertEquals(DefectStatus.RESOLVED, (resolveRes as DomainResult.Success).data.status)
        assertTrue((resolveRes as DomainResult.Success).data.isResolved)

        // 7. Close
        val closeRes = repository.closeDefect(
            defectId = defectId,
            closedBy = "admin-01",
            closedByName = "Admin User",
            notes = "Signed off by production supervisor.",
            timestamp = "2026-08-17T11:00:00Z",
            callerRole = UserRole.ADMIN
        )
        assertTrue(closeRes is DomainResult.Success)
        val finalDefect = (closeRes as DomainResult.Success).data
        assertEquals(DefectStatus.CLOSED, finalDefect.status)
        assertTrue(finalDefect.isTerminal)

        // 8. Verify Queries
        val jobDefects = repository.observeDefectsByJob("job-repo-01").first()
        assertEquals(1, jobDefects.size)
        val closedDefects = repository.observeDefectsByStatus(DefectStatus.CLOSED).first()
        assertEquals(1, closedDefects.size)
    }
}
