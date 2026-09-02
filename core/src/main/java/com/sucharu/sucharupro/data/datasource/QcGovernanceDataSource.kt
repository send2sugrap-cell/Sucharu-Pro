package com.sucharu.sucharupro.data.datasource

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.qc.governance.QcEscalationRule
import com.sucharu.sucharupro.domain.model.qc.governance.QcGovernanceActivityEvent
import com.sucharu.sucharupro.domain.model.qc.governance.QcGovernancePolicy
import com.sucharu.sucharupro.domain.model.qc.governance.QcGovernanceSnapshot
import com.sucharu.sucharupro.domain.model.qc.governance.QcImprovementAction
import com.sucharu.sucharupro.domain.model.qc.governance.QcKpiTarget
import com.sucharu.sucharupro.domain.model.qc.governance.QcQualityAlert
import com.sucharu.sucharupro.domain.model.qc.governance.QcQualityReview
import kotlinx.coroutines.flow.Flow

/**
 * Reactive data source contract for QC Governance, KPI Targets & Continuous Improvement (Module 06 Step 10).
 */
interface QcGovernanceDataSource {

    // KPI Targets
    fun observeTargets(): Flow<List<QcKpiTarget>>
    suspend fun insertTarget(target: QcKpiTarget): DomainResult<QcKpiTarget>
    suspend fun updateTarget(target: QcKpiTarget): DomainResult<QcKpiTarget>

    // Policies
    fun observePolicies(): Flow<List<QcGovernancePolicy>>
    suspend fun insertPolicy(policy: QcGovernancePolicy): DomainResult<QcGovernancePolicy>
    suspend fun updatePolicy(policy: QcGovernancePolicy): DomainResult<QcGovernancePolicy>

    // Quality Alerts
    fun observeAlerts(): Flow<List<QcQualityAlert>>
    suspend fun insertAlert(alert: QcQualityAlert): DomainResult<QcQualityAlert>
    suspend fun updateAlert(alert: QcQualityAlert): DomainResult<QcQualityAlert>

    // Escalation Rules
    fun observeEscalationRules(): Flow<List<QcEscalationRule>>
    suspend fun insertEscalationRule(rule: QcEscalationRule): DomainResult<QcEscalationRule>
    suspend fun updateEscalationRule(rule: QcEscalationRule): DomainResult<QcEscalationRule>

    // Quality Reviews
    fun observeReviews(): Flow<List<QcQualityReview>>
    suspend fun insertReview(review: QcQualityReview): DomainResult<QcQualityReview>
    suspend fun updateReview(review: QcQualityReview): DomainResult<QcQualityReview>

    // Improvement Actions
    fun observeImprovementActions(): Flow<List<QcImprovementAction>>
    suspend fun insertImprovementAction(action: QcImprovementAction): DomainResult<QcImprovementAction>
    suspend fun updateImprovementAction(action: QcImprovementAction): DomainResult<QcImprovementAction>

    // Governance Snapshots
    fun observeSnapshots(): Flow<List<QcGovernanceSnapshot>>
    suspend fun insertSnapshot(snapshot: QcGovernanceSnapshot): DomainResult<QcGovernanceSnapshot>

    // Audit Events
    fun observeActivityEvents(): Flow<List<QcGovernanceActivityEvent>>
    suspend fun recordActivity(event: QcGovernanceActivityEvent): DomainResult<QcGovernanceActivityEvent>
}
