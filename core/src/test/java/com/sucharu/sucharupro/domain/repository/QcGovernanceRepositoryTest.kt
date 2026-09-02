package com.sucharu.sucharupro.domain.repository

import com.sucharu.sucharupro.data.datasource.FakeQcGovernanceDataSource
import com.sucharu.sucharupro.data.repository.QcGovernanceRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.qc.analytics.QcAnalyticsPeriod
import com.sucharu.sucharupro.domain.model.qc.governance.QcGovernanceKpi
import com.sucharu.sucharupro.domain.model.qc.governance.QcImprovementAction
import com.sucharu.sucharupro.domain.model.qc.governance.QcImprovementActionStatus
import com.sucharu.sucharupro.domain.model.qc.governance.QcImprovementActionType
import com.sucharu.sucharupro.domain.model.qc.governance.QcImprovementEffectiveness
import com.sucharu.sucharupro.domain.model.qc.governance.QcKpiTarget
import com.sucharu.sucharupro.domain.model.qc.governance.QcQualityReview
import com.sucharu.sucharupro.domain.model.qc.governance.QcQualityReviewStatus
import com.sucharu.sucharupro.domain.model.user.UserRole
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class QcGovernanceRepositoryTest {

    private lateinit var dataSource: FakeQcGovernanceDataSource
    private lateinit var repository: QcGovernanceRepository

    @Before
    fun setup() {
        dataSource = FakeQcGovernanceDataSource()
        repository = QcGovernanceRepositoryImpl(governanceDataSource = dataSource)
    }

    @Test
    fun `complete quality review and continuous improvement action lifecycle through repository`() = runBlocking {
        // 1. Review Lifecycle
        val review = QcQualityReview(
            reviewId = "REV-01",
            projectId = "PRJ-01",
            title = "Q3 Quality Governance Review",
            reviewPeriod = QcAnalyticsPeriod.thisMonth(),
            reviewerId = "mgr-01",
            createdAt = "2026-08-17T08:00:00Z",
            updatedAt = "2026-08-17T08:00:00Z"
        )
        val createRevRes = repository.createReview(review, callerRole = UserRole.MANAGER)
        assertTrue(createRevRes is DomainResult.Success)

        val startRevRes = repository.startReview("REV-01", "mgr-01", "2026-08-17T09:00:00Z", UserRole.MANAGER)
        assertTrue(startRevRes is DomainResult.Success)
        assertEquals(QcQualityReviewStatus.IN_REVIEW, (startRevRes as DomainResult.Success).data.status)

        val compRevRes = repository.completeReview(
            reviewId = "REV-01",
            reviewerId = "mgr-01",
            recommendations = "Standardize plate dampener tensioning",
            reviewNotes = "Review closed successfully",
            timestamp = "2026-08-17T10:00:00Z",
            callerRole = UserRole.MANAGER
        )
        assertTrue(compRevRes is DomainResult.Success)
        assertEquals(QcQualityReviewStatus.COMPLETED, (compRevRes as DomainResult.Success).data.status)

        // 2. Improvement Action Lifecycle
        val action = QcImprovementAction(
            actionId = "ACT-01",
            projectId = "PRJ-01",
            sourceReviewId = "REV-01",
            proposedBy = "insp-01",
            proposedByName = "Tariq Inspector",
            actionType = QcImprovementActionType.CORRECTIVE_ACTION,
            title = "Replace Dampers on Press 2",
            description = "Plate misregistration due to damper fatigue",
            createdAt = "2026-08-17T10:05:00Z",
            updatedAt = "2026-08-17T10:05:00Z"
        )
        val propRes = repository.proposeImprovementAction(action, callerRole = UserRole.QC_INSPECTOR)
        assertTrue(propRes is DomainResult.Success)

        val appRes = repository.approveImprovementAction("ACT-01", "mgr-01", "Manager Rahim", "2026-08-17T10:10:00Z", UserRole.MANAGER)
        assertTrue(appRes is DomainResult.Success)
        assertEquals(QcImprovementActionStatus.APPROVED, (appRes as DomainResult.Success).data.status)

        val assRes = repository.assignImprovementAction("ACT-01", "tech-01", "Kamal Technician", "2026-08-17T10:15:00Z", UserRole.MANAGER)
        assertTrue(assRes is DomainResult.Success)

        val startActRes = repository.startImprovementAction("ACT-01", "2026-08-17T10:20:00Z", UserRole.QC_INSPECTOR)
        assertTrue(startActRes is DomainResult.Success)
        assertEquals(QcImprovementActionStatus.IN_PROGRESS, (startActRes as DomainResult.Success).data.status)

        val compActRes = repository.completeImprovementAction("ACT-01", "Dampers replaced with OEM parts", "2026-08-17T11:00:00Z", 98.0, UserRole.QC_INSPECTOR)
        assertTrue(compActRes is DomainResult.Success)
        assertEquals(QcImprovementActionStatus.COMPLETED, (compActRes as DomainResult.Success).data.status)

        val verActRes = repository.verifyImprovementAction(
            actionId = "ACT-01",
            verifiedBy = "admin-01",
            verifiedByName = "Admin User",
            effectiveness = QcImprovementEffectiveness.HIGHLY_EFFECTIVE,
            verificationNotes = "First-pass rate increased to 98% over 3 subsequent print runs",
            timestamp = "2026-08-17T11:30:00Z",
            callerRole = UserRole.ADMIN
        )
        assertTrue(verActRes is DomainResult.Success)
        val verifiedAction = (verActRes as DomainResult.Success).data
        assertEquals(QcImprovementActionStatus.VERIFIED, verifiedAction.status)
        assertEquals(QcImprovementEffectiveness.HIGHLY_EFFECTIVE, verifiedAction.effectiveness)
    }
}
