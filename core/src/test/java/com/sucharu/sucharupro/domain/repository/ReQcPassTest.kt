package com.sucharu.sucharupro.domain.repository

import com.sucharu.sucharupro.data.datasource.FakeProductionReQcDataSource
import com.sucharu.sucharupro.data.datasource.FakeProductionReworkDataSource
import com.sucharu.sucharupro.data.repository.ProductionReQcRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.qc.ProductionRework
import com.sucharu.sucharupro.domain.model.qc.ReQcDecision
import com.sucharu.sucharupro.domain.model.qc.ReQcStatus
import com.sucharu.sucharupro.domain.model.qc.ReworkReason
import com.sucharu.sucharupro.domain.model.qc.ReworkStatus
import com.sucharu.sucharupro.domain.model.qc.ReworkType
import com.sucharu.sucharupro.domain.model.user.UserRole
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Tests for passing Re-QC inspection (Module 06 Step 06).
 */
class ReQcPassTest {

    private lateinit var reQcDataSource: FakeProductionReQcDataSource
    private lateinit var reworkDataSource: FakeProductionReworkDataSource
    private lateinit var repository: ProductionReQcRepository

    @Before
    fun setUp() {
        runBlocking {
            reQcDataSource = FakeProductionReQcDataSource()
            reworkDataSource = FakeProductionReworkDataSource()
            repository = ProductionReQcRepositoryImpl(
                reQcDataSource = reQcDataSource,
                reworkDataSource = reworkDataSource
            )

            reworkDataSource.insertRework(
                ProductionRework(
                    reworkId = "rew-001",
                    projectId = "proj-001",
                    productionJobId = "job-001",
                    reworkType = ReworkType.COLOR_CORRECTION,
                    reason = ReworkReason.DEFECT_CORRECTION,
                    status = ReworkStatus.RETURNED_TO_QC,
                    affectedQuantity = 100,
                    description = "Color fixed",
                    requestedBy = "user-01",
                    requestedAt = "2026-08-17T10:00:00Z",
                    createdAt = "2026-08-17T10:00:00Z",
                    updatedAt = "2026-08-17T10:00:00Z"
                )
            )
        }
    }

    @Test
    fun passReQc_lifecycleFlow_success() = runBlocking {
        // 1. Create
        val createRes = repository.createReQc(
            projectId = "proj-001",
            productionJobId = "job-001",
            productionReworkId = "rew-001",
            createdBy = "insp-01",
            timestamp = "2026-08-17T11:00:00Z",
            callerRole = UserRole.QC_INSPECTOR
        )
        val reQcId = (createRes as DomainResult.Success).data.reQcId

        // 2. Start inspection
        val startRes = repository.startInspection(
            reQcId = reQcId,
            inspectorId = "insp-01",
            inspectorName = "Tariq Inspector",
            timestamp = "2026-08-17T11:15:00Z",
            callerRole = UserRole.QC_INSPECTOR
        )
        assertTrue(startRes is DomainResult.Success)
        assertEquals(ReQcStatus.IN_INSPECTION, (startRes as DomainResult.Success).data.status)

        // 3. Pass
        val passRes = repository.passReQc(
            reQcId = reQcId,
            inspectorId = "insp-01",
            inspectorName = "Tariq Inspector",
            passNotes = "All color densities within 0.5 delta-E tolerances.",
            timestamp = "2026-08-17T11:30:00Z",
            callerRole = UserRole.QC_INSPECTOR
        )
        assertTrue(passRes is DomainResult.Success)
        val passed = (passRes as DomainResult.Success).data
        assertEquals(ReQcStatus.PASSED, passed.status)
        assertEquals(ReQcDecision.PASS, passed.decision)
        assertEquals("2026-08-17T11:30:00Z", passed.completedAt)
        assertTrue(passed.isTerminal)
        assertTrue(passed.isPassed)
    }

    @Test
    fun passReQc_withoutStartingInspection_fails() = runBlocking {
        val createRes = repository.createReQc(
            projectId = "proj-001",
            productionJobId = "job-001",
            productionReworkId = "rew-001",
            createdBy = "insp-01",
            timestamp = "2026-08-17T11:00:00Z",
            callerRole = UserRole.QC_INSPECTOR
        )
        val reQcId = (createRes as DomainResult.Success).data.reQcId

        val passRes = repository.passReQc(
            reQcId = reQcId,
            inspectorId = "insp-01",
            timestamp = "2026-08-17T11:30:00Z",
            callerRole = UserRole.QC_INSPECTOR
        )
        assertTrue(passRes is DomainResult.Error)
    }
}
