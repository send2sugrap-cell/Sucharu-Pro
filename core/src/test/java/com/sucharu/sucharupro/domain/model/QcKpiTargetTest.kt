package com.sucharu.sucharupro.domain.model

import com.sucharu.sucharupro.domain.model.qc.governance.QcGovernanceKpi
import com.sucharu.sucharupro.domain.model.qc.governance.QcKpiTarget
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class QcKpiTargetTest {

    @Test
    fun `valid target instantiates correctly`() {
        val target = QcKpiTarget(
            targetId = "TGT-01",
            projectId = "PRJ-01",
            kpiType = QcGovernanceKpi.FIRST_PASS_RATE,
            targetValue = 95.0,
            minimumAcceptableValue = 85.0,
            maximumAcceptableValue = 100.0,
            effectiveFrom = "2026-08-01T00:00:00Z",
            configuredBy = "admin-01",
            createdAt = "2026-08-01T00:00:00Z",
            updatedAt = "2026-08-01T00:00:00Z"
        )
        assertEquals("TGT-01", target.targetId)
        assertEquals(95.0, target.targetValue, 0.001)
        assertTrue(target.active)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `min greater than max throws exception`() {
        QcKpiTarget(
            targetId = "TGT-02",
            projectId = "PRJ-01",
            kpiType = QcGovernanceKpi.FIRST_PASS_RATE,
            targetValue = 95.0,
            minimumAcceptableValue = 99.0,
            maximumAcceptableValue = 85.0,
            effectiveFrom = "2026-08-01T00:00:00Z",
            configuredBy = "admin-01",
            createdAt = "2026-08-01T00:00:00Z",
            updatedAt = "2026-08-01T00:00:00Z"
        )
    }
}
