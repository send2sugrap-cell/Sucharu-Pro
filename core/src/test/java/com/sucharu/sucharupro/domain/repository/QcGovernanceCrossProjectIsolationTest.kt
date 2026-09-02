package com.sucharu.sucharupro.domain.repository

import com.sucharu.sucharupro.data.datasource.FakeQcGovernanceDataSource
import com.sucharu.sucharupro.data.repository.QcGovernanceRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.qc.governance.QcAlertSeverity
import com.sucharu.sucharupro.domain.model.qc.governance.QcGovernanceKpi
import com.sucharu.sucharupro.domain.model.qc.governance.QcKpiTarget
import com.sucharu.sucharupro.domain.model.qc.governance.QcQualityAlert
import com.sucharu.sucharupro.domain.model.user.UserRole
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class QcGovernanceCrossProjectIsolationTest {

    private lateinit var dataSource: FakeQcGovernanceDataSource
    private lateinit var repository: QcGovernanceRepository

    @Before
    fun setup() {
        dataSource = FakeQcGovernanceDataSource()
        repository = QcGovernanceRepositoryImpl(governanceDataSource = dataSource)
    }

    @Test
    fun `project A targets and alerts never leak into project B queries`() = runBlocking {
        // Set targets for PRJ-A and PRJ-B
        repository.setTarget(
            QcKpiTarget(
                targetId = "TGT-A",
                projectId = "PRJ-A",
                kpiType = QcGovernanceKpi.FIRST_PASS_RATE,
                targetValue = 95.0,
                effectiveFrom = "2026-08-01T00:00:00Z",
                configuredBy = "admin-1",
                createdAt = "2026-08-01T00:00:00Z",
                updatedAt = "2026-08-01T00:00:00Z"
            ),
            callerRole = UserRole.ADMIN
        )
        repository.setTarget(
            QcKpiTarget(
                targetId = "TGT-B",
                projectId = "PRJ-B",
                kpiType = QcGovernanceKpi.DEFECT_RATE,
                targetValue = 2.0,
                effectiveFrom = "2026-08-01T00:00:00Z",
                configuredBy = "admin-1",
                createdAt = "2026-08-01T00:00:00Z",
                updatedAt = "2026-08-01T00:00:00Z"
            ),
            callerRole = UserRole.ADMIN
        )

        // Create alerts for PRJ-A
        repository.createAlert(
            QcQualityAlert(
                alertId = "ALT-A",
                projectId = "PRJ-A",
                kpiType = QcGovernanceKpi.FIRST_PASS_RATE,
                currentValue = 70.0,
                targetValue = 95.0,
                severity = QcAlertSeverity.CRITICAL,
                title = "Low pass rate",
                message = "Pass rate is low",
                detectedAt = "2026-08-17T08:00:00Z"
            )
        )

        // Verify PRJ-A
        val targetsA = repository.getTargets("PRJ-A")
        assertTrue(targetsA is DomainResult.Success)
        assertEquals(1, (targetsA as DomainResult.Success).data.size)
        assertEquals("TGT-A", targetsA.data.first().targetId)

        val alertsA = repository.getAlerts("PRJ-A")
        assertTrue(alertsA is DomainResult.Success)
        assertEquals(1, (alertsA as DomainResult.Success).data.size)

        // Verify PRJ-B
        val targetsB = repository.getTargets("PRJ-B")
        assertTrue(targetsB is DomainResult.Success)
        assertEquals(1, (targetsB as DomainResult.Success).data.size)
        assertEquals("TGT-B", targetsB.data.first().targetId)

        val alertsB = repository.getAlerts("PRJ-B")
        assertTrue(alertsB is DomainResult.Success)
        assertEquals(0, (alertsB as DomainResult.Success).data.size) // No alerts for PRJ-B

        // Verify Flow observation isolation
        val streamA = repository.observeAlerts("PRJ-A").first()
        assertEquals(1, streamA.size)
        val streamB = repository.observeAlerts("PRJ-B").first()
        assertEquals(0, streamB.size)
    }
}
