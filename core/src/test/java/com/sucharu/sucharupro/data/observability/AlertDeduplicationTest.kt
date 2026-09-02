package com.sucharu.sucharupro.data.observability

import com.sucharu.sucharupro.data.observability.alert.OperationalAlertEngine
import com.sucharu.sucharupro.domain.observability.AlertSeverity
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Alert deduplication & storm prevention test suite (INFRA-04 Step 09).
 */
class AlertDeduplicationTest {

    private lateinit var alertEngine: OperationalAlertEngine

    @Before
    fun setUp() {
        alertEngine = OperationalAlertEngine()
    }

    @Test
    fun test01_100ConsecutiveErrors_aggregatesToOneAlert() {
        repeat(100) { i ->
            alertEngine.recordCondition(
                projectId = "p-001",
                subsystem = "PROVIDER",
                alertKey = "SMS_PROVIDER_FAILURE",
                title = "Twilio SMS Down",
                summary = "Failed request $i",
                severity = AlertSeverity.CRITICAL
            )
        }

        val activeAlerts = alertEngine.getActiveAlerts("p-001")
        assertEquals("100 errors must produce exactly 1 aggregated alert", 1, activeAlerts.size)
        assertEquals("Occurrences must be tracked accurately", 100, activeAlerts.first().occurrences)
    }

    @Test
    fun test02_distinctAlertKeys_createSeparateAlerts() {
        alertEngine.recordCondition("p-001", "PROVIDER", "SMS_DOWN", "SMS Down", "SMS Error", AlertSeverity.CRITICAL)
        alertEngine.recordCondition("p-001", "PROVIDER", "EMAIL_DOWN", "Email Down", "Email Error", AlertSeverity.CRITICAL)
        val activeAlerts = alertEngine.getActiveAlerts("p-001")
        assertEquals(2, activeAlerts.size)
    }
}
