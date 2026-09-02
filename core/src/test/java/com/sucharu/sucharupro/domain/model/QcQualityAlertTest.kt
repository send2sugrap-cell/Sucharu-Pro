package com.sucharu.sucharupro.domain.model

import com.sucharu.sucharupro.domain.model.qc.governance.QcAlertSeverity
import com.sucharu.sucharupro.domain.model.qc.governance.QcAlertStatus
import com.sucharu.sucharupro.domain.model.qc.governance.QcGovernanceKpi
import com.sucharu.sucharupro.domain.model.qc.governance.QcQualityAlert
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class QcQualityAlertTest {

    @Test
    fun `alert model instantiates with default status and terminal flag`() {
        val alert = QcQualityAlert(
            alertId = "ALT-01",
            projectId = "PRJ-01",
            kpiType = QcGovernanceKpi.DEFECT_RATE,
            currentValue = 8.5,
            targetValue = 3.0,
            severity = QcAlertSeverity.CRITICAL,
            title = "Excessive Defect Rate",
            message = "Defect rate exceeds 8%",
            detectedAt = "2026-08-17T08:00:00Z"
        )
        assertEquals(QcAlertStatus.DETECTED, alert.status)
        assertFalse(alert.isTerminal)

        val resolved = alert.copy(status = QcAlertStatus.RESOLVED)
        assertTrue(resolved.isTerminal)
    }
}
