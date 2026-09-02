package com.sucharu.sucharupro.domain.repository

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.qc.analytics.QcAnalyticsPeriod
import com.sucharu.sucharupro.domain.model.qc.governance.QcEscalationLevel
import com.sucharu.sucharupro.domain.model.qc.governance.QcEscalationRule
import com.sucharu.sucharupro.domain.model.qc.governance.QcGovernanceActivityEvent
import com.sucharu.sucharupro.domain.model.qc.governance.QcGovernancePolicy
import com.sucharu.sucharupro.domain.model.qc.governance.QcGovernanceSnapshot
import com.sucharu.sucharupro.domain.model.qc.governance.QcImprovementAction
import com.sucharu.sucharupro.domain.model.qc.governance.QcImprovementEffectiveness
import com.sucharu.sucharupro.domain.model.qc.governance.QcKpiTarget
import com.sucharu.sucharupro.domain.model.qc.governance.QcKpiThreshold
import com.sucharu.sucharupro.domain.model.qc.governance.QcQualityAlert
import com.sucharu.sucharupro.domain.model.qc.governance.QcQualityReview
import com.sucharu.sucharupro.domain.model.user.UserRole
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface contract for QC Governance, KPI Thresholds & Continuous Quality Improvement (Module 06 Step 10).
 */
interface QcGovernanceRepository {

    // ==========================================
    // 1. KPI Target & Policy Management
    // ==========================================

    fun observeTargets(projectId: String): Flow<List<QcKpiTarget>>

    suspend fun getTargets(projectId: String): DomainResult<List<QcKpiTarget>>

    suspend fun setTarget(
        target: QcKpiTarget,
        callerRole: UserRole? = null
    ): DomainResult<QcKpiTarget>

    suspend fun deactivateTarget(
        targetId: String,
        callerRole: UserRole? = null
    ): DomainResult<QcKpiTarget>

    fun observePolicies(projectId: String): Flow<List<QcGovernancePolicy>>

    suspend fun setPolicy(
        policy: QcGovernancePolicy,
        callerRole: UserRole? = null
    ): DomainResult<QcGovernancePolicy>

    // ==========================================
    // 2. Threshold Evaluation
    // ==========================================

    suspend fun evaluateProjectKpis(
        period: QcAnalyticsPeriod,
        projectId: String,
        callerRole: UserRole? = null
    ): DomainResult<List<QcKpiThreshold>>

    // ==========================================
    // 3. Quality Alert Lifecycle
    // ==========================================

    fun observeAlerts(projectId: String): Flow<List<QcQualityAlert>>

    suspend fun getAlerts(
        projectId: String,
        callerRole: UserRole? = null
    ): DomainResult<List<QcQualityAlert>>

    suspend fun getAlertById(alertId: String): DomainResult<QcQualityAlert>

    suspend fun createAlert(
        alert: QcQualityAlert,
        callerRole: UserRole? = null
    ): DomainResult<QcQualityAlert>

    suspend fun acknowledgeAlert(
        alertId: String,
        acknowledgedBy: String,
        notes: String? = null,
        timestamp: String,
        callerRole: UserRole? = null
    ): DomainResult<QcQualityAlert>

    suspend fun escalateAlert(
        alertId: String,
        targetLevel: QcEscalationLevel,
        escalatedBy: String,
        notes: String? = null,
        timestamp: String,
        callerRole: UserRole? = null
    ): DomainResult<QcQualityAlert>

    suspend fun resolveAlert(
        alertId: String,
        resolvedBy: String,
        notes: String? = null,
        timestamp: String,
        callerRole: UserRole? = null
    ): DomainResult<QcQualityAlert>

    suspend fun dismissAlert(
        alertId: String,
        dismissedBy: String,
        reason: String,
        timestamp: String,
        callerRole: UserRole? = null
    ): DomainResult<QcQualityAlert>

    // ==========================================
    // 4. Escalation Rules
    // ==========================================

    fun observeEscalationRules(projectId: String): Flow<List<QcEscalationRule>>

    suspend fun setEscalationRule(
        rule: QcEscalationRule,
        callerRole: UserRole? = null
    ): DomainResult<QcEscalationRule>

    // ==========================================
    // 5. Management Quality Reviews
    // ==========================================

    fun observeReviews(projectId: String): Flow<List<QcQualityReview>>

    suspend fun getReviews(
        projectId: String,
        callerRole: UserRole? = null
    ): DomainResult<List<QcQualityReview>>

    suspend fun createReview(
        review: QcQualityReview,
        callerRole: UserRole? = null
    ): DomainResult<QcQualityReview>

    suspend fun startReview(
        reviewId: String,
        reviewerId: String,
        timestamp: String,
        callerRole: UserRole? = null
    ): DomainResult<QcQualityReview>

    suspend fun completeReview(
        reviewId: String,
        reviewerId: String,
        recommendations: String?,
        reviewNotes: String?,
        timestamp: String,
        callerRole: UserRole? = null
    ): DomainResult<QcQualityReview>

    suspend fun cancelReview(
        reviewId: String,
        reason: String,
        timestamp: String,
        callerRole: UserRole? = null
    ): DomainResult<QcQualityReview>

    // ==========================================
    // 6. Continuous Improvement Actions (CAPA)
    // ==========================================

    fun observeImprovementActions(projectId: String): Flow<List<QcImprovementAction>>

    suspend fun getImprovementActions(
        projectId: String,
        callerRole: UserRole? = null
    ): DomainResult<List<QcImprovementAction>>

    suspend fun proposeImprovementAction(
        action: QcImprovementAction,
        callerRole: UserRole? = null
    ): DomainResult<QcImprovementAction>

    suspend fun approveImprovementAction(
        actionId: String,
        approvedBy: String,
        approvedByName: String? = null,
        timestamp: String,
        callerRole: UserRole? = null
    ): DomainResult<QcImprovementAction>

    suspend fun assignImprovementAction(
        actionId: String,
        ownerId: String,
        ownerName: String?,
        timestamp: String,
        callerRole: UserRole? = null
    ): DomainResult<QcImprovementAction>

    suspend fun startImprovementAction(
        actionId: String,
        timestamp: String,
        callerRole: UserRole? = null
    ): DomainResult<QcImprovementAction>

    suspend fun completeImprovementAction(
        actionId: String,
        completionNotes: String,
        timestamp: String,
        postKpiValue: Double? = null,
        callerRole: UserRole? = null
    ): DomainResult<QcImprovementAction>

    suspend fun verifyImprovementAction(
        actionId: String,
        verifiedBy: String,
        verifiedByName: String? = null,
        effectiveness: QcImprovementEffectiveness,
        verificationNotes: String,
        timestamp: String,
        callerRole: UserRole? = null
    ): DomainResult<QcImprovementAction>

    suspend fun rejectImprovementAction(
        actionId: String,
        reason: String,
        timestamp: String,
        callerRole: UserRole? = null
    ): DomainResult<QcImprovementAction>

    // ==========================================
    // 7. Governance Snapshots
    // ==========================================

    fun observeSnapshots(projectId: String): Flow<List<QcGovernanceSnapshot>>

    suspend fun createSnapshot(
        period: QcAnalyticsPeriod,
        projectId: String,
        generatedBy: String,
        timestamp: String,
        callerRole: UserRole? = null
    ): DomainResult<QcGovernanceSnapshot>

    // ==========================================
    // 8. Audit Trail
    // ==========================================

    fun observeActivityEvents(projectId: String): Flow<List<QcGovernanceActivityEvent>>
}
