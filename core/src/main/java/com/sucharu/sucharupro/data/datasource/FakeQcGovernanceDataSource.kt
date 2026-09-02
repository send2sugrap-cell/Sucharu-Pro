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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Thread-safe in-memory fake implementation of [QcGovernanceDataSource].
 */
class FakeQcGovernanceDataSource : QcGovernanceDataSource {

    private val mutex = Mutex()

    private val targetsFlow = MutableStateFlow<List<QcKpiTarget>>(emptyList())
    private val policiesFlow = MutableStateFlow<List<QcGovernancePolicy>>(emptyList())
    private val alertsFlow = MutableStateFlow<List<QcQualityAlert>>(emptyList())
    private val rulesFlow = MutableStateFlow<List<QcEscalationRule>>(emptyList())
    private val reviewsFlow = MutableStateFlow<List<QcQualityReview>>(emptyList())
    private val actionsFlow = MutableStateFlow<List<QcImprovementAction>>(emptyList())
    private val snapshotsFlow = MutableStateFlow<List<QcGovernanceSnapshot>>(emptyList())
    private val eventsFlow = MutableStateFlow<List<QcGovernanceActivityEvent>>(emptyList())

    override fun observeTargets(): Flow<List<QcKpiTarget>> = targetsFlow.asStateFlow()

    override suspend fun insertTarget(target: QcKpiTarget): DomainResult<QcKpiTarget> = mutex.withLock {
        val current = targetsFlow.value.toMutableList()
        if (current.any { it.targetId == target.targetId }) {
            return DomainResult.Error(message = "KPI Target with ID '${target.targetId}' already exists.")
        }
        current.add(target)
        targetsFlow.value = current
        DomainResult.Success(target)
    }

    override suspend fun updateTarget(target: QcKpiTarget): DomainResult<QcKpiTarget> = mutex.withLock {
        val current = targetsFlow.value.toMutableList()
        val index = current.indexOfFirst { it.targetId == target.targetId }
        if (index == -1) {
            return DomainResult.Error(message = "KPI Target with ID '${target.targetId}' not found.")
        }
        current[index] = target
        targetsFlow.value = current
        DomainResult.Success(target)
    }

    override fun observePolicies(): Flow<List<QcGovernancePolicy>> = policiesFlow.asStateFlow()

    override suspend fun insertPolicy(policy: QcGovernancePolicy): DomainResult<QcGovernancePolicy> = mutex.withLock {
        val current = policiesFlow.value.toMutableList()
        if (current.any { it.policyId == policy.policyId }) {
            return DomainResult.Error(message = "Policy with ID '${policy.policyId}' already exists.")
        }
        current.add(policy)
        policiesFlow.value = current
        DomainResult.Success(policy)
    }

    override suspend fun updatePolicy(policy: QcGovernancePolicy): DomainResult<QcGovernancePolicy> = mutex.withLock {
        val current = policiesFlow.value.toMutableList()
        val index = current.indexOfFirst { it.policyId == policy.policyId }
        if (index == -1) {
            return DomainResult.Error(message = "Policy with ID '${policy.policyId}' not found.")
        }
        current[index] = policy
        policiesFlow.value = current
        DomainResult.Success(policy)
    }

    override fun observeAlerts(): Flow<List<QcQualityAlert>> = alertsFlow.asStateFlow()

    override suspend fun insertAlert(alert: QcQualityAlert): DomainResult<QcQualityAlert> = mutex.withLock {
        val current = alertsFlow.value.toMutableList()
        if (current.any { it.alertId == alert.alertId }) {
            return DomainResult.Error(message = "Quality Alert with ID '${alert.alertId}' already exists.")
        }
        current.add(alert)
        alertsFlow.value = current
        DomainResult.Success(alert)
    }

    override suspend fun updateAlert(alert: QcQualityAlert): DomainResult<QcQualityAlert> = mutex.withLock {
        val current = alertsFlow.value.toMutableList()
        val index = current.indexOfFirst { it.alertId == alert.alertId }
        if (index == -1) {
            return DomainResult.Error(message = "Quality Alert with ID '${alert.alertId}' not found.")
        }
        current[index] = alert
        alertsFlow.value = current
        DomainResult.Success(alert)
    }

    override fun observeEscalationRules(): Flow<List<QcEscalationRule>> = rulesFlow.asStateFlow()

    override suspend fun insertEscalationRule(rule: QcEscalationRule): DomainResult<QcEscalationRule> = mutex.withLock {
        val current = rulesFlow.value.toMutableList()
        if (current.any { it.ruleId == rule.ruleId }) {
            return DomainResult.Error(message = "Escalation Rule with ID '${rule.ruleId}' already exists.")
        }
        current.add(rule)
        rulesFlow.value = current
        DomainResult.Success(rule)
    }

    override suspend fun updateEscalationRule(rule: QcEscalationRule): DomainResult<QcEscalationRule> = mutex.withLock {
        val current = rulesFlow.value.toMutableList()
        val index = current.indexOfFirst { it.ruleId == rule.ruleId }
        if (index == -1) {
            return DomainResult.Error(message = "Escalation Rule with ID '${rule.ruleId}' not found.")
        }
        current[index] = rule
        rulesFlow.value = current
        DomainResult.Success(rule)
    }

    override fun observeReviews(): Flow<List<QcQualityReview>> = reviewsFlow.asStateFlow()

    override suspend fun insertReview(review: QcQualityReview): DomainResult<QcQualityReview> = mutex.withLock {
        val current = reviewsFlow.value.toMutableList()
        if (current.any { it.reviewId == review.reviewId }) {
            return DomainResult.Error(message = "Quality Review with ID '${review.reviewId}' already exists.")
        }
        current.add(review)
        reviewsFlow.value = current
        DomainResult.Success(review)
    }

    override suspend fun updateReview(review: QcQualityReview): DomainResult<QcQualityReview> = mutex.withLock {
        val current = reviewsFlow.value.toMutableList()
        val index = current.indexOfFirst { it.reviewId == review.reviewId }
        if (index == -1) {
            return DomainResult.Error(message = "Quality Review with ID '${review.reviewId}' not found.")
        }
        current[index] = review
        reviewsFlow.value = current
        DomainResult.Success(review)
    }

    override fun observeImprovementActions(): Flow<List<QcImprovementAction>> = actionsFlow.asStateFlow()

    override suspend fun insertImprovementAction(action: QcImprovementAction): DomainResult<QcImprovementAction> = mutex.withLock {
        val current = actionsFlow.value.toMutableList()
        if (current.any { it.actionId == action.actionId }) {
            return DomainResult.Error(message = "Improvement Action with ID '${action.actionId}' already exists.")
        }
        current.add(action)
        actionsFlow.value = current
        DomainResult.Success(action)
    }

    override suspend fun updateImprovementAction(action: QcImprovementAction): DomainResult<QcImprovementAction> = mutex.withLock {
        val current = actionsFlow.value.toMutableList()
        val index = current.indexOfFirst { it.actionId == action.actionId }
        if (index == -1) {
            return DomainResult.Error(message = "Improvement Action with ID '${action.actionId}' not found.")
        }
        current[index] = action
        actionsFlow.value = current
        DomainResult.Success(action)
    }

    override fun observeSnapshots(): Flow<List<QcGovernanceSnapshot>> = snapshotsFlow.asStateFlow()

    override suspend fun insertSnapshot(snapshot: QcGovernanceSnapshot): DomainResult<QcGovernanceSnapshot> = mutex.withLock {
        val current = snapshotsFlow.value.toMutableList()
        if (current.any { it.snapshotId == snapshot.snapshotId }) {
            return DomainResult.Error(message = "Governance Snapshot with ID '${snapshot.snapshotId}' already exists.")
        }
        current.add(snapshot)
        snapshotsFlow.value = current
        DomainResult.Success(snapshot)
    }

    override fun observeActivityEvents(): Flow<List<QcGovernanceActivityEvent>> = eventsFlow.asStateFlow()

    override suspend fun recordActivity(event: QcGovernanceActivityEvent): DomainResult<QcGovernanceActivityEvent> = mutex.withLock {
        val current = eventsFlow.value.toMutableList()
        current.add(event)
        eventsFlow.value = current
        DomainResult.Success(event)
    }
}
