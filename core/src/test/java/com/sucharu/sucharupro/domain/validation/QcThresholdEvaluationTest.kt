package com.sucharu.sucharupro.domain.validation

import com.sucharu.sucharupro.domain.model.qc.governance.QcGovernanceKpi
import com.sucharu.sucharupro.domain.model.qc.governance.QcKpiTarget
import com.sucharu.sucharupro.domain.model.qc.governance.QcThresholdSeverity
import com.sucharu.sucharupro.domain.model.qc.governance.QcThresholdStatus
import org.junit.Assert.assertEquals
import org.junit.Test

class QcThresholdEvaluationTest {

    @Test
    fun `higher-is-better KPI evaluates within target, warning, and critical breach correctly`() {
        val target = QcKpiTarget(
            targetId = "TGT-1",
            projectId = "PRJ-1",
            kpiType = QcGovernanceKpi.FIRST_PASS_RATE,
            targetValue = 95.0,
            minimumAcceptableValue = 80.0,
            effectiveFrom = "2026-08-01T00:00:00Z",
            configuredBy = "admin-1",
            createdAt = "2026-08-01T00:00:00Z",
            updatedAt = "2026-08-01T00:00:00Z"
        )

        // 1. Within Target
        val passEval = QcGovernanceValidator.evaluateThreshold(QcGovernanceKpi.FIRST_PASS_RATE, 96.0, target)
        assertEquals(QcThresholdStatus.WITHIN_TARGET, passEval.status)
        assertEquals(QcThresholdSeverity.INFO, passEval.severity)

        // 2. Warning
        val warnEval = QcGovernanceValidator.evaluateThreshold(QcGovernanceKpi.FIRST_PASS_RATE, 90.0, target)
        assertEquals(QcThresholdStatus.WARNING, warnEval.status)
        assertEquals(QcThresholdSeverity.WARNING, warnEval.severity)

        // 3. Critical Breach
        val critEval = QcGovernanceValidator.evaluateThreshold(QcGovernanceKpi.FIRST_PASS_RATE, 75.0, target)
        assertEquals(QcThresholdStatus.CRITICAL_BREACH, critEval.status)
        assertEquals(QcThresholdSeverity.CRITICAL, critEval.severity)
    }

    @Test
    fun `lower-is-better KPI evaluates within target, warning, and critical breach correctly`() {
        val target = QcKpiTarget(
            targetId = "TGT-2",
            projectId = "PRJ-1",
            kpiType = QcGovernanceKpi.DEFECT_RATE,
            targetValue = 3.0,
            maximumAcceptableValue = 8.0,
            effectiveFrom = "2026-08-01T00:00:00Z",
            configuredBy = "admin-1",
            createdAt = "2026-08-01T00:00:00Z",
            updatedAt = "2026-08-01T00:00:00Z"
        )

        // 1. Within Target
        val passEval = QcGovernanceValidator.evaluateThreshold(QcGovernanceKpi.DEFECT_RATE, 2.5, target)
        assertEquals(QcThresholdStatus.WITHIN_TARGET, passEval.status)

        // 2. Warning
        val warnEval = QcGovernanceValidator.evaluateThreshold(QcGovernanceKpi.DEFECT_RATE, 5.0, target)
        assertEquals(QcThresholdStatus.WARNING, warnEval.status)

        // 3. Critical Breach
        val critEval = QcGovernanceValidator.evaluateThreshold(QcGovernanceKpi.DEFECT_RATE, 10.0, target)
        assertEquals(QcThresholdStatus.CRITICAL_BREACH, critEval.status)
        assertEquals(QcThresholdSeverity.CRITICAL, critEval.severity)
    }
}
