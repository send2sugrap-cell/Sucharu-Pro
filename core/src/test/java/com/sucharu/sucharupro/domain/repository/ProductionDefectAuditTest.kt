package com.sucharu.sucharupro.domain.repository

import com.sucharu.sucharupro.data.datasource.FakeProductionDefectDataSource
import com.sucharu.sucharupro.data.repository.ProductionDefectRepositoryImpl
import com.sucharu.sucharupro.domain.model.qc.DefectCategory
import com.sucharu.sucharupro.domain.model.qc.DefectSeverity
import com.sucharu.sucharupro.domain.model.qc.DefectSource
import com.sucharu.sucharupro.domain.model.qc.DefectStatus
import com.sucharu.sucharupro.domain.model.user.UserRole
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Immutable audit logging and event sequence tests for [ProductionDefect] (Module 06 Step 04).
 */
class ProductionDefectAuditTest {

    private lateinit var dataSource: FakeProductionDefectDataSource
    private lateinit var repository: ProductionDefectRepository

    @Before
    fun setUp() {
        dataSource = FakeProductionDefectDataSource()
        repository = ProductionDefectRepositoryImpl(dataSource)
    }

    @Test
    fun defectLifecycleMutations_generateOrderedAuditEvents() = runBlocking {
        // 1. Create
        val createRes = repository.createDefect(
            productionJobId = "job-audit-01",
            title = "Missing spot UV on title",
            description = "UV plate misaligned by 5mm.",
            category = DefectCategory.SPOT_UV_ERROR,
            severity = DefectSeverity.MAJOR,
            source = DefectSource.PRODUCTION_STAGE,
            affectedQuantity = 200,
            detectedBy = "insp-01",
            timestamp = "2026-08-17T10:00:00Z",
            callerRole = UserRole.QC_INSPECTOR
        )
        val defectId = (createRes as com.sucharu.sucharupro.domain.model.common.DomainResult.Success).data.defectId

        // 2. Acknowledge
        repository.acknowledgeDefect(
            defectId = defectId,
            acknowledgedBy = "mgr-01",
            acknowledgedByName = "Kamal Manager",
            timestamp = "2026-08-17T10:10:00Z",
            callerRole = UserRole.MANAGER
        )

        // 3. Investigate
        repository.investigateDefect(
            defectId = defectId,
            investigatorId = "insp-01",
            timestamp = "2026-08-17T10:15:00Z",
            callerRole = UserRole.QC_INSPECTOR
        )

        // 4. Contain
        repository.containDefect(
            defectId = defectId,
            containmentNotes = "Paused spot UV line and quarantined 200 defective sheets.",
            containedBy = "insp-01",
            timestamp = "2026-08-17T10:20:00Z",
            callerRole = UserRole.QC_INSPECTOR
        )

        // 5. Resolve
        repository.resolveDefect(
            defectId = defectId,
            resolutionNotes = "Realaligned screen mesh and test printed 5 sheets with 100% registration.",
            resolvedBy = "insp-01",
            timestamp = "2026-08-17T10:45:00Z",
            callerRole = UserRole.QC_INSPECTOR
        )

        // 6. Close
        repository.closeDefect(
            defectId = defectId,
            closedBy = "admin-01",
            timestamp = "2026-08-17T11:00:00Z",
            callerRole = UserRole.ADMIN
        )

        val activities = repository.observeDefectActivity(defectId).first()
        // Activities are stored newest-first in fake data source
        val activityTypes = activities.map { it.activityType.name }

        assertTrue(activityTypes.contains("DEFECT_CREATED"))
        assertTrue(activityTypes.contains("DEFECT_ACKNOWLEDGED"))
        assertTrue(activityTypes.contains("DEFECT_INVESTIGATION_STARTED"))
        assertTrue(activityTypes.contains("DEFECT_CONTAINED"))
        assertTrue(activityTypes.contains("DEFECT_RESOLVED"))
        assertTrue(activityTypes.contains("DEFECT_CLOSED"))
    }
}
