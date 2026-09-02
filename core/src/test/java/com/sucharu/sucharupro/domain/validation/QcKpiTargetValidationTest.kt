package com.sucharu.sucharupro.domain.validation

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.qc.governance.QcGovernanceKpi
import com.sucharu.sucharupro.domain.model.qc.governance.QcKpiTarget
import org.junit.Assert.assertTrue
import org.junit.Test

class QcKpiTargetValidationTest {

    @Test
    fun `valid target passes validation`() {
        val target = QcKpiTarget(
            targetId = "TGT-01",
            projectId = "PRJ-01",
            kpiType = QcGovernanceKpi.DEFECT_RATE,
            targetValue = 3.0,
            minimumAcceptableValue = 0.0,
            maximumAcceptableValue = 5.0,
            effectiveFrom = "2026-08-01T00:00:00Z",
            configuredBy = "admin-01",
            createdAt = "2026-08-01T00:00:00Z",
            updatedAt = "2026-08-01T00:00:00Z"
        )
        val result = QcGovernanceValidator.validateKpiTarget(target)
        assertTrue(result is DomainResult.Success)
    }

    @Test
    fun `percentage target over 100 fails validation`() {
        val target = QcKpiTarget(
            targetId = "TGT-02",
            projectId = "PRJ-01",
            kpiType = QcGovernanceKpi.FIRST_PASS_RATE,
            targetValue = 120.0,
            effectiveFrom = "2026-08-01T00:00:00Z",
            configuredBy = "admin-01",
            createdAt = "2026-08-01T00:00:00Z",
            updatedAt = "2026-08-01T00:00:00Z"
        )
        val result = QcGovernanceValidator.validateKpiTarget(target)
        assertTrue(result is DomainResult.Error)
        assertTrue((result as DomainResult.Error).message.contains("cannot exceed 100%", ignoreCase = true))
    }

    @Test
    fun `effectiveTo before effectiveFrom fails validation`() {
        val target = QcKpiTarget(
            targetId = "TGT-03",
            projectId = "PRJ-01",
            kpiType = QcGovernanceKpi.REWORK_RATE,
            targetValue = 5.0,
            effectiveFrom = "2026-08-10T00:00:00Z",
            effectiveTo = "2026-08-05T00:00:00Z",
            configuredBy = "admin-01",
            createdAt = "2026-08-01T00:00:00Z",
            updatedAt = "2026-08-01T00:00:00Z"
        )
        val result = QcGovernanceValidator.validateKpiTarget(target)
        assertTrue(result is DomainResult.Error)
        assertTrue((result as DomainResult.Error).message.contains("cannot precede", ignoreCase = true))
    }
}
