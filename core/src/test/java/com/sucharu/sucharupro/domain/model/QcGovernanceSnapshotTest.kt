package com.sucharu.sucharupro.domain.model

import com.sucharu.sucharupro.domain.model.qc.analytics.QcAnalyticsPeriod
import com.sucharu.sucharupro.domain.model.qc.governance.QcGovernanceSnapshot
import org.junit.Assert.assertEquals
import org.junit.Test

class QcGovernanceSnapshotTest {

    @Test
    fun `snapshot instantiates with valid metrics and period`() {
        val snapshot = QcGovernanceSnapshot(
            snapshotId = "SNP-01",
            projectId = "PRJ-01",
            period = QcAnalyticsPeriod.thisMonth(),
            kpiValues = mapOf("FIRST_PASS_RATE" to 94.5),
            totalAlertCount = 2,
            openCriticalAlertCount = 0,
            qualityEfficiencyScore = 92.0,
            generatedAt = "2026-08-17T08:00:00Z",
            generatedBy = "admin-01"
        )
        assertEquals("SNP-01", snapshot.snapshotId)
        assertEquals(92.0, snapshot.qualityEfficiencyScore, 0.001)
        assertEquals(2, snapshot.totalAlertCount)
    }
}
