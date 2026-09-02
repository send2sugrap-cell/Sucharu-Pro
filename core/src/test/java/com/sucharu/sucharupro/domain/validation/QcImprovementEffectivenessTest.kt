package com.sucharu.sucharupro.domain.validation

import com.sucharu.sucharupro.domain.model.qc.governance.QcGovernanceKpi
import com.sucharu.sucharupro.domain.model.qc.governance.QcImprovementEffectiveness
import org.junit.Assert.assertEquals
import org.junit.Test

class QcImprovementEffectivenessTest {

    @Test
    fun `evaluates effectiveness deterministically for higher-is-better KPI`() {
        // Baseline 80%, Target 95%
        // Case 1: Post 97% -> Highly Effective (exceeded target)
        assertEquals(
            QcImprovementEffectiveness.HIGHLY_EFFECTIVE,
            QcGovernanceValidator.evaluateEffectiveness(QcGovernanceKpi.FIRST_PASS_RATE, 80.0, 97.0, 95.0)
        )
        // Case 2: Post 95% -> Effective (met target)
        assertEquals(
            QcImprovementEffectiveness.EFFECTIVE,
            QcGovernanceValidator.evaluateEffectiveness(QcGovernanceKpi.FIRST_PASS_RATE, 80.0, 95.0, 95.0)
        )
        // Case 3: Post 88% -> Partially Effective (improved but below target)
        assertEquals(
            QcImprovementEffectiveness.PARTIALLY_EFFECTIVE,
            QcGovernanceValidator.evaluateEffectiveness(QcGovernanceKpi.FIRST_PASS_RATE, 80.0, 88.0, 95.0)
        )
        // Case 4: Post 78% -> Ineffective (declined or no improvement)
        assertEquals(
            QcImprovementEffectiveness.INEFFECTIVE,
            QcGovernanceValidator.evaluateEffectiveness(QcGovernanceKpi.FIRST_PASS_RATE, 80.0, 78.0, 95.0)
        )
    }

    @Test
    fun `evaluates effectiveness deterministically for lower-is-better KPI`() {
        // Baseline 10%, Target 3%
        // Case 1: Post 1% -> Highly Effective (exceeded target reduction)
        assertEquals(
            QcImprovementEffectiveness.HIGHLY_EFFECTIVE,
            QcGovernanceValidator.evaluateEffectiveness(QcGovernanceKpi.DEFECT_RATE, 10.0, 1.0, 3.0)
        )
        // Case 2: Post 3% -> Effective (met target)
        assertEquals(
            QcImprovementEffectiveness.EFFECTIVE,
            QcGovernanceValidator.evaluateEffectiveness(QcGovernanceKpi.DEFECT_RATE, 10.0, 3.0, 3.0)
        )
        // Case 3: Post 6% -> Partially Effective (reduced but above target)
        assertEquals(
            QcImprovementEffectiveness.PARTIALLY_EFFECTIVE,
            QcGovernanceValidator.evaluateEffectiveness(QcGovernanceKpi.DEFECT_RATE, 10.0, 6.0, 3.0)
        )
        // Case 4: Post 12% -> Ineffective (worse)
        assertEquals(
            QcImprovementEffectiveness.INEFFECTIVE,
            QcGovernanceValidator.evaluateEffectiveness(QcGovernanceKpi.DEFECT_RATE, 10.0, 12.0, 3.0)
        )
    }
}
