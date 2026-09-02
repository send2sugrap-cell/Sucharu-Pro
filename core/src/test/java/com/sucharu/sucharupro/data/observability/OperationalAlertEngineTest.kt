package com.sucharu.sucharupro.data.observability

import com.sucharu.sucharupro.data.observability.alert.OperationalAlertEngine
import com.sucharu.sucharupro.domain.observability.AlertSeverity
import com.sucharu.sucharupro.domain.observability.AlertStatus
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Operational Alert lifecycle, acknowledging, and resolving test suite (INFRA-04 Step 09).
 */
class OperationalAlertEngineTest {

    private lateinit var alertEngine: OperationalAlertEngine

    @Before
    fun setUp() {
        alertEngine = OperationalAlertEngine()
    }

    @Test
    fun test01_recordAlert_createsOpenAlert() {
        val alert = alertEngine.recordCondition(
            projectId = "p-001",
            subsystem = "OUTBOX",
            alertKey = "OUTBOX_BACKLOG_HIGH",
            title = "High Outbox Backlog",
            summary = "600 events pending",
            severity = AlertSeverity.WARNING
        )
        assertEquals(AlertStatus.OPEN, alert.status)
        assertEquals(1, alert.occurrences)
    }

    @Test
    fun test02_acknowledgeAlert_updatesStatus() {
        val alert = alertEngine.recordCondition(
            projectId = "p-001",
            subsystem = "OUTBOX",
            alertKey = "OUTBOX_BACKLOG_HIGH",
            title = "High Outbox Backlog",
            summary = "600 events pending",
            severity = AlertSeverity.WARNING
        )
        val acked = alertEngine.acknowledgeAlert(alert.alertId, "admin-user")
        assertNotNull(acked)
        assertEquals(AlertStatus.ACKNOWLEDGED, acked!!.status)
        assertEquals("admin-user", acked.acknowledgedBy)
    }

    @Test
    fun test03_resolveAlert_marksResolved() {
        alertEngine.recordCondition(
            projectId = "p-001",
            subsystem = "OUTBOX",
            alertKey = "OUTBOX_BACKLOG_HIGH",
            title = "High Outbox Backlog",
            summary = "600 events pending",
            severity = AlertSeverity.WARNING
        )
        val resolved = alertEngine.resolveCondition("p-001", "OUTBOX", "OUTBOX_BACKLOG_HIGH")
        assertNotNull(resolved)
        assertEquals(AlertStatus.RESOLVED, resolved!!.status)
        assertNotNull(resolved.resolvedAt)
    }
}
