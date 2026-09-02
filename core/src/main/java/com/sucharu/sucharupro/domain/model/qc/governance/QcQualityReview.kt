package com.sucharu.sucharupro.domain.model.qc.governance

import com.sucharu.sucharupro.domain.model.qc.analytics.QcAnalyticsPeriod

/**
 * Formal Management/QC Quality Review record (Module 06 Step 10).
 */
data class QcQualityReview(
    val reviewId: String,
    val projectId: String,
    val title: String,
    val reviewPeriod: QcAnalyticsPeriod,
    val kpiSnapshot: Map<String, Double> = emptyMap(),
    val majorDefectCount: Int = 0,
    val recurringDefectCount: Int = 0,
    val costVariance: Double = 0.0,
    val timeVarianceMinutes: Long = 0L,
    val reworkCount: Int = 0,
    val reQcCycleCount: Int = 0,
    val finalQcPassRate: Double = 100.0,
    val openAlertIds: List<String> = emptyList(),
    val proposedActionIds: List<String> = emptyList(),
    val recommendations: String? = null,
    val reviewNotes: String? = null,
    val reviewerId: String,
    val reviewerName: String? = null,
    val status: QcQualityReviewStatus = QcQualityReviewStatus.DRAFT,
    val scheduledDate: String? = null,
    val completedAt: String? = null,
    val createdAt: String,
    val updatedAt: String
) {
    val isTerminal: Boolean get() = status.isTerminal

    init {
        require(reviewId.isNotBlank()) { "Review ID cannot be blank" }
        require(projectId.isNotBlank()) { "Project ID cannot be blank" }
        require(title.isNotBlank()) { "Review title cannot be blank" }
        require(reviewerId.isNotBlank()) { "Reviewer ID cannot be blank" }
        require(createdAt.isNotBlank()) { "CreatedAt timestamp cannot be blank" }
    }
}
