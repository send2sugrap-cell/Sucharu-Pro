package com.sucharu.sucharupro.data.repository

import com.sucharu.sucharupro.data.datasource.FinalQcDataSource
import com.sucharu.sucharupro.data.datasource.ProductionDefectDataSource
import com.sucharu.sucharupro.data.datasource.ProductionJobDataSource
import com.sucharu.sucharupro.data.datasource.ProductionQcDataSource
import com.sucharu.sucharupro.data.datasource.ProductionReQcDataSource
import com.sucharu.sucharupro.data.datasource.ProductionReworkDataSource
import com.sucharu.sucharupro.data.datasource.QcCostTimeDataSource
import com.sucharu.sucharupro.data.datasource.QcGovernanceDataSource
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.qc.DefectSeverity
import com.sucharu.sucharupro.domain.model.qc.DefectStatus
import com.sucharu.sucharupro.domain.model.qc.analytics.QcAnalyticsPeriod
import com.sucharu.sucharupro.domain.model.qc.governance.QcAlertSeverity
import com.sucharu.sucharupro.domain.model.qc.governance.QcAlertStatus
import com.sucharu.sucharupro.domain.model.qc.governance.QcEscalationLevel
import com.sucharu.sucharupro.domain.model.qc.governance.QcEscalationRule
import com.sucharu.sucharupro.domain.model.qc.governance.QcGovernanceActivityEvent
import com.sucharu.sucharupro.domain.model.qc.governance.QcGovernanceActivityType
import com.sucharu.sucharupro.domain.model.qc.governance.QcGovernanceKpi
import com.sucharu.sucharupro.domain.model.qc.governance.QcGovernancePolicy
import com.sucharu.sucharupro.domain.model.qc.governance.QcGovernanceSnapshot
import com.sucharu.sucharupro.domain.model.qc.governance.QcImprovementAction
import com.sucharu.sucharupro.domain.model.qc.governance.QcImprovementActionStatus
import com.sucharu.sucharupro.domain.model.qc.governance.QcImprovementEffectiveness
import com.sucharu.sucharupro.domain.model.qc.governance.QcKpiTarget
import com.sucharu.sucharupro.domain.model.qc.governance.QcKpiThreshold
import com.sucharu.sucharupro.domain.model.qc.governance.QcQualityAlert
import com.sucharu.sucharupro.domain.model.qc.governance.QcQualityReview
import com.sucharu.sucharupro.domain.model.qc.governance.QcQualityReviewStatus
import com.sucharu.sucharupro.domain.model.user.UserRole
import com.sucharu.sucharupro.domain.repository.QcAnalyticsRepository
import com.sucharu.sucharupro.domain.repository.QcGovernanceRepository
import com.sucharu.sucharupro.domain.validation.QcGovernanceValidator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID

/**
 * Thread-safe production-grade repository implementation for QC Governance, KPI Thresholds & Continuous Quality Improvement.
 */
class QcGovernanceRepositoryImpl(
    private val governanceDataSource: QcGovernanceDataSource,
    private val analyticsRepository: QcAnalyticsRepository? = null,
    private val productionJobDataSource: ProductionJobDataSource? = null,
    private val qcDataSource: ProductionQcDataSource? = null,
    private val defectDataSource: ProductionDefectDataSource? = null,
    private val reworkDataSource: ProductionReworkDataSource? = null,
    private val reQcDataSource: ProductionReQcDataSource? = null,
    private val finalQcDataSource: FinalQcDataSource? = null,
    private val qcCostTimeDataSource: QcCostTimeDataSource? = null
) : QcGovernanceRepository {

    private val repositoryMutex = Mutex()

    // ==========================================
    // 1. KPI Target & Policy Management
    // ==========================================

    override fun observeTargets(projectId: String): Flow<List<QcKpiTarget>> {
        return governanceDataSource.observeTargets().map { list ->
            list.filter { it.projectId == projectId }
        }
    }

    override suspend fun getTargets(projectId: String): DomainResult<List<QcKpiTarget>> = repositoryMutex.withLock {
        val targets = governanceDataSource.observeTargets().first().filter { it.projectId == projectId }
        DomainResult.Success(targets)
    }

    override suspend fun setTarget(
        target: QcKpiTarget,
        callerRole: UserRole?
    ): DomainResult<QcKpiTarget> = repositoryMutex.withLock {
        val rbacResult = QcGovernanceValidator.validateConfigPermission(callerRole)
        if (rbacResult is DomainResult.Error) return rbacResult

        val valResult = QcGovernanceValidator.validateKpiTarget(target)
        if (valResult is DomainResult.Error) return valResult

        val existing = governanceDataSource.observeTargets().first()
        val match = existing.find { it.targetId == target.targetId }

        val result = if (match != null) {
            governanceDataSource.updateTarget(target)
        } else {
            governanceDataSource.insertTarget(target)
        }

        if (result is DomainResult.Success) {
            recordActivityInternal(
                projectId = target.projectId,
                eventType = if (match != null) QcGovernanceActivityType.KPI_TARGET_UPDATED else QcGovernanceActivityType.KPI_TARGET_CREATED,
                targetId = target.targetId,
                targetType = "QcKpiTarget",
                actorId = target.configuredBy,
                description = "Configured target for ${target.kpiType.defaultLabel} (${target.targetValue} ${target.unit})",
                timestamp = target.updatedAt
            )
        }
        return result
    }

    override suspend fun deactivateTarget(
        targetId: String,
        callerRole: UserRole?
    ): DomainResult<QcKpiTarget> = repositoryMutex.withLock {
        val rbacResult = QcGovernanceValidator.validateConfigPermission(callerRole)
        if (rbacResult is DomainResult.Error) return rbacResult

        val existing = governanceDataSource.observeTargets().first()
        val match = existing.find { it.targetId == targetId }
            ?: return DomainResult.Error(message = "KPI Target with ID '$targetId' not found.")

        val updated = match.copy(active = false)
        val result = governanceDataSource.updateTarget(updated)
        return result
    }

    override fun observePolicies(projectId: String): Flow<List<QcGovernancePolicy>> {
        return governanceDataSource.observePolicies().map { list ->
            list.filter { it.projectId == projectId }
        }
    }

    override suspend fun setPolicy(
        policy: QcGovernancePolicy,
        callerRole: UserRole?
    ): DomainResult<QcGovernancePolicy> = repositoryMutex.withLock {
        val rbacResult = QcGovernanceValidator.validateConfigPermission(callerRole)
        if (rbacResult is DomainResult.Error) return rbacResult

        val valResult = QcGovernanceValidator.validatePolicy(policy)
        if (valResult is DomainResult.Error) return valResult

        val existing = governanceDataSource.observePolicies().first()
        val match = existing.find { it.policyId == policy.policyId }

        val result = if (match != null) {
            governanceDataSource.updatePolicy(policy)
        } else {
            governanceDataSource.insertPolicy(policy)
        }

        if (result is DomainResult.Success) {
            recordActivityInternal(
                projectId = policy.projectId,
                eventType = QcGovernanceActivityType.GOVERNANCE_CREATED,
                targetId = policy.policyId,
                targetType = "QcGovernancePolicy",
                actorId = policy.configuredBy,
                description = "Configured governance policy '${policy.name}'",
                timestamp = policy.updatedAt
            )
        }
        return result
    }

    // ==========================================
    // 2. Threshold Evaluation Engine
    // ==========================================

    override suspend fun evaluateProjectKpis(
        period: QcAnalyticsPeriod,
        projectId: String,
        callerRole: UserRole?
    ): DomainResult<List<QcKpiThreshold>> = repositoryMutex.withLock {
        evaluateProjectKpisInternal(period, projectId, callerRole)
    }

    private suspend fun evaluateProjectKpisInternal(
        period: QcAnalyticsPeriod,
        projectId: String,
        callerRole: UserRole?
    ): DomainResult<List<QcKpiThreshold>> {
        if (projectId.isBlank()) {
            return DomainResult.Error(message = "Project ID cannot be blank.")
        }

        val targets = governanceDataSource.observeTargets().first().filter { it.projectId == projectId && it.active }
        val targetMap = targets.associateBy { it.kpiType }

        val calculatedKpis = mutableMapOf<QcGovernanceKpi, Double>()

        if (analyticsRepository != null) {
            val summaryRes = analyticsRepository.getSummary(period, projectId, callerRole)
            if (summaryRes is DomainResult.Success) {
                val s = summaryRes.data
                val jobsRes = analyticsRepository.getJobAnalytics(period, projectId, callerRole)
                val jobAnalyticsList = if (jobsRes is DomainResult.Success) jobsRes.data else emptyList()
                val avgScore = if (jobAnalyticsList.isNotEmpty()) jobAnalyticsList.map { it.efficiencyScore }.average() else 100.0

                calculatedKpis[QcGovernanceKpi.FIRST_PASS_RATE] = s.firstPassQcRate
                calculatedKpis[QcGovernanceKpi.DEFECT_RATE] = s.averageDefectsPerJob
                calculatedKpis[QcGovernanceKpi.REWORK_RATE] = s.reworkRate
                calculatedKpis[QcGovernanceKpi.RE_QC_RATE] = s.reQcRate
                calculatedKpis[QcGovernanceKpi.FINAL_QC_PASS_RATE] = s.finalQcPassRate
                calculatedKpis[QcGovernanceKpi.OPEN_DEFECT_RATE] = s.openDefectCount.toDouble()
                calculatedKpis[QcGovernanceKpi.REWORK_COMPLETION_RATE] = if (s.totalReworks > 0) ((s.totalReworks - s.activeReworkCount).toDouble() / s.totalReworks) * 100.0 else 100.0
                calculatedKpis[QcGovernanceKpi.RE_QC_FAILURE_RATE] = if (s.totalReQcCycles > 0) (s.failedReQcCount.toDouble() / s.totalReQcCycles) * 100.0 else 0.0
                calculatedKpis[QcGovernanceKpi.QC_COST_VARIANCE] = s.totalCostVariance
                calculatedKpis[QcGovernanceKpi.QC_TIME_VARIANCE] = s.totalTimeVariance.toDouble()
                calculatedKpis[QcGovernanceKpi.QUALITY_EFFICIENCY_SCORE] = avgScore
            }
        }

        val evaluations = QcGovernanceKpi.entries.map { kpi ->
            val actual = calculatedKpis[kpi] ?: defaultKpiValue(kpi)
            val target = targetMap[kpi]
            QcGovernanceValidator.evaluateThreshold(kpi, actual, target)
        }

        return DomainResult.Success(evaluations)
    }

    private fun defaultKpiValue(kpi: QcGovernanceKpi): Double {
        return if (kpi.isHigherBetter) 100.0 else 0.0
    }

    // ==========================================
    // 3. Quality Alerts Lifecycle
    // ==========================================

    override fun observeAlerts(projectId: String): Flow<List<QcQualityAlert>> {
        return governanceDataSource.observeAlerts().map { list ->
            list.filter { it.projectId == projectId }
        }
    }

    override suspend fun getAlerts(
        projectId: String,
        callerRole: UserRole?
    ): DomainResult<List<QcQualityAlert>> = repositoryMutex.withLock {
        val alerts = governanceDataSource.observeAlerts().first().filter { it.projectId == projectId }
        DomainResult.Success(alerts)
    }

    override suspend fun getAlertById(alertId: String): DomainResult<QcQualityAlert> = repositoryMutex.withLock {
        val alert = governanceDataSource.observeAlerts().first().find { it.alertId == alertId }
            ?: return DomainResult.Error(message = "Quality Alert with ID '$alertId' not found.")
        DomainResult.Success(alert)
    }

    override suspend fun createAlert(
        alert: QcQualityAlert,
        callerRole: UserRole?
    ): DomainResult<QcQualityAlert> = repositoryMutex.withLock {
        val insertResult = governanceDataSource.insertAlert(alert)
        if (insertResult is DomainResult.Success) {
            recordActivityInternal(
                projectId = alert.projectId,
                eventType = QcGovernanceActivityType.ALERT_CREATED,
                targetId = alert.alertId,
                targetType = "QcQualityAlert",
                actorId = alert.detectedBy,
                description = "Quality alert detected: ${alert.title} (${alert.severity.defaultLabel})",
                timestamp = alert.detectedAt
            )
        }
        return insertResult
    }

    override suspend fun acknowledgeAlert(
        alertId: String,
        acknowledgedBy: String,
        notes: String?,
        timestamp: String,
        callerRole: UserRole?
    ): DomainResult<QcQualityAlert> = repositoryMutex.withLock {
        val rbacResult = QcGovernanceValidator.validateAlertAckPermission(callerRole)
        if (rbacResult is DomainResult.Error) return rbacResult

        val alerts = governanceDataSource.observeAlerts().first()
        val current = alerts.find { it.alertId == alertId }
            ?: return DomainResult.Error(message = "Quality Alert with ID '$alertId' not found.")

        val transitionResult = QcGovernanceValidator.validateAlertTransition(current.status, QcAlertStatus.ACKNOWLEDGED)
        if (transitionResult is DomainResult.Error) return transitionResult

        val updatedNotes = if (!notes.isNullOrBlank()) {
            if (current.notes.isNullOrBlank()) notes else "${current.notes}\n$notes"
        } else {
            current.notes
        }

        val updated = current.copy(
            status = QcAlertStatus.ACKNOWLEDGED,
            acknowledgedAt = timestamp,
            acknowledgedBy = acknowledgedBy,
            notes = updatedNotes
        )

        val updateResult = governanceDataSource.updateAlert(updated)
        if (updateResult is DomainResult.Success) {
            recordActivityInternal(
                projectId = updated.projectId,
                eventType = QcGovernanceActivityType.ALERT_ACKNOWLEDGED,
                targetId = updated.alertId,
                targetType = "QcQualityAlert",
                actorId = acknowledgedBy,
                description = "Quality alert acknowledged by '$acknowledgedBy'",
                timestamp = timestamp
            )
        }
        return updateResult
    }

    override suspend fun escalateAlert(
        alertId: String,
        targetLevel: QcEscalationLevel,
        escalatedBy: String,
        notes: String?,
        timestamp: String,
        callerRole: UserRole?
    ): DomainResult<QcQualityAlert> = repositoryMutex.withLock {
        val alerts = governanceDataSource.observeAlerts().first()
        val current = alerts.find { it.alertId == alertId }
            ?: return DomainResult.Error(message = "Quality Alert with ID '$alertId' not found.")

        if (current.isTerminal) {
            return DomainResult.Error(message = "Cannot escalate alert in terminal status '${current.status.defaultLabel}'.")
        }

        val updatedNotes = if (!notes.isNullOrBlank()) {
            if (current.notes.isNullOrBlank()) "[Escalation]: $notes" else "${current.notes}\n[Escalation]: $notes"
        } else {
            current.notes
        }

        val updated = current.copy(
            escalationLevel = targetLevel,
            status = if (current.status == QcAlertStatus.DETECTED) QcAlertStatus.ACKNOWLEDGED else current.status,
            notes = updatedNotes
        )

        val updateResult = governanceDataSource.updateAlert(updated)
        if (updateResult is DomainResult.Success) {
            recordActivityInternal(
                projectId = updated.projectId,
                eventType = QcGovernanceActivityType.ALERT_ESCALATED,
                targetId = updated.alertId,
                targetType = "QcQualityAlert",
                actorId = escalatedBy,
                description = "Alert escalated to '${targetLevel.defaultLabel}' by '$escalatedBy'",
                timestamp = timestamp
            )
        }
        return updateResult
    }

    override suspend fun resolveAlert(
        alertId: String,
        resolvedBy: String,
        notes: String?,
        timestamp: String,
        callerRole: UserRole?
    ): DomainResult<QcQualityAlert> = repositoryMutex.withLock {
        val alerts = governanceDataSource.observeAlerts().first()
        val current = alerts.find { it.alertId == alertId }
            ?: return DomainResult.Error(message = "Quality Alert with ID '$alertId' not found.")

        val transitionResult = QcGovernanceValidator.validateAlertTransition(current.status, QcAlertStatus.RESOLVED)
        if (transitionResult is DomainResult.Error) return transitionResult

        val updatedNotes = if (!notes.isNullOrBlank()) {
            if (current.notes.isNullOrBlank()) "[Resolution]: $notes" else "${current.notes}\n[Resolution]: $notes"
        } else {
            current.notes
        }

        val updated = current.copy(
            status = QcAlertStatus.RESOLVED,
            resolvedAt = timestamp,
            resolvedBy = resolvedBy,
            notes = updatedNotes
        )

        val updateResult = governanceDataSource.updateAlert(updated)
        if (updateResult is DomainResult.Success) {
            recordActivityInternal(
                projectId = updated.projectId,
                eventType = QcGovernanceActivityType.ALERT_RESOLVED,
                targetId = updated.alertId,
                targetType = "QcQualityAlert",
                actorId = resolvedBy,
                description = "Quality alert resolved by '$resolvedBy'",
                timestamp = timestamp
            )
        }
        return updateResult
    }

    override suspend fun dismissAlert(
        alertId: String,
        dismissedBy: String,
        reason: String,
        timestamp: String,
        callerRole: UserRole?
    ): DomainResult<QcQualityAlert> = repositoryMutex.withLock {
        val alerts = governanceDataSource.observeAlerts().first()
        val current = alerts.find { it.alertId == alertId }
            ?: return DomainResult.Error(message = "Quality Alert with ID '$alertId' not found.")

        val transitionResult = QcGovernanceValidator.validateAlertTransition(current.status, QcAlertStatus.DISMISSED)
        if (transitionResult is DomainResult.Error) return transitionResult

        val updatedNotes = if (current.notes.isNullOrBlank()) "[Dismissed]: $reason" else "${current.notes}\n[Dismissed]: $reason"

        val updated = current.copy(
            status = QcAlertStatus.DISMISSED,
            resolvedAt = timestamp,
            resolvedBy = dismissedBy,
            notes = updatedNotes
        )

        val updateResult = governanceDataSource.updateAlert(updated)
        if (updateResult is DomainResult.Success) {
            recordActivityInternal(
                projectId = updated.projectId,
                eventType = QcGovernanceActivityType.ALERT_DISMISSED,
                targetId = updated.alertId,
                targetType = "QcQualityAlert",
                actorId = dismissedBy,
                description = "Quality alert dismissed by '$dismissedBy': $reason",
                timestamp = timestamp
            )
        }
        return updateResult
    }

    // ==========================================
    // 4. Escalation Rules
    // ==========================================

    override fun observeEscalationRules(projectId: String): Flow<List<QcEscalationRule>> {
        return governanceDataSource.observeEscalationRules().map { list ->
            list.filter { it.projectId == projectId }
        }
    }

    override suspend fun setEscalationRule(
        rule: QcEscalationRule,
        callerRole: UserRole?
    ): DomainResult<QcEscalationRule> = repositoryMutex.withLock {
        val rbacResult = QcGovernanceValidator.validateConfigPermission(callerRole)
        if (rbacResult is DomainResult.Error) return rbacResult

        val existing = governanceDataSource.observeEscalationRules().first()
        val match = existing.find { it.ruleId == rule.ruleId }

        val result = if (match != null) {
            governanceDataSource.updateEscalationRule(rule)
        } else {
            governanceDataSource.insertEscalationRule(rule)
        }
        return result
    }

    // ==========================================
    // 5. Management Quality Reviews
    // ==========================================

    override fun observeReviews(projectId: String): Flow<List<QcQualityReview>> {
        return governanceDataSource.observeReviews().map { list ->
            list.filter { it.projectId == projectId }
        }
    }

    override suspend fun getReviews(
        projectId: String,
        callerRole: UserRole?
    ): DomainResult<List<QcQualityReview>> = repositoryMutex.withLock {
        val reviews = governanceDataSource.observeReviews().first().filter { it.projectId == projectId }
        DomainResult.Success(reviews)
    }

    override suspend fun createReview(
        review: QcQualityReview,
        callerRole: UserRole?
    ): DomainResult<QcQualityReview> = repositoryMutex.withLock {
        val rbacResult = QcGovernanceValidator.validateReviewPermission(callerRole)
        if (rbacResult is DomainResult.Error) return rbacResult

        val insertResult = governanceDataSource.insertReview(review)
        if (insertResult is DomainResult.Success) {
            recordActivityInternal(
                projectId = review.projectId,
                eventType = QcGovernanceActivityType.REVIEW_CREATED,
                targetId = review.reviewId,
                targetType = "QcQualityReview",
                actorId = review.reviewerId,
                description = "Created quality review '${review.title}'",
                timestamp = review.createdAt
            )
        }
        return insertResult
    }

    override suspend fun startReview(
        reviewId: String,
        reviewerId: String,
        timestamp: String,
        callerRole: UserRole?
    ): DomainResult<QcQualityReview> = repositoryMutex.withLock {
        val rbacResult = QcGovernanceValidator.validateReviewPermission(callerRole)
        if (rbacResult is DomainResult.Error) return rbacResult

        val reviews = governanceDataSource.observeReviews().first()
        val current = reviews.find { it.reviewId == reviewId }
            ?: return DomainResult.Error(message = "Quality Review with ID '$reviewId' not found.")

        val transitionResult = QcGovernanceValidator.validateReviewTransition(current.status, QcQualityReviewStatus.IN_REVIEW)
        if (transitionResult is DomainResult.Error) return transitionResult

        val updated = current.copy(
            status = QcQualityReviewStatus.IN_REVIEW,
            reviewerId = reviewerId,
            updatedAt = timestamp
        )

        val updateResult = governanceDataSource.updateReview(updated)
        if (updateResult is DomainResult.Success) {
            recordActivityInternal(
                projectId = updated.projectId,
                eventType = QcGovernanceActivityType.REVIEW_STARTED,
                targetId = updated.reviewId,
                targetType = "QcQualityReview",
                actorId = reviewerId,
                description = "Quality review '${updated.title}' started by '$reviewerId'",
                timestamp = timestamp
            )
        }
        return updateResult
    }

    override suspend fun completeReview(
        reviewId: String,
        reviewerId: String,
        recommendations: String?,
        reviewNotes: String?,
        timestamp: String,
        callerRole: UserRole?
    ): DomainResult<QcQualityReview> = repositoryMutex.withLock {
        val rbacResult = QcGovernanceValidator.validateReviewPermission(callerRole)
        if (rbacResult is DomainResult.Error) return rbacResult

        val reviews = governanceDataSource.observeReviews().first()
        val current = reviews.find { it.reviewId == reviewId }
            ?: return DomainResult.Error(message = "Quality Review with ID '$reviewId' not found.")

        val transitionResult = QcGovernanceValidator.validateReviewTransition(current.status, QcQualityReviewStatus.COMPLETED)
        if (transitionResult is DomainResult.Error) return transitionResult

        val updated = current.copy(
            status = QcQualityReviewStatus.COMPLETED,
            recommendations = recommendations ?: current.recommendations,
            reviewNotes = reviewNotes ?: current.reviewNotes,
            completedAt = timestamp,
            updatedAt = timestamp
        )

        val updateResult = governanceDataSource.updateReview(updated)
        if (updateResult is DomainResult.Success) {
            recordActivityInternal(
                projectId = updated.projectId,
                eventType = QcGovernanceActivityType.REVIEW_COMPLETED,
                targetId = updated.reviewId,
                targetType = "QcQualityReview",
                actorId = reviewerId,
                description = "Quality review '${updated.title}' completed by '$reviewerId'",
                timestamp = timestamp
            )
        }
        return updateResult
    }

    override suspend fun cancelReview(
        reviewId: String,
        reason: String,
        timestamp: String,
        callerRole: UserRole?
    ): DomainResult<QcQualityReview> = repositoryMutex.withLock {
        val rbacResult = QcGovernanceValidator.validateReviewPermission(callerRole)
        if (rbacResult is DomainResult.Error) return rbacResult

        val reviews = governanceDataSource.observeReviews().first()
        val current = reviews.find { it.reviewId == reviewId }
            ?: return DomainResult.Error(message = "Quality Review with ID '$reviewId' not found.")

        val transitionResult = QcGovernanceValidator.validateReviewTransition(current.status, QcQualityReviewStatus.CANCELLED)
        if (transitionResult is DomainResult.Error) return transitionResult

        val updated = current.copy(
            status = QcQualityReviewStatus.CANCELLED,
            reviewNotes = if (current.reviewNotes.isNullOrBlank()) "[Cancelled]: $reason" else "${current.reviewNotes}\n[Cancelled]: $reason",
            updatedAt = timestamp
        )

        val updateResult = governanceDataSource.updateReview(updated)
        if (updateResult is DomainResult.Success) {
            recordActivityInternal(
                projectId = updated.projectId,
                eventType = QcGovernanceActivityType.REVIEW_CANCELLED,
                targetId = updated.reviewId,
                targetType = "QcQualityReview",
                actorId = "SYSTEM",
                description = "Quality review '${updated.title}' cancelled: $reason",
                timestamp = timestamp
            )
        }
        return updateResult
    }

    // ==========================================
    // 6. Continuous Improvement Actions (CAPA)
    // ==========================================

    override fun observeImprovementActions(projectId: String): Flow<List<QcImprovementAction>> {
        return governanceDataSource.observeImprovementActions().map { list ->
            list.filter { it.projectId == projectId }
        }
    }

    override suspend fun getImprovementActions(
        projectId: String,
        callerRole: UserRole?
    ): DomainResult<List<QcImprovementAction>> = repositoryMutex.withLock {
        val actions = governanceDataSource.observeImprovementActions().first().filter { it.projectId == projectId }
        DomainResult.Success(actions)
    }

    override suspend fun proposeImprovementAction(
        action: QcImprovementAction,
        callerRole: UserRole?
    ): DomainResult<QcImprovementAction> = repositoryMutex.withLock {
        val rbacResult = QcGovernanceValidator.validateActionProposePermission(callerRole)
        if (rbacResult is DomainResult.Error) return rbacResult

        val insertResult = governanceDataSource.insertImprovementAction(action)
        if (insertResult is DomainResult.Success) {
            recordActivityInternal(
                projectId = action.projectId,
                eventType = QcGovernanceActivityType.IMPROVEMENT_ACTION_CREATED,
                targetId = action.actionId,
                targetType = "QcImprovementAction",
                actorId = action.proposedBy,
                description = "Proposed improvement action: '${action.title}' (${action.actionType.defaultLabel})",
                timestamp = action.createdAt
            )
        }
        return insertResult
    }

    override suspend fun approveImprovementAction(
        actionId: String,
        approvedBy: String,
        approvedByName: String?,
        timestamp: String,
        callerRole: UserRole?
    ): DomainResult<QcImprovementAction> = repositoryMutex.withLock {
        val rbacResult = QcGovernanceValidator.validateActionApprovePermission(callerRole)
        if (rbacResult is DomainResult.Error) return rbacResult

        val actions = governanceDataSource.observeImprovementActions().first()
        val current = actions.find { it.actionId == actionId }
            ?: return DomainResult.Error(message = "Improvement Action with ID '$actionId' not found.")

        val sodResult = QcGovernanceValidator.validateSeparationOfDuties(
            proposedBy = current.proposedBy,
            approverId = approvedBy
        )
        if (sodResult is DomainResult.Error) return sodResult

        val transitionResult = QcGovernanceValidator.validateActionTransition(current.status, QcImprovementActionStatus.APPROVED)
        if (transitionResult is DomainResult.Error) return transitionResult

        val updated = current.copy(
            status = QcImprovementActionStatus.APPROVED,
            approvedBy = approvedBy,
            approvedByName = approvedByName,
            updatedAt = timestamp
        )

        val updateResult = governanceDataSource.updateImprovementAction(updated)
        if (updateResult is DomainResult.Success) {
            recordActivityInternal(
                projectId = updated.projectId,
                eventType = QcGovernanceActivityType.IMPROVEMENT_ACTION_APPROVED,
                targetId = updated.actionId,
                targetType = "QcImprovementAction",
                actorId = approvedBy,
                description = "Approved improvement action '${updated.title}'",
                timestamp = timestamp
            )
        }
        return updateResult
    }

    override suspend fun assignImprovementAction(
        actionId: String,
        ownerId: String,
        ownerName: String?,
        timestamp: String,
        callerRole: UserRole?
    ): DomainResult<QcImprovementAction> = repositoryMutex.withLock {
        val rbacResult = QcGovernanceValidator.validateActionApprovePermission(callerRole)
        if (rbacResult is DomainResult.Error) return rbacResult

        val actions = governanceDataSource.observeImprovementActions().first()
        val current = actions.find { it.actionId == actionId }
            ?: return DomainResult.Error(message = "Improvement Action with ID '$actionId' not found.")

        val transitionResult = QcGovernanceValidator.validateActionTransition(current.status, QcImprovementActionStatus.ASSIGNED)
        if (transitionResult is DomainResult.Error) return transitionResult

        val updated = current.copy(
            status = QcImprovementActionStatus.ASSIGNED,
            ownerId = ownerId,
            ownerName = ownerName,
            updatedAt = timestamp
        )

        val updateResult = governanceDataSource.updateImprovementAction(updated)
        if (updateResult is DomainResult.Success) {
            recordActivityInternal(
                projectId = updated.projectId,
                eventType = QcGovernanceActivityType.IMPROVEMENT_ACTION_ASSIGNED,
                targetId = updated.actionId,
                targetType = "QcImprovementAction",
                actorId = ownerId,
                description = "Assigned improvement action '${updated.title}' to '$ownerId'",
                timestamp = timestamp
            )
        }
        return updateResult
    }

    override suspend fun startImprovementAction(
        actionId: String,
        timestamp: String,
        callerRole: UserRole?
    ): DomainResult<QcImprovementAction> = repositoryMutex.withLock {
        val actions = governanceDataSource.observeImprovementActions().first()
        val current = actions.find { it.actionId == actionId }
            ?: return DomainResult.Error(message = "Improvement Action with ID '$actionId' not found.")

        val transitionResult = QcGovernanceValidator.validateActionTransition(current.status, QcImprovementActionStatus.IN_PROGRESS)
        if (transitionResult is DomainResult.Error) return transitionResult

        val updated = current.copy(
            status = QcImprovementActionStatus.IN_PROGRESS,
            updatedAt = timestamp
        )

        val updateResult = governanceDataSource.updateImprovementAction(updated)
        if (updateResult is DomainResult.Success) {
            recordActivityInternal(
                projectId = updated.projectId,
                eventType = QcGovernanceActivityType.IMPROVEMENT_ACTION_STARTED,
                targetId = updated.actionId,
                targetType = "QcImprovementAction",
                actorId = updated.ownerId ?: "UNKNOWN",
                description = "Started work on improvement action '${updated.title}'",
                timestamp = timestamp
            )
        }
        return updateResult
    }

    override suspend fun completeImprovementAction(
        actionId: String,
        completionNotes: String,
        timestamp: String,
        postKpiValue: Double?,
        callerRole: UserRole?
    ): DomainResult<QcImprovementAction> = repositoryMutex.withLock {
        val actions = governanceDataSource.observeImprovementActions().first()
        val current = actions.find { it.actionId == actionId }
            ?: return DomainResult.Error(message = "Improvement Action with ID '$actionId' not found.")

        val transitionResult = QcGovernanceValidator.validateActionTransition(current.status, QcImprovementActionStatus.COMPLETED)
        if (transitionResult is DomainResult.Error) return transitionResult

        val updated = current.copy(
            status = QcImprovementActionStatus.COMPLETED,
            completionNotes = completionNotes,
            completedAt = timestamp,
            postImprovementKpiValue = postKpiValue ?: current.postImprovementKpiValue,
            updatedAt = timestamp
        )

        val updateResult = governanceDataSource.updateImprovementAction(updated)
        if (updateResult is DomainResult.Success) {
            recordActivityInternal(
                projectId = updated.projectId,
                eventType = QcGovernanceActivityType.IMPROVEMENT_ACTION_COMPLETED,
                targetId = updated.actionId,
                targetType = "QcImprovementAction",
                actorId = updated.ownerId ?: "UNKNOWN",
                description = "Completed improvement action '${updated.title}'",
                timestamp = timestamp
            )
        }
        return updateResult
    }

    override suspend fun verifyImprovementAction(
        actionId: String,
        verifiedBy: String,
        verifiedByName: String?,
        effectiveness: QcImprovementEffectiveness,
        verificationNotes: String,
        timestamp: String,
        callerRole: UserRole?
    ): DomainResult<QcImprovementAction> = repositoryMutex.withLock {
        val rbacResult = QcGovernanceValidator.validateActionVerifyPermission(callerRole)
        if (rbacResult is DomainResult.Error) return rbacResult

        val actions = governanceDataSource.observeImprovementActions().first()
        val current = actions.find { it.actionId == actionId }
            ?: return DomainResult.Error(message = "Improvement Action with ID '$actionId' not found.")

        val sodResult = QcGovernanceValidator.validateSeparationOfDuties(
            proposedBy = current.proposedBy,
            approverId = current.approvedBy ?: "",
            actionOwnerId = current.ownerId,
            verifierId = verifiedBy
        )
        if (sodResult is DomainResult.Error) return sodResult

        val transitionResult = QcGovernanceValidator.validateActionTransition(current.status, QcImprovementActionStatus.VERIFIED)
        if (transitionResult is DomainResult.Error) return transitionResult

        val updated = current.copy(
            status = QcImprovementActionStatus.VERIFIED,
            verifiedBy = verifiedBy,
            verifiedByName = verifiedByName,
            verifiedAt = timestamp,
            effectiveness = effectiveness,
            verificationNotes = verificationNotes,
            updatedAt = timestamp
        )

        val updateResult = governanceDataSource.updateImprovementAction(updated)
        if (updateResult is DomainResult.Success) {
            recordActivityInternal(
                projectId = updated.projectId,
                eventType = QcGovernanceActivityType.IMPROVEMENT_ACTION_VERIFIED,
                targetId = updated.actionId,
                targetType = "QcImprovementAction",
                actorId = verifiedBy,
                description = "Verified improvement action '${updated.title}' with effectiveness: ${effectiveness.defaultLabel}",
                timestamp = timestamp
            )
            recordActivityInternal(
                projectId = updated.projectId,
                eventType = QcGovernanceActivityType.IMPROVEMENT_EFFECTIVENESS_RECORDED,
                targetId = updated.actionId,
                targetType = "QcImprovementAction",
                actorId = verifiedBy,
                description = "Recorded effectiveness '${effectiveness.defaultLabel}' for action '${updated.title}'",
                timestamp = timestamp
            )
        }
        return updateResult
    }

    override suspend fun rejectImprovementAction(
        actionId: String,
        reason: String,
        timestamp: String,
        callerRole: UserRole?
    ): DomainResult<QcImprovementAction> = repositoryMutex.withLock {
        val rbacResult = QcGovernanceValidator.validateActionApprovePermission(callerRole)
        if (rbacResult is DomainResult.Error) return rbacResult

        val actions = governanceDataSource.observeImprovementActions().first()
        val current = actions.find { it.actionId == actionId }
            ?: return DomainResult.Error(message = "Improvement Action with ID '$actionId' not found.")

        val transitionResult = QcGovernanceValidator.validateActionTransition(current.status, QcImprovementActionStatus.REJECTED)
        if (transitionResult is DomainResult.Error) return transitionResult

        val updated = current.copy(
            status = QcImprovementActionStatus.REJECTED,
            verificationNotes = if (current.verificationNotes.isNullOrBlank()) "[Rejected]: $reason" else "${current.verificationNotes}\n[Rejected]: $reason",
            updatedAt = timestamp
        )

        val updateResult = governanceDataSource.updateImprovementAction(updated)
        if (updateResult is DomainResult.Success) {
            recordActivityInternal(
                projectId = updated.projectId,
                eventType = QcGovernanceActivityType.IMPROVEMENT_ACTION_REJECTED,
                targetId = updated.actionId,
                targetType = "QcImprovementAction",
                actorId = "SYSTEM",
                description = "Rejected improvement action '${updated.title}': $reason",
                timestamp = timestamp
            )
        }
        return updateResult
    }

    // ==========================================
    // 7. Governance Snapshots
    // ==========================================

    override fun observeSnapshots(projectId: String): Flow<List<QcGovernanceSnapshot>> {
        return governanceDataSource.observeSnapshots().map { list ->
            list.filter { it.projectId == projectId }
        }
    }

    override suspend fun createSnapshot(
        period: QcAnalyticsPeriod,
        projectId: String,
        generatedBy: String,
        timestamp: String,
        callerRole: UserRole?
    ): DomainResult<QcGovernanceSnapshot> = repositoryMutex.withLock {
        val evalResult = evaluateProjectKpisInternal(period, projectId, callerRole)
        val evaluations = if (evalResult is DomainResult.Success) evalResult.data else emptyList()

        val kpiValues = evaluations.associate { it.kpiType.name to it.currentValue }
        val kpiTargets = evaluations.associate { it.kpiType.name to it.targetValue }
        val thresholdStates = evaluations.associate { it.kpiType.name to it.status.name }

        val alerts = governanceDataSource.observeAlerts().first().filter { it.projectId == projectId }
        val openCriticalAlerts = alerts.count { !it.isTerminal && it.severity == QcAlertSeverity.CRITICAL }

        val summary = if (analyticsRepository != null) {
            val res = analyticsRepository.getSummary(period, projectId, callerRole)
            if (res is DomainResult.Success) res.data else null
        } else null

        val jobsRes = if (analyticsRepository != null) analyticsRepository.getJobAnalytics(period, projectId, callerRole) else null
        val jobList = if (jobsRes is DomainResult.Success) jobsRes.data else emptyList()
        val avgScore = if (jobList.isNotEmpty()) jobList.map { it.efficiencyScore }.average() else 100.0

        val snapshot = QcGovernanceSnapshot(
            snapshotId = UUID.randomUUID().toString(),
            projectId = projectId,
            period = period,
            kpiValues = kpiValues,
            kpiTargets = kpiTargets,
            thresholdStates = thresholdStates,
            totalAlertCount = alerts.size,
            openCriticalAlertCount = openCriticalAlerts,
            totalDefectCount = summary?.totalDefects ?: 0,
            recurringDefectCount = 0,
            reworkCount = summary?.totalReworks ?: 0,
            reQcCycleCount = summary?.totalReQcCycles ?: 0,
            costVariance = summary?.totalCostVariance ?: 0.0,
            timeVarianceMinutes = summary?.totalTimeVariance ?: 0L,
            qualityEfficiencyScore = avgScore,
            generatedAt = timestamp,
            generatedBy = generatedBy
        )

        val insertResult = governanceDataSource.insertSnapshot(snapshot)
        if (insertResult is DomainResult.Success) {
            recordActivityInternal(
                projectId = projectId,
                eventType = QcGovernanceActivityType.SNAPSHOT_CREATED,
                targetId = snapshot.snapshotId,
                targetType = "QcGovernanceSnapshot",
                actorId = generatedBy,
                description = "Created governance snapshot for period ${period.startTimestamp} to ${period.endTimestamp}",
                timestamp = timestamp
            )
        }
        return insertResult
    }

    // ==========================================
    // 8. Audit Trail & Internal Helper
    // ==========================================

    override fun observeActivityEvents(projectId: String): Flow<List<QcGovernanceActivityEvent>> {
        return governanceDataSource.observeActivityEvents().map { list ->
            list.filter { it.projectId == projectId }
        }
    }

    private suspend fun recordActivityInternal(
        projectId: String,
        eventType: QcGovernanceActivityType,
        targetId: String,
        targetType: String,
        actorId: String,
        actorName: String? = null,
        description: String,
        timestamp: String
    ) {
        val event = QcGovernanceActivityEvent(
            eventId = UUID.randomUUID().toString(),
            projectId = projectId,
            eventType = eventType,
            targetId = targetId,
            targetType = targetType,
            actorId = actorId,
            actorName = actorName,
            description = description,
            timestamp = timestamp
        )
        governanceDataSource.recordActivity(event)
    }
}
