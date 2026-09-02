package com.sucharu.sucharupro.ui.features.qc.analytics

import com.sucharu.sucharupro.domain.model.qc.analytics.QcAnalyticsPeriod
import com.sucharu.sucharupro.domain.model.qc.analytics.QcAnalyticsSummary
import com.sucharu.sucharupro.domain.model.qc.analytics.QcAnalyticsThresholdConfig
import com.sucharu.sucharupro.domain.model.qc.analytics.QcDefectAnalytics
import com.sucharu.sucharupro.domain.model.qc.analytics.QcJobAnalytics
import com.sucharu.sucharupro.domain.model.qc.analytics.QcOperationalInsight
import com.sucharu.sucharupro.domain.model.qc.analytics.QcPeriodType
import com.sucharu.sucharupro.domain.model.qc.analytics.QcStageAnalytics
import com.sucharu.sucharupro.domain.model.qc.analytics.QcTrendPoint

/**
 * Navigation tabs for the QC Analytics Dashboard.
 */
enum class QcAnalyticsTab(val label: String) {
    OVERVIEW("Overview"),
    JOBS("Job Analytics"),
    DEFECTS("Defect Categories"),
    STAGES("Production Stages"),
    INSIGHTS("Operational Insights")
}

/**
 * UI State for QC Analytics Dashboard.
 */
data class QcAnalyticsDashboardUiState(
    val isLoading: Boolean = false,
    val selectedTab: QcAnalyticsTab = QcAnalyticsTab.OVERVIEW,
    val selectedPeriodType: QcPeriodType = QcPeriodType.THIS_MONTH,
    val period: QcAnalyticsPeriod = QcAnalyticsPeriod.thisMonth(),
    val selectedProjectId: String? = null,
    val thresholdConfig: QcAnalyticsThresholdConfig = QcAnalyticsThresholdConfig.DEFAULT,
    val summary: QcAnalyticsSummary? = null,
    val jobAnalytics: List<QcJobAnalytics> = emptyList(),
    val defectAnalytics: List<QcDefectAnalytics> = emptyList(),
    val stageAnalytics: List<QcStageAnalytics> = emptyList(),
    val trends: List<QcTrendPoint> = emptyList(),
    val insights: List<QcOperationalInsight> = emptyList(),
    val errorMessage: String? = null
)
