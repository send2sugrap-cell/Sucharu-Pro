package com.sucharu.sucharupro.ui.features.returns.analytics

import com.sucharu.sucharupro.domain.model.returns.ReturnAnalyticsPeriod
import com.sucharu.sucharupro.domain.model.returns.ReturnAnalyticsSummary
import com.sucharu.sucharupro.domain.model.returns.ReturnAnalyticsTrendPoint
import com.sucharu.sucharupro.domain.model.returns.ReturnDefectBreakdown
import com.sucharu.sucharupro.domain.model.returns.ReturnFinancialBreakdown

/**
 * UI State for Return Analytics Dashboard (Module 11 Step 06).
 */
data class ReturnAnalyticsUiState(
    val isLoading: Boolean = false,
    val summary: ReturnAnalyticsSummary? = null,
    val defectBreakdown: List<ReturnDefectBreakdown> = emptyList(),
    val financialBreakdown: List<ReturnFinancialBreakdown> = emptyList(),
    val trends: List<ReturnAnalyticsTrendPoint> = emptyList(),
    val selectedPeriod: ReturnAnalyticsPeriod = ReturnAnalyticsPeriod.THIS_MONTH,
    val totalDispatchedCount: Int? = null,
    val errorMessage: String? = null,
    val projectId: String = ""
)
