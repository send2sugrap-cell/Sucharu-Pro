package com.sucharu.sucharupro.domain.validation

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.qc.governance.QcQualityReviewStatus
import org.junit.Assert.assertTrue
import org.junit.Test

class QcQualityReviewLifecycleTest {

    @Test
    fun `valid quality review lifecycle transitions succeed`() {
        assertTrue(QcGovernanceValidator.validateReviewTransition(QcQualityReviewStatus.DRAFT, QcQualityReviewStatus.SCHEDULED) is DomainResult.Success)
        assertTrue(QcGovernanceValidator.validateReviewTransition(QcQualityReviewStatus.SCHEDULED, QcQualityReviewStatus.IN_REVIEW) is DomainResult.Success)
        assertTrue(QcGovernanceValidator.validateReviewTransition(QcQualityReviewStatus.IN_REVIEW, QcQualityReviewStatus.COMPLETED) is DomainResult.Success)
    }

    @Test
    fun `terminal completed or cancelled reviews cannot transition`() {
        val completedResult = QcGovernanceValidator.validateReviewTransition(QcQualityReviewStatus.COMPLETED, QcQualityReviewStatus.IN_REVIEW)
        assertTrue(completedResult is DomainResult.Error)
        assertTrue((completedResult as DomainResult.Error).message.contains("terminal status", ignoreCase = true))

        val cancelledResult = QcGovernanceValidator.validateReviewTransition(QcQualityReviewStatus.CANCELLED, QcQualityReviewStatus.DRAFT)
        assertTrue(cancelledResult is DomainResult.Error)
    }
}
