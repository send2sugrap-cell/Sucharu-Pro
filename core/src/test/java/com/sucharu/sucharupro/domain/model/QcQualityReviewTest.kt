package com.sucharu.sucharupro.domain.model

import com.sucharu.sucharupro.domain.model.qc.analytics.QcAnalyticsPeriod
import com.sucharu.sucharupro.domain.model.qc.governance.QcQualityReview
import com.sucharu.sucharupro.domain.model.qc.governance.QcQualityReviewStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class QcQualityReviewTest {

    @Test
    fun `quality review model instantiates with draft status and tracks terminal completion`() {
        val review = QcQualityReview(
            reviewId = "REV-01",
            projectId = "PRJ-01",
            title = "Monthly Quality Review",
            reviewPeriod = QcAnalyticsPeriod.thisMonth(),
            reviewerId = "mgr-01",
            createdAt = "2026-08-17T08:00:00Z",
            updatedAt = "2026-08-17T08:00:00Z"
        )
        assertEquals(QcQualityReviewStatus.DRAFT, review.status)
        assertFalse(review.isTerminal)

        val completed = review.copy(status = QcQualityReviewStatus.COMPLETED)
        assertTrue(completed.isTerminal)
    }
}
