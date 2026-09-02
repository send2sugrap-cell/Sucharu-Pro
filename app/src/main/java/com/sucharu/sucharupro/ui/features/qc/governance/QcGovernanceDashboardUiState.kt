package com.sucharu.sucharupro.ui.features.qc.governance

import com.sucharu.sucharupro.domain.model.qc.analytics.QcAnalyticsPeriod
import com.sucharu.sucharupro.domain.model.qc.governance.QcGovernanceActivityEvent
import com.sucharu.sucharupro.domain.model.qc.governance.QcGovernanceSnapshot
import com.sucharu.sucharupro.domain.model.qc.governance.QcImprovementAction
import com.sucharu.sucharupro.domain.model.qc.governance.QcKpiTarget
import com.sucharu.sucharupro.domain.model.qc.governance.QcKpiThreshold
import com.sucharu.sucharupro.domain.model.qc.governance.QcQualityAlert
import com.sucharu.sucharupro.domain.model.qc.governance.QcQualityReview

/**
 * UI State model for QC Governance Dashboard (Module 06 Step 10).
 */
data class QcGovernanceDashboardUiState(
    val isLoading: Boolean = false,
    val selectedPeriod: QcAnalyticsPeriod = QcAnalyticsPeriod.thisMonth(),
    val selectedProjectId: String = "",
    val overallQualityScore: Double = 100.0,
    val kpiEvaluations: List<QcKpiThreshold> = emptyList(),
    val activeTargets: List<QcKpiTarget> = emptyList(),
    val alerts: List<QcQualityAlert> = emptyList(),
    val activeAlerts: List<QcQualityAlert> = emptyList(),
    val criticalAlertCount: Int = 0,
    val warningAlertCount: Int = 0,
    val improvementActions: List<QcImprovementAction> = emptyList(),
    val openActionCount: Int = 0,
    val overdueActionCount: Int = 0,
    val reviews: List<QcQualityReview> = emptyList(),
    val snapshots: List<QcGovernanceSnapshot> = emptyList(),
    val recentActivity: List<QcGovernanceActivityEvent> = emptyList(),
    val errorMessage: String? = null
)
