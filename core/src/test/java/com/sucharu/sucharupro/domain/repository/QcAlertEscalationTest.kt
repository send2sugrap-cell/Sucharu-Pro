package com.sucharu.sucharupro.domain.repository

import com.sucharu.sucharupro.data.datasource.FakeQcGovernanceDataSource
import com.sucharu.sucharupro.data.repository.QcGovernanceRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.qc.governance.QcAlertSeverity
import com.sucharu.sucharupro.domain.model.qc.governance.QcAlertStatus
import com.sucharu.sucharupro.domain.model.qc.governance.QcEscalationLevel
import com.sucharu.sucharupro.domain.model.qc.governance.QcGovernanceKpi
import com.sucharu.sucharupro.domain.model.qc.governance.QcQualityAlert
import com.sucharu.sucharupro.domain.model.user.UserRole
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class QcAlertEscalationTest {

    private lateinit var dataSource: FakeQcGovernanceDataSource
    private lateinit var repository: QcGovernanceRepository

    @Before
    fun setup() {
        dataSource = FakeQcGovernanceDataSource()
        repository = QcGovernanceRepositoryImpl(governanceDataSource = dataSource)
    }

    @Test
    fun `alert escalation updates escalation level and appends escalation notes`() = runBlocking {
        val alert = QcQualityAlert(
            alertId = "ALT-01",
            projectId = "PRJ-01",
            kpiType = QcGovernanceKpi.CRITICAL_DEFECT_RATE,
            currentValue = 2.0,
            targetValue = 0.0,
            severity = QcAlertSeverity.CRITICAL,
            title = "Critical defect breach",
            message = "Critical defects detected",
            detectedAt = "2026-08-17T08:00:00Z"
        )
        repository.createAlert(alert)

        // Escalate to Manager
        val escRes = repository.escalateAlert(
            alertId = "ALT-01",
            targetLevel = QcEscalationLevel.MANAGER,
            escalatedBy = "insp-01",
            notes = "Unresolved for 2 hours, escalating to shift manager",
            timestamp = "2026-08-17T10:00:00Z",
            callerRole = UserRole.QC_INSPECTOR
        )

        assertTrue(escRes is DomainResult.Success)
        val updated = (escRes as DomainResult.Success).data
        assertEquals(QcEscalationLevel.MANAGER, updated.escalationLevel)
        assertEquals(QcAlertStatus.ACKNOWLEDGED, updated.status)
        assertTrue(updated.notes?.contains("[Escalation]") == true)
    }
}
