package com.sucharu.sucharupro.data.datasource

import com.sucharu.sucharupro.domain.model.qc.analytics.QcAnalyticsPeriod
import com.sucharu.sucharupro.domain.model.qc.analytics.QcAnalyticsSummary
import com.sucharu.sucharupro.domain.model.qc.analytics.QcDefectAnalytics
import com.sucharu.sucharupro.domain.model.qc.analytics.QcJobAnalytics
import com.sucharu.sucharupro.domain.model.qc.analytics.QcOperationalInsight
import com.sucharu.sucharupro.domain.model.qc.analytics.QcStageAnalytics
import com.sucharu.sucharupro.domain.model.qc.analytics.QcTrendPoint
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Thread-safe in-memory implementation of QcAnalyticsDataSource.
 */
class FakeQcAnalyticsDataSource : QcAnalyticsDataSource {

    private val mutex = Mutex()

    private val summaryFlow = MutableStateFlow<QcAnalyticsSummary?>(null)
    private val jobAnalyticsFlow = MutableStateFlow<List<QcJobAnalytics>>(emptyList())
    private val defectAnalyticsFlow = MutableStateFlow<List<QcDefectAnalytics>>(emptyList())
    private val stageAnalyticsFlow = MutableStateFlow<List<QcStageAnalytics>>(emptyList())
    private val trendsFlow = MutableStateFlow<List<QcTrendPoint>>(emptyList())
    private val insightsFlow = MutableStateFlow<List<QcOperationalInsight>>(emptyList())

    override fun observeSummary(period: QcAnalyticsPeriod, projectId: String?): Flow<QcAnalyticsSummary?> {
        return summaryFlow.map { summary ->
            if (summary != null && (projectId == null || summary.projectId == projectId)) {
                summary
            } else {
                null
            }
        }
    }

    override fun observeJobAnalytics(projectId: String?): Flow<List<QcJobAnalytics>> {
        return jobAnalyticsFlow.map { list ->
            if (projectId != null) list.filter { it.projectId == projectId } else list
        }
    }

    override fun observeDefectAnalytics(projectId: String?): Flow<List<QcDefectAnalytics>> {
        return defectAnalyticsFlow
    }

    override fun observeStageAnalytics(projectId: String?): Flow<List<QcStageAnalytics>> {
        return stageAnalyticsFlow
    }

    override fun observeTrends(period: QcAnalyticsPeriod, projectId: String?): Flow<List<QcTrendPoint>> {
        return trendsFlow
    }

    override fun observeOperationalInsights(projectId: String?): Flow<List<QcOperationalInsight>> {
        return insightsFlow.map { list ->
            if (projectId != null) list.filter { it.projectId == null || it.projectId == projectId } else list
        }
    }

    override suspend fun saveSummaryCache(summary: QcAnalyticsSummary) {
        mutex.withLock {
            summaryFlow.value = summary
        }
    }

    override suspend fun saveJobAnalyticsCache(projectId: String?, list: List<QcJobAnalytics>) {
        mutex.withLock {
            jobAnalyticsFlow.value = list
        }
    }

    override suspend fun saveDefectAnalyticsCache(projectId: String?, list: List<QcDefectAnalytics>) {
        mutex.withLock {
            defectAnalyticsFlow.value = list
        }
    }

    override suspend fun saveStageAnalyticsCache(projectId: String?, list: List<QcStageAnalytics>) {
        mutex.withLock {
            stageAnalyticsFlow.value = list
        }
    }

    override suspend fun saveTrendsCache(period: QcAnalyticsPeriod, projectId: String?, list: List<QcTrendPoint>) {
        mutex.withLock {
            trendsFlow.value = list
        }
    }

    override suspend fun saveOperationalInsightsCache(projectId: String?, list: List<QcOperationalInsight>) {
        mutex.withLock {
            insightsFlow.value = list
        }
    }
}
