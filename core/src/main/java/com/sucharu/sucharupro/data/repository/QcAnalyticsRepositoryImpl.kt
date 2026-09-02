package com.sucharu.sucharupro.data.repository

import com.sucharu.sucharupro.data.datasource.FinalQcDataSource
import com.sucharu.sucharupro.data.datasource.ProductionDefectDataSource
import com.sucharu.sucharupro.data.datasource.ProductionJobDataSource
import com.sucharu.sucharupro.data.datasource.ProductionQcDataSource
import com.sucharu.sucharupro.data.datasource.ProductionReQcDataSource
import com.sucharu.sucharupro.data.datasource.ProductionReworkDataSource
import com.sucharu.sucharupro.data.datasource.QcAnalyticsDataSource
import com.sucharu.sucharupro.data.datasource.QcCostTimeDataSource
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.job.ProductionJob
import com.sucharu.sucharupro.domain.model.production.ProductionStageType
import com.sucharu.sucharupro.domain.model.qc.DefectCategory
import com.sucharu.sucharupro.domain.model.qc.DefectStatus
import com.sucharu.sucharupro.domain.model.qc.FinalQcInspection
import com.sucharu.sucharupro.domain.model.qc.FinalQcStatus
import com.sucharu.sucharupro.domain.model.qc.ProductionDefect
import com.sucharu.sucharupro.domain.model.qc.ProductionRework
import com.sucharu.sucharupro.domain.model.qc.QcCostEntry
import com.sucharu.sucharupro.domain.model.qc.QcCostStatus
import com.sucharu.sucharupro.domain.model.qc.QcTimeEntry
import com.sucharu.sucharupro.domain.model.qc.QcTimeStatus
import com.sucharu.sucharupro.domain.model.qc.ReQcInspection
import com.sucharu.sucharupro.domain.model.qc.ReQcStatus
import com.sucharu.sucharupro.domain.model.qc.ReworkStatus
import com.sucharu.sucharupro.domain.model.qc.analytics.QcAnalyticsMetricType
import com.sucharu.sucharupro.domain.model.qc.analytics.QcAnalyticsPeriod
import com.sucharu.sucharupro.domain.model.qc.analytics.QcAnalyticsSummary
import com.sucharu.sucharupro.domain.model.qc.analytics.QcAnalyticsThresholdConfig
import com.sucharu.sucharupro.domain.model.qc.analytics.QcDefectAnalytics
import com.sucharu.sucharupro.domain.model.qc.analytics.QcInsightSeverity
import com.sucharu.sucharupro.domain.model.qc.analytics.QcInsightType
import com.sucharu.sucharupro.domain.model.qc.analytics.QcJobAnalytics
import com.sucharu.sucharupro.domain.model.qc.analytics.QcOperationalInsight
import com.sucharu.sucharupro.domain.model.qc.analytics.QcStageAnalytics
import com.sucharu.sucharupro.domain.model.qc.analytics.QcTrendPoint
import com.sucharu.sucharupro.domain.model.user.UserRole
import com.sucharu.sucharupro.domain.repository.QcAnalyticsRepository
import com.sucharu.sucharupro.domain.validation.QcAnalyticsValidator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID

/**
 * Thread-safe implementation of QcAnalyticsRepository (Module 06 Step 09).
 */
class QcAnalyticsRepositoryImpl(
    private val analyticsDataSource: QcAnalyticsDataSource,
    private val productionJobDataSource: ProductionJobDataSource,
    private val qcDataSource: ProductionQcDataSource? = null,
    private val defectDataSource: ProductionDefectDataSource? = null,
    private val reworkDataSource: ProductionReworkDataSource? = null,
    private val reQcDataSource: ProductionReQcDataSource? = null,
    private val finalQcDataSource: FinalQcDataSource? = null,
    private val qcCostTimeDataSource: QcCostTimeDataSource? = null
) : QcAnalyticsRepository {

    private val mutex = Mutex()

    override suspend fun getSummary(
        period: QcAnalyticsPeriod,
        projectId: String?,
        callerRole: UserRole?
    ): DomainResult<QcAnalyticsSummary> = mutex.withLock {
        calculateSummaryInternal(period, projectId, callerRole)
    }

    override suspend fun getJobAnalytics(
        period: QcAnalyticsPeriod,
        projectId: String?,
        callerRole: UserRole?
    ): DomainResult<List<QcJobAnalytics>> = mutex.withLock {
        calculateJobAnalyticsListInternal(period, projectId, callerRole)
    }

    override suspend fun getDefectAnalytics(
        period: QcAnalyticsPeriod,
        projectId: String?,
        callerRole: UserRole?
    ): DomainResult<List<QcDefectAnalytics>> = mutex.withLock {
        calculateDefectAnalyticsListInternal(period, projectId, callerRole)
    }

    override suspend fun getStageAnalytics(
        period: QcAnalyticsPeriod,
        projectId: String?,
        callerRole: UserRole?
    ): DomainResult<List<QcStageAnalytics>> = mutex.withLock {
        calculateStageAnalyticsListInternal(period, projectId, callerRole)
    }

    override suspend fun getTrends(
        period: QcAnalyticsPeriod,
        projectId: String?,
        callerRole: UserRole?
    ): DomainResult<List<QcTrendPoint>> = mutex.withLock {
        calculateTrendsListInternal(period, projectId, callerRole)
    }

    override suspend fun getOperationalInsights(
        period: QcAnalyticsPeriod,
        projectId: String?,
        thresholdConfig: QcAnalyticsThresholdConfig,
        callerRole: UserRole?
    ): DomainResult<List<QcOperationalInsight>> = mutex.withLock {
        val rbac = QcAnalyticsValidator.validateRbac(callerRole)
        if (rbac is DomainResult.Error) return rbac

        val configVal = QcAnalyticsValidator.validateThresholdConfig(thresholdConfig)
        if (configVal is DomainResult.Error) return configVal

        val summaryRes = calculateSummaryInternal(period, projectId, callerRole)
        if (summaryRes is DomainResult.Error) return summaryRes
        val summary = (summaryRes as DomainResult.Success).data

        val jobAnalyticsRes = calculateJobAnalyticsListInternal(period, projectId, callerRole)
        if (jobAnalyticsRes is DomainResult.Error) return jobAnalyticsRes
        val jobAnalyticsList = (jobAnalyticsRes as DomainResult.Success).data

        val stageAnalyticsRes = calculateStageAnalyticsListInternal(period, projectId, callerRole)
        val stageList = if (stageAnalyticsRes is DomainResult.Success) stageAnalyticsRes.data else emptyList()

        val insights = mutableListOf<QcOperationalInsight>()
        val now = Instant.now().toString()

        // 1. Cost Overrun Rule
        if (summary.totalCostVariance > thresholdConfig.maxAcceptableCostVariance) {
            val isCritical = summary.totalCostVariance > (thresholdConfig.maxAcceptableCostVariance * 2)
            insights.add(
                QcOperationalInsight(
                    id = "INSIGHT-${UUID.randomUUID().toString().take(8)}",
                    projectId = projectId,
                    severity = if (isCritical) QcInsightSeverity.CRITICAL else QcInsightSeverity.WARNING,
                    type = QcInsightType.COST_OVERRUN,
                    title = "QC Cost Overrun Detected",
                    description = "Actual QC operational cost exceeded planned budget by ${String.format("%.2f", summary.totalCostVariance)} BDT (Threshold: ${thresholdConfig.maxAcceptableCostVariance} BDT).",
                    metricValue = summary.totalCostVariance,
                    thresholdValue = thresholdConfig.maxAcceptableCostVariance,
                    generatedAt = now
                )
            )
        }

        // 2. Time Overrun Rule
        if (summary.totalTimeVariance > thresholdConfig.maxAcceptableTimeVarianceMinutes) {
            val isCritical = summary.totalTimeVariance > (thresholdConfig.maxAcceptableTimeVarianceMinutes * 2)
            insights.add(
                QcOperationalInsight(
                    id = "INSIGHT-${UUID.randomUUID().toString().take(8)}",
                    projectId = projectId,
                    severity = if (isCritical) QcInsightSeverity.CRITICAL else QcInsightSeverity.WARNING,
                    type = QcInsightType.TIME_OVERRUN,
                    title = "QC Time Overrun Detected",
                    description = "Actual QC inspection time exceeded planned duration by ${summary.totalTimeVariance} minutes (Threshold: ${thresholdConfig.maxAcceptableTimeVarianceMinutes} mins).",
                    metricValue = summary.totalTimeVariance.toDouble(),
                    thresholdValue = thresholdConfig.maxAcceptableTimeVarianceMinutes.toDouble(),
                    generatedAt = now
                )
            )
        }

        // 3. High Defect Rate Rule
        val defectRate = if (summary.totalJobs > 0) (summary.totalDefects.toDouble() / summary.totalJobs) * 100.0 else 0.0
        if (defectRate > thresholdConfig.maxAcceptableDefectRate) {
            val isCritical = defectRate > (thresholdConfig.maxAcceptableDefectRate * 2)
            insights.add(
                QcOperationalInsight(
                    id = "INSIGHT-${UUID.randomUUID().toString().take(8)}",
                    projectId = projectId,
                    severity = if (isCritical) QcInsightSeverity.CRITICAL else QcInsightSeverity.WARNING,
                    type = QcInsightType.HIGH_DEFECT_RATE,
                    title = "High Defect Rate Identified",
                    description = "Defect rate of ${String.format("%.1f", defectRate)}% exceeds acceptable benchmark of ${thresholdConfig.maxAcceptableDefectRate}%.",
                    metricValue = defectRate,
                    thresholdValue = thresholdConfig.maxAcceptableDefectRate,
                    generatedAt = now
                )
            )
        }

        // 4. High Rework Rate Rule
        if (summary.reworkRate > thresholdConfig.maxAcceptableReworkRate) {
            insights.add(
                QcOperationalInsight(
                    id = "INSIGHT-${UUID.randomUUID().toString().take(8)}",
                    projectId = projectId,
                    severity = QcInsightSeverity.WARNING,
                    type = QcInsightType.HIGH_REWORK_RATE,
                    title = "Elevated Rework Rate",
                    description = "Rework rate reached ${String.format("%.1f", summary.reworkRate)}% (Threshold: ${thresholdConfig.maxAcceptableReworkRate}%).",
                    metricValue = summary.reworkRate,
                    thresholdValue = thresholdConfig.maxAcceptableReworkRate,
                    generatedAt = now
                )
            )
        }

        // 5. Repeated Failure Loop Rule
        val repeatedJobs = jobAnalyticsList.filter { it.reQcCycleCount >= thresholdConfig.repeatedFailureCycleThreshold }
        for (job in repeatedJobs) {
            insights.add(
                QcOperationalInsight(
                    id = "INSIGHT-${UUID.randomUUID().toString().take(8)}",
                    productionJobId = job.productionJobId,
                    projectId = job.projectId,
                    severity = QcInsightSeverity.CRITICAL,
                    type = QcInsightType.REPEATED_FAILURE,
                    title = "Repeated QC Failure on Job ${job.productionJobId}",
                    description = "Job ${job.productionJobId} experienced ${job.reQcCycleCount} Re-QC failure cycles (Threshold: ${thresholdConfig.repeatedFailureCycleThreshold}).",
                    metricValue = job.reQcCycleCount.toDouble(),
                    thresholdValue = thresholdConfig.repeatedFailureCycleThreshold.toDouble(),
                    generatedAt = now
                )
            )
        }

        // 6. Stage Quality Risk Rule
        for (stage in stageList) {
            if (stage.defectCount > 0 && stage.defectRate > 25.0) {
                insights.add(
                    QcOperationalInsight(
                        id = "INSIGHT-${UUID.randomUUID().toString().take(8)}",
                        projectId = projectId,
                        severity = QcInsightSeverity.WARNING,
                        type = QcInsightType.STAGE_QUALITY_RISK,
                        title = "Stage Quality Risk: ${stage.productionStage.defaultLabel}",
                        description = "Stage ${stage.productionStage.defaultLabel} has a ${String.format("%.1f", stage.defectRate)}% defect rate with ${stage.defectCount} recorded defects.",
                        metricValue = stage.defectRate,
                        thresholdValue = 25.0,
                        generatedAt = now
                    )
                )
            }
        }

        // 7. Improvement Opportunity (when clean pass)
        if (summary.totalJobs > 0 && summary.firstPassQcRate == 100.0 && summary.totalDefects == 0) {
            insights.add(
                QcOperationalInsight(
                    id = "INSIGHT-${UUID.randomUUID().toString().take(8)}",
                    projectId = projectId,
                    severity = QcInsightSeverity.INFO,
                    type = QcInsightType.IMPROVEMENT_OPPORTUNITY,
                    title = "Optimal Quality Performance",
                    description = "100% First-Pass QC rate achieved across all ${summary.totalJobs} jobs in this period with zero defects.",
                    metricValue = 100.0,
                    thresholdValue = 100.0,
                    generatedAt = now
                )
            )
        }

        val sortedInsights = insights.sortedWith(
            compareBy<QcOperationalInsight> {
                when (it.severity) {
                    QcInsightSeverity.CRITICAL -> 0
                    QcInsightSeverity.WARNING -> 1
                    QcInsightSeverity.INFO -> 2
                }
            }.thenBy { it.id }
        )

        analyticsDataSource.saveOperationalInsightsCache(projectId, sortedInsights)
        DomainResult.Success(sortedInsights)
    }

    override suspend fun getMetric(
        metricType: QcAnalyticsMetricType,
        period: QcAnalyticsPeriod,
        projectId: String?,
        callerRole: UserRole?
    ): DomainResult<Double> = mutex.withLock {
        val summaryRes = calculateSummaryInternal(period, projectId, callerRole)
        if (summaryRes is DomainResult.Error) return summaryRes
        val summary = (summaryRes as DomainResult.Success).data

        val value = when (metricType) {
            QcAnalyticsMetricType.TOTAL_QC_COST -> summary.totalQcCost
            QcAnalyticsMetricType.TOTAL_QC_TIME -> summary.totalQcTimeMinutes.toDouble()
            QcAnalyticsMetricType.AVERAGE_QC_COST_PER_JOB -> summary.averageQcCostPerJob
            QcAnalyticsMetricType.AVERAGE_QC_TIME_PER_JOB -> summary.averageQcTimeMinutesPerJob
            QcAnalyticsMetricType.TOTAL_DEFECTS -> summary.totalDefects.toDouble()
            QcAnalyticsMetricType.AVERAGE_DEFECTS_PER_JOB -> summary.averageDefectsPerJob
            QcAnalyticsMetricType.TOTAL_REWORKS -> summary.totalReworks.toDouble()
            QcAnalyticsMetricType.TOTAL_RE_QC_CYCLES -> summary.totalReQcCycles.toDouble()
            QcAnalyticsMetricType.FIRST_PASS_QC_RATE -> summary.firstPassQcRate
            QcAnalyticsMetricType.REWORK_RATE -> summary.reworkRate
            QcAnalyticsMetricType.RE_QC_RATE -> summary.reQcRate
            QcAnalyticsMetricType.FINAL_QC_PASS_RATE -> summary.finalQcPassRate
            QcAnalyticsMetricType.COST_VARIANCE -> summary.totalCostVariance
            QcAnalyticsMetricType.TIME_VARIANCE -> summary.totalTimeVariance.toDouble()
            QcAnalyticsMetricType.OPEN_DEFECT_COUNT -> summary.openDefectCount.toDouble()
            QcAnalyticsMetricType.ACTIVE_REWORK_COUNT -> summary.activeReworkCount.toDouble()
            QcAnalyticsMetricType.FAILED_RE_QC_COUNT -> summary.failedReQcCount.toDouble()
            QcAnalyticsMetricType.RELEASED_JOB_COUNT -> summary.releasedJobCount.toDouble()
        }
        DomainResult.Success(value)
    }

    override fun observeSummary(
        period: QcAnalyticsPeriod,
        projectId: String?,
        callerRole: UserRole?
    ): Flow<DomainResult<QcAnalyticsSummary>> = flow {
        emit(getSummary(period, projectId, callerRole))
    }

    override fun observeJobAnalytics(
        period: QcAnalyticsPeriod,
        projectId: String?,
        callerRole: UserRole?
    ): Flow<DomainResult<List<QcJobAnalytics>>> = flow {
        emit(getJobAnalytics(period, projectId, callerRole))
    }

    override fun observeDefectAnalytics(
        period: QcAnalyticsPeriod,
        projectId: String?,
        callerRole: UserRole?
    ): Flow<DomainResult<List<QcDefectAnalytics>>> = flow {
        emit(getDefectAnalytics(period, projectId, callerRole))
    }

    override fun observeStageAnalytics(
        period: QcAnalyticsPeriod,
        projectId: String?,
        callerRole: UserRole?
    ): Flow<DomainResult<List<QcStageAnalytics>>> = flow {
        emit(getStageAnalytics(period, projectId, callerRole))
    }

    override fun observeTrends(
        period: QcAnalyticsPeriod,
        projectId: String?,
        callerRole: UserRole?
    ): Flow<DomainResult<List<QcTrendPoint>>> = flow {
        emit(getTrends(period, projectId, callerRole))
    }

    override fun observeInsights(
        period: QcAnalyticsPeriod,
        projectId: String?,
        thresholdConfig: QcAnalyticsThresholdConfig,
        callerRole: UserRole?
    ): Flow<DomainResult<List<QcOperationalInsight>>> = flow {
        emit(getOperationalInsights(period, projectId, thresholdConfig, callerRole))
    }

    // ==========================================
    // Internal Calculation Functions (No Mutex)
    // ==========================================

    private suspend fun calculateSummaryInternal(
        period: QcAnalyticsPeriod,
        projectId: String?,
        callerRole: UserRole?
    ): DomainResult<QcAnalyticsSummary> {
        val rbac = QcAnalyticsValidator.validateRbac(callerRole)
        if (rbac is DomainResult.Error) return rbac

        val periodVal = QcAnalyticsValidator.validatePeriod(period)
        if (periodVal is DomainResult.Error) return periodVal

        val allJobs = productionJobDataSource.observeJobs().first()
        val filteredJobs = if (projectId != null) allJobs.filter { it.orderId == projectId } else allJobs

        val allCostEntries = qcCostTimeDataSource?.observeCostEntries()?.first() ?: emptyList()
        val costEntries = allCostEntries.filter { entry ->
            (projectId == null || entry.projectId == projectId) &&
                    entry.status != QcCostStatus.CANCELLED &&
                    isTimestampInPeriod(entry.createdAt, period)
        }

        val allTimeEntries = qcCostTimeDataSource?.observeTimeEntries()?.first() ?: emptyList()
        val timeEntries = allTimeEntries.filter { entry ->
            (projectId == null || entry.projectId == projectId) &&
                    entry.status != QcTimeStatus.CANCELLED &&
                    isTimestampInPeriod(entry.createdAt, period)
        }

        val allDefects = defectDataSource?.observeDefects()?.first() ?: emptyList()
        val defects = allDefects.filter { defect ->
            val job = allJobs.find { it.jobId == defect.productionJobId }
            (projectId == null || job?.orderId == projectId) &&
                    isTimestampInPeriod(defect.detectedAt, period)
        }

        val allReworks = reworkDataSource?.observeReworks()?.first() ?: emptyList()
        val reworks = allReworks.filter { rework ->
            (projectId == null || rework.projectId == projectId) &&
                    isTimestampInPeriod(rework.createdAt, period)
        }

        val allReQc = reQcDataSource?.observeReQcList()?.first() ?: emptyList()
        val reQcList = allReQc.filter { reqc ->
            (projectId == null || reqc.projectId == projectId) &&
                    isTimestampInPeriod(reqc.createdAt, period)
        }

        val allFinalQc = finalQcDataSource?.observeFinalQcList()?.first() ?: emptyList()
        val finalQcList = allFinalQc.filter { fqc ->
            (projectId == null || fqc.projectId == projectId) &&
                    isTimestampInPeriod(fqc.createdAt, period)
        }

        val totalJobs = filteredJobs.size
        val totalQcCost = costEntries.sumOf { it.totalCost }
        val totalQcTimeMinutes = timeEntries.sumOf { it.durationMinutes }
        val avgCost = if (totalJobs > 0) totalQcCost / totalJobs else 0.0
        val avgTime = if (totalJobs > 0) totalQcTimeMinutes.toDouble() / totalJobs else 0.0

        val totalDefects = defects.size
        val avgDefects = if (totalJobs > 0) totalDefects.toDouble() / totalJobs else 0.0
        val totalReworks = reworks.size
        val totalReQcCycles = reQcList.size

        val jobsWithDefects = filteredJobs.count { job -> defects.any { it.productionJobId == job.jobId } }
        val jobsWithReworks = filteredJobs.count { job -> reworks.any { it.productionJobId == job.jobId } }
        val jobsWithReQc = filteredJobs.count { job -> reQcList.any { it.productionJobId == job.jobId } }

        val firstPassJobs = filteredJobs.count { job ->
            val hasDefect = defects.any { it.productionJobId == job.jobId }
            val hasRework = reworks.any { it.productionJobId == job.jobId }
            val hasReQc = reQcList.any { it.productionJobId == job.jobId }
            !hasDefect && !hasRework && !hasReQc
        }

        val firstPassRate = if (totalJobs > 0) (firstPassJobs.toDouble() / totalJobs) * 100.0 else 0.0
        val reworkRate = if (totalJobs > 0) (jobsWithReworks.toDouble() / totalJobs) * 100.0 else 0.0
        val reQcRate = if (totalJobs > 0) (jobsWithReQc.toDouble() / totalJobs) * 100.0 else 0.0

        val passedFinalQc = finalQcList.count { it.status == FinalQcStatus.PASSED || it.isReleased }
        val finalQcPassRate = if (finalQcList.isNotEmpty()) (passedFinalQc.toDouble() / finalQcList.size) * 100.0 else 0.0

        val openDefects = defects.count { it.status != DefectStatus.RESOLVED && it.status != DefectStatus.CLOSED }
        val activeReworks = reworks.count { it.status != ReworkStatus.COMPLETED && it.status != ReworkStatus.CANCELLED }
        val failedReQc = reQcList.count { it.status == ReQcStatus.FAILED }
        val releasedJobs = finalQcList.count { it.isReleased }

        // Variance aggregation from Job Analytics
        val jobAnalyticsList = calculateJobAnalyticsInternal(filteredJobs, costEntries, timeEntries, defects, reworks, reQcList, finalQcList)
        val totalCostVariance = jobAnalyticsList.sumOf { it.costVariance }
        val totalTimeVariance = jobAnalyticsList.sumOf { it.timeVarianceMinutes }

        val summary = QcAnalyticsSummary(
            period = period,
            projectId = projectId,
            totalJobs = totalJobs,
            totalQcCost = totalQcCost,
            totalQcTimeMinutes = totalQcTimeMinutes,
            averageQcCostPerJob = avgCost,
            averageQcTimeMinutesPerJob = avgTime,
            totalDefects = totalDefects,
            averageDefectsPerJob = avgDefects,
            totalReworks = totalReworks,
            totalReQcCycles = totalReQcCycles,
            firstPassQcRate = firstPassRate,
            reworkRate = reworkRate,
            reQcRate = reQcRate,
            finalQcPassRate = finalQcPassRate,
            totalCostVariance = totalCostVariance,
            totalTimeVariance = totalTimeVariance,
            openDefectCount = openDefects,
            activeReworkCount = activeReworks,
            failedReQcCount = failedReQc,
            releasedJobCount = releasedJobs
        )

        analyticsDataSource.saveSummaryCache(summary)
        return DomainResult.Success(summary)
    }

    private suspend fun calculateJobAnalyticsListInternal(
        period: QcAnalyticsPeriod,
        projectId: String?,
        callerRole: UserRole?
    ): DomainResult<List<QcJobAnalytics>> {
        val rbac = QcAnalyticsValidator.validateRbac(callerRole)
        if (rbac is DomainResult.Error) return rbac

        val periodVal = QcAnalyticsValidator.validatePeriod(period)
        if (periodVal is DomainResult.Error) return periodVal

        val allJobs = productionJobDataSource.observeJobs().first()
        val filteredJobs = if (projectId != null) allJobs.filter { it.orderId == projectId } else allJobs

        val allCostEntries = qcCostTimeDataSource?.observeCostEntries()?.first() ?: emptyList()
        val costEntries = allCostEntries.filter { entry ->
            (projectId == null || entry.projectId == projectId) &&
                    entry.status != QcCostStatus.CANCELLED &&
                    isTimestampInPeriod(entry.createdAt, period)
        }

        val allTimeEntries = qcCostTimeDataSource?.observeTimeEntries()?.first() ?: emptyList()
        val timeEntries = allTimeEntries.filter { entry ->
            (projectId == null || entry.projectId == projectId) &&
                    entry.status != QcTimeStatus.CANCELLED &&
                    isTimestampInPeriod(entry.createdAt, period)
        }

        val allDefects = defectDataSource?.observeDefects()?.first() ?: emptyList()
        val defects = allDefects.filter { defect ->
            val job = allJobs.find { it.jobId == defect.productionJobId }
            (projectId == null || job?.orderId == projectId) &&
                    isTimestampInPeriod(defect.detectedAt, period)
        }

        val allReworks = reworkDataSource?.observeReworks()?.first() ?: emptyList()
        val reworks = allReworks.filter { rework ->
            (projectId == null || rework.projectId == projectId) &&
                    isTimestampInPeriod(rework.createdAt, period)
        }

        val allReQc = reQcDataSource?.observeReQcList()?.first() ?: emptyList()
        val reQcList = allReQc.filter { reqc ->
            (projectId == null || reqc.projectId == projectId) &&
                    isTimestampInPeriod(reqc.createdAt, period)
        }

        val allFinalQc = finalQcDataSource?.observeFinalQcList()?.first() ?: emptyList()
        val finalQcList = allFinalQc.filter { fqc ->
            (projectId == null || fqc.projectId == projectId) &&
                    isTimestampInPeriod(fqc.createdAt, period)
        }

        val jobAnalyticsList = calculateJobAnalyticsInternal(filteredJobs, costEntries, timeEntries, defects, reworks, reQcList, finalQcList)
            .sortedWith(compareByDescending<QcJobAnalytics> { it.totalQcCost }.thenBy { it.productionJobId })

        analyticsDataSource.saveJobAnalyticsCache(projectId, jobAnalyticsList)
        return DomainResult.Success(jobAnalyticsList)
    }

    private suspend fun calculateDefectAnalyticsListInternal(
        period: QcAnalyticsPeriod,
        projectId: String?,
        callerRole: UserRole?
    ): DomainResult<List<QcDefectAnalytics>> {
        val rbac = QcAnalyticsValidator.validateRbac(callerRole)
        if (rbac is DomainResult.Error) return rbac

        val periodVal = QcAnalyticsValidator.validatePeriod(period)
        if (periodVal is DomainResult.Error) return periodVal

        val allJobs = productionJobDataSource.observeJobs().first()
        val allDefects = defectDataSource?.observeDefects()?.first() ?: emptyList()
        val defects = allDefects.filter { defect ->
            val job = allJobs.find { it.jobId == defect.productionJobId }
            (projectId == null || job?.orderId == projectId) &&
                    isTimestampInPeriod(defect.detectedAt, period)
        }

        val totalDefects = defects.size
        val allCostEntries = qcCostTimeDataSource?.observeCostEntries()?.first() ?: emptyList()
        val allTimeEntries = qcCostTimeDataSource?.observeTimeEntries()?.first() ?: emptyList()
        val allReworks = reworkDataSource?.observeReworks()?.first() ?: emptyList()
        val allReQc = reQcDataSource?.observeReQcList()?.first() ?: emptyList()

        val list = DefectCategory.values().map { category ->
            val categoryDefects = defects.filter { it.category == category }
            val count = categoryDefects.size
            val affectedQty = categoryDefects.sumOf { it.affectedQuantity }
            val defectIds = categoryDefects.map { it.defectId }

            val cost = allCostEntries.filter { it.productionDefectId in defectIds && it.status != QcCostStatus.CANCELLED }
                .sumOf { it.totalCost }
            val time = allTimeEntries.filter { it.productionDefectId in defectIds && it.status != QcTimeStatus.CANCELLED }
                .sumOf { it.durationMinutes }

            val reworks = allReworks.count { it.defectId in defectIds }
            val reQcCount = allReQc.count { it.originalDefectId in defectIds }

            val percent = if (totalDefects > 0) (count.toDouble() / totalDefects) * 100.0 else 0.0

            QcDefectAnalytics(
                defectCategory = category,
                defectCount = count,
                affectedQuantity = affectedQty,
                totalQcCost = cost,
                totalQcTimeMinutes = time,
                reworkCount = reworks,
                reQcCycleCount = reQcCount,
                averageResolutionTimeMinutes = if (count > 0) time.toDouble() / count else 0.0,
                percentageOfTotalDefects = percent
            )
        }.sortedWith(
            compareByDescending<QcDefectAnalytics> { it.defectCount }
                .thenByDescending { it.affectedQuantity }
                .thenBy { it.defectCategory.name }
        )

        analyticsDataSource.saveDefectAnalyticsCache(projectId, list)
        return DomainResult.Success(list)
    }

    private suspend fun calculateStageAnalyticsListInternal(
        period: QcAnalyticsPeriod,
        projectId: String?,
        callerRole: UserRole?
    ): DomainResult<List<QcStageAnalytics>> {
        val rbac = QcAnalyticsValidator.validateRbac(callerRole)
        if (rbac is DomainResult.Error) return rbac

        val periodVal = QcAnalyticsValidator.validatePeriod(period)
        if (periodVal is DomainResult.Error) return periodVal

        val allJobs = productionJobDataSource.observeJobs().first()
        val allDefects = defectDataSource?.observeDefects()?.first() ?: emptyList()
        val defects = allDefects.filter { defect ->
            val job = allJobs.find { it.jobId == defect.productionJobId }
            (projectId == null || job?.orderId == projectId) &&
                    isTimestampInPeriod(defect.detectedAt, period)
        }

        val allReworks = reworkDataSource?.observeReworks()?.first() ?: emptyList()
        val reworks = allReworks.filter { rework ->
            (projectId == null || rework.projectId == projectId) &&
                    isTimestampInPeriod(rework.createdAt, period)
        }

        val allReQc = reQcDataSource?.observeReQcList()?.first() ?: emptyList()
        val reQcList = allReQc.filter { reqc ->
            (projectId == null || reqc.projectId == projectId) &&
                    isTimestampInPeriod(reqc.createdAt, period)
        }

        val allCostEntries = qcCostTimeDataSource?.observeCostEntries()?.first() ?: emptyList()
        val allTimeEntries = qcCostTimeDataSource?.observeTimeEntries()?.first() ?: emptyList()

        val list = ProductionStageType.values().map { stage ->
            val stageDefects = defects.filter { defect ->
                defect.source.name.contains(stage.name, ignoreCase = true) ||
                        (stage == ProductionStageType.PRINTING && defect.category == DefectCategory.PRINT_QUALITY) ||
                        (stage == ProductionStageType.QC && defect.source.name.contains("QC", ignoreCase = true)) ||
                        (stage == ProductionStageType.CTP && defect.category == DefectCategory.ARTWORK_ERROR)
            }
            val stageReworks = reworks.filter { rew ->
                stageDefects.any { it.defectId == rew.defectId }
            }
            val stageReQc = reQcList.filter { reqc ->
                stageDefects.any { it.defectId == reqc.originalDefectId }
            }

            val defectIds = stageDefects.map { it.defectId }
            val stageCost = allCostEntries.filter { it.productionDefectId in defectIds }.sumOf { it.totalCost }
            val stageTime = allTimeEntries.filter { it.productionDefectId in defectIds }.sumOf { it.durationMinutes }

            val totalInspections = stageDefects.size.coerceAtLeast(1)
            val defectRate = if (totalInspections > 0) (stageDefects.size.toDouble() / totalInspections) * 100.0 else 0.0
            val reworkRate = if (stageDefects.isNotEmpty()) (stageReworks.size.toDouble() / stageDefects.size) * 100.0 else 0.0

            QcStageAnalytics(
                productionStage = stage,
                inspectionCount = totalInspections,
                defectCount = stageDefects.size,
                reworkCount = stageReworks.size,
                reQcCount = stageReQc.size,
                totalQcCost = stageCost,
                totalQcTimeMinutes = stageTime,
                defectRate = defectRate,
                reworkRate = reworkRate,
                averageQcTimeMinutes = if (totalInspections > 0) stageTime.toDouble() / totalInspections else 0.0
            )
        }.sortedBy { it.productionStage.displayOrder }

        analyticsDataSource.saveStageAnalyticsCache(projectId, list)
        return DomainResult.Success(list)
    }

    private suspend fun calculateTrendsListInternal(
        period: QcAnalyticsPeriod,
        projectId: String?,
        callerRole: UserRole?
    ): DomainResult<List<QcTrendPoint>> {
        val rbac = QcAnalyticsValidator.validateRbac(callerRole)
        if (rbac is DomainResult.Error) return rbac

        val periodVal = QcAnalyticsValidator.validatePeriod(period)
        if (periodVal is DomainResult.Error) return periodVal

        val start = Instant.parse(period.startTimestamp)
        val end = Instant.parse(period.endTimestamp)
        val days = ChronoUnit.DAYS.between(start, end).coerceAtLeast(1)

        val allCostEntries = qcCostTimeDataSource?.observeCostEntries()?.first() ?: emptyList()
        val allTimeEntries = qcCostTimeDataSource?.observeTimeEntries()?.first() ?: emptyList()
        val allDefects = defectDataSource?.observeDefects()?.first() ?: emptyList()
        val allReworks = reworkDataSource?.observeReworks()?.first() ?: emptyList()
        val allReQc = reQcDataSource?.observeReQcList()?.first() ?: emptyList()

        val points = (0 until days).map { offset ->
            val pStart = start.plus(offset, ChronoUnit.DAYS)
            val pEnd = start.plus(offset + 1, ChronoUnit.DAYS)
            val bucketPeriod = QcAnalyticsPeriod.custom(pStart.toString(), pEnd.toString())

            val cost = allCostEntries.filter { (projectId == null || it.projectId == projectId) && isTimestampInPeriod(it.createdAt, bucketPeriod) }
                .sumOf { it.totalCost }
            val time = allTimeEntries.filter { (projectId == null || it.projectId == projectId) && isTimestampInPeriod(it.createdAt, bucketPeriod) }
                .sumOf { it.durationMinutes }
            val defects = allDefects.filter { isTimestampInPeriod(it.detectedAt, bucketPeriod) }.size
            val reworks = allReworks.filter { (projectId == null || it.projectId == projectId) && isTimestampInPeriod(it.createdAt, bucketPeriod) }.size
            val reqc = allReQc.filter { (projectId == null || it.projectId == projectId) && isTimestampInPeriod(it.createdAt, bucketPeriod) }.size

            QcTrendPoint(
                periodStart = pStart.toString(),
                periodEnd = pEnd.toString(),
                totalQcCost = cost,
                totalQcTimeMinutes = time,
                defectCount = defects,
                reworkCount = reworks,
                reQcCycleCount = reqc,
                costVariance = 0.0,
                timeVariance = 0L
            )
        }

        analyticsDataSource.saveTrendsCache(period, projectId, points)
        return DomainResult.Success(points)
    }

    private fun calculateJobAnalyticsInternal(
        jobs: List<ProductionJob>,
        costEntries: List<QcCostEntry>,
        timeEntries: List<QcTimeEntry>,
        defects: List<ProductionDefect>,
        reworks: List<ProductionRework>,
        reQcList: List<ReQcInspection>,
        finalQcList: List<FinalQcInspection>
    ): List<QcJobAnalytics> {
        return jobs.map { job ->
            val jobCosts = costEntries.filter { it.productionJobId == job.jobId }
            val actualCost = jobCosts.sumOf { it.totalCost }

            val jobTimes = timeEntries.filter { it.productionJobId == job.jobId }
            val actualTime = jobTimes.sumOf { it.durationMinutes }

            val jobDefects = defects.filter { it.productionJobId == job.jobId }
            val defectCount = jobDefects.size

            val jobReworks = reworks.filter { it.productionJobId == job.jobId }
            val reworkCount = jobReworks.size

            val jobReQc = reQcList.filter { it.productionJobId == job.jobId }
            val reQcCount = jobReQc.size

            val jobFinalQc = finalQcList.find { it.productionJobId == job.jobId }
            val isFinalQcPassed = jobFinalQc?.status == FinalQcStatus.PASSED || jobFinalQc?.isReleased == true
            val isReleased = jobFinalQc?.isReleased == true

            val isFirstPass = defectCount == 0 && reworkCount == 0 && reQcCount == 0

            // Baseline planned cost/time estimation if not explicitly set
            val plannedCost = 100.0 // Standard baseline benchmark per job
            val plannedTime = 30L  // Standard baseline benchmark (30 mins)

            val costVariance = actualCost - plannedCost
            val timeVariance = actualTime - plannedTime

            val score = QcJobAnalytics.calculateEfficiencyScore(
                plannedCost = plannedCost,
                actualCost = actualCost,
                plannedMinutes = plannedTime,
                actualMinutes = actualTime,
                defectCount = defectCount,
                reworkCount = reworkCount,
                reQcCycleCount = reQcCount
            )

            QcJobAnalytics(
                productionJobId = job.jobId,
                projectId = job.orderId,
                totalQcCost = actualCost,
                totalQcTimeMinutes = actualTime,
                plannedQcCost = plannedCost,
                actualQcCost = actualCost,
                costVariance = costVariance,
                plannedQcTimeMinutes = plannedTime,
                actualQcTimeMinutes = actualTime,
                timeVarianceMinutes = timeVariance,
                defectCount = defectCount,
                reworkCount = reworkCount,
                reQcCycleCount = reQcCount,
                finalQcPassed = isFinalQcPassed,
                productionReleased = isReleased,
                firstPassQc = isFirstPass,
                efficiencyScore = score
            )
        }
    }

    private fun isTimestampInPeriod(timestamp: String?, period: QcAnalyticsPeriod): Boolean {
        if (timestamp.isNullOrBlank()) return true
        return try {
            val ts = Instant.parse(timestamp)
            val start = Instant.parse(period.startTimestamp)
            val end = Instant.parse(period.endTimestamp)
            !ts.isBefore(start) && !ts.isAfter(end)
        } catch (e: Exception) {
            true
        }
    }
}
