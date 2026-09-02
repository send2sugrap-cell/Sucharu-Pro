package com.sucharu.sucharupro.domain.repository

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.qc.analytics.QcAnalyticsMetricType
import com.sucharu.sucharupro.domain.model.qc.analytics.QcAnalyticsPeriod
import com.sucharu.sucharupro.domain.model.qc.analytics.QcAnalyticsSummary
import com.sucharu.sucharupro.domain.model.qc.analytics.QcAnalyticsThresholdConfig
import com.sucharu.sucharupro.domain.model.qc.analytics.QcDefectAnalytics
import com.sucharu.sucharupro.domain.model.qc.analytics.QcJobAnalytics
import com.sucharu.sucharupro.domain.model.qc.analytics.QcOperationalInsight
import com.sucharu.sucharupro.domain.model.qc.analytics.QcStageAnalytics
import com.sucharu.sucharupro.domain.model.qc.analytics.QcTrendPoint
import com.sucharu.sucharupro.domain.model.user.UserRole
import kotlinx.coroutines.flow.Flow

/**
 * Repository contract for QC Analytics and Operational Insights (Module 06 Step 09).
 */
interface QcAnalyticsRepository {

    suspend fun getSummary(
        period: QcAnalyticsPeriod = QcAnalyticsPeriod.thisMonth(),
        projectId: String? = null,
        callerRole: UserRole? = UserRole.ADMIN
    ): DomainResult<QcAnalyticsSummary>

    suspend fun getJobAnalytics(
        period: QcAnalyticsPeriod = QcAnalyticsPeriod.thisMonth(),
        projectId: String? = null,
        callerRole: UserRole? = UserRole.ADMIN
    ): DomainResult<List<QcJobAnalytics>>

    suspend fun getDefectAnalytics(
        period: QcAnalyticsPeriod = QcAnalyticsPeriod.thisMonth(),
        projectId: String? = null,
        callerRole: UserRole? = UserRole.ADMIN
    ): DomainResult<List<QcDefectAnalytics>>

    suspend fun getStageAnalytics(
        period: QcAnalyticsPeriod = QcAnalyticsPeriod.thisMonth(),
        projectId: String? = null,
        callerRole: UserRole? = UserRole.ADMIN
    ): DomainResult<List<QcStageAnalytics>>

    suspend fun getTrends(
        period: QcAnalyticsPeriod = QcAnalyticsPeriod.thisMonth(),
        projectId: String? = null,
        callerRole: UserRole? = UserRole.ADMIN
    ): DomainResult<List<QcTrendPoint>>

    suspend fun getOperationalInsights(
        period: QcAnalyticsPeriod = QcAnalyticsPeriod.thisMonth(),
        projectId: String? = null,
        thresholdConfig: QcAnalyticsThresholdConfig = QcAnalyticsThresholdConfig.DEFAULT,
        callerRole: UserRole? = UserRole.ADMIN
    ): DomainResult<List<QcOperationalInsight>>

    suspend fun getMetric(
        metricType: QcAnalyticsMetricType,
        period: QcAnalyticsPeriod = QcAnalyticsPeriod.thisMonth(),
        projectId: String? = null,
        callerRole: UserRole? = UserRole.ADMIN
    ): DomainResult<Double>

    fun observeSummary(
        period: QcAnalyticsPeriod = QcAnalyticsPeriod.thisMonth(),
        projectId: String? = null,
        callerRole: UserRole? = UserRole.ADMIN
    ): Flow<DomainResult<QcAnalyticsSummary>>

    fun observeJobAnalytics(
        period: QcAnalyticsPeriod = QcAnalyticsPeriod.thisMonth(),
        projectId: String? = null,
        callerRole: UserRole? = UserRole.ADMIN
    ): Flow<DomainResult<List<QcJobAnalytics>>>

    fun observeDefectAnalytics(
        period: QcAnalyticsPeriod = QcAnalyticsPeriod.thisMonth(),
        projectId: String? = null,
        callerRole: UserRole? = UserRole.ADMIN
    ): Flow<DomainResult<List<QcDefectAnalytics>>>

    fun observeStageAnalytics(
        period: QcAnalyticsPeriod = QcAnalyticsPeriod.thisMonth(),
        projectId: String? = null,
        callerRole: UserRole? = UserRole.ADMIN
    ): Flow<DomainResult<List<QcStageAnalytics>>>

    fun observeTrends(
        period: QcAnalyticsPeriod = QcAnalyticsPeriod.thisMonth(),
        projectId: String? = null,
        callerRole: UserRole? = UserRole.ADMIN
    ): Flow<DomainResult<List<QcTrendPoint>>>

    fun observeInsights(
        period: QcAnalyticsPeriod = QcAnalyticsPeriod.thisMonth(),
        projectId: String? = null,
        thresholdConfig: QcAnalyticsThresholdConfig = QcAnalyticsThresholdConfig.DEFAULT,
        callerRole: UserRole? = UserRole.ADMIN
    ): Flow<DomainResult<List<QcOperationalInsight>>>
}
