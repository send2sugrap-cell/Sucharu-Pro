package com.sucharu.sucharupro.domain.repository

import com.sucharu.sucharupro.data.datasource.FakeQcGovernanceDataSource
import com.sucharu.sucharupro.data.repository.QcGovernanceRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.qc.governance.QcAlertSeverity
import com.sucharu.sucharupro.domain.model.qc.governance.QcGovernanceActivityType
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

class QcGovernanceAuditTest {

    private lateinit var dataSource: FakeQcGovernanceDataSource
    private lateinit var repository: QcGovernanceRepository

    @Before
    fun setup() {
        dataSource = FakeQcGovernanceDataSource()
        repository = QcGovernanceRepositoryImpl(governanceDataSource = dataSource)
    }

    @Test
    fun `governance actions automatically append immutable audit activity events`() = runBlocking {
        // 1. Set KPI Target
        val target = QcKpiTarget(
            targetId = "TGT-01",
            projectId = "PRJ-01",
            kpiType = QcGovernanceKpi.FIRST_PASS_RATE,
            targetValue = 95.0,
            effectiveFrom = "2026-08-01T00:00:00Z",
            configuredBy = "admin-01",
            createdAt = "2026-08-01T00:00:00Z",
            updatedAt = "2026-08-01T00:00:00Z"
        )
        repository.setTarget(target, callerRole = UserRole.ADMIN)

        // 2. Create Alert
        val alert = QcQualityAlert(
            alertId = "ALT-01",
            projectId = "PRJ-01",
            kpiType = QcGovernanceKpi.FIRST_PASS_RATE,
            currentValue = 82.0,
            targetValue = 95.0,
            severity = QcAlertSeverity.WARNING,
            title = "Pass rate low",
            message = "Pass rate low",
            detectedAt = "2026-08-17T08:00:00Z"
        )
        repository.createAlert(alert)

        // 3. Acknowledge Alert
        repository.acknowledgeAlert("ALT-01", "insp-01", "Checking root cause", "2026-08-17T08:30:00Z", UserRole.QC_INSPECTOR)

        val events = repository.observeActivityEvents("PRJ-01").first()
        assertEquals(3, events.size)

        assertEquals(QcGovernanceActivityType.KPI_TARGET_CREATED, events[0].eventType)
        assertEquals(QcGovernanceActivityType.ALERT_CREATED, events[1].eventType)
        assertEquals(QcGovernanceActivityType.ALERT_ACKNOWLEDGED, events[2].eventType)
    }
}
