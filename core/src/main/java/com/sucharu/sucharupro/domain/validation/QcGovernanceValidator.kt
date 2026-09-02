package com.sucharu.sucharupro.domain.validation

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.qc.governance.QcAlertSeverity
import com.sucharu.sucharupro.domain.model.qc.governance.QcAlertStatus
import com.sucharu.sucharupro.domain.model.qc.governance.QcGovernanceKpi
import com.sucharu.sucharupro.domain.model.qc.governance.QcGovernancePolicy
import com.sucharu.sucharupro.domain.model.qc.governance.QcImprovementAction
import com.sucharu.sucharupro.domain.model.qc.governance.QcImprovementActionStatus
import com.sucharu.sucharupro.domain.model.qc.governance.QcImprovementEffectiveness
import com.sucharu.sucharupro.domain.model.qc.governance.QcKpiTarget
import com.sucharu.sucharupro.domain.model.qc.governance.QcKpiThreshold
import com.sucharu.sucharupro.domain.model.qc.governance.QcQualityReview
import com.sucharu.sucharupro.domain.model.qc.governance.QcQualityReviewStatus
import com.sucharu.sucharupro.domain.model.qc.governance.QcThresholdSeverity
import com.sucharu.sucharupro.domain.model.qc.governance.QcThresholdStatus
import com.sucharu.sucharupro.domain.model.user.UserRole

/**
 * Authoritative domain validator for QC Governance, KPI Thresholds & Continuous Improvement (Module 06 Step 10).
 */
object QcGovernanceValidator {

    val AUTHORIZED_GOVERNANCE_CONFIG_ROLES = setOf(UserRole.ADMIN, UserRole.MANAGER)
    val AUTHORIZED_REVIEW_ROLES = setOf(UserRole.ADMIN, UserRole.MANAGER)
    val AUTHORIZED_ALERT_ACK_ROLES = setOf(UserRole.ADMIN, UserRole.MANAGER, UserRole.QC_INSPECTOR)
    val AUTHORIZED_ACTION_PROPOSE_ROLES = setOf(UserRole.ADMIN, UserRole.MANAGER, UserRole.QC_INSPECTOR)
    val AUTHORIZED_ACTION_APPROVE_ROLES = setOf(UserRole.ADMIN, UserRole.MANAGER)
    val AUTHORIZED_ACTION_VERIFY_ROLES = setOf(UserRole.ADMIN, UserRole.MANAGER)

    // ==========================================
    // 1. RBAC Validation
    // ==========================================

    fun validateConfigPermission(callerRole: UserRole?): DomainResult<Unit> {
        if (callerRole != null && callerRole !in AUTHORIZED_GOVERNANCE_CONFIG_ROLES) {
            return DomainResult.Error(
                message = "User with role '${callerRole.defaultLabel}' is not authorized to manage QC governance configuration."
            )
        }
        return DomainResult.Success(Unit)
    }

    fun validateReviewPermission(callerRole: UserRole?): DomainResult<Unit> {
        if (callerRole != null && callerRole !in AUTHORIZED_REVIEW_ROLES) {
            return DomainResult.Error(
                message = "User with role '${callerRole.defaultLabel}' is not authorized to conduct or complete quality reviews."
            )
        }
        return DomainResult.Success(Unit)
    }

    fun validateAlertAckPermission(callerRole: UserRole?): DomainResult<Unit> {
        if (callerRole != null && callerRole !in AUTHORIZED_ALERT_ACK_ROLES) {
            return DomainResult.Error(
                message = "User with role '${callerRole.defaultLabel}' is not authorized to acknowledge quality alerts."
            )
        }
        return DomainResult.Success(Unit)
    }

    fun validateActionProposePermission(callerRole: UserRole?): DomainResult<Unit> {
        if (callerRole != null && callerRole !in AUTHORIZED_ACTION_PROPOSE_ROLES) {
            return DomainResult.Error(
                message = "User with role '${callerRole.defaultLabel}' is not authorized to propose quality improvement actions."
            )
        }
        return DomainResult.Success(Unit)
    }

    fun validateActionApprovePermission(callerRole: UserRole?): DomainResult<Unit> {
        if (callerRole != null && callerRole !in AUTHORIZED_ACTION_APPROVE_ROLES) {
            return DomainResult.Error(
                message = "User with role '${callerRole.defaultLabel}' is not authorized to approve quality improvement actions."
            )
        }
        return DomainResult.Success(Unit)
    }

    fun validateActionVerifyPermission(callerRole: UserRole?): DomainResult<Unit> {
        if (callerRole != null && callerRole !in AUTHORIZED_ACTION_VERIFY_ROLES) {
            return DomainResult.Error(
                message = "User with role '${callerRole.defaultLabel}' is not authorized to verify quality improvement effectiveness."
            )
        }
        return DomainResult.Success(Unit)
    }

    // ==========================================
    // 2. KPI Target & Policy Validation
    // ==========================================

    fun validateKpiTarget(target: QcKpiTarget): DomainResult<Unit> {
        if (target.targetId.isBlank()) {
            return DomainResult.Error(message = "Target ID cannot be blank.")
        }
        if (target.projectId.isBlank()) {
            return DomainResult.Error(message = "Project ID cannot be blank.")
        }
        if (target.configuredBy.isBlank()) {
            return DomainResult.Error(message = "ConfiguredBy user ID cannot be blank.")
        }
        if (target.targetValue < 0.0) {
            return DomainResult.Error(message = "Target value cannot be negative.")
        }
        if (target.kpiType.unit == "%" && target.targetValue > 100.0) {
            return DomainResult.Error(message = "Percentage KPI target cannot exceed 100%.")
        }
        if (target.minimumAcceptableValue != null && target.minimumAcceptableValue < 0.0) {
            return DomainResult.Error(message = "Minimum acceptable value cannot be negative.")
        }
        if (target.minimumAcceptableValue != null && target.maximumAcceptableValue != null) {
            if (target.minimumAcceptableValue > target.maximumAcceptableValue) {
                return DomainResult.Error(message = "Minimum acceptable value cannot exceed maximum acceptable value.")
            }
        }
        if (!target.effectiveTo.isNullOrBlank() && target.effectiveTo < target.effectiveFrom) {
            return DomainResult.Error(message = "EffectiveTo date cannot precede effectiveFrom date.")
        }
        return DomainResult.Success(Unit)
    }

    fun validatePolicy(policy: QcGovernancePolicy): DomainResult<Unit> {
        if (policy.policyId.isBlank()) {
            return DomainResult.Error(message = "Policy ID cannot be blank.")
        }
        if (policy.projectId.isBlank()) {
            return DomainResult.Error(message = "Project ID cannot be blank.")
        }
        if (policy.name.isBlank()) {
            return DomainResult.Error(message = "Policy name cannot be blank.")
        }
        if (policy.maxOverdueActionDays < 1) {
            return DomainResult.Error(message = "Max overdue action days must be at least 1.")
        }
        if (policy.reviewCycleDays < 1) {
            return DomainResult.Error(message = "Review cycle days must be at least 1.")
        }
        if (!policy.effectiveTo.isNullOrBlank() && policy.effectiveTo < policy.effectiveFrom) {
            return DomainResult.Error(message = "Policy effectiveTo date cannot precede effectiveFrom date.")
        }
        return DomainResult.Success(Unit)
    }

    // ==========================================
    // 3. Deterministic Threshold Evaluation Engine
    // ==========================================

    fun evaluateThreshold(
        kpi: QcGovernanceKpi,
        actualValue: Double,
        target: QcKpiTarget?
    ): QcKpiThreshold {
        val targetVal = target?.targetValue ?: defaultTargetForKpi(kpi)
        val minVal = target?.minimumAcceptableValue
        val maxVal = target?.maximumAcceptableValue

        return if (kpi.isHigherBetter) {
            when {
                actualValue >= targetVal -> {
                    QcKpiThreshold(
                        kpiType = kpi,
                        currentValue = actualValue,
                        targetValue = targetVal,
                        minimumAcceptableValue = minVal,
                        maximumAcceptableValue = maxVal,
                        status = QcThresholdStatus.WITHIN_TARGET,
                        severity = QcThresholdSeverity.INFO,
                        message = "${kpi.defaultLabel} ($actualValue ${kpi.unit}) meets or exceeds target ($targetVal ${kpi.unit})."
                    )
                }
                minVal != null && actualValue < minVal -> {
                    QcKpiThreshold(
                        kpiType = kpi,
                        currentValue = actualValue,
                        targetValue = targetVal,
                        minimumAcceptableValue = minVal,
                        maximumAcceptableValue = maxVal,
                        status = QcThresholdStatus.CRITICAL_BREACH,
                        severity = QcThresholdSeverity.CRITICAL,
                        message = "${kpi.defaultLabel} ($actualValue ${kpi.unit}) is below minimum acceptable threshold ($minVal ${kpi.unit})."
                    )
                }
                else -> {
                    QcKpiThreshold(
                        kpiType = kpi,
                        currentValue = actualValue,
                        targetValue = targetVal,
                        minimumAcceptableValue = minVal,
                        maximumAcceptableValue = maxVal,
                        status = QcThresholdStatus.WARNING,
                        severity = QcThresholdSeverity.WARNING,
                        message = "${kpi.defaultLabel} ($actualValue ${kpi.unit}) is below target ($targetVal ${kpi.unit})."
                    )
                }
            }
        } else {
            when {
                actualValue <= targetVal -> {
                    QcKpiThreshold(
                        kpiType = kpi,
                        currentValue = actualValue,
                        targetValue = targetVal,
                        minimumAcceptableValue = minVal,
                        maximumAcceptableValue = maxVal,
                        status = QcThresholdStatus.WITHIN_TARGET,
                        severity = QcThresholdSeverity.INFO,
                        message = "${kpi.defaultLabel} ($actualValue ${kpi.unit}) is within target threshold ($targetVal ${kpi.unit})."
                    )
                }
                maxVal != null && actualValue > maxVal -> {
                    QcKpiThreshold(
                        kpiType = kpi,
                        currentValue = actualValue,
                        targetValue = targetVal,
                        minimumAcceptableValue = minVal,
                        maximumAcceptableValue = maxVal,
                        status = QcThresholdStatus.CRITICAL_BREACH,
                        severity = QcThresholdSeverity.CRITICAL,
                        message = "${kpi.defaultLabel} ($actualValue ${kpi.unit}) exceeds maximum acceptable threshold ($maxVal ${kpi.unit})."
                    )
                }
                else -> {
                    QcKpiThreshold(
                        kpiType = kpi,
                        currentValue = actualValue,
                        targetValue = targetVal,
                        minimumAcceptableValue = minVal,
                        maximumAcceptableValue = maxVal,
                        status = QcThresholdStatus.WARNING,
                        severity = QcThresholdSeverity.WARNING,
                        message = "${kpi.defaultLabel} ($actualValue ${kpi.unit}) exceeds target ($targetVal ${kpi.unit})."
                    )
                }
            }
        }
    }

    private fun defaultTargetForKpi(kpi: QcGovernanceKpi): Double {
        return when (kpi) {
            QcGovernanceKpi.FIRST_PASS_RATE -> 95.0
            QcGovernanceKpi.DEFECT_RATE -> 5.0
            QcGovernanceKpi.REWORK_RATE -> 5.0
            QcGovernanceKpi.RE_QC_RATE -> 5.0
            QcGovernanceKpi.FINAL_QC_PASS_RATE -> 98.0
            QcGovernanceKpi.CRITICAL_DEFECT_RATE -> 0.0
            QcGovernanceKpi.MAJOR_DEFECT_RATE -> 2.0
            QcGovernanceKpi.RECURRING_DEFECT_RATE -> 1.0
            QcGovernanceKpi.OPEN_DEFECT_RATE -> 2.0
            QcGovernanceKpi.DEFECT_CLOSURE_RATE -> 95.0
            QcGovernanceKpi.REWORK_COMPLETION_RATE -> 95.0
            QcGovernanceKpi.REWORK_TURNAROUND_TIME -> 60.0
            QcGovernanceKpi.RE_QC_FAILURE_RATE -> 2.0
            QcGovernanceKpi.QC_COST_VARIANCE -> 50.0
            QcGovernanceKpi.QC_TIME_VARIANCE -> 30.0
            QcGovernanceKpi.QUALITY_EFFICIENCY_SCORE -> 90.0
        }
    }

    // ==========================================
    // 4. Quality Alert Lifecycle Validation
    // ==========================================

    fun validateAlertTransition(
        currentStatus: QcAlertStatus,
        nextStatus: QcAlertStatus
    ): DomainResult<Unit> {
        if (currentStatus.isTerminal) {
            return DomainResult.Error(
                message = "Cannot transition alert in terminal status '${currentStatus.defaultLabel}'."
            )
        }
        val allowed = when (currentStatus) {
            QcAlertStatus.DETECTED -> setOf(
                QcAlertStatus.ACKNOWLEDGED,
                QcAlertStatus.DISMISSED
            )
            QcAlertStatus.ACKNOWLEDGED -> setOf(
                QcAlertStatus.INVESTIGATING,
                QcAlertStatus.ACTION_REQUIRED,
                QcAlertStatus.RESOLVED,
                QcAlertStatus.DISMISSED
            )
            QcAlertStatus.INVESTIGATING -> setOf(
                QcAlertStatus.ACTION_REQUIRED,
                QcAlertStatus.RESOLVED,
                QcAlertStatus.DISMISSED
            )
            QcAlertStatus.ACTION_REQUIRED -> setOf(
                QcAlertStatus.RESOLVED,
                QcAlertStatus.DISMISSED
            )
            QcAlertStatus.RESOLVED,
            QcAlertStatus.DISMISSED -> emptySet()
        }
        if (nextStatus !in allowed) {
            return DomainResult.Error(
                message = "Invalid alert transition from '${currentStatus.defaultLabel}' to '${nextStatus.defaultLabel}'."
            )
        }
        return DomainResult.Success(Unit)
    }

    // ==========================================
    // 5. Quality Review Lifecycle Validation
    // ==========================================

    fun validateReviewTransition(
        currentStatus: QcQualityReviewStatus,
        nextStatus: QcQualityReviewStatus
    ): DomainResult<Unit> {
        if (currentStatus.isTerminal) {
            return DomainResult.Error(
                message = "Cannot transition quality review in terminal status '${currentStatus.defaultLabel}'."
            )
        }
        val allowed = when (currentStatus) {
            QcQualityReviewStatus.DRAFT -> setOf(
                QcQualityReviewStatus.SCHEDULED,
                QcQualityReviewStatus.IN_REVIEW,
                QcQualityReviewStatus.CANCELLED
            )
            QcQualityReviewStatus.SCHEDULED -> setOf(
                QcQualityReviewStatus.IN_REVIEW,
                QcQualityReviewStatus.CANCELLED
            )
            QcQualityReviewStatus.IN_REVIEW -> setOf(
                QcQualityReviewStatus.COMPLETED,
                QcQualityReviewStatus.CANCELLED
            )
            QcQualityReviewStatus.COMPLETED,
            QcQualityReviewStatus.CANCELLED -> emptySet()
        }
        if (nextStatus !in allowed) {
            return DomainResult.Error(
                message = "Invalid quality review transition from '${currentStatus.defaultLabel}' to '${nextStatus.defaultLabel}'."
            )
        }
        return DomainResult.Success(Unit)
    }

    // ==========================================
    // 6. Improvement Action Lifecycle Validation
    // ==========================================

    fun validateActionTransition(
        currentStatus: QcImprovementActionStatus,
        nextStatus: QcImprovementActionStatus
    ): DomainResult<Unit> {
        if (currentStatus.isTerminal) {
            return DomainResult.Error(
                message = "Cannot transition improvement action in terminal status '${currentStatus.defaultLabel}'."
            )
        }
        val allowed = when (currentStatus) {
            QcImprovementActionStatus.PROPOSED -> setOf(
                QcImprovementActionStatus.APPROVED,
                QcImprovementActionStatus.REJECTED,
                QcImprovementActionStatus.CANCELLED
            )
            QcImprovementActionStatus.APPROVED -> setOf(
                QcImprovementActionStatus.ASSIGNED,
                QcImprovementActionStatus.IN_PROGRESS,
                QcImprovementActionStatus.CANCELLED
            )
            QcImprovementActionStatus.ASSIGNED -> setOf(
                QcImprovementActionStatus.IN_PROGRESS,
                QcImprovementActionStatus.CANCELLED
            )
            QcImprovementActionStatus.IN_PROGRESS -> setOf(
                QcImprovementActionStatus.COMPLETED,
                QcImprovementActionStatus.CANCELLED
            )
            QcImprovementActionStatus.COMPLETED -> setOf(
                QcImprovementActionStatus.VERIFIED,
                QcImprovementActionStatus.IN_PROGRESS,
                QcImprovementActionStatus.CANCELLED
            )
            QcImprovementActionStatus.VERIFIED,
            QcImprovementActionStatus.REJECTED,
            QcImprovementActionStatus.CANCELLED -> emptySet()
        }
        if (nextStatus !in allowed) {
            return DomainResult.Error(
                message = "Invalid improvement action transition from '${currentStatus.defaultLabel}' to '${nextStatus.defaultLabel}'."
            )
        }
        return DomainResult.Success(Unit)
    }

    // ==========================================
    // 7. Separation of Duties Validation
    // ==========================================

    fun validateSeparationOfDuties(
        proposedBy: String,
        approverId: String,
        actionOwnerId: String? = null,
        verifierId: String? = null
    ): DomainResult<Unit> {
        if (proposedBy.isNotBlank() && approverId.isNotBlank() && proposedBy == approverId) {
            return DomainResult.Error(
                message = "Separation of duties violation: Action proposer '$proposedBy' cannot self-approve improvement action."
            )
        }
        if (verifierId != null && actionOwnerId != null && verifierId.isNotBlank() && verifierId == actionOwnerId) {
            return DomainResult.Error(
                message = "Separation of duties violation: Action assignee '$actionOwnerId' cannot self-verify improvement action effectiveness."
            )
        }
        return DomainResult.Success(Unit)
    }

    // ==========================================
    // 8. Deterministic Effectiveness Evaluation
    // ==========================================

    fun evaluateEffectiveness(
        kpi: QcGovernanceKpi,
        baselineValue: Double,
        postValue: Double,
        targetValue: Double
    ): QcImprovementEffectiveness {
        return if (kpi.isHigherBetter) {
            val baselineDelta = targetValue - baselineValue
            val actualImprovement = postValue - baselineValue
            when {
                postValue >= targetValue && actualImprovement > baselineDelta -> QcImprovementEffectiveness.HIGHLY_EFFECTIVE
                postValue >= targetValue -> QcImprovementEffectiveness.EFFECTIVE
                postValue > baselineValue -> QcImprovementEffectiveness.PARTIALLY_EFFECTIVE
                else -> QcImprovementEffectiveness.INEFFECTIVE
            }
        } else {
            val baselineDelta = baselineValue - targetValue
            val actualReduction = baselineValue - postValue
            when {
                postValue <= targetValue && actualReduction > baselineDelta -> QcImprovementEffectiveness.HIGHLY_EFFECTIVE
                postValue <= targetValue -> QcImprovementEffectiveness.EFFECTIVE
                postValue < baselineValue -> QcImprovementEffectiveness.PARTIALLY_EFFECTIVE
                else -> QcImprovementEffectiveness.INEFFECTIVE
            }
        }
    }
}
