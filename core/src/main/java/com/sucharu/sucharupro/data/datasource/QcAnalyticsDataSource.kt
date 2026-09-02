package com.sucharu.sucharupro.data.datasource

import com.sucharu.sucharupro.domain.model.qc.analytics.QcAnalyticsPeriod
import com.sucharu.sucharupro.domain.model.qc.analytics.QcAnalyticsSummary
import com.sucharu.sucharupro.domain.model.qc.analytics.QcDefectAnalytics
import com.sucharu.sucharupro.domain.model.qc.analytics.QcJobAnalytics
import com.sucharu.sucharupro.domain.model.qc.analytics.QcOperationalInsight
import com.sucharu.sucharupro.domain.model.qc.analytics.QcStageAnalytics
import com.sucharu.sucharupro.domain.model.qc.analytics.QcTrendPoint
import kotlinx.coroutines.flow.Flow

/**
 * Data Source abstraction for QC analytics projections and reactive updates.
 */
interface QcAnalyticsDataSource {
    fun observeSummary(period: QcAnalyticsPeriod, projectId: String? = null): Flow<QcAnalyticsSummary?>
    fun observeJobAnalytics(projectId: String? = null): Flow<List<QcJobAnalytics>>
    fun observeDefectAnalytics(projectId: String? = null): Flow<List<QcDefectAnalytics>>
    fun observeStageAnalytics(projectId: String? = null): Flow<List<QcStageAnalytics>>
    fun observeTrends(period: QcAnalyticsPeriod, projectId: String? = null): Flow<List<QcTrendPoint>>
    fun observeOperationalInsights(projectId: String? = null): Flow<List<QcOperationalInsight>>

    suspend fun saveSummaryCache(summary: QcAnalyticsSummary)
    suspend fun saveJobAnalyticsCache(projectId: String?, list: List<QcJobAnalytics>)
    suspend fun saveDefectAnalyticsCache(projectId: String?, list: List<QcDefectAnalytics>)
    suspend fun saveStageAnalyticsCache(projectId: String?, list: List<QcStageAnalytics>)
    suspend fun saveTrendsCache(period: QcAnalyticsPeriod, projectId: String?, list: List<QcTrendPoint>)
    suspend fun saveOperationalInsightsCache(projectId: String?, list: List<QcOperationalInsight>)
}
