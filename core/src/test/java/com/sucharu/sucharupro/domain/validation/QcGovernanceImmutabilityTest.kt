package com.sucharu.sucharupro.domain.validation

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.qc.governance.QcAlertStatus
import com.sucharu.sucharupro.domain.model.qc.governance.QcImprovementActionStatus
import com.sucharu.sucharupro.domain.model.qc.governance.QcQualityReviewStatus
import org.junit.Assert.assertTrue
import org.junit.Test

class QcGovernanceImmutabilityTest {

    @Test
    fun `terminal records are protected from any further status mutations`() {
        // 1. Alert resolved/dismissed
        val alertRes = QcGovernanceValidator.validateAlertTransition(QcAlertStatus.RESOLVED, QcAlertStatus.DETECTED)
        assertTrue(alertRes is DomainResult.Error)

        // 2. Review completed/cancelled
        val reviewRes = QcGovernanceValidator.validateReviewTransition(QcQualityReviewStatus.COMPLETED, QcQualityReviewStatus.DRAFT)
        assertTrue(reviewRes is DomainResult.Error)

        // 3. Action verified/rejected/cancelled
        val actionRes = QcGovernanceValidator.validateActionTransition(QcImprovementActionStatus.VERIFIED, QcImprovementActionStatus.IN_PROGRESS)
        assertTrue(actionRes is DomainResult.Error)
    }
}
